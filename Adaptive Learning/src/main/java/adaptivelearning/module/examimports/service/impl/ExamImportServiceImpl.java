package adaptivelearning.module.examimports.service.impl;

import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.examimports.dto.ExamImportQuestion;
import adaptivelearning.module.examimports.dto.ExamImportResponse;
import adaptivelearning.module.examimports.entity.ExamImport;
import adaptivelearning.module.examimports.entity.ExamImportStatus;
import adaptivelearning.module.examimports.repository.ExamImportRepository;
import adaptivelearning.module.examimports.service.ExamImportService;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.rag.service.PdfTextExtractor;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.shared.enums.ContentStatus;
import adaptivelearning.shared.enums.QuestionDifficulty;
import adaptivelearning.utils.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamImportServiceImpl implements ExamImportService {
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final int MAX_PROMPT_CHARS = 120_000;
    private static final int MAX_QUESTIONS = 100;
    private static final Set<String> QUESTION_TYPES = Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_BLANK", "ESSAY");

    private final ExamImportRepository imports;
    private final QuestionRepository questions;
    private final CertificateRepository certificates;
    private final TopicRepository topics;
    private final AiProvider aiProvider;
    private final ObjectMapper mapper;

    @Value("${gemini.model:${ai.model:gemini-2.5-flash}}")
    private String model;

    @Override
    public ExamImportResponse create(Long userId, String title, Long certificateId, Long topicId,
                                     MultipartFile examFile, MultipartFile answerFile) {
        if (title == null || title.isBlank() || title.trim().length() > 200 || certificateId == null
                || topicId == null || !certificates.existsById(certificateId)) {
            throw new IllegalArgumentException("Hãy nhập tiêu đề, kỳ thi và môn/kỹ năng hợp lệ.");
        }
        topics.findById(topicId)
                .filter(topic -> topic.isActive() && certificateId.equals(topic.getCertificateId()))
                .orElseThrow(() -> new IllegalArgumentException("Môn/kỹ năng không thuộc kỳ thi đã chọn."));
        String examText = extract(examFile, "đề thi");
        String answerText = answerFile == null || answerFile.isEmpty() ? null : extract(answerFile, "đáp án");
        if (examText.length() + (answerText == null ? 0 : answerText.length()) > MAX_PROMPT_CHARS) {
            throw new IllegalArgumentException("Tổng nội dung PDF vượt giới hạn phân tích 120.000 ký tự.");
        }

        String system = "Extract the complete existing exam as structured questions. Preserve the source wording and choices; "
                + "do not invent, omit, combine, or rewrite questions. If an answer key is supplied, match its answers. "
                + "If no key is supplied, solve each question and mark every answer as AI-inferred in explanation. "
                + "Return only a JSON array. Each object: questionType (MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, ESSAY), "
                + "difficulty (EASY, MEDIUM, HARD), content, options [{label,text}], correctAnswer, explanation, points. "
                + "For MCQ correctAnswer must be an option label. Use [] for options on non-MCQ questions. "
                + "Do not claim certainty when a question is ambiguous; state uncertainty in explanation.";
        String prompt = "Exam title: " + title.trim() + "\nExam source text:\n" + examText
                + (answerText == null ? "\n\nNo answer-key file was provided." : "\n\nAnswer-key source text:\n" + answerText);
        List<ExamImportQuestion> parsed;
        try {
            String raw = aiProvider.chatCompletionSync(system, prompt, model, 16000).trim();
            if (raw.startsWith("```")) raw = raw.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
            parsed = mapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể phân tích đề bằng AI. Hãy thử PDF có text rõ hơn.", exception);
        }
        if (parsed.isEmpty() || parsed.size() > MAX_QUESTIONS) {
            throw new IllegalArgumentException("AI không nhận diện được câu hỏi hoặc số câu vượt giới hạn 100.");
        }
        parsed.forEach(question -> validateQuestion(question, false));

        ExamImport examImport = new ExamImport();
        examImport.setTitle(title.trim());
        examImport.setCertificateId(certificateId);
        examImport.setTopicId(topicId);
        examImport.setSourceContent(examText);
        examImport.setAnswerContent(answerText);
        examImport.setAnswerGenerated(answerText == null);
        examImport.setQuestionsJson(write(parsed));
        examImport.setUploadedBy(userId);
        return toResponse(imports.save(examImport), parsed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamImportResponse> pending() {
        return imports.findByStatusOrderByCreatedAtDesc(ExamImportStatus.PENDING_REVIEW)
                .stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamImportResponse get(Long importId) {
        ExamImport item = findPending(importId);
        return toResponse(item, read(item.getQuestionsJson()));
    }

    @Override
    public ExamImportResponse updateQuestions(Long importId, List<ExamImportQuestion> reviewed) {
        ExamImport item = findPending(importId);
        List<ExamImportQuestion> existing = read(item.getQuestionsJson());
        if (reviewed == null || reviewed.size() != existing.size() || reviewed.size() > MAX_QUESTIONS) {
            throw new IllegalArgumentException("Cần giữ nguyên số câu đã trích xuất trong bộ đề.");
        }
        reviewed.forEach(question -> validateQuestion(question, false));
        item.setQuestionsJson(write(reviewed));
        return toResponse(imports.save(item), reviewed);
    }

    @Override
    public ExamImportResponse confirm(Long importId) {
        ExamImport item = findPending(importId);
        List<ExamImportQuestion> finalQuestions = read(item.getQuestionsJson());
        finalQuestions.forEach(question -> validateQuestion(question, true));
        List<Question> approved = IntStream.range(0, finalQuestions.size()).mapToObj(index -> {
            ExamImportQuestion source = finalQuestions.get(index);
            String metadata = write(java.util.Map.of("examImportId", item.getImportId(), "questionNumber", index + 1));
            return Question.builder()
                    .certificateId(item.getCertificateId()).topicId(item.getTopicId())
                    .questionType(source.getQuestionType().toUpperCase(Locale.ROOT))
                    .difficulty(QuestionDifficulty.valueOf(source.getDifficulty().toUpperCase(Locale.ROOT)))
                    .content(source.getContent().trim()).contentJson(metadata)
                    .optionsJson(write(source.getOptions() == null ? List.of() : source.getOptions()))
                    .correctAnswer(write(source.getCorrectAnswer().trim()))
                    .explanation(source.getExplanation()).points(source.getPoints() == null ? 1.0 : source.getPoints())
                    .estimatedTimeSeconds(60).status(ContentStatus.APPROVED).version(1)
                    .createdBy(SecurityUtils.getCurrentUsername()).tags("CONFIRMED_EXAM_IMPORT")
                    .build();
        }).toList();
        questions.saveAll(approved);
        item.setStatus(ExamImportStatus.CONFIRMED);
        return toResponse(imports.save(item), finalQuestions);
    }

    private ExamImport findPending(Long id) {
        return imports.findById(id).filter(item -> item.getStatus() == ExamImportStatus.PENDING_REVIEW)
                .orElseThrow(() -> new IllegalArgumentException("Bộ đề không tồn tại hoặc đã được xác nhận."));
    }

    private String extract(MultipartFile file, String label) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("PDF " + label + " bắt buộc và không được vượt 20 MB.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("Chỉ nhận file PDF cho " + label + ".");
        }
        try {
            return PdfTextExtractor.extract(file.getBytes());
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Không đọc được PDF " + label + ": " + exception.getMessage());
        }
    }

    private void validateQuestion(ExamImportQuestion question, boolean requireAnswer) {
        if (question == null || question.getQuestionType() == null
                || !QUESTION_TYPES.contains(question.getQuestionType().trim().toUpperCase(Locale.ROOT))
                || question.getDifficulty() == null || question.getContent() == null || question.getContent().isBlank()) {
            throw new IllegalArgumentException("Câu hỏi thiếu nội dung, loại hoặc độ khó.");
        }
        boolean hasAnswer = question.getCorrectAnswer() != null && !question.getCorrectAnswer().isBlank();
        if (requireAnswer && !hasAnswer) throw new IllegalArgumentException("Mỗi câu cần có đáp án trước khi xác nhận.");
        QuestionDifficulty.valueOf(question.getDifficulty().trim().toUpperCase(Locale.ROOT));
        if ("MULTIPLE_CHOICE".equalsIgnoreCase(question.getQuestionType())) {
            if (question.getOptions() == null || question.getOptions().size() < 2) {
                throw new IllegalArgumentException("Câu trắc nghiệm cần có ít nhất hai lựa chọn.");
            }
            Set<String> labels = new java.util.HashSet<>();
            for (var option : question.getOptions()) {
                if (option == null || option.getLabel() == null || option.getLabel().isBlank()
                        || option.getText() == null || option.getText().isBlank()
                        || !labels.add(option.getLabel().trim().toUpperCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Lựa chọn trắc nghiệm cần nhãn và nội dung không trùng nhau.");
                }
            }
            if (hasAnswer && question.getOptions().stream().noneMatch(option ->
                    question.getCorrectAnswer().equalsIgnoreCase(option.getLabel()))) {
                throw new IllegalArgumentException("Đáp án đúng phải trùng nhãn của một lựa chọn.");
            }
        } else if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            throw new IllegalArgumentException("Chỉ câu trắc nghiệm mới có lựa chọn.");
        }
        if (question.getPoints() != null && (question.getPoints() <= 0 || question.getPoints() > 100)) {
            throw new IllegalArgumentException("Điểm câu hỏi phải trong khoảng 0 đến 100.");
        }
    }

    private List<ExamImportQuestion> read(String json) {
        try { return mapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception exception) { throw new IllegalStateException("Dữ liệu bộ đề không hợp lệ.", exception); }
    }

    private String write(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("Không thể lưu dữ liệu bộ đề.", exception); }
    }

    private ExamImportResponse toResponse(ExamImport item, List<ExamImportQuestion> questions) {
        return ExamImportResponse.builder().importId(item.getImportId()).title(item.getTitle())
                .certificateId(item.getCertificateId()).topicId(item.getTopicId())
                .answerGenerated(item.isAnswerGenerated()).status(item.getStatus()).questionCount(questions.size())
                .questions(questions).sourceContent(item.getSourceContent()).answerContent(item.getAnswerContent())
                .createdAt(item.getCreatedAt()).build();
    }

    private ExamImportResponse toSummary(ExamImport item) {
        return ExamImportResponse.builder().importId(item.getImportId()).title(item.getTitle())
                .certificateId(item.getCertificateId()).topicId(item.getTopicId())
                .answerGenerated(item.isAnswerGenerated()).status(item.getStatus())
                .questionCount(read(item.getQuestionsJson()).size()).createdAt(item.getCreatedAt()).build();
    }
}
