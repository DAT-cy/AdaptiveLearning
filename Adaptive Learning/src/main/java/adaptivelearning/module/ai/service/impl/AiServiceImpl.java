package adaptivelearning.module.ai.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.ai.dto.request.AiGradeRequest;
import adaptivelearning.module.ai.dto.request.RubricRequest;
import adaptivelearning.module.ai.dto.response.AiEvaluationResponse;
import adaptivelearning.module.ai.dto.response.AiJobResponse;
import adaptivelearning.module.ai.dto.response.RubricResponse;
import adaptivelearning.module.ai.entity.AiEvaluation;
import adaptivelearning.module.ai.entity.AiJob;
import adaptivelearning.module.ai.entity.Rubric;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.ai.repository.AiEvaluationRepository;
import adaptivelearning.module.ai.repository.AiJobRepository;
import adaptivelearning.module.ai.repository.RubricRepository;
import adaptivelearning.module.ai.service.AiService;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.shared.enums.AiJobStatus;
import adaptivelearning.shared.enums.AiJobType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String PROMPT_VERSION = "v1.0";

    private final AiEvaluationRepository evaluationRepository;
    private final AiJobRepository jobRepository;
    private final RubricRepository rubricRepository;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;
    private final ExamAttemptRepository attemptRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamRepository examRepository;

    @Override
    public AiEvaluationResponse gradeWriting(AiGradeRequest request) {
        validateGradeRequest(request);
        ExamAttempt attempt = requireQuestionInAttempt(request.getAttemptId(), request.getQuestionId());

        Rubric rubric = null;
        if (request.getRubricId() != null) {
            rubric = findRubric(request.getRubricId());
            requireRubricMatchesAttempt(rubric, attempt);
        }

        String systemPrompt = buildWritingPrompt(rubric);
        String userPrompt = "Grade the following answer and return only valid JSON:\n\n"
                + request.getUserAnswerText();

        String raw = aiProvider.chatCompletionSync(systemPrompt, userPrompt, DEFAULT_MODEL, 1500);

        AiEvaluation evaluation = evaluationRepository
                .findByAttemptIdAndQuestionId(request.getAttemptId(), request.getQuestionId())
                .orElseGet(AiEvaluation::new);

        evaluation.setAttemptId(request.getAttemptId());
        evaluation.setQuestionId(request.getQuestionId());
        evaluation.setRubricId(request.getRubricId());
        evaluation.setUserAnswerText(request.getUserAnswerText());
        evaluation.setModel(DEFAULT_MODEL);
        evaluation.setPromptVersion(PROMPT_VERSION);
        evaluation.setStatus(AiJobStatus.SUCCEEDED);
        evaluation.setRawResponseId(UUID.randomUUID().toString());

        parseEvaluationResponse(raw, evaluation);

        AiEvaluation saved = evaluationRepository.save(evaluation);
        log.info("Graded attempt {} question {}, score={}", saved.getAttemptId(), saved.getQuestionId(), saved.getOverallScore());
        return toEvaluationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvaluationResponse getEvaluation(Long evaluationId, Long userId, boolean admin) {
        AiEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        requireAttemptOwner(evaluation.getAttemptId(), userId, admin);
        return toEvaluationResponse(evaluation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvaluationResponse> getEvaluationByAttempt(Long attemptId, Long userId, boolean admin) {
        requireAttemptOwner(attemptId, userId, admin);
        return evaluationRepository.findByAttemptId(attemptId).stream()
                .map(this::toEvaluationResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RubricResponse createRubric(RubricRequest request) {
        validateRubricCriteria(request.getCriteriaJson());
        Rubric rubric = Rubric.builder()
                .certificateId(request.getCertificateId())
                .skill(request.getSkill())
                .name(request.getName())
                .description(request.getDescription())
                .criteriaJson(request.getCriteriaJson())
                .build();
        Rubric saved = rubricRepository.save(rubric);
        log.info("Created rubric {} for certificate {}", saved.getRubricId(), saved.getCertificateId());
        return toRubricResponse(saved);
    }

    @Override
    public RubricResponse updateRubric(Long rubricId, RubricRequest request) {
        validateRubricCriteria(request.getCriteriaJson());
        Rubric rubric = findRubric(rubricId);
        rubric.setCertificateId(request.getCertificateId());
        rubric.setSkill(request.getSkill());
        rubric.setName(request.getName());
        rubric.setDescription(request.getDescription());
        rubric.setCriteriaJson(request.getCriteriaJson());
        rubric.setVersion(rubric.getVersion() + 1);
        Rubric saved = rubricRepository.save(rubric);
        log.info("Updated rubric {}", saved.getRubricId());
        return toRubricResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RubricResponse> getRubricsByCertificate(Long certificateId) {
        return rubricRepository.findByCertificateIdAndIsActive(certificateId, true).stream()
                .map(this::toRubricResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AiJobResponse submitGradeJob(AiGradeRequest request, Long userId, boolean admin) {
        validateGradeRequest(request);
        ExamAttempt attempt = requireAttemptOwner(request.getAttemptId(), userId, admin);
        requireQuestionInAttempt(request.getAttemptId(), request.getQuestionId());
        if (request.getRubricId() != null) {
            requireRubricMatchesAttempt(findRubric(request.getRubricId()), attempt);
        }

        String idempotencyKey = "GRADE_WRITING:" + request.getAttemptId() + ":" + request.getQuestionId();

        AiJob job = jobRepository.findByIdempotencyKey(idempotencyKey).orElseGet(() ->
                jobRepository.save(AiJob.builder()
                        .jobType(AiJobType.GRADE_WRITING)
                        .status(AiJobStatus.PENDING)
                        .idempotencyKey(idempotencyKey)
                        .inputRefType("ATTEMPT_ANSWER")
                        .inputRefId(request.getAttemptId())
                        .model(DEFAULT_MODEL)
                        .promptVersion(PROMPT_VERSION)
                        .build()));

        evaluationRepository.findByAttemptIdAndQuestionId(request.getAttemptId(), request.getQuestionId())
                .orElseGet(() -> evaluationRepository.save(AiEvaluation.builder()
                        .attemptId(request.getAttemptId())
                        .questionId(request.getQuestionId())
                        .rubricId(request.getRubricId())
                        .userAnswerText(request.getUserAnswerText())
                        .status(AiJobStatus.PENDING)
                        .build()));

        return toJobResponse(job);
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public void processPendingJobs() {
        List<AiJobStatus> claimable = List.of(AiJobStatus.PENDING, AiJobStatus.RETRYING);
        List<AiJob> candidates = jobRepository.findByStatusInOrderByJobIdAsc(claimable);
        for (AiJob candidate : candidates) {
            if (candidate.getJobType() != AiJobType.GRADE_WRITING) continue;
            int claimed = jobRepository.claimIfStatusMatches(candidate.getJobId(), claimable,
                    AiJobStatus.PROCESSING, LocalDateTime.now());
            if (claimed == 0) continue;
            jobRepository.findById(candidate.getJobId()).ifPresent(this::processClaimedJob);
        }
    }

    @Override
    public AiJobResponse retryJob(Long jobId) {
        AiJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (job.getStatus() != AiJobStatus.FAILED) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        job.setRetryCount(0);
        job.setStatus(AiJobStatus.PENDING);
        job.setErrorMessage(null);
        job.setCompletedAt(null);
        Long questionId = questionIdFromIdempotencyKey(job.getIdempotencyKey());
        evaluationRepository.findByAttemptIdAndQuestionId(job.getInputRefId(), questionId)
                .ifPresent(evaluation -> {
                    evaluation.setStatus(AiJobStatus.PENDING);
                    evaluationRepository.save(evaluation);
                });
        AiJob saved = jobRepository.save(job);
        log.info("Retrying job {}", saved.getJobId());
        return toJobResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiJobResponse> getJobs() {
        return jobRepository.findAll().stream()
                .map(this::toJobResponse)
                .collect(Collectors.toList());
    }

    private void processClaimedJob(AiJob job) {
        try {
            Long questionId = questionIdFromIdempotencyKey(job.getIdempotencyKey());
            AiEvaluation evaluation = evaluationRepository.findByAttemptIdAndQuestionIdAndStatusIn(
                            job.getInputRefId(), questionId, List.of(AiJobStatus.PENDING, AiJobStatus.RETRYING))
                    .orElseThrow(() -> new IllegalStateException("Pending evaluation not found"));
            AiGradeRequest request = AiGradeRequest.builder()
                    .attemptId(evaluation.getAttemptId())
                    .questionId(evaluation.getQuestionId())
                    .rubricId(evaluation.getRubricId())
                    .userAnswerText(evaluation.getUserAnswerText())
                    .build();
            AiEvaluationResponse response = gradeWriting(request);
            job.setStatus(AiJobStatus.SUCCEEDED);
            job.setOutputRefType("AI_EVALUATION");
            job.setOutputRefId(response.getEvaluationId());
            job.setCompletedAt(LocalDateTime.now());
        } catch (RuntimeException ex) {
            job.setRetryCount(job.getRetryCount() + 1);
            job.setErrorMessage("AI grading failed (" + ex.getClass().getSimpleName() + ")");
            job.setStatus(job.getRetryCount() < job.getMaxRetries()
                    ? AiJobStatus.RETRYING : AiJobStatus.FAILED);
            log.warn("AI job {} failed with {}", job.getJobId(), ex.getClass().getSimpleName());
            evaluationRepository.findByAttemptIdAndQuestionId(job.getInputRefId(), questionIdFromIdempotencyKey(
                    job.getIdempotencyKey())).ifPresent(evaluation -> {
                evaluation.setStatus(job.getStatus());
                evaluationRepository.save(evaluation);
            });
        }

        jobRepository.save(job);
    }

    private Long questionIdFromIdempotencyKey(String idempotencyKey) {
        try {
            String[] parts = idempotencyKey.split(":");
            if (parts.length != 3 || !"GRADE_WRITING".equals(parts[0])) throw new IllegalArgumentException();
            return Long.valueOf(parts[2]);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid AI job reference");
        }
    }

    private String buildWritingPrompt(Rubric rubric) {
        String rubricSection = rubric != null
                ? "Use this rubric criteria: " + rubric.getCriteriaJson()
                : "Evaluate based on general writing quality criteria (grammar, vocabulary, coherence, task response).";
        return "You are an expert language examiner. Score the student's writing answer objectively. "
                + rubricSection + " "
                + "Return ONLY a valid JSON object with the following fields: "
                + "overallScore (double, 0-10), "
                + "criteria (array of {name, score, feedback}), "
                + "errors (array of {type, original, correction, explanation}), "
                + "strengths (array of strings), "
                + "rewriteSuggestion (string), "
                + "confidence (double, 0.0-1.0).";
    }

    private void parseEvaluationResponse(String raw, AiEvaluation evaluation) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode score = root.path("overallScore");
            JsonNode criteria = root.path("criteria");
            JsonNode errors = root.path("errors");
            JsonNode strengths = root.path("strengths");
            JsonNode confidence = root.path("confidence");
            if (!root.isObject() || !score.isNumber() || score.doubleValue() < 0 || score.doubleValue() > 10
                    || !criteria.isArray() || !errors.isArray() || !strengths.isArray()
                    || !confidence.isNumber() || confidence.doubleValue() < 0 || confidence.doubleValue() > 1
                    || !root.path("rewriteSuggestion").isTextual()) {
                throw new IllegalArgumentException("AI evaluation response does not match grading schema");
            }
            evaluation.setOverallScore(score.doubleValue());
            evaluation.setCriteriaJson(criteria.toString());
            evaluation.setErrorsJson(errors.toString());
            evaluation.setStrengthsJson(strengths.toString());
            evaluation.setRewriteSuggestion(root.path("rewriteSuggestion").asText());
            evaluation.setConfidence(confidence.doubleValue());
        } catch (Exception ex) {
            log.warn("AI evaluation response failed schema validation");
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private String extractField(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.toString();
    }

    private void validateGradeRequest(AiGradeRequest request) {
        if (request == null || request.getAttemptId() == null || request.getQuestionId() == null
                || request.getUserAnswerText() == null || request.getUserAnswerText().isBlank()
                || request.getUserAnswerText().length() > 20000) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private ExamAttempt requireAttemptOwner(Long attemptId, Long userId, boolean admin) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (!admin && !java.util.Objects.equals(attempt.getUserId(), userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN_ERROR);
        }
        return attempt;
    }

    private ExamAttempt requireQuestionInAttempt(Long attemptId, Long questionId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (examQuestionRepository.findByExamIdAndQuestionId(attempt.getExamId(), questionId).isEmpty()) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        return attempt;
    }

    private void requireRubricMatchesAttempt(Rubric rubric, ExamAttempt attempt) {
        Exam exam = examRepository.findById(attempt.getExamId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (!java.util.Objects.equals(rubric.getCertificateId(), exam.getCertificateId())) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateRubricCriteria(String criteriaJson) {
        try {
            JsonNode criteria = objectMapper.readTree(criteriaJson);
            if (!criteria.isArray() || criteria.isEmpty()) throw new IllegalArgumentException();
            for (JsonNode criterion : criteria) {
                if (!criterion.isObject() || !criterion.hasNonNull("name")
                        || !criterion.hasNonNull("maxScore")
                        || !criterion.path("name").isTextual()
                        || criterion.path("name").asText().isBlank()
                        || !criterion.path("maxScore").isNumber()
                        || criterion.path("maxScore").doubleValue() <= 0) {
                    throw new IllegalArgumentException();
                }
            }
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private Rubric findRubric(Long rubricId) {
        return rubricRepository.findByRubricIdAndIsActive(rubricId, true)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private AiEvaluationResponse toEvaluationResponse(AiEvaluation e) {
        return AiEvaluationResponse.builder()
                .evaluationId(e.getEvaluationId())
                .attemptId(e.getAttemptId())
                .questionId(e.getQuestionId())
                .rubricId(e.getRubricId())
                .userAnswerText(e.getUserAnswerText())
                .overallScore(e.getOverallScore())
                .criteriaJson(e.getCriteriaJson())
                .errorsJson(e.getErrorsJson())
                .strengthsJson(e.getStrengthsJson())
                .rewriteSuggestion(e.getRewriteSuggestion())
                .confidence(e.getConfidence())
                .model(e.getModel())
                .promptVersion(e.getPromptVersion())
                .rawResponseId(e.getRawResponseId())
                .status(e.getStatus())
                .build();
    }

    private AiJobResponse toJobResponse(AiJob j) {
        return AiJobResponse.builder()
                .jobId(j.getJobId())
                .jobType(j.getJobType())
                .status(j.getStatus())
                .idempotencyKey(j.getIdempotencyKey())
                .inputRefType(j.getInputRefType())
                .inputRefId(j.getInputRefId())
                .outputRefType(j.getOutputRefType())
                .outputRefId(j.getOutputRefId())
                .model(j.getModel())
                .promptVersion(j.getPromptVersion())
                .retryCount(j.getRetryCount())
                .maxRetries(j.getMaxRetries())
                .tokenInput(j.getTokenInput())
                .tokenOutput(j.getTokenOutput())
                .cost(j.getCost())
                .errorMessage(j.getErrorMessage())
                .startedAt(j.getStartedAt())
                .completedAt(j.getCompletedAt())
                .build();
    }

    private RubricResponse toRubricResponse(Rubric r) {
        return RubricResponse.builder()
                .rubricId(r.getRubricId())
                .certificateId(r.getCertificateId())
                .skill(r.getSkill())
                .name(r.getName())
                .description(r.getDescription())
                .criteriaJson(r.getCriteriaJson())
                .version(r.getVersion())
                .isActive(r.isActive())
                .build();
    }
}
