package adaptivelearning.module.adaptive.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.adaptive.dto.response.AdaptiveRecommendationResponse;
import adaptivelearning.module.adaptive.dto.response.LearnerProfileResponse;
import adaptivelearning.module.adaptive.dto.response.ScoreForecastResponse;
import adaptivelearning.module.adaptive.dto.request.BktEstimateRequest;
import adaptivelearning.module.adaptive.dto.request.BktInteraction;
import adaptivelearning.module.adaptive.entity.LearnerProfile;
import adaptivelearning.module.adaptive.repository.LearnerProfileRepository;
import adaptivelearning.module.adaptive.service.AdaptiveService;
import adaptivelearning.module.adaptive.service.BktClient;
import adaptivelearning.module.attempts.entity.AttemptAnswer;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.AttemptAnswerRepository;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.entity.ExamQuestion;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.shared.enums.AttemptStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdaptiveServiceImpl implements AdaptiveService {

    private final LearnerProfileRepository profileRepository;
    private final ExamAttemptRepository attemptRepository;
    private final AttemptAnswerRepository answerRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final BktClient bktClient;
    private final ObjectMapper objectMapper;

    @Override
    public LearnerProfileResponse getOrCreateProfile(Long userId, Long certificateId) {
        LearnerProfile profile = profileRepository.findByUserIdAndCertificateId(userId, certificateId)
                .orElseGet(() -> profileRepository.save(LearnerProfile.builder()
                        .userId(userId)
                        .certificateId(certificateId)
                        .abilityBySkillJson("{}")
                        .lastUpdated(LocalDateTime.now())
                        .totalAttempts(0)
                        .build()));
        return toResponse(profile);
    }

    @Override
    public LearnerProfileResponse updateProfile(Long userId, Long attemptId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .filter(a -> userId.equals(a.getUserId()))
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (attempt.getSubmittedAt() == null || attempt.getScore() == null
                || !EnumSet.of(AttemptStatus.SUBMITTED, AttemptStatus.GRADED, AttemptStatus.VIOLATED)
                        .contains(attempt.getStatus())) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }

        Long certificateId = resolveCertificateId(attempt);

        LearnerProfile profile = profileRepository.findByUserIdAndCertificateId(userId, certificateId)
                .orElseGet(() -> LearnerProfile.builder()
                        .userId(userId)
                        .certificateId(certificateId)
                        .abilityBySkillJson("{}")
                        .totalAttempts(0)
                        .build());

        Map<String, Double> abilityMap = parseAbilityJson(profile.getAbilityBySkillJson());

        updateHeuristicFromAttempt(attempt, abilityMap);
        bktClient.estimate(buildBktRequest(userId, certificateId)).ifPresent(response -> {
            if (response.isReady() && response.getMasteryBySkill() != null) {
                response.getMasteryBySkill().forEach((skill, mastery) -> {
                    if (mastery != null && Double.isFinite(mastery)) {
                        abilityMap.put(skill, Math.max(0.0, Math.min(1.0, mastery)));
                    }
                });
                log.info("Updated BKT mastery for user {} using {} training learners and {} response events",
                        userId, response.getTrainingStudentCount(), response.getTrainingEventCount());
            } else {
                log.info("BKT not calibrated for user {} yet: {}", userId, response.getMessage());
            }
        });

        profile.setAbilityBySkillJson(writeAbilityJson(abilityMap));
        profile.setTotalAttempts(profile.getTotalAttempts() + 1);
        profile.setLastUpdated(LocalDateTime.now());

        LearnerProfile saved = profileRepository.save(profile);
        log.info("Updated learner profile {} for user {} (attempt {})", saved.getProfileId(), userId, attemptId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdaptiveRecommendationResponse> recommend(Long userId, Long certificateId) {
        LearnerProfile profile = profileRepository.findByUserIdAndCertificateId(userId, certificateId)
                .orElse(null);

        if (profile == null) {
            return List.of(new AdaptiveRecommendationResponse(
                    "GENERAL", "MEDIUM", 0.5, "No learner history yet"));
        }

        Map<String, Double> abilityMap = parseAbilityJson(profile.getAbilityBySkillJson());
        if (abilityMap.isEmpty()) {
            return List.of(new AdaptiveRecommendationResponse(
                    "GENERAL", "MEDIUM", 0.5, "No skill history yet"));
        }

        // Sort by lowest ability first, recommend target difficulty based on weakness
        return abilityMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(3)
                .map(entry -> {
                    double ability = entry.getValue();
                    String target;
                    if (ability < 0.4) {
                        target = "EASY";
                    } else if (ability < 0.7) {
                        target = "MEDIUM";
                    } else {
                        target = "HARD";
                    }
                    double confidence = 1.0 - Math.abs(0.5 - ability);
                    return new AdaptiveRecommendationResponse(
                            entry.getKey(), target, confidence,
                            "Based on lowest observed ability (" + String.format("%.2f", ability) + ")");
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreForecastResponse forecast(Long userId, Long certificateId) {
        Optional<LearnerProfile> profileOpt =
                profileRepository.findByUserIdAndCertificateId(userId, certificateId);

        if (profileOpt.isEmpty()) {
            return new ScoreForecastResponse(
                    certificateId, 0.0, 0.2,
                    "Uncalibrated heuristic estimate: no learner profile exists for this certificate");
        }

        LearnerProfile profile = profileOpt.get();
        Map<String, Double> abilityMap = parseAbilityJson(profile.getAbilityBySkillJson());
        if (abilityMap.isEmpty()) {
            return new ScoreForecastResponse(certificateId, 0.0, 0.2,
                    "Uncalibrated heuristic estimate: no valid skill history exists for this certificate");
        }

        // Simple heuristic: average ability scaled to 0-990 range
        double avgAbility = abilityMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.5);

        double forecastScore = Math.round(avgAbility * 9900.0) / 100.0;
        double confidence = Math.min(1.0, Math.max(0.2, profile.getTotalAttempts() / 10.0));

        return new ScoreForecastResponse(
                certificateId, forecastScore, confidence,
                "Uncalibrated heuristic estimate from " + abilityMap.size() + " skill abilities across "
                        + profile.getTotalAttempts() + " attempts; score and confidence are not empirically calibrated");
    }

    // ---- helpers ----

    private void updateHeuristicFromAttempt(ExamAttempt attempt, Map<String, Double> abilityMap) {
        Map<Long, Question> questions = new HashMap<>();
        List<ExamQuestion> examQuestions = examQuestionRepository.findByExamId(attempt.getExamId());
        questionRepository.findAllById(examQuestions.stream()
                        .map(ExamQuestion::getQuestionId).toList())
                .forEach(question -> questions.put(question.getQuestionId(), question));

        Map<String, List<Boolean>> correctnessByTopic = new LinkedHashMap<>();
        for (AttemptAnswer answer : answerRepository.findByAttemptId(attempt.getAttemptId())) {
            Question question = questions.get(answer.getQuestionId());
            if (question == null || answer.getIsCorrect() == null
                    || "ESSAY".equalsIgnoreCase(question.getQuestionType())) {
                continue;
            }
            correctnessByTopic.computeIfAbsent("TOPIC_" + question.getTopicId(), key -> new ArrayList<>())
                    .add(answer.getIsCorrect());
        }

        correctnessByTopic.forEach((topic, results) -> {
            double attemptAccuracy = results.stream().filter(Boolean::booleanValue).count()
                    / (double) results.size();
            abilityMap.merge(topic, attemptAccuracy, (oldValue, newValue) ->
                    (oldValue + newValue) / 2.0);
        });
    }

    private BktEstimateRequest buildBktRequest(Long learnerId, Long certificateId) {
        List<ExamAttempt> candidateAttempts = attemptRepository.findByStatusIn(
                EnumSet.of(AttemptStatus.SUBMITTED, AttemptStatus.GRADED));
        Map<Long, Exam> examsById = examRepository.findAllById(candidateAttempts.stream()
                        .map(ExamAttempt::getExamId).distinct().toList())
                .stream()
                .filter(exam -> certificateId.equals(exam.getCertificateId()))
                .collect(Collectors.toMap(Exam::getExamId, exam -> exam));
        List<ExamAttempt> completedAttempts = candidateAttempts.stream()
                .filter(attempt -> attempt.getSubmittedAt() != null && attempt.getScore() != null
                        && examsById.containsKey(attempt.getExamId()))
                .toList();
        if (completedAttempts.isEmpty()) {
            return BktEstimateRequest.builder()
                    .learnerId(String.valueOf(learnerId))
                    .trainingInteractions(List.of())
                    .learnerInteractions(List.of())
                    .build();
        }

        Map<Long, ExamAttempt> attemptsById = new HashMap<>();
        completedAttempts.forEach(attempt -> attemptsById.put(attempt.getAttemptId(), attempt));
        Map<Long, Map<Long, Integer>> questionOrdersByExam = new HashMap<>();
        completedAttempts.stream().map(ExamAttempt::getExamId).distinct().forEach(examId -> {
            Map<Long, Integer> orders = new HashMap<>();
            examQuestionRepository.findByExamId(examId).forEach(examQuestion ->
                    orders.put(examQuestion.getQuestionId(), examQuestion.getQuestionOrder()));
            questionOrdersByExam.put(examId, orders);
        });
        List<AttemptAnswer> answers = answerRepository.findByAttemptIdIn(attemptsById.keySet());
        Set<Long> questionIds = answers.stream().map(AttemptAnswer::getQuestionId)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, Question> questionsById = new HashMap<>();
        questionRepository.findAllById(questionIds)
                .forEach(question -> questionsById.put(question.getQuestionId(), question));

        List<AnswerEvent> training = new ArrayList<>();
        List<AnswerEvent> learner = new ArrayList<>();
        for (AttemptAnswer answer : answers) {
            ExamAttempt owner = attemptsById.get(answer.getAttemptId());
            Question question = questionsById.get(answer.getQuestionId());
            if (owner == null || question == null || answer.getIsCorrect() == null
                    || "ESSAY".equalsIgnoreCase(question.getQuestionType())
                    || !certificateId.equals(question.getCertificateId())) {
                continue;
            }

            AnswerEvent event = new AnswerEvent(
                    owner.getSubmittedAt() == null ? owner.getStartedAt() : owner.getSubmittedAt(),
                    questionOrdersByExam.getOrDefault(owner.getExamId(), Map.of())
                            .getOrDefault(answer.getQuestionId(), answer.getOrderAnswered()),
                    BktInteraction.builder()
                            .studentId(String.valueOf(owner.getUserId()))
                            .skill("TOPIC_" + question.getTopicId())
                            .correct(answer.getIsCorrect())
                            .build());
            (learnerId.equals(owner.getUserId()) ? learner : training).add(event);
        }

        Comparator<AnswerEvent> eventOrder = Comparator
                .comparing(AnswerEvent::timestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(AnswerEvent::answerOrder);
        training.sort(eventOrder);
        learner.sort(eventOrder);

        return BktEstimateRequest.builder()
                .learnerId(String.valueOf(learnerId))
                .trainingInteractions(training.stream().map(AnswerEvent::interaction).toList())
                .learnerInteractions(learner.stream().map(AnswerEvent::interaction).toList())
                .build();
    }

    private record AnswerEvent(
            LocalDateTime timestamp, int answerOrder, BktInteraction interaction) {}

    private Long resolveCertificateId(ExamAttempt attempt) {
        return examRepository.findById(attempt.getExamId())
                .map(Exam::getCertificateId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private Map<String, Double> parseAbilityJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new HashMap<>();
            }
            Map<String, Double> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Double>>() {});
            parsed.entrySet().removeIf(entry -> entry.getValue() == null || !Double.isFinite(entry.getValue()));
            parsed.replaceAll((skill, ability) -> Math.max(0.0, Math.min(1.0, ability)));
            return parsed;
        } catch (Exception e) {
            log.warn("Failed to parse ability JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private String writeAbilityJson(Map<String, Double> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private LearnerProfileResponse toResponse(LearnerProfile p) {
        return LearnerProfileResponse.builder()
                .profileId(p.getProfileId())
                .userId(p.getUserId())
                .certificateId(p.getCertificateId())
                .abilityBySkillJson(p.getAbilityBySkillJson())
                .lastUpdated(p.getLastUpdated())
                .totalAttempts(p.getTotalAttempts())
                .build();
    }
}
