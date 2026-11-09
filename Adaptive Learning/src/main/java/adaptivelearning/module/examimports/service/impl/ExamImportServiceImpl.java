package adaptivelearning.module.examimports.service.impl;

import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.examimports.dto.ExamImportQuestion;
import adaptivelearning.module.examimports.dto.ExamImportResponse;
import adaptivelearning.module.examimports.dto.ExamImportSourceFile;
import adaptivelearning.module.examimports.dto.ExamImportResourceResponse;
import adaptivelearning.module.examimports.dto.ExamImportAnswerKeyResult;
import adaptivelearning.module.examimports.entity.ExamImport;
import adaptivelearning.module.examimports.entity.ExamImportResource;
import adaptivelearning.module.examimports.entity.ExamImportStatus;
import adaptivelearning.module.examimports.repository.ExamImportRepository;
import adaptivelearning.module.examimports.repository.ExamImportResourceRepository;
import adaptivelearning.module.examimports.service.ExamImportService;
import adaptivelearning.module.examimports.storage.FileStorage;
import adaptivelearning.module.examimports.storage.StoredFile;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.rag.service.PdfTextExtractor;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.shared.enums.ContentStatus;
import adaptivelearning.shared.enums.QuestionDifficulty;
import adaptivelearning.utils.SecurityUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExamImportServiceImpl implements ExamImportService {
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    private static final int MAX_PROMPT_CHARS = 120_000;
    private static final int MAX_CHUNK_CHARS = 10_000;
    private static final int MAX_CHUNK_OUTPUT_TOKENS = 6_000;
    private static final Set<String> QUESTION_TYPES = Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "FILL_BLANK", "ESSAY");

    private final ExamImportRepository imports;
    private final ExamImportResourceRepository importResources;
    private final QuestionRepository questions;
    private final CertificateRepository certificates;
    private final TopicRepository topics;
    private final AiProvider aiProvider;
    private final ObjectMapper mapper;
    private final FileStorage fileStorage;

    @Value("${gemini.model:${ai.model:gemini-2.5-flash}}")
    private String model;

    @Override
    public ExamImportResponse create(Long userId, String title, Long certificateId, Long topicId,
                                     MultipartFile examFile, MultipartFile answerFile) {
        return create(userId, title, certificateId, topicId, examFile, answerFile, false);
    }

    @Override
    public ExamImportResponse create(Long userId, String title, Long certificateId, Long topicId,
                                     MultipartFile examFile, MultipartFile answerFile, boolean answerInExam) {
        return createWithProgress(userId, title, certificateId, topicId, examFile, answerFile, answerInExam,
                (completed, total) -> {});
    }

    @Override
    public ExamImportResponse createWithProgress(Long userId, String title, Long certificateId, Long topicId,
                                                 MultipartFile examFile, MultipartFile answerFile,
                                                 boolean answerInExam, BiConsumer<Integer, Integer> progress) {
        if (title == null || title.isBlank() || title.trim().length() > 200 || certificateId == null
                || !certificates.existsById(certificateId)) {
            throw new IllegalArgumentException("Hãy nhập tiêu đề và kỳ thi hợp lệ.");
        }
        if (answerInExam && answerFile != null && !answerFile.isEmpty()) {
            throw new IllegalArgumentException("Đã chọn đề có đáp án; hãy bỏ file đáp án riêng hoặc tắt lựa chọn này.");
        }
        if (topicId != null) {
            topics.findById(topicId)
                    .filter(topic -> topic.isActive() && certificateId.equals(topic.getCertificateId()))
                    .orElseThrow(() -> new IllegalArgumentException("Môn/kỹ năng không thuộc kỳ thi đã chọn."));
        }
        boolean isExamScannedPdf = false;
        byte[] examFileBytes = null;
        String examText = "";
        try {
            examText = extract(examFile, "đề thi");
        } catch (IllegalArgumentException ex) {
            String fname = examFile != null ? examFile.getOriginalFilename() : "";
            if (fname != null && fname.toLowerCase(Locale.ROOT).endsWith(".pdf") && ex.getMessage().contains("không có lớp văn bản")) {
                isExamScannedPdf = true;
                try {
                    examFileBytes = examFile.getBytes();
                } catch (IOException ioException) {
                    throw new IllegalArgumentException("Không đọc được tệp đề thi: " + ioException.getMessage());
                }
            } else {
                throw ex;
            }
        }

        String answerText = answerFile == null || answerFile.isEmpty() ? null : extract(answerFile, "đáp án");
        if (!isExamScannedPdf && examText.length() + (answerText == null ? 0 : answerText.length()) > MAX_PROMPT_CHARS) {
            throw new IllegalArgumentException("Tổng nội dung vượt giới hạn phân tích 120.000 ký tự.");
        }

        List<adaptivelearning.module.topics.entity.Topic> certTopics = topics.findByCertificateId(certificateId).stream()
                .filter(adaptivelearning.module.topics.entity.Topic::isActive)
                .toList();
        String topicsHint = buildTopicHierarchyHint(certTopics);

        String system = "Extract the complete existing exam as structured questions. Preserve the source wording and choices; "
                + "do not invent, omit, combine, or rewrite questions. If an answer key is supplied, match its answers. "
                + "If no answer key is supplied, leave correctAnswer and explanation empty; never solve or infer answers. "
                + "Classify every question individually using its section heading, section instructions, passage/task context, and question range. "
                + "The allowed topics are hierarchical: first identify the major skill/section, then select the most specific matching child topic. "
                + "Use the major topic itself when no child topic clearly matches. Never mix questions across major sections and never guess or use a default topic. "
                + "Return the exact topicId and exact topic name/code in skill. Use null only when the source gives no reliable skill context. "
                + "For every question include sourceQuestionNumber exactly as printed in the source, or null if no number is shown. "
                + "Also include sourceSectionOrder, sourceSectionTitle, sourceSectionQuestionStart, sourceSectionQuestionEnd, and sectionInstructions copied from the nearest original section headings and directions; repeat section metadata for every question in that section. The start/end fields must preserve the printed range such as Questions 1-10, not just the current question number. Include stimulusContent when questions depend on a shared passage, note, chart description, or scenario; otherwise use an empty string. "
                + topicsHint
                + "Return only a JSON array. Each object: questionType (MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, ESSAY), "
                + "difficulty (EASY, MEDIUM, HARD), topicId (Long, matching one of available topics if detected, or null), "
                + "skill (string name/code of skill if detected), sourceQuestionNumber (string or null), sourcePageNumber (integer or null), "
                + "sourceSectionOrder (integer or null), sourceSectionTitle (string or null), sourceSectionQuestionStart (string or null), sourceSectionQuestionEnd (string or null), sectionInstructions (string or empty), stimulusContent (string or empty), "
                + "content, options [{label,text}], correctAnswer, explanation, points. "
                + "For MCQ correctAnswer must be an option label. Use [] for options on non-MCQ questions. "
                + "Do not claim certainty when a question is ambiguous; state uncertainty in explanation.";
        List<AnalysisChunk> chunks = isExamScannedPdf ? splitPdf(examFileBytes) : splitText(examText);
        if (chunks.isEmpty()) throw new IllegalArgumentException("Không tìm thấy nội dung đề để phân tích.");
        progress.accept(0, chunks.size());
        List<ExamImportQuestion> parsed;
        Set<String> seenScannedQuestionKeys = new HashSet<>();
        int currentChunk = 0;
        try {
            parsed = new ArrayList<>();
            for (int index = 0; index < chunks.size(); index++) {
                currentChunk = index + 1;
                AnalysisChunk chunk = chunks.get(index);
                String prompt = "Exam title: " + title.trim() + "\n"
                        + "This is chunk " + (index + 1) + " of " + chunks.size() + ". Extract only questions present in this chunk; "
                        + "preserve their original question numbers and do not invent missing questions.\n"
                        + (chunk.pdfBytes() != null
                                ? "The exam is a scanned PDF. Perform OCR. This chunk covers target pages "
                                    + chunk.firstPage() + "-" + chunk.lastPage()
                                    + "; neighboring pages may be included only as context. Emit questions that begin on target pages only; "
                                    + "do not repeat questions shown on context-only pages.\n"
                                : "Exam source text:\n[Chunk " + (index + 1) + "/" + chunks.size() + "]\n" + chunk.text())
                        + (answerInExam ? "\nThe exam document itself contains the official answer key. Find and match those answers; do not infer answers." : "")
                        + (answerText == null ? "\n\nNo answer-key file was provided. Return empty strings for correctAnswer and explanation." : "\n\nAnswer-key source text:\n" + answerText);
                String raw = chunk.pdfBytes() != null
                        ? aiProvider.analyzeDocumentSync(system, prompt, chunk.pdfBytes(), "application/pdf", MAX_CHUNK_OUTPUT_TOKENS).trim()
                        : aiProvider.chatCompletionSync(system, prompt, model, MAX_CHUNK_OUTPUT_TOKENS).trim();
                List<ExamImportQuestion> partQuestions = parseQuestions(raw);
                if (partQuestions != null) {
                    for (ExamImportQuestion question : partQuestions) {
                        if (question.getSourcePageNumber() == null && chunk.pdfBytes() != null) {
                            question.setSourcePageNumber(chunk.firstPage());
                        }
                        if (isExamScannedPdf && !seenScannedQuestionKeys.add(scannedQuestionKey(question))) continue;
                        parsed.add(question);
                    }
                }
                progress.accept(index + 1, chunks.size());
            }
        } catch (Exception exception) {
            log.error("Exam import AI analysis failed (scannedPdf={}, chunk={}/{}): {}",
                    isExamScannedPdf, currentChunk, chunks.size(), deepestMessage(exception), exception);
            throw new IllegalStateException(userFacingAiError(exception), exception);
        }
        if (parsed == null || parsed.isEmpty()) {
            throw new IllegalArgumentException("AI không nhận diện được câu hỏi trong đề.");
        }

        // Gán fallback topicId nếu AI chưa phân loại hoặc người dùng đã chọn cố định topicId
        for (ExamImportQuestion q : parsed) {
            if (topicId != null) {
                q.setTopicId(topicId);
            } else if (q.getTopicId() == null && !certTopics.isEmpty()) {
                if (q.getSkill() != null && !q.getSkill().isBlank()) {
                    certTopics.stream()
                            .filter(t -> t.getName().equalsIgnoreCase(q.getSkill().trim())
                                    || (t.getCode() != null && t.getCode().equalsIgnoreCase(q.getSkill().trim())))
                            .findFirst()
                            .ifPresent(matched -> q.setTopicId(matched.getTopicId()));
                }
            }
            if (!certTopics.isEmpty() && q.getTopicId() != null
                    && certTopics.stream().noneMatch(t -> t.getTopicId().equals(q.getTopicId()))) {
                throw new IllegalArgumentException("AI đã gán kỹ năng không thuộc kỳ thi. Hãy kiểm tra danh sách kỹ năng.");
            }
            normalizeAiQuestion(q);
            normalizeMultipleChoiceAnswer(q);
            if (answerText == null && !answerInExam) {
                q.setCorrectAnswer("");
                q.setExplanation("");
            }
            validateQuestion(q, false);
        }

        ExamImport examImport = new ExamImport();
        examImport.setTitle(title.trim());
        examImport.setCertificateId(certificateId);
        examImport.setTopicId(topicId);
        examImport.setSourceContent(examText);
        examImport.setAnswerContent(answerText);
        examImport.setAnswerGenerated(false);
        examImport.setQuestionsJson(write(parsed));
        examImport.setUploadedBy(userId);
        try {
            StoredFile storedFile = fileStorage.store(examFile.getBytes(), examFile.getOriginalFilename(),
                    examFile.getContentType());
            examImport.setSourceFileKey(storedFile.key());
            examImport.setSourceFileName(storedFile.originalName());
            examImport.setSourceFileContentType(storedFile.contentType());
            examImport.setSourceFileSize(storedFile.size());
            examImport.setSourceFileSha256(storedFile.sha256());
            return toResponse(imports.save(examImport), parsed, userId);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc lại file đề gốc.", exception);
        } catch (RuntimeException exception) {
            if (examImport.getSourceFileKey() != null) fileStorage.delete(examImport.getSourceFileKey());
            throw exception;
        }
    }

    private List<AnalysisChunk> splitText(String text) {
        List<AnalysisChunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : text.split("(?<=\\n)")) {
            String remaining = line;
            while (remaining.length() > MAX_CHUNK_CHARS) {
                int splitAt = remaining.lastIndexOf(' ', MAX_CHUNK_CHARS);
                if (splitAt <= 0) splitAt = MAX_CHUNK_CHARS;
                appendTextUnit(chunks, current, remaining.substring(0, splitAt));
                remaining = remaining.substring(splitAt);
            }
            appendTextUnit(chunks, current, remaining);
        }
        if (!current.isEmpty()) chunks.add(new AnalysisChunk(current.toString(), null, 0, 0));
        return chunks;
    }

    private void appendTextUnit(List<AnalysisChunk> chunks, StringBuilder current, String unit) {
        if (current.length() > 0 && current.length() + unit.length() > MAX_CHUNK_CHARS) {
            chunks.add(new AnalysisChunk(current.toString(), null, 0, 0));
            current.setLength(0);
        }
        current.append(unit);
    }

    private List<AnalysisChunk> splitPdf(byte[] bytes) {
        List<AnalysisChunk> chunks = new ArrayList<>();
        try (PDDocument source = Loader.loadPDF(bytes)) {
            int pageCount = source.getNumberOfPages();
            for (int start = 0; start < pageCount; start++) {
                int end = start;
                try (PDDocument part = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    part.importPage(source.getPage(start));
                    part.save(output);
                    chunks.add(new AnalysisChunk(null, output.toByteArray(), start + 1, end + 1));
                }
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể chia PDF scan thành các phần để OCR.", exception);
        }
        return chunks;
    }

    private record AnalysisChunk(String text, byte[] pdfBytes, int firstPage, int lastPage) {}

    private String scannedQuestionKey(ExamImportQuestion question) {
        String number = question.getSourceQuestionNumber() == null ? "" : question.getSourceQuestionNumber().trim();
        String topic = question.getTopicId() == null ? "" : question.getTopicId().toString();
        if (!number.isBlank()) {
            return "number:" + number.toLowerCase(Locale.ROOT) + "|topic:" + topic;
        }
        String content = normalizeAnswerText(question.getContent());
        String options = question.getOptions() == null ? "" : question.getOptions().stream()
                .map(option -> option == null ? "" : normalizeAnswerText(option.getLabel())
                        + ":" + normalizeAnswerText(option.getText()))
                .collect(java.util.stream.Collectors.joining("|"));
        return "content:" + topic + "|" + question.getQuestionType() + "|" + content + "|" + options;
    }

    private void normalizeMultipleChoiceAnswer(ExamImportQuestion question) {
        if (question == null || !"MULTIPLE_CHOICE".equalsIgnoreCase(question.getQuestionType())
                || question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()
                || question.getOptions() == null) {
            return;
        }
        String answer = question.getCorrectAnswer().trim();
        for (var option : question.getOptions()) {
            if (option == null || option.getLabel() == null || option.getLabel().isBlank()) continue;
            String label = option.getLabel().trim();
            if (answer.equalsIgnoreCase(label) || sameAnswerText(answer, option.getText())) {
                question.setCorrectAnswer(label);
                return;
            }
            String answerPrefix = answer.replaceFirst("(?i)^(?:the\\s+)?(?:correct\\s+)?answer\\s*(?:(?:is)\\s+|[:=]\\s*)", "");
            String labelPattern = "(?i)^\\s*[\\(\\[]?" + java.util.regex.Pattern.quote(label)
                    + "[\\)\\]]?(?:\\s*[.):\\-].*)?\\s*$";
            if (answerPrefix.matches(labelPattern)) {
                question.setCorrectAnswer(label);
                return;
            }
        }
        question.setCorrectAnswer("");
        String warning = "AI chưa đối chiếu chắc chắn được đáp án với nhãn lựa chọn; giáo viên cần chọn đáp án đúng.";
        String explanation = question.getExplanation();
        if (explanation == null || explanation.isBlank()) question.setExplanation(warning);
        else if (!explanation.contains(warning)) question.setExplanation(explanation.trim() + " " + warning);
    }

    private void normalizeAiQuestion(ExamImportQuestion question) {
        if (question == null || "MULTIPLE_CHOICE".equalsIgnoreCase(question.getQuestionType())
                || question.getOptions() == null || question.getOptions().isEmpty()) {
            return;
        }
        log.warn("AI returned answer options for non-MCQ question {}. Ignoring those options.",
                question.getSourceQuestionNumber() == null ? "without a source number" : question.getSourceQuestionNumber());
        question.setOptions(List.of());
    }

    private boolean sameAnswerText(String answer, String optionText) {
        if (optionText == null || optionText.isBlank()) return false;
        return normalizeAnswerText(answer).equals(normalizeAnswerText(optionText));
    }

    private String normalizeAnswerText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private String buildTopicHierarchyHint(List<adaptivelearning.module.topics.entity.Topic> certTopics) {
        if (certTopics.isEmpty()) return "No active topics are configured for this certificate.";
        StringBuilder hint = new StringBuilder("Allowed topic hierarchy for this certificate (parent is the major skill/section):\n");
        var topicById = certTopics.stream().collect(java.util.stream.Collectors.toMap(
                adaptivelearning.module.topics.entity.Topic::getTopicId, topic -> topic));
        for (var topic : certTopics) {
            hint.append("topicId=").append(topic.getTopicId())
                    .append(" path='").append(topicPath(topic, topicById, new java.util.HashSet<>()))
                    .append("' code='").append(topic.getCode()).append("'\n");
        }
        return hint.toString();
    }

    private String topicPath(adaptivelearning.module.topics.entity.Topic topic,
                             java.util.Map<Long, adaptivelearning.module.topics.entity.Topic> topicById,
                             Set<Long> visited) {
        if (!visited.add(topic.getTopicId())) return topic.getName();
        var parent = topic.getParentId() == null ? null : topicById.get(topic.getParentId());
        return parent == null ? topic.getName()
                : topicPath(parent, topicById, visited) + " / " + topic.getName();
    }

    private List<ExamImportQuestion> parseQuestions(String raw) throws JsonProcessingException {
        String candidate = raw == null ? "" : raw.trim();
        java.util.regex.Matcher fenceMatcher = java.util.regex.Pattern
                .compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
                .matcher(candidate);
        if (fenceMatcher.find()) {
            candidate = fenceMatcher.group(1).trim();
        } else if (candidate.startsWith("```")) {
            candidate = candidate.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }

        try {
            return mapper.readValue(candidate, new TypeReference<>() {});
        } catch (Exception ignored) {
        }

        try {
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(candidate);
            if (rootNode.isArray()) {
                return mapper.convertValue(rootNode, new TypeReference<List<ExamImportQuestion>>() {});
            }
            if (rootNode.isObject()) {
                for (String fieldName : List.of("questions", "items", "data", "exam", "content", "result")) {
                    com.fasterxml.jackson.databind.JsonNode arr = rootNode.get(fieldName);
                    if (arr != null && arr.isArray()) {
                        return mapper.convertValue(arr, new TypeReference<List<ExamImportQuestion>>() {});
                    }
                }
                java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = rootNode.fields();
                while (fields.hasNext()) {
                    java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
                    if (entry.getValue().isArray()) {
                        return mapper.convertValue(entry.getValue(), new TypeReference<List<ExamImportQuestion>>() {});
                    }
                }
            }
        } catch (Exception ignored) {
        }

        int start = candidate.indexOf('[');
        int end = candidate.lastIndexOf(']');
        if (start >= 0 && end > start) {
            try {
                return mapper.readValue(candidate.substring(start, end + 1), new TypeReference<>() {});
            } catch (Exception ignored) {
                String fixed = candidate.substring(start, end + 1).replaceAll(",\\s*([}\\]])", "$1");
                try {
                    return mapper.readValue(fixed, new TypeReference<>() {});
                } catch (Exception ignored2) {
                }
            }
        }

        if (start >= 0) {
            int lastCloseObj = candidate.lastIndexOf('}');
            if (lastCloseObj > start) {
                String recovered = candidate.substring(start, lastCloseObj + 1) + "]";
                String fixed = recovered.replaceAll(",\\s*([}\\]])", "$1");
                try {
                    return mapper.readValue(fixed, new TypeReference<>() {});
                } catch (Exception ignored) {
                }
            }
        }

        log.warn("Failed to parse AI question output. Raw length: {}, preview: {}",
                raw != null ? raw.length() : 0,
                raw != null ? raw.substring(0, Math.min(raw.length(), 500)) : "null");
        throw new com.fasterxml.jackson.core.JsonParseException(null, "Không thể chuyển đổi dữ liệu AI thành danh sách câu hỏi.");
    }

    private String userFacingAiError(Throwable exception) {
        String messages = exceptionMessages(exception).toLowerCase(Locale.ROOT);
        if (messages.contains("403") && messages.contains("leaked")) {
            return "Gemini từ chối API key vì key bị báo lộ. Hãy cập nhật GEMINI_API_KEY rồi tạo lại backend.";
        }
        if (messages.contains("403") || messages.contains("401")) {
            return "Gemini từ chối API key (HTTP 401/403). Hãy kiểm tra key và quyền truy cập Gemini API.";
        }
        if (messages.contains("429")) {
            return "Gemini đang giới hạn lượt gọi hoặc quota đã hết (HTTP 429). Hãy đợi rồi thử lại.";
        }
        if (messages.contains("not configured")) {
            return "Backend chưa nhận được GEMINI_API_KEY. Hãy kiểm tra cấu hình Docker Compose rồi tạo lại backend.";
        }
        if (messages.contains("400")) {
            return "Gemini từ chối dữ liệu gửi lên (HTTP 400). Hãy kiểm tra PDF không lỗi và thử file nhỏ hơn.";
        }
        if (hasCause(exception, JsonProcessingException.class)) {
            return "Gemini đã phản hồi nhưng nội dung không đúng định dạng câu hỏi. Hãy thử file ngắn hơn hoặc chia đề thành nhiều phần.";
        }
        return "Gemini không xử lý được tài liệu. Chi tiết đã được ghi vào log backend.";
    }

    private boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    private String exceptionMessages(Throwable exception) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = exception; current != null; current = current.getCause()) {
            if (current.getMessage() != null) messages.append(current.getMessage()).append(' ');
        }
        return messages.toString();
    }

    private String deepestMessage(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? exception.getClass().getSimpleName() : current.getMessage();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamImportResponse> pending() {
        return pending(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamImportResponse> pending(Long viewerId) {
        return imports.findByStatusOrderByCreatedAtDesc(ExamImportStatus.PENDING_REVIEW)
                .stream().map(item -> toSummary(item, viewerId)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamImportResponse get(Long importId) {
        return get(importId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamImportResponse get(Long importId, Long viewerId) {
        ExamImport item = findPending(importId);
        return toResponse(item, read(item.getQuestionsJson()), viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public ExamImportSourceFile getSourceFile(Long importId) {
        ExamImport item = imports.findById(importId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        if (item.getSourceFileKey() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Bản nhập cũ không lưu file đề gốc.");
        }
        return new ExamImportSourceFile(item.getSourceFileName(), item.getSourceFileContentType(),
                fileStorage.read(item.getSourceFileKey()));
    }

    @Override
    public ExamImportResourceResponse uploadSectionAudio(Long importId, String sectionKey, String sectionTitle,
                                                         MultipartFile audioFile, Long userId) {
        return uploadSectionResource(importId, sectionKey, sectionTitle, audioFile, userId,
                "AUDIO", List.of(".mp3", ".wav", ".m4a", ".ogg", ".aac"), 50L * 1024 * 1024);
    }

    @Override
    public ExamImportResourceResponse uploadSectionImage(Long importId, String sectionKey, String sectionTitle,
                                                         MultipartFile imageFile, Long userId) {
        return uploadSectionResource(importId, sectionKey, sectionTitle, imageFile, userId,
                "IMAGE", List.of(".png", ".jpg", ".jpeg", ".webp"), 10L * 1024 * 1024);
    }

    private ExamImportResourceResponse uploadSectionResource(Long importId, String sectionKey, String sectionTitle,
                                                             MultipartFile file, Long userId, String resourceType,
                                                             List<String> extensions, long maxFileSize) {
        findPending(importId);
        if (sectionKey == null || !sectionKey.matches("[A-Za-z0-9:_-]{1,64}")
                || sectionTitle == null || sectionTitle.isBlank() || sectionTitle.length() > 120) {
            throw new IllegalArgumentException("Phần thi không hợp lệ.");
        }
        if (file == null || file.isEmpty() || file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(resourceType.equals("AUDIO")
                    ? "File nghe bắt buộc và không được vượt quá 50 MB."
                    : "File hình bắt buộc và không được vượt quá 10 MB.");
        }
        String filename = file.getOriginalFilename();
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (extensions.stream().noneMatch(lowerName::endsWith)) {
            throw new IllegalArgumentException(resourceType.equals("AUDIO")
                    ? "File nghe phải là MP3, WAV, M4A, OGG hoặc AAC."
                    : "File hình phải là PNG, JPG, JPEG hoặc WEBP.");
        }

        StoredFile stored;
        try {
            stored = fileStorage.store(file.getBytes(), filename, file.getContentType());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc file tài nguyên.", exception);
        }
        try {
            ExamImportResource resource = new ExamImportResource();
            resource.setImportId(importId);
            resource.setSectionKey(sectionKey);
            resource.setSectionTitle(sectionTitle.trim());
            resource.setResourceType(resourceType);
            resource.setStorageKey(stored.key());
            resource.setFileName(stored.originalName());
            resource.setContentType(stored.contentType());
            resource.setFileSize(stored.size());
            resource.setSha256(stored.sha256());
            resource.setUploadedBy(userId);
            return toResourceResponse(importResources.save(resource));
        } catch (RuntimeException exception) {
            fileStorage.delete(stored.key());
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ExamImportSourceFile getResourceFile(Long importId, Long resourceId) {
        if (!imports.existsById(importId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND);
        }
        ExamImportResource resource = importResources.findByResourceIdAndImportId(resourceId, importId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
        return new ExamImportSourceFile(resource.getFileName(), resource.getContentType(),
                fileStorage.read(resource.getStorageKey()));
    }

    private boolean isAuthorizedToDelete(ExamImport item, Long viewerId) {
        if (item == null || item.getStatus() != ExamImportStatus.PENDING_REVIEW) {
            return false;
        }
        User user = SecurityUtils.getCurrentUser();
        if (user != null && user.getRole() == Role.ROLE_ADMIN) {
            return true;
        }
        return viewerId != null && viewerId.equals(item.getUploadedBy());
    }

    @Override
    public void deleteOwned(Long importId, Long viewerId) {
        ExamImport item = imports.findById(importId)
                .filter(imported -> imported.getStatus() == ExamImportStatus.PENDING_REVIEW)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Bộ đề không tồn tại hoặc đã được duyệt."));
        User user = SecurityUtils.getCurrentUser();
        boolean isAdmin = user != null && user.getRole() == Role.ROLE_ADMIN;
        boolean isOwner = viewerId != null && viewerId.equals(item.getUploadedBy());
        if (!isAdmin && !isOwner) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "Bộ đề không tồn tại hoặc đã được duyệt.");
        }
        List<ExamImportResource> resources = importResources.findByImportIdOrderByUploadedAtAsc(importId);
        importResources.deleteByImportId(importId);
        imports.delete(item);
        resources.forEach(resource -> fileStorage.delete(resource.getStorageKey()));
        if (item.getSourceFileKey() != null) fileStorage.delete(item.getSourceFileKey());
    }

    @Override
    public ExamImportResponse updateQuestions(Long importId, List<ExamImportQuestion> reviewed) {
        ExamImport item = findPending(importId);
        List<ExamImportQuestion> existing = read(item.getQuestionsJson());
        if (reviewed == null || reviewed.size() != existing.size()) {
            throw new IllegalArgumentException("Cần giữ nguyên số câu đã trích xuất trong bộ đề.");
        }
        List<Long> allowedTopicIds = topics.findByCertificateId(item.getCertificateId()).stream()
                .filter(adaptivelearning.module.topics.entity.Topic::isActive)
                .map(adaptivelearning.module.topics.entity.Topic::getTopicId).toList();
        reviewed.forEach(question -> {
            validateQuestion(question, false);
            validateTopic(question.getTopicId(), item.getTopicId(), allowedTopicIds, false);
        });
        item.setQuestionsJson(write(reviewed));
        return toResponse(imports.save(item), reviewed);
    }

    @Override
    public ExamImportAnswerKeyResult importAnswerKey(Long importId, MultipartFile answerFile) {
        ExamImport item = findPending(importId);
        if (answerFile == null || answerFile.isEmpty() || answerFile.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File đáp án bắt buộc và không được vượt 20 MB.");
        }
        String filename = answerFile.getOriginalFilename();
        if (filename == null || !(filename.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || filename.toLowerCase(Locale.ROOT).endsWith(".md")
                || filename.toLowerCase(Locale.ROOT).endsWith(".markdown")
                || filename.toLowerCase(Locale.ROOT).endsWith(".txt"))) {
            throw new IllegalArgumentException("File đáp án phải là PDF, Markdown hoặc TXT.");
        }

        String answerText = null;
        byte[] scannedAnswerPdf = null;
        try {
            answerText = extract(answerFile, "đáp án");
        } catch (IllegalArgumentException exception) {
            if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")
                    || !exception.getMessage().contains("không có lớp văn bản")) {
                throw exception;
            }
            try {
                scannedAnswerPdf = answerFile.getBytes();
            } catch (IOException ioException) {
                throw new IllegalArgumentException("Không thể đọc file đáp án.", ioException);
            }
        }
        if (answerText != null && answerText.length() > MAX_PROMPT_CHARS) {
            throw new IllegalArgumentException("Nội dung file đáp án vượt giới hạn 120.000 ký tự.");
        }

        List<ExamImportQuestion> questions = read(item.getQuestionsJson());
        Set<Integer> matchedIndexes = new java.util.HashSet<>();
        String answerSource = answerText == null ? "Scanned PDF answer key: " + filename : answerText;
        for (int start = 0; start < questions.size(); start += 20) {
            int end = Math.min(start + 20, questions.size());
            List<java.util.Map<String, Object>> questionSlice = new ArrayList<>();
            for (int index = start; index < end; index++) {
                ExamImportQuestion question = questions.get(index);
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("questionIndex", index + 1);
                row.put("sourceQuestionNumber", question.getSourceQuestionNumber());
                row.put("questionType", question.getQuestionType());
                row.put("content", question.getContent());
                row.put("options", question.getOptions() == null ? List.of() : question.getOptions());
                questionSlice.add(row);
            }
            String system = "Match answers from the supplied official answer key to the provided existing questions. "
                    + "Do not create questions, change question text, or infer answers not present in the key. "
                    + "Return only a JSON array with questionIndex (the exact 1-based index supplied) and correctAnswer. "
                    + "For MULTIPLE_CHOICE return an existing option label. For other question types return the answer text. "
                    + "Omit questions whose answer cannot be matched confidently.";
            String prompt = "Answer key mapping request:\nExam title: " + item.getTitle()
                    + "\nQuestions " + (start + 1) + "-" + end + ":\n"
                    + write(questionSlice) + "\nAnswer key:\n"
                    + (answerText == null ? "The answer key is attached as a scanned PDF document." : answerText);
            try {
                String raw = scannedAnswerPdf == null
                        ? aiProvider.chatCompletionSync(system, prompt, model, MAX_CHUNK_OUTPUT_TOKENS).trim()
                        : aiProvider.analyzeDocumentSync(system, prompt, scannedAnswerPdf, "application/pdf",
                                MAX_CHUNK_OUTPUT_TOKENS).trim();
                for (AnswerKeyMatch match : parseAnswerMatches(raw)) {
                    if (match.getQuestionIndex() == null || match.getQuestionIndex() < start + 1
                            || match.getQuestionIndex() > end || match.getCorrectAnswer() == null
                            || match.getCorrectAnswer().isBlank() || !matchedIndexes.add(match.getQuestionIndex())) {
                        continue;
                    }
                    ExamImportQuestion target = questions.get(match.getQuestionIndex() - 1);
                    String originalAnswer = target.getCorrectAnswer();
                    target.setCorrectAnswer(match.getCorrectAnswer().trim());
                    normalizeMultipleChoiceAnswer(target);
                    if (target.getCorrectAnswer() == null || target.getCorrectAnswer().isBlank()) {
                        target.setCorrectAnswer(originalAnswer);
                        matchedIndexes.remove(match.getQuestionIndex());
                    }
                }
            } catch (Exception exception) {
                log.error("Answer-key matching failed for exam import {} questions {}-{}: {}",
                        importId, start + 1, end, deepestMessage(exception), exception);
                throw new IllegalStateException("Không thể ghép đáp án vào câu hỏi. File đề vẫn được giữ để rà soát.", exception);
            }
        }
        if (matchedIndexes.isEmpty()) {
            throw new IllegalArgumentException("Không ghép được đáp án nào. Hãy kiểm tra file đáp án có số câu hoặc nhãn rõ ràng.");
        }

        item.setAnswerContent(answerSource);
        item.setAnswerGenerated(false);
        item.setQuestionsJson(write(questions));
        ExamImport saved = imports.save(item);
        return ExamImportAnswerKeyResult.builder().examImport(toResponse(saved, questions))
                .matchedAnswerCount(matchedIndexes.size()).totalQuestionCount(questions.size()).build();
    }

    private List<AnswerKeyMatch> parseAnswerMatches(String raw) throws JsonProcessingException {
        String candidate = raw == null ? "" : raw.trim();
        java.util.regex.Matcher fence = java.util.regex.Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```")
                .matcher(candidate);
        if (fence.find()) candidate = fence.group(1).trim();
        int start = candidate.indexOf('[');
        int end = candidate.lastIndexOf(']');
        if (start >= 0 && end > start) candidate = candidate.substring(start, end + 1);
        return mapper.readValue(candidate, new TypeReference<>() {});
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    private static class AnswerKeyMatch {
        private Integer questionIndex;
        private String correctAnswer;
    }

    @Override
    public ExamImportResponse confirm(Long importId) {
        ExamImport item = findPending(importId);
        List<ExamImportQuestion> finalQuestions = read(item.getQuestionsJson());
        List<Long> allowedTopicIds = topics.findByCertificateId(item.getCertificateId()).stream()
                .filter(adaptivelearning.module.topics.entity.Topic::isActive)
                .map(adaptivelearning.module.topics.entity.Topic::getTopicId).toList();
        finalQuestions.forEach(question -> {
            validateQuestion(question, true);
            validateTopic(question.getTopicId(), item.getTopicId(), allowedTopicIds, true);
        });
        List<Question> approved = IntStream.range(0, finalQuestions.size()).mapToObj(index -> {
            ExamImportQuestion source = finalQuestions.get(index);
            Long resolvedTopicId = source.getTopicId() != null ? source.getTopicId() : item.getTopicId();
            var metadata = new java.util.LinkedHashMap<String, Object>();
            metadata.put("examImportId", item.getImportId());
            metadata.put("questionNumber", index + 1);
            if (source.getSourceQuestionNumber() != null) metadata.put("sourceQuestionNumber", source.getSourceQuestionNumber());
            if (source.getSourcePageNumber() != null) metadata.put("sourcePageNumber", source.getSourcePageNumber());
            if (source.getSourceSectionOrder() != null) metadata.put("sourceSectionOrder", source.getSourceSectionOrder());
            if (source.getSourceSectionTitle() != null) metadata.put("sourceSectionTitle", source.getSourceSectionTitle());
            if (source.getSourceSectionQuestionStart() != null) metadata.put("sourceSectionQuestionStart", source.getSourceSectionQuestionStart());
            if (source.getSourceSectionQuestionEnd() != null) metadata.put("sourceSectionQuestionEnd", source.getSourceSectionQuestionEnd());
            if (source.getSectionInstructions() != null && !source.getSectionInstructions().isBlank()) {
                metadata.put("sectionInstructions", source.getSectionInstructions());
            }
            if (source.getStimulusContent() != null && !source.getStimulusContent().isBlank()) {
                metadata.put("stimulusContent", source.getStimulusContent());
            }
            return Question.builder()
                    .certificateId(item.getCertificateId()).topicId(resolvedTopicId)
                    .questionType(source.getQuestionType().toUpperCase(Locale.ROOT))
                    .difficulty(QuestionDifficulty.valueOf(source.getDifficulty().toUpperCase(Locale.ROOT)))
                    .content(source.getContent().trim()).contentJson(write(metadata))
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

    private void validateTopic(Long questionTopicId, Long importTopicId, List<Long> allowedTopicIds,
                               boolean requiredWhenTopicsExist) {
        Long resolvedTopicId = questionTopicId != null ? questionTopicId : importTopicId;
        if (requiredWhenTopicsExist && !allowedTopicIds.isEmpty() && resolvedTopicId == null) {
            throw new IllegalArgumentException("Cần chọn kỹ năng/môn cho từng câu trước khi xác nhận.");
        }
        if (!allowedTopicIds.isEmpty() && resolvedTopicId != null && !allowedTopicIds.contains(resolvedTopicId)) {
            throw new IllegalArgumentException("Kỹ năng/môn của câu hỏi không thuộc kỳ thi đã chọn.");
        }
    }

    private ExamImport findPending(Long id) {
        return imports.findById(id).filter(item -> item.getStatus() == ExamImportStatus.PENDING_REVIEW)
                .orElseThrow(() -> new IllegalArgumentException("Bộ đề không tồn tại hoặc đã được xác nhận."));
    }

    private String extract(MultipartFile file, String label) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File " + label + " bắt buộc và không được vượt 20 MB.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Tên file " + label + " không hợp lệ.");
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            try {
                return PdfTextExtractor.extract(file.getBytes());
            } catch (IOException | IllegalArgumentException exception) {
                throw new IllegalArgumentException("Không đọc được PDF " + label + ": " + exception.getMessage());
            }
        } else if (lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt")) {
            try {
                return new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalArgumentException("Không đọc được file văn bản " + label + ": " + exception.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Chỉ chấp nhận file PDF hoặc Markdown (.md, .markdown, .txt) cho " + label + ".");
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
        return toResponse(item, questions, null);
    }

    private ExamImportResponse toResponse(ExamImport item, List<ExamImportQuestion> questions, Long viewerId) {
        List<ExamImportResourceResponse> resources = item.getImportId() == null ? List.of()
                : importResources.findByImportIdOrderByUploadedAtAsc(item.getImportId()).stream()
                        .map(this::toResourceResponse).toList();
        return ExamImportResponse.builder().importId(item.getImportId()).title(item.getTitle())
                .certificateId(item.getCertificateId()).topicId(item.getTopicId())
                .canDelete(isAuthorizedToDelete(item, viewerId))
                .answerGenerated(item.isAnswerGenerated()).hasAnswers(questions.stream().anyMatch(question ->
                        question.getCorrectAnswer() != null && !question.getCorrectAnswer().isBlank()))
                .sourceFileName(item.getSourceFileName()).sourceFileContentType(item.getSourceFileContentType())
                .sourceFileSize(item.getSourceFileSize())
                .resources(resources)
                .status(item.getStatus()).questionCount(questions.size())
                .questions(questions).sourceContent(item.getSourceContent()).answerContent(item.getAnswerContent())
                .createdAt(item.getCreatedAt()).build();
    }

    private ExamImportResponse toSummary(ExamImport item) {
        return toSummary(item, null);
    }

    private ExamImportResponse toSummary(ExamImport item, Long viewerId) {
        List<ExamImportQuestion> questions = read(item.getQuestionsJson());
        return ExamImportResponse.builder().importId(item.getImportId()).title(item.getTitle())
                .certificateId(item.getCertificateId()).topicId(item.getTopicId())
                .canDelete(isAuthorizedToDelete(item, viewerId))
                .answerGenerated(item.isAnswerGenerated()).hasAnswers(questions.stream().anyMatch(question ->
                        question.getCorrectAnswer() != null && !question.getCorrectAnswer().isBlank()))
                .sourceFileName(item.getSourceFileName()).sourceFileContentType(item.getSourceFileContentType())
                .sourceFileSize(item.getSourceFileSize())
                .status(item.getStatus()).questionCount(questions.size()).createdAt(item.getCreatedAt()).build();
    }

    private ExamImportResourceResponse toResourceResponse(ExamImportResource resource) {
        return new ExamImportResourceResponse(resource.getResourceId(), resource.getSectionKey(),
                resource.getSectionTitle(), resource.getResourceType(), resource.getFileName(),
                resource.getContentType(), resource.getFileSize(), resource.getUploadedAt());
    }
}
