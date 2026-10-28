package adaptivelearning.module.tutor.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.tutor.dto.request.AskTutorRequest;
import adaptivelearning.module.tutor.dto.request.CreateConversationRequest;
import adaptivelearning.module.tutor.entity.TutorConversation;
import adaptivelearning.module.tutor.entity.TutorMessage;
import adaptivelearning.module.tutor.repository.ConversationRepository;
import adaptivelearning.module.tutor.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TutorServiceImplTest {

    private ConversationRepository conversations;
    private MessageRepository messages;
    private AiProvider ai;
    private TutorServiceImpl service;

    @BeforeEach
    void setUp() {
        conversations = mock(ConversationRepository.class);
        messages = mock(MessageRepository.class);
        ai = mock(AiProvider.class);
        service = new TutorServiceImpl(conversations, messages, ai, new ObjectMapper());
        when(conversations.save(any(TutorConversation.class))).thenAnswer(invocation -> {
            TutorConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) conversation.setId(10L);
            return conversation;
        });
        when(messages.save(any(TutorMessage.class))).thenAnswer(invocation -> {
            TutorMessage message = invocation.getArgument(0);
            if (message.getId() == null) message.setId(20L);
            return message;
        });
    }

    @Test
    void createConversationUsesCallerAndBoundsTitle() {
        CreateConversationRequest request = new CreateConversationRequest();
        request.setCertificateId(4L);
        request.setTitle("  Writing help  ");

        var response = service.createConversation(5L, request);

        assertEquals(5L, response.getUserId());
        assertEquals("Writing help", response.getTitle());
    }

    @Test
    void ownerCanAskAndResponseIsStoredAsAnswerAndCitations() {
        TutorConversation conversation = conversation(5L, true);
        when(conversations.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(conversation));
        TutorMessage priorMessage = new TutorMessage();
        priorMessage.setConversationId(10L);
        priorMessage.setRole(adaptivelearning.shared.enums.TutorRole.USER);
        priorMessage.setContent("Explain this");
        when(messages.findByConversationIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(priorMessage));
        when(ai.chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn("{\"answer\":\"The concept means...\",\"citations\":[{\"source\":\"lesson\",\"quote\":\"key idea\"}]}");

        AskTutorRequest request = new AskTutorRequest();
        request.setContent("Explain this");
        request.setContextJson("{\"topic\":\"grammar\"}");
        var response = service.ask(5L, 10L, request);

        assertEquals("The concept means...", response.getContent());
        assertTrue(response.getCitationsJson().contains("lesson"));
        verify(ai).chatCompletionSync(anyString(), org.mockito.ArgumentMatchers.contains("SUPPLIED_CONTEXT_JSON"),
                org.mockito.ArgumentMatchers.eq("gpt-4o-mini"), org.mockito.ArgumentMatchers.eq(1200));
    }

    @Test
    void foreignOwnerCannotReadAskOrArchiveConversation() {
        when(conversations.findByIdAndUserId(10L, 6L)).thenReturn(Optional.empty());
        CommonException ask = assertThrows(CommonException.class, () -> service.ask(6L, 10L, request("Question")));
        assertEquals(ErrorCode.ENTITY_NOT_FOUND, ask.getErrorCode());
        assertThrows(CommonException.class, () -> service.getMessages(6L, 10L));
        assertThrows(CommonException.class, () -> service.archive(6L, 10L));
        verify(ai, never()).chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void inactiveConversationAndInvalidContextAreRejectedBeforeProvider() {
        when(conversations.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(conversation(5L, false)));
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.ask(5L, 10L, request("Question"))).getErrorCode());

        when(conversations.findByIdAndUserId(11L, 5L)).thenReturn(Optional.of(conversation(5L, true)));
        AskTutorRequest invalidContext = request("Question");
        invalidContext.setContextJson("not-json");
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.ask(5L, 11L, invalidContext)).getErrorCode());
        verify(ai, never()).chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void providerFailureAndMalformedOutputAreMappedWithoutLeakingProviderBody() {
        when(conversations.findByIdAndUserId(10L, 5L)).thenReturn(Optional.of(conversation(5L, true)));
        when(messages.findByConversationIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(ai.chatCompletionSync(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IllegalStateException("sensitive provider body"));
        CommonException unavailable = assertThrows(CommonException.class,
                () -> service.ask(5L, 10L, request("Question")));
        assertEquals(ErrorCode.AI_PROVIDER_UNAVAILABLE, unavailable.getErrorCode());
        assertTrue(!unavailable.getErrorCode().getMessage().contains("sensitive provider body"));

        doReturn("not-json").when(ai).chatCompletionSync(
                anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
        CommonException invalid = assertThrows(CommonException.class,
                () -> service.ask(5L, 10L, request("Question")));
        assertEquals(ErrorCode.AI_PROVIDER_INVALID_RESPONSE, invalid.getErrorCode());
    }

    private TutorConversation conversation(Long owner, boolean active) {
        TutorConversation conversation = new TutorConversation();
        conversation.setId(10L);
        conversation.setUserId(owner);
        conversation.setActive(active);
        return conversation;
    }

    private AskTutorRequest request(String content) {
        AskTutorRequest request = new AskTutorRequest();
        request.setContent(content);
        return request;
    }
}
