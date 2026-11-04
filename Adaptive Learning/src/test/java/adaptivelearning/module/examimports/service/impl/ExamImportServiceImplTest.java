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
import static org.mockito.Mockito.when;

import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.examimports.entity.ExamImport;
import adaptivelearning.module.examimports.entity.ExamImportStatus;
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
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
    @Mock private QuestionRepository questions;
    @Mock private CertificateRepository certificates;
    @Mock private TopicRepository topics;
    @Mock private AiProvider aiProvider;
    private ExamImportServiceImpl service;

    @BeforeEach
    void configureModel() {
        service = new ExamImportServiceImpl(imports, questions, certificates, topics, aiProvider, new ObjectMapper());
        ReflectionTestUtils.setField(service, "model", "test-model");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadWithoutAnswerKeyCreatesPendingAiSuggestedQuestions() throws Exception {
        when(certificates.existsById(1L)).thenReturn(true);
        when(topics.findById(2L)).thenReturn(Optional.of(Topic.builder().certificateId(1L).isActive(true).build()));
        when(aiProvider.chatCompletionSync(anyString(), anyString(), eq("test-model"), anyInt()))
                .thenReturn(mcqJson());
        when(imports.save(any(ExamImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(3L, "Sample exam", 1L, 2L, pdf("exam.pdf", "Question text"), null);

        assertTrue(response.isAnswerGenerated());
        assertEquals(ExamImportStatus.PENDING_REVIEW, response.getStatus());
        assertEquals(1, response.getQuestionCount());
        verify(imports).save(any(ExamImport.class));
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
    void confirmationRejectsQuestionsWithoutReviewedAnswers() {
        ExamImport item = new ExamImport();
        item.setImportId(11L);
        item.setStatus(ExamImportStatus.PENDING_REVIEW);
        item.setQuestionsJson(mcqJson().replace("\"correctAnswer\":\"A\"", "\"correctAnswer\":\"\""));
        when(imports.findById(11L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> service.confirm(11L));
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

    private String mcqJson() {
        return "[{\"questionType\":\"MULTIPLE_CHOICE\",\"difficulty\":\"MEDIUM\","
                + "\"content\":\"Question?\",\"options\":[{\"label\":\"A\",\"text\":\"One\"},"
                + "{\"label\":\"B\",\"text\":\"Two\"}],\"correctAnswer\":\"A\","
                + "\"explanation\":\"Because\",\"points\":1}]";
    }
}
