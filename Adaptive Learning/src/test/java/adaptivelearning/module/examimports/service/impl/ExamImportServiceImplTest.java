package adaptivelearning.module.examimports.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.examimports.entity.ExamImport;
import adaptivelearning.module.examimports.entity.ExamImportStatus;
import adaptivelearning.module.examimports.dto.ExamImportAnswerKeyResult;
import adaptivelearning.module.examimports.storage.FileStorage;
import adaptivelearning.module.examimports.storage.StoredFile;
import adaptivelearning.module.examimports.entity.ExamImportResource;
import adaptivelearning.module.examimports.repository.ExamImportResourceRepository;
import adaptivelearning.module.examimports.repository.ExamImportRepository;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.shared.enums.ContentStatus;
import adaptivelearning.shared.enums.QuestionDifficulty;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.IntStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExamImportServiceImplTest {
    @Mock private ExamImportRepository imports;
    @Mock private ExamImportResourceRepository importResources;
    @Mock private QuestionRepository questions;
    @Mock private CertificateRepository certificates;
    @Mock private TopicRepository topics;
    @Mock private AiProvider aiProvider;
    @Mock private FileStorage fileStorage;
    private ExamImportServiceImpl service;

    @BeforeEach
    void configureModel() {
        service = new ExamImportServiceImpl(imports, importResources, questions, certificates, topics, aiProvider,
                new ObjectMapper(), fileStorage);
        lenient().when(fileStorage.store(any(byte[].class), any(), any()))
                .thenReturn(new StoredFile("12345678-1234-1234-1234-123456789abc", "exam.pdf",
                        "application/pdf", 123L, "0".repeat(64)));
        ReflectionTestUtils.setField(service, "model", "test-model");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadWithoutAnswerKeyLeavesAnswersEmptyForReview() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(topics.findById(2L)).thenReturn(Optional.of(Topic.builder().certificateId(1L).isActive(true).build()));
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(mcqJson());
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Sample exam", 1L, 2L, pdf("exam.pdf", "Question text"), null);

        assertFalse(response.isAnswerGenerated());
        assertFalse(response.isHasAnswers());
        assertEquals("", response.getQuestions().get(0).getCorrectAnswer());
        assertEquals("", response.getQuestions().get(0).getExplanation());
        assertEquals(ExamImportStatus.PENDING_REVIEW, response.getStatus());
        assertEquals(1, response.getQuestionCount());
        verify(imports).save(any(ExamImport.class));
    }

    @Test
    void doesNotInferMultipleChoiceAnswersWithoutAnAnswerKey() {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(mcqJsonWithAnswer("(A)"))
                .thenReturn(mcqJsonWithAnswer("Two"))
                .thenReturn(mcqJsonWithAnswer("uncertain answer"));
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var labeledAnswer = service.create(3L, "Exam A", 1L, null,
                textFile("exam-a.md", "Question?"), null);
        var textAnswer = service.create(3L, "Exam B", 1L, null,
                textFile("exam-b.md", "Question?"), null);
        var unclearAnswer = service.create(3L, "Exam C", 1L, null,
                textFile("exam-c.md", "Question?"), null);

        assertEquals("", labeledAnswer.getQuestions().get(0).getCorrectAnswer());
        assertEquals("", textAnswer.getQuestions().get(0).getCorrectAnswer());
        assertEquals("", unclearAnswer.getQuestions().get(0).getCorrectAnswer());
        assertEquals("", unclearAnswer.getQuestions().get(0).getExplanation());
    }

    @Test
    void ignoresOptionsReturnedForNonMultipleChoiceQuestion() {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn("[{\"questionType\":\"FILL_BLANK\",\"difficulty\":\"MEDIUM\","
                        + "\"content\":\"The island became an [1].\",\"options\":["
                        + "{\"label\":\"A\",\"text\":\"answer\"},{\"label\":\"B\",\"text\":\"option\"}],"
                        + "\"correctAnswer\":\"independent country\",\"explanation\":\"From source key\",\"points\":1}]");
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Reading exam", 1L, null,
                textFile("exam.md", "The island became an [1]."), null);

        assertEquals(1, response.getQuestionCount());
        assertTrue(response.getQuestions().get(0).getOptions().isEmpty());
        assertEquals("", response.getQuestions().get(0).getCorrectAnswer());
    }

    @Test
    void uploadWithAnswerKeyMarksAnswersAsSourceProvided() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(topics.findById(2L)).thenReturn(Optional.of(Topic.builder().certificateId(1L).isActive(true).build()));
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(mcqJson());
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Sample exam", 1L, 2L,
                pdf("exam.pdf", "Question text"), pdf("answers.pdf", "1. A"));

        assertFalse(response.isAnswerGenerated());
        assertEquals("1. A", response.getAnswerContent());
        assertEquals(ExamImportStatus.PENDING_REVIEW, response.getStatus());
    }

    @Test
    void importsAnswerKeyIntoExistingOcrQuestionsWithoutChangingQuestionContent() {
        ExamImport item = new ExamImport();
        item.setImportId(31L);
        item.setTitle("OCR exam");
        item.setCertificateId(1L);
        item.setStatus(ExamImportStatus.PENDING_REVIEW);
        item.setAnswerGenerated(true);
        item.setSourceContent("OCR source stays unchanged");
        item.setQuestionsJson("[{\"questionType\":\"MULTIPLE_CHOICE\",\"difficulty\":\"MEDIUM\","
                + "\"content\":\"Original OCR question?\",\"options\":[{\"label\":\"A\",\"text\":\"One\"},"
                + "{\"label\":\"B\",\"text\":\"Two\"}],\"correctAnswer\":\"\","
                + "\"explanation\":\"Original explanation\",\"points\":1}]");
        when(imports.findById(31L)).thenReturn(Optional.of(item));
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn("[{\"questionIndex\":1,\"correctAnswer\":\"Two\"}]");
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExamImportAnswerKeyResult result = service.importAnswerKey(31L,
                textFile("answer-key.md", "1. B"));

        assertEquals(1, result.getMatchedAnswerCount());
        assertEquals(1, result.getTotalQuestionCount());
        assertEquals("Original OCR question?", result.getExamImport().getQuestions().get(0).getContent());
        assertEquals("B", result.getExamImport().getQuestions().get(0).getCorrectAnswer());
        assertEquals("OCR source stays unchanged", result.getExamImport().getSourceContent());
        assertEquals("1. B", result.getExamImport().getAnswerContent());
        assertFalse(result.getExamImport().isAnswerGenerated());
    }

    @Test
    void uploadWithAnswersInsideExamDoesNotRequireSeparateAnswerFile() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenAnswer(invocation -> {
                    assertTrue(invocation.getArgument(1, String.class).contains("official answer key"));
                    return mcqJson();
                });
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Answers inside exam", 1L, null,
                textFile("exam.md", "Question and answer key"), null, true);

        assertFalse(response.isAnswerGenerated());
        assertEquals(1, response.getQuestionCount());
    }

    @Test
    void uploadWithEmbeddedAnswersRejectsSeparateAnswerFile() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(3L, "Conflict", 1L, null,
                        textFile("exam.md", "Question"), textFile("answers.md", "Answer"), true));

        assertTrue(exception.getMessage().contains("Đã chọn đề có đáp án"));
    }

    @Test
    void autoClassificationDoesNotSilentlyAssignFirstTopicWhenSkillIsUnclear() throws Exception {
        Topic reading = Topic.builder().topicId(21L).certificateId(1L).name("Reading").code("READING").isActive(true).build();
        Topic matchingHeadings = Topic.builder().topicId(23L).certificateId(1L).name("Matching Headings")
                .code("MATCHING_HEADINGS").parentId(21L).isActive(true).build();
        Topic paragraphMatching = Topic.builder().topicId(24L).certificateId(1L).name("Paragraph Matching")
                .code("PARAGRAPH_MATCHING").parentId(23L).isActive(true).build();
        when(certificates.existsById(1L)).thenReturn(true);
        when(topics.findByCertificateId(1L)).thenReturn(List.of(reading, matchingHeadings, paragraphMatching));
        when(aiProvider.chatCompletionSync(anyString(), anyString(), anyString(), anyInt())).thenReturn(mcqJson());
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Mixed exam", 1L, null,
                textFile("exam.md", "Question?"), null);

        assertEquals(null, response.getQuestions().get(0).getTopicId());
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(aiProvider).chatCompletionSync(prompt.capture(), anyString(), anyString(), anyInt());
        assertTrue(prompt.getValue().contains("path='Reading / Matching Headings / Paragraph Matching'"));
    }

    @Test
    void confirmingReviewPublishesQuestionsToApprovedBank() throws Exception {
        ExamImport item = new ExamImport();
        item.setImportId(9L);
        item.setCertificateId(1L);
        item.setTopicId(2L);
        item.setTitle("Sample exam");
        item.setStatus(ExamImportStatus.PENDING_REVIEW);
        item.setQuestionsJson(mcqJson());
        when(imports.findById(9L)).thenReturn(Optional.of(item));
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questions.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        User principal = new User();
        principal.setUsername("reviewer@example.test");
        principal.setRole(Role.ROLE_TEACHER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));

        var response = service.confirm(9L);

        assertEquals(ExamImportStatus.CONFIRMED, response.getStatus());
        ArgumentCaptor<List<Question>> saved = ArgumentCaptor.forClass(List.class);
        verify(questions).saveAll(saved.capture());
        assertEquals(1, saved.getValue().size());
        assertEquals(ContentStatus.APPROVED, saved.getValue().get(0).getStatus());
        assertEquals(QuestionDifficulty.MEDIUM, saved.getValue().get(0).getDifficulty());
        assertEquals("\"A\"", saved.getValue().get(0).getCorrectAnswer());
    }

    @Test
    void uploadWithMarkdownFileSucceeds() throws Exception {
        Topic topic = new Topic();
        topic.setTopicId(2L);
        topic.setCertificateId(1L);
        topic.setActive(true);

        when(certificates.existsById(1L)).thenReturn(true);
        when(topics.findById(2L)).thenReturn(Optional.of(topic));
        when(aiProvider.chatCompletionSync(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn("Kết quả trích xuất:\n```json\n" + mcqJson() + "\n```");
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> {
            ExamImport item = invocation.getArgument(0);
            item.setImportId(99L);
            return item;
        });

        MockMultipartFile mdFile = new MockMultipartFile(
                "examFile",
                "sample-exam.md",
                "text/markdown",
                "# IELTS Reading\n\nQuestion 1: What is the main idea?\nA. One\nB. Two\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        var response = service.create(10L, "Sample Markdown Exam", 1L, 2L, mdFile, null);

        assertEquals(99L, response.getImportId());
        assertEquals("Sample Markdown Exam", response.getTitle());
        assertFalse(response.isAnswerGenerated());
        assertEquals(1, response.getQuestions().size());
    }

    @Test
    void scannedPdfUsesDocumentAnalysisAndCreatesPendingImport() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt())).thenReturn("Question 1: transcribed question and choices.");
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(mcqJson());
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Scanned exam", 1L, null, blankPdf("scan.pdf"), null);

        assertFalse(response.isAnswerGenerated());
        assertEquals(1, response.getQuestionCount());
        verify(aiProvider).analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt());
        verify(aiProvider).chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt());
        verify(imports).save(any(ExamImport.class));
    }

    @Test
    void longTextIsAnalyzedAsOneCompleteSourceAndReportsProgress() {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(questionListJson(12));
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String content = IntStream.range(1, 13)
                .mapToObj(index -> "Question " + index + ": " + "context ".repeat(850))
                .reduce((left, right) -> left + "\n" + right).orElseThrow();
        List<String> progressUpdates = new ArrayList<>();

        var response = service.createWithProgress(3L, "Long exam", 1L, null,
                textFile("exam.md", content), null, false,
                (completed, total) -> progressUpdates.add(completed + "/" + total));

        assertEquals(12, response.getQuestionCount());
        assertEquals("0/1", progressUpdates.get(0));
        assertEquals("1/1", progressUpdates.get(progressUpdates.size() - 1));
        verify(aiProvider).chatCompletionSync(anyString(), anyString(), eq("test-model"), eq(48_000));
    }

    @Test
    void retriesOnceWhenAiReturnsInvalidQuestionJson() {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn("I found one question, here it is:", mcqJson());
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Sample exam", 1L, null,
                textFile("exam.md", "Question text"), null);

        assertEquals(1, response.getQuestionCount());
        verify(aiProvider, times(2)).chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt());
    }

    @Test
    void scannedPdfIsSentWholeAndReportsSinglePassProgress() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt())).thenAnswer(invocation -> {
                    byte[] part = invocation.getArgument(2);
                    try (PDDocument document = Loader.loadPDF(part)) {
                        assertEquals(1, document.getNumberOfPages());
                    }
                    return "Transcribed page content";
                });
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenAnswer(invocation -> {
                    String fullTranscriptPrompt = invocation.getArgument(1);
                    assertTrue(fullTranscriptPrompt.contains("[PDF PAGE 1 / 22]"));
                    assertTrue(fullTranscriptPrompt.contains("[PDF PAGE 22 / 22]"));
                    return sectionQuestionsJson(1, 10);
                });
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<String> progressUpdates = new ArrayList<>();

        var response = service.createWithProgress(3L, "Scanned exam", 1L, null,
                blankPdfPages("scan.pdf", 22), null, false,
                (completed, total) -> progressUpdates.add(completed + "/" + total));

        assertEquals(10, response.getQuestionCount());
        assertEquals("0/23", progressUpdates.get(0));
        assertEquals("23/23", progressUpdates.get(progressUpdates.size() - 1));
        verify(aiProvider, times(22)).analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt());
        verify(aiProvider).chatCompletionSync(anyString(), anyString(), eq("test-model"), eq(48_000));
    }

    @Test
    void scannedPdfAnalyzesLongOcrTranscriptInOneRequest() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt())).thenReturn("x".repeat(7_000));
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenAnswer(new org.mockito.stubbing.Answer<>() {
                    @Override
                    public String answer(org.mockito.invocation.InvocationOnMock invocation) {
                        String prompt = invocation.getArgument(1);
                        assertTrue(prompt.contains("complete OCR transcript"));
                        assertTrue(prompt.contains("[PDF PAGE 1 / 2]"));
                        assertTrue(prompt.contains("[PDF PAGE 2 / 2]"));
                        return questionListJson(3);
                    }
                });
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        List<String> progressUpdates = new ArrayList<>();

        var response = service.createWithProgress(3L, "Scanned exam", 1L, null,
                blankPdfPages("scan.pdf", 2), null, false,
                (completed, total) -> progressUpdates.add(completed + "/" + total));

        assertEquals(3, response.getQuestionCount());
        verify(aiProvider).chatCompletionSync(anyString(), anyString(), eq("test-model"), eq(48_000));
        assertEquals("3/3", progressUpdates.get(progressUpdates.size() - 1));
    }

    @Test
    void fillBlankQuestionMarksItsNumberAtTheBlankLocation() {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn("[{\"questionType\":\"FILL_BLANK\",\"difficulty\":\"MEDIUM\","
                        + "\"content\":\"Now perform dances for the benefit of 8\","
                        + "\"sourceQuestionNumber\":\"8\",\"points\":1}]");
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Listening section", 1L, null,
                textFile("exam.txt", "Questions 1-10. Complete the sentences."), null);

        assertEquals("Now perform dances for the benefit of [8]", response.getQuestions().get(0).getContent());
    }

    @Test
    void scannedPdfUsesTargetPageForSourceQuestionPageNumber() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt()))
                .thenReturn("Transcribed page content");
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(scannedQuestionsJson());
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Scanned exam", 1L, null,
                blankPdfPages("scan.pdf", 2), null);

        assertEquals(List.of("11", "12"), response.getQuestions().stream()
                .map(adaptivelearning.module.examimports.dto.ExamImportQuestion::getSourceQuestionNumber)
                .toList());
        assertEquals(List.of(1, 2), response.getQuestions().stream()
                .map(adaptivelearning.module.examimports.dto.ExamImportQuestion::getSourcePageNumber)
                .toList());
    }

    @Test
    void scannedPdfKeepsInitialResultsAndReportsMissingQuestionsForLaterRescan() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt())).thenReturn("Transcribed page");
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(sectionQuestionsJson(1, 7));
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Scanned exam", 1L, null,
                blankPdfPages("scan.pdf", 2), null);

        assertEquals(7, response.getQuestionCount());
        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7"),
                response.getQuestions().stream()
                        .map(adaptivelearning.module.examimports.dto.ExamImportQuestion::getSourceQuestionNumber)
                        .toList());
        verify(aiProvider, times(2)).analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt());
    }

    @Test
    void scannedPdfDoesNotAutomaticallyRescanMissingQuestions() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt())).thenAnswer(invocation -> {
                    String prompt = invocation.getArgument(1);
                    if (prompt.contains("Transcribe all readable text")) return "Transcribed page";
                    return "[]";
                });
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(sectionQuestionJson(26, 26, 36, 16, 3));
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Scanned exam", 1L, null,
                blankPdfPages("scan.pdf", 18), null);

        assertEquals(1, response.getQuestionCount());
        assertEquals("26", response.getQuestions().get(0).getSourceQuestionNumber());
        verify(aiProvider, times(18)).analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt());
    }

    @Test
    void rescanMissingQuestionsReadsOnlyTargetPagesAndMergesRecoveredItems() throws Exception {
        ExamImport item = pendingImport(61L, 3L);
        item.setCertificateId(1L);
        item.setTitle("Scanned exam");
        item.setSourceFileKey("source-key");
        item.setSourceFileName("scan.pdf");
        item.setSourceFileContentType("application/pdf");
        item.setQuestionsJson("[" + scannedQuestionWithRangeJson(1, 1, 1, 3, 1) + ","
                + scannedQuestionWithRangeJson(3, 2, 1, 3, 1) + "]");
        when(imports.findById(61L)).thenReturn(Optional.of(item));
        when(topics.findByCertificateId(1L)).thenReturn(List.of());
        when(fileStorage.read("source-key")).thenReturn(blankPdfPages("scan.pdf", 2).getBytes());
        when(aiProvider.analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt())).thenAnswer(invocation -> {
                    byte[] pageBytes = invocation.getArgument(2);
                    try (PDDocument page = Loader.loadPDF(pageBytes)) {
                        assertEquals(1, page.getNumberOfPages());
                    }
                    String prompt = invocation.getArgument(1);
                    return prompt.contains("page 2")
                            ? "[" + scannedQuestionWithRangeJson(2, 2, 1, 3, 1) + "]" : "[]";
                });
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.rescanMissingQuestions(61L);

        assertEquals(3, response.getQuestionCount());
        assertEquals(List.of("1", "3", "2"), response.getQuestions().stream()
                .map(adaptivelearning.module.examimports.dto.ExamImportQuestion::getSourceQuestionNumber).toList());
        verify(aiProvider, org.mockito.Mockito.atLeastOnce()).analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt());
    }

    @Test
    void scannedPdfKeepsSameQuestionNumberWhenSeparatePagesContainDifferentQuestions() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt())).thenAnswer(new org.mockito.stubbing.Answer<>() {
                    @Override
                    public String answer(org.mockito.invocation.InvocationOnMock invocation) {
                        String first = scannedQuestionJson("24", 2);
                        String second = scannedQuestionJson("24", 4).replace("Question 24?", "Different Question 24?");
                        return "[" + first.substring(1, first.length() - 1) + ","
                                + second.substring(1, second.length() - 1) + "]";
                    }
                });
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(scannedRepeatedQuestionsJson());
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Scanned exam", 1L, null,
                blankPdfPages("scan.pdf", 5), null);

        assertEquals(2, response.getQuestionCount());
        assertEquals("24", response.getQuestions().get(0).getSourceQuestionNumber());
        assertEquals(List.of(2, 4), response.getQuestions().stream()
                .map(adaptivelearning.module.examimports.dto.ExamImportQuestion::getSourcePageNumber).toList());
        verify(aiProvider, times(5)).analyzeDocumentSync(anyString(), anyString(), any(byte[].class),
                eq("application/pdf"), anyInt());
    }

    @Test
    void extractsQuestionGroupsWithRangesAndInstructionsWithinSection() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        String aiResponse = sectionQuestionsJsonWithRange(1, 10, 1, 10, 1);
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(aiResponse);
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String sourceExam = "SECTION 1\nQuestions 1-10\n\n"
                + "Questions 1-4\nComplete the sentences below.\n\n"
                + "Questions 5-10\nChoose the correct answer.\n";

        var response = service.create(3L, "Exam with groups", 1L, null,
                textFile("exam.md", sourceExam), null);

        assertEquals(10, response.getQuestionCount());
        var q1 = response.getQuestions().get(0);
        var q4 = response.getQuestions().get(3);
        var q5 = response.getQuestions().get(4);
        var q10 = response.getQuestions().get(9);

        // Group 1: Questions 1-4
        assertEquals(1, q1.getSourceGroupOrder());
        assertEquals("Questions 1–4", q1.getSourceGroupTitle());
        assertEquals("1", q1.getSourceGroupQuestionStart());
        assertEquals("4", q1.getSourceGroupQuestionEnd());
        assertEquals("Complete the sentences below.", q1.getGroupInstruction());
        assertEquals(1, q4.getSourceGroupOrder());

        // Group 2: Questions 5-10
        assertEquals(2, q5.getSourceGroupOrder());
        assertEquals("Questions 5–10", q5.getSourceGroupTitle());
        assertEquals("5", q5.getSourceGroupQuestionStart());
        assertEquals("10", q5.getSourceGroupQuestionEnd());
        assertEquals("Choose the correct answer.", q5.getGroupInstruction());
        assertEquals(2, q10.getSourceGroupOrder());
    }

    @Test
    void importsAllRecognizedQuestionsAbovePreviousHundredQuestionLimit() {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(questionListJson(125));
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Large exam", 1L, null,
                textFile("exam.md", "Exam has 125 questions"), null);

        assertEquals(125, response.getQuestionCount());
        verify(imports).save(any(ExamImport.class));
    }

    @Test
    void providerFailureReturnsActionableErrorInsteadOfGenericMessage() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(aiProvider.chatCompletionSync(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalStateException(
                        "AI provider error: 403 FORBIDDEN: Your API key was reported as leaked."));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.create(3L, "Sample exam", 1L, null, textFile("exam.md", "Question?"), null));

        assertTrue(exception.getMessage().contains("key bị báo lộ"));
    }

    @Test
    void confirmationRejectsQuestionsWithoutReviewedAnswers() {
        ExamImport item = new ExamImport();
        item.setImportId(11L);
        item.setStatus(ExamImportStatus.PENDING_REVIEW);
        item.setQuestionsJson(mcqJson().replace("\"correctAnswer\":\"A\"", "\"correctAnswer\":\"\""));
        when(imports.findById(11L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> service.confirm(11L));
        verify(questions, never()).saveAll(any());
    }

    @Test
    void pendingListMarksOnlyUploaderOwnedImportsAsDeletable() {
        ExamImport mine = pendingImport(41L, 7L);
        ExamImport other = pendingImport(42L, 8L);
        when(imports.findByStatusOrderByCreatedAtDesc(ExamImportStatus.PENDING_REVIEW))
                .thenReturn(List.of(mine, other));

        var response = service.pending(7L);

        assertTrue(response.get(0).isCanDelete());
        assertFalse(response.get(1).isCanDelete());
    }

    @Test
    void uploaderCanDeleteOwnPendingImport() {
        ExamImport mine = pendingImport(43L, 7L);
        when(imports.findById(43L)).thenReturn(Optional.of(mine));

        service.deleteOwned(43L, 7L);

        verify(imports).delete(mine);
    }

    @Test
    void returnsStoredSourceFileForReview() {
        ExamImport item = pendingImport(45L, 7L);
        item.setSourceFileKey("12345678-1234-1234-1234-123456789abc");
        item.setSourceFileName("source.pdf");
        item.setSourceFileContentType("application/pdf");
        byte[] bytes = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(imports.findById(45L)).thenReturn(Optional.of(item));
        when(fileStorage.read(item.getSourceFileKey())).thenReturn(bytes);

        var source = service.getSourceFile(45L);

        assertEquals("source.pdf", source.fileName());
        assertEquals("application/pdf", source.contentType());
        assertEquals(bytes.length, source.content().length);
    }

    @Test
    void uploadsAudioAsAResourceOfItsListeningSection() throws Exception {
        when(imports.findById(46L)).thenReturn(Optional.of(pendingImport(46L, 7L)));
        when(fileStorage.store(any(byte[].class), any(), any())).thenReturn(new StoredFile(
                "12345678-1234-1234-1234-123456789abc", "listening.mp3", "audio/mpeg", 5L, "1".repeat(64)));
        when(importResources.save(any(ExamImportResource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var audio = new MockMultipartFile("audioFile", "listening.mp3", "audio/mpeg", new byte[] {1, 2, 3, 4, 5});

        var resource = service.uploadSectionAudio(46L, "21", "Listening", audio, 7L);

        assertEquals("21", resource.sectionKey());
        assertEquals("Listening", resource.sectionTitle());
        assertEquals("AUDIO", resource.resourceType());
        assertEquals("listening.mp3", resource.fileName());
    }

    @Test
    void uploaderCannotDeleteAnotherUsersImport() {
        ExamImport other = pendingImport(44L, 8L);
        when(imports.findById(44L)).thenReturn(Optional.of(other));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.deleteOwned(44L, 7L));

        verify(imports, never()).delete(any(ExamImport.class));
    }

    private ExamImport pendingImport(Long id, Long uploaderId) {
        ExamImport item = new ExamImport();
        item.setImportId(id);
        item.setUploadedBy(uploaderId);
        item.setStatus(ExamImportStatus.PENDING_REVIEW);
        item.setQuestionsJson("[]");
        return item;
    }

    @Test
    void confirmationRejectsUnassignedSkillWhenCertificateHasTopics() {
        ExamImport item = new ExamImport();
        item.setImportId(12L);
        item.setCertificateId(1L);
        item.setStatus(ExamImportStatus.PENDING_REVIEW);
        item.setQuestionsJson(mcqJson());
        when(imports.findById(12L)).thenReturn(Optional.of(item));
        when(topics.findByCertificateId(1L)).thenReturn(List.of(
                Topic.builder().topicId(21L).certificateId(1L).name("Reading").isActive(true).build()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.confirm(12L));

        assertTrue(exception.getMessage().contains("Cần chọn kỹ năng/môn"));
        verify(questions, never()).saveAll(any());
    }

    private MockMultipartFile pdf(String name, String text) throws Exception {
        byte[] bytes;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(40, 700);
                content.showText(text);
                content.endText();
            }
            document.save(output);
            bytes = output.toByteArray();
        }
        return new MockMultipartFile("file", name, "application/pdf", bytes);
    }

    private MockMultipartFile blankPdf(String name) throws Exception {
        return blankPdfPages(name, 1);
    }

    private MockMultipartFile blankPdfPages(String name, int pageCount) throws Exception {
        byte[] bytes;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int page = 0; page < pageCount; page++) document.addPage(new PDPage());
            document.save(output);
            bytes = output.toByteArray();
        }
        return new MockMultipartFile("examFile", name, "application/pdf", bytes);
    }

    private MockMultipartFile textFile(String name, String content) {
        return new MockMultipartFile("examFile", name, "text/markdown",
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String mcqJson() {
        return mcqJsonWithAnswer("A");
    }

    private String mcqJsonWithAnswer(String answer) {
        return "[{\"questionType\":\"MULTIPLE_CHOICE\",\"difficulty\":\"MEDIUM\","
                + "\"content\":\"Question?\",\"options\":[{\"label\":\"A\",\"text\":\"One\"},"
                + "{\"label\":\"B\",\"text\":\"Two\"}],\"correctAnswer\":\"" + answer + "\","
                + "\"explanation\":\"Because\",\"points\":1}]";
    }

    private String questionListJson(int count) {
        return IntStream.range(1, count + 1).mapToObj(index ->
                        "{\"questionType\":\"MULTIPLE_CHOICE\",\"difficulty\":\"MEDIUM\","
                                + "\"content\":\"Question " + index + "?\","
                                + "\"options\":[{\"label\":\"A\",\"text\":\"One\"},{\"label\":\"B\",\"text\":\"Two\"}],"
                                + "\"correctAnswer\":\"A\",\"explanation\":\"Because\",\"points\":1}")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String scannedQuestionJson(String number, int incorrectPage) {
        return "[{\"questionType\":\"MULTIPLE_CHOICE\",\"difficulty\":\"MEDIUM\","
                + "\"content\":\"Question " + number + "?\","
                + "\"options\":[{\"label\":\"A\",\"text\":\"One\"},{\"label\":\"B\",\"text\":\"Two\"}],"
                + "\"correctAnswer\":\"A\",\"explanation\":\"Because\",\"points\":1,"
                + "\"sourceQuestionNumber\":\"" + number + "\",\"sourcePageNumber\":" + incorrectPage + "}]";
    }

    private String scannedQuestionsJson() {
        String first = scannedQuestionJson("11", 1);
        String second = scannedQuestionJson("12", 2);
        return "[" + first.substring(1, first.length() - 1) + ","
                + second.substring(1, second.length() - 1) + "]";
    }

    private String scannedRepeatedQuestionsJson() {
        String first = scannedQuestionJson("24", 2);
        String second = scannedQuestionJson("24", 4).replace("Question 24?", "Different Question 24?");
        return "[" + first.substring(1, first.length() - 1) + ","
                + second.substring(1, second.length() - 1) + "]";
    }

    private String sectionQuestionsJson(int start, int end) {
        return sectionQuestionsJsonWithRange(start, end, 1, 10, 2);
    }

    private String sectionQuestionsJsonWithRange(int start, int end, int rangeStart, int rangeEnd, int page) {
        return IntStream.rangeClosed(start, end).mapToObj(number ->
                        "{\"questionType\":\"MULTIPLE_CHOICE\",\"difficulty\":\"MEDIUM\","
                                + "\"content\":\"Question " + number + "?\","
                                + "\"options\":[{\"label\":\"A\",\"text\":\"One\"},{\"label\":\"B\",\"text\":\"Two\"}],"
                                + "\"correctAnswer\":\"A\",\"points\":1,"
                                + "\"sourceQuestionNumber\":\"" + number + "\","
                                + "\"sourcePageNumber\":" + page + ","
                                + "\"sourceSectionOrder\":1,\"sourceSectionTitle\":\"SECTION 1\","
                                + "\"sourceSectionQuestionStart\":\"" + rangeStart + "\","
                                + "\"sourceSectionQuestionEnd\":\"" + rangeEnd + "\","
                                + "\"sectionInstructions\":\"Questions " + rangeStart + "-" + rangeEnd + "\"}")
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String sectionQuestionJson(int number, int start, int end, int page, int sectionOrder) {
        return sectionQuestionsJsonWithRange(number, number, start, end, page)
                .replace("\"sourceSectionOrder\":1", "\"sourceSectionOrder\":" + sectionOrder);
    }

    private String scannedQuestionWithRangeJson(int number, int page, int rangeStart, int rangeEnd, int sectionOrder) {
        return sectionQuestionsJsonWithRange(number, number, rangeStart, rangeEnd, page)
                .replace("\"sourceSectionOrder\":1", "\"sourceSectionOrder\":" + sectionOrder)
                .substring(1, sectionQuestionsJsonWithRange(number, number, rangeStart, rangeEnd, page).length() - 1);
    }
}
