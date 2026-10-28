package adaptivelearning.module.tutor.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.tutor.dto.request.AskTutorRequest;
import adaptivelearning.module.tutor.dto.request.CreateConversationRequest;
import adaptivelearning.module.tutor.dto.response.ConversationResponse;
import adaptivelearning.module.tutor.dto.response.MessageResponse;
import adaptivelearning.module.tutor.entity.TutorConversation;
import adaptivelearning.module.tutor.entity.TutorMessage;
import adaptivelearning.module.tutor.repository.ConversationRepository;
import adaptivelearning.module.tutor.repository.MessageRepository;
import adaptivelearning.module.tutor.service.TutorService;
import adaptivelearning.shared.enums.TutorRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TutorServiceImpl implements TutorService {

    private static final String MODEL = "gpt-4o-mini";
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;

    @Override
    public ConversationResponse createConversation(Long userId, CreateConversationRequest request) {
        TutorConversation conversation = new TutorConversation();
        conversation.setUserId(userId);
        conversation.setCertificateId(request.getCertificateId());
        conversation.setTitle(request.getTitle() == null || request.getTitle().isBlank()
                ? "Tutor conversation" : request.getTitle().trim());
        return toConversation(conversations.save(conversation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(Long userId) {
        return conversations.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toConversation).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long userId, Long conversationId) {
        requireOwner(userId, conversationId);
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessage).toList();
    }

    @Override
    public MessageResponse ask(Long userId, Long conversationId, AskTutorRequest request) {
        TutorConversation conversation = requireOwner(userId, conversationId);
        if (!conversation.isActive()) throw new CommonException(ErrorCode.BAD_REQUEST);
        validateContext(request.getContextJson());

        TutorMessage userMessage = new TutorMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setRole(TutorRole.USER);
        userMessage.setContent(request.getContent().trim());
        userMessage.setTokenCount(request.getContent().trim().length());
        messages.save(userMessage);
        if (request.getContextJson() != null && !request.getContextJson().isBlank()) {
            conversation.setContextJson(request.getContextJson());
        }

        List<TutorMessage> history = messages.findByConversationIdOrderByCreatedAtAsc(conversationId);
        int start = Math.max(0, history.size() - 10);
        StringBuilder prompt = new StringBuilder("Use only the supplied context and conversation; do not invent facts. "
                + "Return a JSON object with answer (string) and citations (array of {source, quote}).\n");
        if (conversation.getContextJson() != null && !conversation.getContextJson().isBlank()) {
            prompt.append("SUPPLIED_CONTEXT_JSON:\n").append(conversation.getContextJson()).append("\n");
        }
        for (int index = start; index < history.size(); index++) {
            prompt.append(history.get(index).getRole()).append(": ")
                    .append(history.get(index).getContent()).append("\n");
        }

        final String raw;
        try {
            raw = aiProvider.chatCompletionSync("You are a careful educational tutor.",
                    prompt.toString(), MODEL, 1200);
        } catch (RuntimeException exception) {
            log.warn("Tutor provider failed for conversation {} with {}",
                    conversationId, exception.getClass().getSimpleName());
            throw new CommonException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
        }
        JsonNode response = parseResponse(raw);

        TutorMessage assistantMessage = new TutorMessage();
        assistantMessage.setConversationId(conversationId);
        assistantMessage.setRole(TutorRole.ASSISTANT);
        assistantMessage.setContent(response.path("answer").asText());
        assistantMessage.setCitationsJson(response.path("citations").toString());
        assistantMessage.setModel(MODEL);
        assistantMessage.setTokenCount(assistantMessage.getContent().length());
        return toMessage(messages.save(assistantMessage));
    }

    @Override
    public void archive(Long userId, Long conversationId) {
        TutorConversation conversation = requireOwner(userId, conversationId);
        conversation.setActive(false);
        conversations.save(conversation);
    }

    private TutorConversation requireOwner(Long userId, Long conversationId) {
        return conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private void validateContext(String contextJson) {
        if (contextJson == null || contextJson.isBlank()) return;
        try {
            objectMapper.readTree(contextJson);
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private JsonNode parseResponse(String raw) {
        try {
            JsonNode response = objectMapper.readTree(raw);
            if (!response.isObject() || !response.path("answer").isTextual()
                    || response.path("answer").asText().isBlank()
                    || !response.path("citations").isArray()) {
                throw new IllegalArgumentException("Invalid tutor response");
            }
            return response;
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.AI_PROVIDER_INVALID_RESPONSE);
        }
    }

    private ConversationResponse toConversation(TutorConversation conversation) {
        return ConversationResponse.builder().id(conversation.getId()).userId(conversation.getUserId())
                .certificateId(conversation.getCertificateId()).title(conversation.getTitle())
                .isActive(conversation.isActive())
                .createdAt(conversation.getCreatedAt() == null ? null
                        : conversation.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .messageCount(messages.countByConversationId(conversation.getId())).build();
    }

    private MessageResponse toMessage(TutorMessage message) {
        return MessageResponse.builder().id(message.getId()).conversationId(message.getConversationId())
                .role(message.getRole()).content(message.getContent()).citationsJson(message.getCitationsJson())
                .model(message.getModel()).tokenCount(message.getTokenCount())
                .createdAt(message.getCreatedAt()).build();
    }
}
