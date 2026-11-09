package adaptivelearning.module.examimports.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import adaptivelearning.module.ai.gateway.OpenAiProvider;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.examimports.entity.ExamImport;
import adaptivelearning.module.examimports.entity.ExamImportStatus;
import adaptivelearning.module.examimports.storage.FileStorage;
import adaptivelearning.module.examimports.storage.StoredFile;
import adaptivelearning.module.examimports.repository.ExamImportResourceRepository;
import adaptivelearning.module.examimports.repository.ExamImportRepository;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.rag.service.PdfTextExtractor;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@ExtendWith(MockitoExtension.class)
class ExamImportSimulationTest {
    @Mock private ExamImportRepository imports;
    @Mock private QuestionRepository questions;
    @Mock private CertificateRepository certificates;
    @Mock private TopicRepository topics;

    @Test
    @EnabledIfEnvironmentVariable(named = "AI_SIM_BASE_URL", matches = "http://.+")
    void examImportCallsConfiguredAiHttpStandInAndParsesQuestions() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(5))));
        OpenAiProvider provider = new OpenAiProvider(builder, mapper, "simulation-test-key",
                System.getenv("AI_SIM_BASE_URL"), "mock-exam-model");
        ExamImportServiceImpl service = new ExamImportServiceImpl(
                imports, mock(ExamImportResourceRepository.class), questions, certificates, topics, provider, mapper, fileStorage());
        ReflectionTestUtils.setField(service, "model", "mock-exam-model");
        when(certificates.existsById(4L)).thenReturn(true);
        when(topics.findById(8L)).thenReturn(Optional.of(Topic.builder().certificateId(4L).isActive(true).build()));
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(2L, "Simulation exam", 4L, 8L,
                pdf("exam.pdf", "What is the answer? A. First B. Second"), null);

        assertEquals(ExamImportStatus.PENDING_REVIEW, response.getStatus());
        assertEquals(1, response.getQuestionCount());
        assertEquals("Simulation question?", response.getQuestions().get(0).getContent());
        assertEquals("A", response.getQuestions().get(0).getCorrectAnswer());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AI_SIM_BASE_URL", matches = "http://.+")
    void longExamUsesMultipleSmallAiRequestsAgainstStandIn() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(5))));
        OpenAiProvider provider = new OpenAiProvider(builder, mapper, "simulation-test-key",
                System.getenv("AI_SIM_BASE_URL"), "mock-exam-model");
        ExamImportServiceImpl service = new ExamImportServiceImpl(
                imports, mock(ExamImportResourceRepository.class), questions, certificates, topics, provider, mapper, fileStorage());
        ReflectionTestUtils.setField(service, "model", "mock-exam-model");
        when(certificates.existsById(4L)).thenReturn(true);
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String source = java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(index -> "Question " + index + ": " + "context ".repeat(800))
                .collect(java.util.stream.Collectors.joining("\n"));

        var response = service.create(2L, "Long simulation exam", 4L, null,
                textFile("exam.md", source), null);

        assertEquals(4, response.getQuestionCount());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AI_SIM_BASE_URL", matches = "http://.+")
    void answerKeyUploadUsesConfiguredAiHttpStandInAndOnlyUpdatesTheAnswer() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(5))));
        OpenAiProvider provider = new OpenAiProvider(builder, mapper, "simulation-test-key",
                System.getenv("AI_SIM_BASE_URL"), "mock-exam-model");
        ExamImportServiceImpl service = new ExamImportServiceImpl(
                imports, mock(ExamImportResourceRepository.class), questions, certificates, topics, provider, mapper, fileStorage());
        ReflectionTestUtils.setField(service, "model", "mock-exam-model");
        ExamImport item = new ExamImport();
        item.setImportId(9L);
        item.setTitle("Simulation exam");
        item.setCertificateId(4L);
        item.setStatus(ExamImportStatus.PENDING_REVIEW);
        item.setAnswerGenerated(true);
        item.setQuestionsJson("[{\"questionType\":\"MULTIPLE_CHOICE\",\"difficulty\":\"MEDIUM\","
                + "\"content\":\"Existing OCR question?\",\"options\":[{\"label\":\"A\",\"text\":\"First\"},"
                + "{\"label\":\"B\",\"text\":\"Second\"}],\"correctAnswer\":\"\","
                + "\"explanation\":\"Existing explanation\",\"points\":1}]");
        when(imports.findById(9L)).thenReturn(Optional.of(item));
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.importAnswerKey(9L, textFile("answers.md", "1. A"));

        assertEquals(1, result.getMatchedAnswerCount());
        assertEquals("Existing OCR question?", result.getExamImport().getQuestions().get(0).getContent());
        assertEquals("A", result.getExamImport().getQuestions().get(0).getCorrectAnswer());
    }

    private FileStorage fileStorage() {
        FileStorage storage = mock(FileStorage.class);
        when(storage.store(any(), any(), any())).thenReturn(new StoredFile(
                "12345678-1234-1234-1234-123456789abc", "exam.pdf", "application/pdf", 123L, "0".repeat(64)));
        return storage;
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
        return new MockMultipartFile("examFile", name, "application/pdf", bytes);
    }

    private MockMultipartFile textFile(String name, String text) {
        return new MockMultipartFile("examFile", name, "text/markdown",
                text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
