package adaptivelearning.module.speaking.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.ai.entity.Rubric;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.ai.repository.RubricRepository;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.speaking.dto.request.SpeakingEvaluationRequest;
import adaptivelearning.module.speaking.dto.request.SpeakingSubmissionRequest;
import adaptivelearning.module.speaking.dto.response.SpeakingEvaluationResponse;
import adaptivelearning.module.speaking.dto.response.SpeakingSubmissionResponse;
import adaptivelearning.module.speaking.entity.SpeakingEvaluation;
import adaptivelearning.module.speaking.entity.SpeakingSubmission;
import adaptivelearning.module.speaking.repository.SpeakingEvaluationRepository;
import adaptivelearning.module.speaking.repository.SpeakingSubmissionRepository;
import adaptivelearning.module.speaking.service.SpeakingService;
import adaptivelearning.shared.enums.SpeakingStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SpeakingServiceImpl implements SpeakingService {

    private static final String MODEL = "gpt-4o-mini";

    private final SpeakingSubmissionRepository submissions;
    private final SpeakingEvaluationRepository evaluations;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;
    private final ExamAttemptRepository attempts;
    private final ExamQuestionRepository examQuestions;
    private final ExamRepository exams;
    private final QuestionRepository questions;
    private final RubricRepository rubrics;

    @Override
    public SpeakingSubmissionResponse submit(Long userId, SpeakingSubmissionRequest request) {
        ExamAttempt attempt = attempts.findById(request.getAttemptId())
                .filter(value -> Objects.equals(value.getUserId(), userId))
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        examQuestions.findByExamIdAndQuestionId(attempt.getExamId(), request.getQuestionId())
                .orElseThrow(() -> new CommonException(ErrorCode.BAD_REQUEST));
        if (!questions.existsById(request.getQuestionId())) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }

        SpeakingSubmission submission = SpeakingSubmission.builder()
                .attemptId(request.getAttemptId())
                .userId(userId)
                .questionId(request.getQuestionId())
                .transcriptText(request.getTranscriptText().trim())
                .status(SpeakingStatus.UPLOADED)
                .build();
        return toSubmission(submissions.save(submission));
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakingSubmissionResponse getSubmission(Long userId, Long submissionId) {
        return toSubmission(owned(userId, submissionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpeakingSubmissionResponse> listByAttempt(Long userId, Long attemptId) {
        attempts.findById(attemptId).filter(attempt -> Objects.equals(attempt.getUserId(), userId))
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        return submissions.findByAttemptId(attemptId).stream()
                .filter(submission -> Objects.equals(submission.getUserId(), userId))
                .map(this::toSubmission).toList();
    }

    @Override
    public SpeakingEvaluationResponse evaluate(Long userId, Long submissionId, Long rubricId) {
        SpeakingSubmission submission = owned(userId, submissionId);
        Rubric rubric = rubricId == null ? null : findSpeakingRubric(submission, rubricId);
        String rubricText = rubric == null ? "Use general speaking criteria." : rubric.getCriteriaJson();
        String prompt = "Evaluate this provided speaking transcript only. Do not claim audio was analyzed. "
                + "Use rubric criteria: " + rubricText + " Return a JSON object with overallScore (0-10), "
                + "criteria (array), errors (array), transcript (array or object), strengths (array), "
                + "rewriteSuggestion (string), and confidence (0-1). Transcript:\n" + submission.getTranscriptText();

        final String raw;
        try {
            raw = aiProvider.chatCompletionSync("You are a speaking transcript examiner.", prompt, MODEL, 1200);
        } catch (RuntimeException exception) {
            log.warn("Speaking evaluation provider failed for submission {} with {}",
                    submissionId, exception.getClass().getSimpleName());
            throw new CommonException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        }

        JsonNode response = parseEvaluation(raw);
        SpeakingEvaluation evaluation = evaluations.findBySubmissionIdAndQuestionId(
                        submissionId, submission.getQuestionId()).stream().findFirst()
                .orElseGet(() -> SpeakingEvaluation.builder()
                        .submissionId(submissionId).questionId(submission.getQuestionId()).build());
        evaluation.setOverallScore(response.path("overallScore").doubleValue());
        evaluation.setCriteriaJson(response.path("criteria").toString());
        evaluation.setErrorsJson(response.path("errors").toString());
        evaluation.setTranscriptJson(response.path("transcript").toString());
        evaluation.setStrengthsJson(response.path("strengths").toString());
        evaluation.setRewriteSuggestion(response.path("rewriteSuggestion").asText());
        evaluation.setConfidence(response.path("confidence").doubleValue());
        evaluation.setModel(MODEL);
        evaluation.setPromptVersion("v1.0");
        submission.setStatus(SpeakingStatus.GRADED);
        submissions.save(submission);
        return toEvaluation(evaluations.save(evaluation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpeakingEvaluationResponse> listEvaluations(Long userId, Long submissionId) {
        owned(userId, submissionId);
        return evaluations.findBySubmissionId(submissionId).stream().map(this::toEvaluation).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakingEvaluationResponse getEvaluation(Long userId, Long evaluationId) {
        SpeakingEvaluation evaluation = evaluations.findById(evaluationId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        owned(userId, evaluation.getSubmissionId());
        return toEvaluation(evaluation);
    }

    private SpeakingSubmission owned(Long userId, Long submissionId) {
        return submissions.findBySubmissionIdAndUserId(submissionId, userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private Rubric findSpeakingRubric(SpeakingSubmission submission, Long rubricId) {
        Rubric rubric = rubrics.findByRubricIdAndIsActive(rubricId, true)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        ExamAttempt attempt = attempts.findById(submission.getAttemptId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        Exam exam = exams.findById(attempt.getExamId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (!Objects.equals(rubric.getCertificateId(), exam.getCertificateId())
                || !"SPEAKING".equalsIgnoreCase(rubric.getSkill())) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        return rubric;
    }

    private JsonNode parseEvaluation(String raw) {
        try {
            JsonNode response = objectMapper.readTree(raw);
            JsonNode score = response.path("overallScore");
            JsonNode confidence = response.path("confidence");
            if (!response.isObject() || !score.isNumber() || score.doubleValue() < 0 || score.doubleValue() > 10
                    || !confidence.isNumber() || confidence.doubleValue() < 0 || confidence.doubleValue() > 1
                    || !response.path("criteria").isArray() || !response.path("errors").isArray()
                    || !response.path("transcript").isContainerNode()
                    || !response.path("strengths").isArray()
                    || !response.path("rewriteSuggestion").isTextual()) {
                throw new IllegalArgumentException("Invalid speaking evaluation schema");
            }
            return response;
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.AI_PROVIDER_INVALID_RESPONSE);
        }
    }

    private SpeakingSubmissionResponse toSubmission(SpeakingSubmission submission) {
        return SpeakingSubmissionResponse.builder().submissionId(submission.getSubmissionId())
                .attemptId(submission.getAttemptId()).userId(submission.getUserId())
                .questionId(submission.getQuestionId()).audioUrl(submission.getAudioUrl())
                .transcriptText(submission.getTranscriptText()).durationSeconds(submission.getDurationSeconds())
                .status(submission.getStatus()).build();
    }

    private SpeakingEvaluationResponse toEvaluation(SpeakingEvaluation evaluation) {
        return SpeakingEvaluationResponse.builder().evaluationId(evaluation.getEvaluationId())
                .submissionId(evaluation.getSubmissionId()).questionId(evaluation.getQuestionId())
                .overallScore(evaluation.getOverallScore()).criteriaJson(evaluation.getCriteriaJson())
                .errorsJson(evaluation.getErrorsJson()).transcriptJson(evaluation.getTranscriptJson())
                .strengthsJson(evaluation.getStrengthsJson()).rewriteSuggestion(evaluation.getRewriteSuggestion())
                .confidence(evaluation.getConfidence()).model(evaluation.getModel())
                .promptVersion(evaluation.getPromptVersion()).build();
    }
}
