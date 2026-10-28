package adaptivelearning.module.attempts.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.billing.service.EntitlementService;
import adaptivelearning.module.attempts.dto.request.SaveAnswerRequest;
import adaptivelearning.module.attempts.dto.request.SubmitAttemptRequest;
import adaptivelearning.module.attempts.dto.request.ViolationRequest;
import adaptivelearning.module.attempts.dto.response.AttemptAnswerResponse;
import adaptivelearning.module.attempts.dto.response.AttemptDetailResponse;
import adaptivelearning.module.attempts.dto.response.AttemptResponse;
import adaptivelearning.module.attempts.dto.response.AttemptResultResponse;
import adaptivelearning.module.attempts.entity.AttemptAnswer;
import adaptivelearning.module.attempts.entity.AttemptEvent;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.AttemptAnswerRepository;
import adaptivelearning.module.attempts.repository.AttemptEventRepository;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.attempts.service.AttemptService;
import adaptivelearning.module.attempts.service.AttemptExpiredException;
import adaptivelearning.module.attempts.service.AttemptSubmissionOutcome;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.entity.ExamQuestion;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.shared.enums.AttemptStatus;
import adaptivelearning.shared.enums.ContentStatus;
import adaptivelearning.shared.enums.EventType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AttemptServiceImpl implements AttemptService {

    private final ExamAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final AttemptEventRepository eventRepository;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final EntitlementService entitlementService;

    @Override
    public AttemptResponse startAttempt(Long userId, Long examId) {
        Exam exam = examRepository.findByIdForUpdate(examId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (exam.getStatus() != ContentStatus.PUBLISHED) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        if (exam.getOwnerUserId() != null && !exam.getOwnerUserId().equals(userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN_ERROR);
        }
        entitlementService.requireTopicAccess(userId, exam.getTopicId());
        if (Objects.equals(exam.getOwnerUserId(), userId)
                && !entitlementService.hasPremiumAccess(userId)
                && attemptRepository.existsByUserIdAndExamIdAndSubmittedAtIsNotNull(userId, examId)) {
            throw new CommonException(ErrorCode.FREE_LIMIT_REACHED);
        }
        attemptRepository.findByUserIdAndExamId(userId, examId).ifPresent(existing -> {
            if (existing.getStatus() == AttemptStatus.IN_PROGRESS) {
                throw new CommonException(ErrorCode.BAD_REQUEST);
            }
        });

        LocalDateTime startedAt = LocalDateTime.now();
        ExamAttempt attempt = ExamAttempt.builder()
                .userId(userId)
                .examId(examId)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(startedAt)
                .expiresAt(startedAt.plusMinutes(exam.getDurationMinutes()))
                .maxScore(exam.getTotalPoints())
                .build();

        ExamAttempt saved = attemptRepository.save(attempt);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptDetailResponse getAttempt(Long attemptId) {
        ExamAttempt attempt = requireAttempt(attemptId);
        List<AttemptAnswerResponse> answers = answerRepository.findByAttemptId(attemptId).stream()
                .map(this::toAnswerResponse).toList();

        return AttemptDetailResponse.builder()
                .attemptId(attempt.getAttemptId())
                .userId(attempt.getUserId())
                .examId(attempt.getExamId())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .expiresAt(attempt.getExpiresAt())
                .submittedAt(attempt.getSubmittedAt())
                .score(attempt.getScore())
                .maxScore(attempt.getMaxScore())
                .bandScore(attempt.getBandScore())
                .scoreBreakdownJson(attempt.getScoreBreakdownJson())
                .violationCount(attempt.getViolationCount())
                .ipAddress(attempt.getIpAddress())
                .userAgent(attempt.getUserAgent())
                .answers(answers)
                .timeRemainingSeconds(timeRemainingSeconds(attempt))
                .build();
    }

    @Override
    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public void saveAnswer(Long attemptId, Long userId, SaveAnswerRequest request) {
        ExamAttempt attempt = requireOwnedAttemptForUpdate(attemptId, userId);
        ensureActive(attempt);
        saveAnswerForAttempt(attempt, request);
    }

    @Override
    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public AttemptResultResponse submitAttempt(Long attemptId, Long userId, SubmitAttemptRequest request) {
        return submitAttemptWithOutcome(attemptId, userId, request).result();
    }

    @Override
    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public AttemptSubmissionOutcome submitAttemptWithOutcome(Long attemptId, Long userId,
                                                              SubmitAttemptRequest request) {
        ExamAttempt attempt = requireOwnedAttemptForUpdate(attemptId, userId);
        if ((attempt.getStatus() == AttemptStatus.GRADED || attempt.getStatus() == AttemptStatus.VIOLATED)
                && attempt.getScore() != null) {
            return new AttemptSubmissionOutcome(buildResultResponse(attempt), false);
        }
        ensureActive(attempt);

        if (request.getAnswers() != null) {
            for (SaveAnswerRequest ans : request.getAnswers()) {
                saveAnswerForAttempt(attempt, ans);
            }
        }

        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.SUBMITTED);
        eventRepository.save(AttemptEvent.builder()
                .attemptId(attemptId)
                .eventType(EventType.SUBMIT)
                .build());

        return new AttemptSubmissionOutcome(scoreAttempt(attempt), true);
    }

    @Override
    @Transactional(readOnly = true)
    public AttemptResultResponse getResult(Long attemptId) {
        ExamAttempt attempt = requireAttempt(attemptId);
        if (attempt.getScore() == null) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        return buildResultResponse(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttemptResponse> getHistory(Long userId) {
        return attemptRepository.findByUserIdOrderByStartedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public void recordViolation(Long attemptId, Long userId, boolean admin, ViolationRequest request) {
        ExamAttempt attempt = requireAttemptForUpdate(attemptId);
        if (!admin && !Objects.equals(attempt.getUserId(), userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN_ERROR);
        }
        ensureActive(attempt);

        attempt.setViolationCount(attempt.getViolationCount() + 1);

        eventRepository.save(AttemptEvent.builder()
                .attemptId(attemptId)
                .eventType(request.getEventType() != null ? request.getEventType() : EventType.VIOLATION)
                .payloadJson(request.getPayloadJson())
                .build());

        if (attempt.getViolationCount() >= 5) {
            attempt.setSubmittedAt(LocalDateTime.now());
            attempt.setStatus(AttemptStatus.VIOLATED);
            scoreAttempt(attempt);
        } else {
            attemptRepository.save(attempt);
        }
    }

    @Override
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOverdueAttempts() {
        LocalDateTime now = LocalDateTime.now();
        List<ExamAttempt> overdue = attemptRepository.findOverdueForUpdate(AttemptStatus.IN_PROGRESS, now);
        for (ExamAttempt attempt : overdue) {
            attempt.setStatus(AttemptStatus.EXPIRED);
        }
        attemptRepository.saveAll(overdue);
    }

    // ---------------------------------------------------------------------
    // Scoring
    // ---------------------------------------------------------------------

    private AttemptResultResponse scoreAttempt(ExamAttempt attempt) {
        AttemptStatus terminalStatus = attempt.getStatus() == AttemptStatus.VIOLATED
                ? AttemptStatus.VIOLATED : AttemptStatus.GRADED;
        attempt.setStatus(AttemptStatus.GRADING);
        attemptRepository.save(attempt);

        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamId(attempt.getExamId());
        List<Long> questionIds = examQuestions.stream().map(ExamQuestion::getQuestionId).toList();
        Map<Long, Question> questionMap = questionIds.isEmpty()
                ? Map.of()
                : questionRepository.findAllById(questionIds).stream()
                        .collect(Collectors.toMap(Question::getQuestionId, Function.identity(), (first, last) -> first));

        List<AttemptAnswer> answers = answerRepository.findByAttemptId(attempt.getAttemptId());
        Map<Long, AttemptAnswer> answersByQuestion = answers.stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, Function.identity(), (first, last) -> last));
        double score = 0d;

        for (ExamQuestion examQuestion : examQuestions) {
            AttemptAnswer answer = answersByQuestion.get(examQuestion.getQuestionId());
            if (answer == null) continue;
            if (!hasAnswer(answer.getUserAnswerJson())) {
                answer.setIsCorrect(null);
                answer.setPointsEarned(0d);
                continue;
            }
            Question question = questionMap.get(examQuestion.getQuestionId());
            if (question == null) {
                answer.setIsCorrect(null);
                answer.setPointsEarned(0d);
                continue;
            }

            if (question.getCorrectAnswer() == null) {
                answer.setIsCorrect(null);
                answer.setPointsEarned(0d);
                continue;
            }

            Boolean correct = isCorrect(answer.getUserAnswerJson(), question.getCorrectAnswer(), question.getQuestionType());
            answer.setIsCorrect(correct);

            double points = examQuestion.getPointsOverride() != null
                    ? examQuestion.getPointsOverride() : question.getPoints();
            if (Boolean.TRUE.equals(correct)) {
                answer.setPointsEarned(points);
                score += points;
            } else {
                answer.setPointsEarned(0d);
            }
        }

        answerRepository.saveAll(answers);

        attempt.setScore(score);
        attempt.setStatus(terminalStatus);
        attemptRepository.save(attempt);

        return buildResultResponse(attempt, examQuestions, answers);
    }

    private Boolean isCorrect(String userAnswerJson, String correctAnswer, String questionType) {
        if (userAnswerJson == null || correctAnswer == null) {
            return false;
        }
        String type = questionType == null ? "" : questionType.trim().toUpperCase();
        if ("ESSAY".equals(type)) return null;
        try {
            JsonNode userNode = objectMapper.readTree(userAnswerJson);
            JsonNode correctNode;
            try {
                correctNode = objectMapper.readTree(correctAnswer);
            } catch (Exception invalidCorrectJson) {
                if ("FILL_BLANK".equals(type)) {
                    String userText = answerText(userNode);
                    return userText != null && userText.trim().equalsIgnoreCase(correctAnswer.trim());
                }
                throw invalidCorrectJson;
            }

            if ("MULTIPLE_CHOICE".equals(type) || "TRUE_FALSE".equals(type)
                    || "TRUE_FALSE_NOT_GIVEN".equals(type)) {
                if (userNode.isObject() && userNode.hasNonNull("selected")) {
                    userNode = userNode.get("selected");
                }
                return userNode.equals(correctNode);
            }
            if ("FILL_BLANK".equals(type)) {
                String userText = answerText(userNode);
                String correctText = answerText(correctNode);
                return userText != null && correctText != null
                        && userText.trim().equalsIgnoreCase(correctText.trim());
            }
            return null;
        } catch (Exception e) {
            if ("FILL_BLANK".equals(type)) {
                return userAnswerJson.trim().equalsIgnoreCase(correctAnswer.trim());
            }
            if ("MULTIPLE_CHOICE".equals(type) || "TRUE_FALSE".equals(type)
                    || "TRUE_FALSE_NOT_GIVEN".equals(type)) {
                return userAnswerJson.trim().equals(correctAnswer.trim());
            }
            return null;
        }
    }

    private String answerText(JsonNode node) {
        if (node.isTextual() || node.isNumber() || node.isBoolean()) return node.asText();
        if (node.isObject()) {
            for (String key : List.of("text", "value", "selected")) {
                if (node.hasNonNull(key) && node.get(key).isValueNode()) return node.get(key).asText();
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private ExamAttempt requireAttempt(Long id) {
        return attemptRepository.findById(id)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private ExamAttempt requireAttemptForUpdate(Long id) {
        return attemptRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private ExamAttempt requireOwnedAttempt(Long id, Long userId) {
        ExamAttempt attempt = requireAttempt(id);
        if (!Objects.equals(attempt.getUserId(), userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN_ERROR);
        }
        return attempt;
    }

    private ExamAttempt requireOwnedAttemptForUpdate(Long id, Long userId) {
        ExamAttempt attempt = requireAttemptForUpdate(id);
        if (!Objects.equals(attempt.getUserId(), userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN_ERROR);
        }
        return attempt;
    }

    private void ensureActive(ExamAttempt attempt) {
        if (attempt.getStatus() == AttemptStatus.EXPIRED) {
            throw new AttemptExpiredException();
        }
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        if (attempt.getExpiresAt() != null && attempt.getExpiresAt().isBefore(LocalDateTime.now())) {
            attempt.setStatus(AttemptStatus.EXPIRED);
            attemptRepository.save(attempt);
            throw new AttemptExpiredException();
        }
    }

    private void saveAnswerForAttempt(ExamAttempt attempt, SaveAnswerRequest request) {
        examQuestionRepository.findByExamIdAndQuestionId(attempt.getExamId(), request.getQuestionId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        AttemptAnswer answer = answerRepository.findByAttemptIdAndQuestionId(
                        attempt.getAttemptId(), request.getQuestionId())
                .orElseGet(() -> AttemptAnswer.builder()
                        .attemptId(attempt.getAttemptId())
                        .questionId(request.getQuestionId())
                        .build());
        answer.setUserAnswerJson(request.getUserAnswerJson());
        answer.setTimeSpentSeconds(Math.max(0, request.getTimeSpentSeconds()));
        answerRepository.save(answer);
        eventRepository.save(AttemptEvent.builder()
                .attemptId(attempt.getAttemptId())
                .eventType(EventType.AUTOSAVE)
                .build());
    }

    private long timeRemainingSeconds(ExamAttempt attempt) {
        if (attempt.getExpiresAt() == null || attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return 0;
        }
        return Math.max(0, Duration.between(LocalDateTime.now(), attempt.getExpiresAt()).getSeconds());
    }

    private AttemptResponse toResponse(ExamAttempt attempt) {
        return AttemptResponse.builder()
                .attemptId(attempt.getAttemptId())
                .userId(attempt.getUserId())
                .examId(attempt.getExamId())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .expiresAt(attempt.getExpiresAt())
                .submittedAt(attempt.getSubmittedAt())
                .score(attempt.getScore())
                .maxScore(attempt.getMaxScore())
                .bandScore(attempt.getBandScore())
                .scoreBreakdownJson(attempt.getScoreBreakdownJson())
                .violationCount(attempt.getViolationCount())
                .ipAddress(attempt.getIpAddress())
                .userAgent(attempt.getUserAgent())
                .build();
    }

    private AttemptAnswerResponse toAnswerResponse(AttemptAnswer answer) {
        return AttemptAnswerResponse.builder()
                .id(answer.getId())
                .questionId(answer.getQuestionId())
                .userAnswerJson(answer.getUserAnswerJson())
                .isCorrect(answer.getIsCorrect())
                .pointsEarned(answer.getPointsEarned())
                .timeSpentSeconds(answer.getTimeSpentSeconds())
                .orderAnswered(answer.getOrderAnswered())
                .build();
    }

    private AttemptResultResponse buildResultResponse(ExamAttempt attempt) {
        List<AttemptAnswer> answers = answerRepository.findByAttemptId(attempt.getAttemptId());
        List<ExamQuestion> questions = examQuestionRepository.findByExamId(attempt.getExamId());
        return buildResultResponse(attempt, questions, answers);
    }

    private AttemptResultResponse buildResultResponse(ExamAttempt attempt, List<ExamQuestion> questions,
                                                      List<AttemptAnswer> answers) {
        java.util.Set<Long> examQuestionIds = questions.stream()
                .map(ExamQuestion::getQuestionId).collect(Collectors.toSet());
        List<AttemptAnswer> examAnswers = answers.stream()
                .filter(answer -> examQuestionIds.contains(answer.getQuestionId()))
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, Function.identity(), (first, last) -> last))
                .values().stream().toList();
        List<AttemptAnswer> answered = examAnswers.stream().filter(answer -> hasAnswer(answer.getUserAnswerJson())).toList();
        int correctCount = (int) answered.stream().filter(answer -> Boolean.TRUE.equals(answer.getIsCorrect())).count();
        long wrongCount = examAnswers.stream().filter(a -> Boolean.FALSE.equals(a.getIsCorrect())).count();
        int unanswered = Math.max(0, questions.size() - answered.size());

        long timeTaken = 0;
        if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null) {
            timeTaken = Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).getSeconds();
        }

        return AttemptResultResponse.builder()
                .attemptId(attempt.getAttemptId())
                .score(attempt.getScore())
                .maxScore(attempt.getMaxScore())
                .bandScore(attempt.getBandScore())
                .totalQuestions(questions.size())
                .correctCount(correctCount)
                .wrongCount((int) wrongCount)
                .unansweredCount(Math.max(0, unanswered))
                .scoreBreakdownJson(attempt.getScoreBreakdownJson())
                .timeTakenSeconds(Math.max(0, timeTaken))
                .violationCount(attempt.getViolationCount())
                .build();
    }

    private boolean hasAnswer(String userAnswerJson) {
        if (userAnswerJson == null || userAnswerJson.isBlank()) return false;
        try {
            JsonNode node = objectMapper.readTree(userAnswerJson);
            if (node.isNull()) return false;
            String text = answerText(node);
            if (text != null) return !text.isBlank();
            if (node.isObject()) return !node.isEmpty();
            if (node.isArray()) return !node.isEmpty();
            return false;
        } catch (Exception exception) {
            return !userAnswerJson.isBlank();
        }
    }
}
