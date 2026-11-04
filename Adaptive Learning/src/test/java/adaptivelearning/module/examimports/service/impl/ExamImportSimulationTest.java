package adaptivelearning.module.examimports.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import adaptivelearning.module.ai.gateway.OpenAiProvider;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.examimports.entity.ExamImport;
import adaptivelearning.module.examimports.entity.ExamImportStatus;
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
                imports, questions, certificates, topics, provider, mapper);
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
}
