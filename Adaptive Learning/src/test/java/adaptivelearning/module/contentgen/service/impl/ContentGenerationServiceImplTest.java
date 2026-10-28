package adaptivelearning.module.contentgen.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.contentgen.dto.request.GenerateQuestionsRequest;
import adaptivelearning.module.contentgen.entity.ContentGeneration;
import adaptivelearning.module.contentgen.repository.ContentGenerationRepository;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.rag.dto.response.KnowledgeContextResponse;
import adaptivelearning.module.rag.service.RagService;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.shared.enums.ContentStatus;
import adaptivelearning.shared.enums.GenerationStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ContentGenerationServiceImplTest {

    private ContentGenerationRepository generations;
    private QuestionRepository questions;
    private RagService rag;
    private AiProvider ai;
    private CertificateRepository certificates;
    private TopicRepository topics;
    private ContentGenerationServiceImpl service;

    @BeforeEach
    void setUp() {
        generations = mock(ContentGenerationRepository.class);
        questions = mock(QuestionRepository.class);
        rag = mock(RagService.class);
        ai = mock(AiProvider.class);
        certificates = mock(CertificateRepository.class);
        topics = mock(TopicRepository.class);
        service = new ContentGenerationServiceImpl(generations, questions, rag, ai,
                new com.fasterxml.jackson.databind.ObjectMapper(), certificates, topics);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "model", "test-model");
        when(certificates.existsById(2L)).thenReturn(true);
        when(topics.findById(3L)).thenReturn(Optional.of(Topic.builder()
                .topicId(3L).certificateId(2L).isActive(true).build()));
        when(generations.save(any(ContentGeneration.class))).thenAnswer(invocation -> {
            ContentGeneration generation = invocation.getArgument(0);
            if (generation.getGenerationId() == null) generation.setGenerationId(8L);
            return generation;
        });
        when(rag.retrieveKnowledge(eq(2L), eq(3L), anyString(), eq(6)))
                .thenReturn(List.of(KnowledgeContextResponse.builder().chunkId(99L).title("Source")
                        .chunkIndex(0).content("Grounded source text").build()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitUsesRetrievedKnowledgeAndPersistsValidatedDraftGeneration() {
        when(ai.chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(validResponse(99));

        var response = service.submit(5L, request());

        assertEquals(8L, response.getGenerationId());
        assertEquals(GenerationStatus.READY, response.getStatus());
        assertEquals(1, response.getItemCount());
        verify(rag).retrieveKnowledge(eq(2L), eq(3L),
                org.mockito.ArgumentMatchers.contains("MULTIPLE_CHOICE"), org.mockito.ArgumentMatchers.eq(6));
        verify(ai).chatCompletionSync(anyString(), org.mockito.ArgumentMatchers.contains("Grounded source text"),
                org.mockito.ArgumentMatchers.eq("test-model"), org.mockito.ArgumentMatchers.eq(4000));
        verify(questions, never()).saveAll(anyList());
    }

    @Test
    void generationRejectsInvalidCertificateTopicAndQuestionTypeBeforeProviderCall() {
        when(certificates.existsById(2L)).thenReturn(false);
        assertEquals(ErrorCode.ENTITY_NOT_FOUND,
                assertThrows(CommonException.class, () -> service.submit(5L, request())).getErrorCode());
        when(certificates.existsById(2L)).thenReturn(true);
        when(topics.findById(3L)).thenReturn(Optional.of(Topic.builder()
                .topicId(3L).certificateId(4L).isActive(true).build()));
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.submit(5L, request())).getErrorCode());
        when(topics.findById(3L)).thenReturn(Optional.of(Topic.builder()
                .topicId(3L).certificateId(2L).isActive(true).build()));
        GenerateQuestionsRequest badType = request();
        badType.setQuestionType("EXECUTE_TEXT");
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.submit(5L, badType)).getErrorCode());
        verify(ai, never()).chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void noKnowledgeChunksFailsGenerationWithoutCallingProvider() {
        when(rag.retrieveKnowledge(eq(2L), eq(3L), anyString(), eq(6)))
                .thenReturn(List.of());

        var response = service.submit(5L, request());

        assertEquals(GenerationStatus.FAILED, response.getStatus());
        assertTrue(response.getErrorMessage().contains("IllegalStateException"));
        verify(ai, never()).chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void providerFailureIsPersistedWithoutRawSensitiveMessage() {
        when(ai.chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IllegalStateException("provider response echoed private essay"));

        var response = service.submit(5L, request());

        assertEquals(GenerationStatus.FAILED, response.getStatus());
        assertTrue(response.getErrorMessage().contains("IllegalStateException"));
        org.junit.jupiter.api.Assertions.assertFalse(response.getErrorMessage().contains("private essay"));
    }

    @Test
    void invalidOrUngroundedGeneratedQuestionIsRejected() {
        when(ai.chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(validResponse(1000));

        var response = service.submit(5L, request());

        assertEquals(GenerationStatus.FAILED, response.getStatus());
        assertTrue(response.getErrorMessage().contains("CommonException"));
        verify(questions, never()).saveAll(anyList());
    }

    @Test
    void applyIsOwnerScopedAndOnlyCreatesDraftQuestionsOnce() {
        ContentGeneration ready = readyGeneration(5L);
        when(generations.findById(8L)).thenReturn(Optional.of(ready));
        assertEquals(ErrorCode.FORBIDDEN_ERROR,
                assertThrows(CommonException.class, () -> service.apply(8L, 6L)).getErrorCode());

        when(generations.save(any(ContentGeneration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        establishCurrentAdmin(5L);
        var applied = service.apply(8L, 5L);
        assertEquals(GenerationStatus.APPLIED, applied.getStatus());
        var captor = org.mockito.ArgumentCaptor.forClass(Question.class);
        verify(questions).saveAll(org.mockito.ArgumentMatchers.argThat(iterable -> {
            java.util.Iterator<Question> iterator = iterable.iterator();
            if (!iterator.hasNext()) return false;
            Question question = iterator.next();
            return !iterator.hasNext() && question.getStatus() == ContentStatus.DRAFT
                    && question.getCertificateId().equals(2L) && question.getTopicId().equals(3L)
                    && question.getCorrectAnswer().equals("\"A\"");
        }));
        assertEquals(GenerationStatus.APPLIED, ready.getStatus());
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.apply(8L, 5L)).getErrorCode());
    }

    private void establishCurrentAdmin(Long id) {
        User user = new User();
        user.setUserId(id);
        user.setUsername("content-admin");
        user.setRole(Role.ROLE_ADMIN);
        user.setEnable(true);
        user.setEmailVerified(true);
        user.setIsDeleted(false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private GenerateQuestionsRequest request() {
        GenerateQuestionsRequest request = new GenerateQuestionsRequest();
        request.setCertificateId(2L);
        request.setTopicId(3L);
        request.setQuestionType("MULTIPLE_CHOICE");
        request.setDifficulty("MEDIUM");
        request.setCount(3);
        request.setInstructions("Only use the topic source.");
        return request;
    }

    private ContentGeneration readyGeneration(Long ownerId) {
        ContentGeneration generation = new ContentGeneration();
        generation.setGenerationId(8L);
        generation.setRequestedBy(ownerId);
        generation.setCertificateId(2L);
        generation.setTopicId(3L);
        generation.setParametersJson("{\"certificateId\":2,\"topicId\":3,\"questionType\":\"MULTIPLE_CHOICE\","
                + "\"difficulty\":\"MEDIUM\",\"count\":3,\"instructions\":\"test\"}");
        generation.setResultJson(validResponse(99));
        generation.setStatus(GenerationStatus.READY);
        return generation;
    }

    private String validResponse(long sourceChunkId) {
        return "[{\"questionType\":\"MULTIPLE_CHOICE\",\"content\":\"Which color is stated?\","
                + "\"options\":[{\"label\":\"A\",\"text\":\"Blue\"},{\"label\":\"B\",\"text\":\"Red\"}],"
                + "\"correctAnswer\":\"A\",\"explanation\":\"The source says blue.\","
                + "\"sourceChunkIds\":[" + sourceChunkId + "]}]";
    }
}
