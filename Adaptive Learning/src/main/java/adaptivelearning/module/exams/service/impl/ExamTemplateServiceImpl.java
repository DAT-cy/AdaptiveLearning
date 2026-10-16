package adaptivelearning.module.exams.service.impl;

import adaptivelearning.module.exams.dto.request.ExamTemplateRequest;
import adaptivelearning.module.exams.dto.request.ExamTemplateSectionRequest;
import adaptivelearning.module.exams.dto.request.GenerateExamFromTemplateRequest;
import adaptivelearning.module.exams.dto.response.ExamResponse;
import adaptivelearning.module.exams.dto.response.ExamTemplateResponse;
import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.module.exams.entity.ExamQuestion;
import adaptivelearning.module.exams.entity.ExamTemplate;
import adaptivelearning.module.exams.entity.ExamTemplateSection;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.exams.repository.ExamTemplateRepository;
import adaptivelearning.module.exams.service.ExamTemplateService;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.shared.enums.ContentStatus;
import adaptivelearning.shared.enums.QuestionDifficulty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamTemplateServiceImpl implements ExamTemplateService {

    private static final TypeReference<List<ExamTemplateSectionRequest>> SECTION_LIST = new TypeReference<>() {};

    private final ExamTemplateRepository templates;
    private final ExamRepository exams;
    private final ExamQuestionRepository examQuestions;
    private final QuestionRepository questions;
    private final TopicRepository topics;
    private final ObjectMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ExamTemplateResponse> getActiveTemplates() {
        return templates.findByActiveTrueOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Override
    public ExamTemplateResponse createTemplate(ExamTemplateRequest request) {
        var topic = topics.findById(request.getTopicId())
                .filter(value -> request.getCertificateId().equals(value.getCertificateId()) && value.isActive())
                .orElseThrow(() -> new IllegalArgumentException("Kỹ năng/môn thi không thuộc kỳ thi đã chọn."));
        validateTargetRange(topic, request.getTargetScoreMin(), request.getTargetScoreMax());
        List<ExamTemplateSectionRequest> sections = normalizeSections(request.getSections());
        if (sections.size() > 20) throw new IllegalArgumentException("Mẫu đề tối đa 20 phần.");

        double maximumPoints = sections.stream()
                .mapToDouble(section -> section.getQuestionCount() * section.getPointsPerQuestion())
                .sum();
        if (request.getPassingScore() < 0 || request.getPassingScore() > maximumPoints) {
            throw new IllegalArgumentException("Passing score must not exceed the total points configured by the sections.");
        }

        for (ExamTemplateSectionRequest section : sections) {
            topics.findById(section.getTopicId())
                    .filter(value -> value.isActive() && request.getCertificateId().equals(value.getCertificateId()))
                    .orElseThrow(() -> new IllegalArgumentException("Every section must use an active skill/subject from the selected exam."));
        }

        ExamTemplate template = ExamTemplate.builder()
                .certificateId(request.getCertificateId())
                .topicId(topic.getTopicId())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .targetScoreMin(request.getTargetScoreMin())
                .targetScoreMax(request.getTargetScoreMax())
                .targetScoreUnit(topic.getScoreUnit())
                .passingScore(request.getPassingScore())
                .active(true)
                .build();
        List<ExamTemplateSection> sectionEntities = new ArrayList<>();
        for (int index = 0; index < sections.size(); index++) {
            ExamTemplateSectionRequest section = sections.get(index);
            sectionEntities.add(ExamTemplateSection.builder()
                    .template(template)
                    .sectionOrder(index + 1)
                    .topicId(section.getTopicId())
                    .name(section.getName())
                    .questionType(section.getQuestionType())
                    .questionCount(section.getQuestionCount())
                    .pointsPerQuestion(section.getPointsPerQuestion())
                    .build());
        }
        template.setSections(sectionEntities);
        return toResponse(templates.save(template), sections);
    }

    @Override
    public void deactivateTemplate(Long templateId) {
        ExamTemplate template = templates.findById(templateId)
                .filter(ExamTemplate::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu đề đang hoạt động."));
        template.setActive(false);
        templates.save(template);
    }

    @Override
    public ExamResponse generateExam(Long templateId, Long userId, GenerateExamFromTemplateRequest request) {
        ExamTemplate template = templates.findById(templateId)
                .filter(ExamTemplate::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Mẫu đề không tồn tại hoặc đã tắt."));
        List<ExamTemplateSectionRequest> sections = template.getSections().stream()
                .map(this::toSectionRequest).toList();
        if (request.getSectionDifficulties() == null || request.getSectionDifficulties().size() != sections.size()) {
            throw new IllegalArgumentException("Cần chọn độ khó cho từng phần của mẫu đề.");
        }
        if (request.getQuestionIdsBySection() != null
                && request.getQuestionIdsBySection().size() != sections.size()) {
            throw new IllegalArgumentException("Danh sách câu hỏi không khớp với cấu trúc bài thi.");
        }

        var topic = topics.findById(template.getTopicId())
                .filter(value -> value.isActive() && template.getCertificateId().equals(value.getCertificateId()))
                .orElseThrow(() -> new IllegalArgumentException("Template topic is inactive or belongs to another certificate."));
        validateTargetRange(topic, template.getTargetScoreMin(), template.getTargetScoreMax());

        Set<Long> selectedIds = new HashSet<>();
        List<SelectedQuestion> selected = new ArrayList<>();
        List<Map<String, Object>> snapshotSections = new ArrayList<>();
        int order = 1;
        double totalPoints = 0;

        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            ExamTemplateSectionRequest section = sections.get(sectionIndex);
            QuestionDifficulty difficulty = request.getSectionDifficulties().get(sectionIndex);
            var sectionTopic = topics.findById(section.getTopicId())
                    .filter(value -> value.isActive() && template.getCertificateId().equals(value.getCertificateId()))
                    .orElseThrow(() -> new IllegalArgumentException("A section skill/subject is inactive or belongs to another exam."));
            List<Long> requestedIds = request.getQuestionIdsBySection() == null
                    ? null
                    : request.getQuestionIdsBySection().get(sectionIndex);
            List<Question> sectionQuestions;
            if (requestedIds != null) {
                if (requestedIds.size() != section.getQuestionCount()
                        || requestedIds.contains(null)
                        || new HashSet<>(requestedIds).size() != requestedIds.size()) {
                    throw new IllegalArgumentException("Số câu được chọn không khớp với cấu hình phần thi.");
                }
                Map<Long, Question> byId = new LinkedHashMap<>();
                questions.findAllById(requestedIds).forEach(question -> byId.put(question.getQuestionId(), question));
                if (byId.size() != requestedIds.size()) {
                    throw new IllegalArgumentException("Một hoặc nhiều câu hỏi đã bị xóa.");
                }
                sectionQuestions = requestedIds.stream().map(byId::get).toList();
                for (Question question : sectionQuestions) {
                    if (!template.getCertificateId().equals(question.getCertificateId())
                            || !sectionTopic.getTopicId().equals(question.getTopicId())
                            || !section.getQuestionType().equalsIgnoreCase(question.getQuestionType())
                            || difficulty != question.getDifficulty()
                            || question.getStatus() != ContentStatus.APPROVED
                            || selectedIds.contains(question.getQuestionId())) {
                        throw new IllegalArgumentException("Câu hỏi được chọn không phù hợp với phần thi.");
                    }
                }
            } else {
                List<Question> available = new ArrayList<>(questions
                        .findByCertificateIdAndTopicIdAndQuestionTypeIgnoreCaseAndDifficultyAndStatus(
                                template.getCertificateId(), sectionTopic.getTopicId(), section.getQuestionType(),
                                difficulty, ContentStatus.APPROVED));
                available.removeIf(question -> selectedIds.contains(question.getQuestionId()));
                Collections.shuffle(available);

                if (available.size() < section.getQuestionCount()) {
                    throw new IllegalStateException(String.format(
                            "Phần %s cần %d câu độ khó %s nhưng chỉ còn %d câu đã duyệt phù hợp.",
                            section.getName(), section.getQuestionCount(), difficulty, available.size()));
                }
                sectionQuestions = available.subList(0, section.getQuestionCount());
            }

            List<Long> sectionQuestionIds = new ArrayList<>();
            for (Question question : sectionQuestions) {
                selectedIds.add(question.getQuestionId());
                sectionQuestionIds.add(question.getQuestionId());
                selected.add(new SelectedQuestion(question, section, order++));
                totalPoints += section.getPointsPerQuestion();
            }

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("name", section.getName());
            snapshot.put("questionType", section.getQuestionType());
            snapshot.put("topicId", sectionTopic.getTopicId());
            snapshot.put("questionCount", section.getQuestionCount());
            snapshot.put("difficulty", difficulty.name());
            snapshot.put("questionIds", sectionQuestionIds);
            snapshotSections.add(snapshot);
        }

        String blueprintJson = writeJson(Map.of(
                "templateId", template.getTemplateId(),
                "templateTitle", template.getTitle(),
                "sections", snapshotSections));
        String scoringJson = writeJson(Map.of(
                "type", "topic_scale",
                "maxScore", totalPoints,
                "topicId", topic.getTopicId(),
                "scoreUnit", topic.getScoreUnit(),
                "targetMin", template.getTargetScoreMin(),
                "targetMax", template.getTargetScoreMax()));
        Exam exam = exams.save(Exam.builder()
                .certificateId(template.getCertificateId())
                .ownerUserId(userId)
                .topicId(template.getTopicId())
                .targetScoreMin(template.getTargetScoreMin())
                .targetScoreMax(template.getTargetScoreMax())
                .targetScoreUnit(template.getTargetScoreUnit())
                .title(template.getTitle())
                .description(template.getDescription())
                .durationMinutes(template.getDurationMinutes())
                .passingScore(template.getPassingScore())
                .totalQuestions(selected.size())
                .totalPoints(totalPoints)
                .blueprintJson(blueprintJson)
                .scoringJson(scoringJson)
                .status(ContentStatus.PUBLISHED)
                .build());

        List<ExamQuestion> links = selected.stream().map(item -> ExamQuestion.builder()
                .examId(exam.getExamId())
                .questionId(item.question().getQuestionId())
                .questionOrder(item.order())
                .pointsOverride(item.section().getPointsPerQuestion())
                .sectionName(item.section().getName())
                .build()).toList();
        examQuestions.saveAll(links);

        return ExamResponse.builder()
                .examId(exam.getExamId())
                .certificateId(exam.getCertificateId())
                .topicId(exam.getTopicId())
                .targetScoreMin(exam.getTargetScoreMin())
                .targetScoreMax(exam.getTargetScoreMax())
                .targetScoreUnit(exam.getTargetScoreUnit())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .durationMinutes(exam.getDurationMinutes())
                .totalQuestions(exam.getTotalQuestions())
                .totalPoints(exam.getTotalPoints())
                .passingScore(exam.getPassingScore())
                .status(exam.getStatus())
                .blueprintJson(exam.getBlueprintJson())
                .scoringJson(exam.getScoringJson())
                .version(exam.getVersion())
                .questionCount(links.size())
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }

    private List<ExamTemplateSectionRequest> normalizeSections(List<ExamTemplateSectionRequest> source) {
        if (source == null || source.isEmpty()) throw new IllegalArgumentException("Mẫu đề cần ít nhất một phần.");
        List<ExamTemplateSectionRequest> sections = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (ExamTemplateSectionRequest section : source) {
            if (section.getName() == null || section.getName().isBlank()
                    || section.getQuestionType() == null || section.getQuestionType().isBlank()
                    || section.getTopicId() == null
                    || section.getQuestionCount() == null || section.getQuestionCount() < 1) {
                throw new IllegalArgumentException("Tên phần, dạng câu hỏi và số câu là bắt buộc.");
            }
            String name = section.getName().trim();
            if (!names.add(name.toLowerCase())) throw new IllegalArgumentException("Tên các phần phải khác nhau.");
            sections.add(ExamTemplateSectionRequest.builder()
                    .name(name)
                    .topicId(section.getTopicId())
                    .questionType(section.getQuestionType().trim().toUpperCase())
                    .questionCount(section.getQuestionCount())
                    .pointsPerQuestion(section.getPointsPerQuestion() > 0 ? section.getPointsPerQuestion() : 1.0)
                    .build());
        }
        return sections;
    }

    private ExamTemplateSectionRequest toSectionRequest(ExamTemplateSection section) {
        return ExamTemplateSectionRequest.builder()
                .name(section.getName())
                .topicId(section.getTopicId())
                .questionType(section.getQuestionType())
                .questionCount(section.getQuestionCount())
                .pointsPerQuestion(section.getPointsPerQuestion())
                .build();
    }

    private List<ExamTemplateSectionRequest> readSections(String json) {
        try {
            return mapper.readValue(json, SECTION_LIST);
        } catch (Exception exception) {
            throw new IllegalStateException("Cấu trúc các phần trong mẫu đề không hợp lệ.");
        }
    }

    private String writeJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể lưu cấu hình mẫu đề.");
        }
    }

    private void validateTargetRange(adaptivelearning.module.topics.entity.Topic topic, Double min, Double max) {
        if (topic.getScoreMin() == null || topic.getScoreMax() == null || topic.getScoreStep() == null
                || topic.getScoreStep() <= 0 || topic.getScoreUnit() == null || topic.getScoreUnit().isBlank()) {
            throw new IllegalStateException("Topic score scale is not configured.");
        }
        if (min == null || max == null || min < topic.getScoreMin() || max > topic.getScoreMax() || min > max
                || !isOnScale(min, topic) || !isOnScale(max, topic)) {
            throw new IllegalArgumentException(String.format(
                    "Target range must be between %s and %s %s in increments of %s.",
                    topic.getScoreMin(), topic.getScoreMax(), topic.getScoreUnit(), topic.getScoreStep()));
        }
    }

    private boolean isOnScale(double score, adaptivelearning.module.topics.entity.Topic topic) {
        double steps = (score - topic.getScoreMin()) / topic.getScoreStep();
        return Math.abs(steps - Math.rint(steps)) < 1e-7;
    }

    private ExamTemplateResponse toResponse(ExamTemplate template) {
        return toResponse(template, template.getSections().stream().map(this::toSectionRequest).toList());
    }

    private ExamTemplateResponse toResponse(ExamTemplate template, List<ExamTemplateSectionRequest> sections) {
        return ExamTemplateResponse.builder()
                .templateId(template.getTemplateId())
                .certificateId(template.getCertificateId())
                .topicId(template.getTopicId())
                .title(template.getTitle())
                .description(template.getDescription())
                .durationMinutes(template.getDurationMinutes())
                .targetScoreMin(template.getTargetScoreMin())
                .targetScoreMax(template.getTargetScoreMax())
                .targetScoreUnit(template.getTargetScoreUnit())
                .passingScore(template.getPassingScore())
                .sections(sections)
                .active(template.isActive())
                .createdAt(template.getCreatedAt())
                .build();
    }

    private record SelectedQuestion(Question question, ExamTemplateSectionRequest section, int order) {}
}
