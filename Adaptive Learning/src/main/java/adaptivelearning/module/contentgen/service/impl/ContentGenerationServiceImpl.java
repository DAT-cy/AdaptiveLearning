package adaptivelearning.module.contentgen.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.contentgen.dto.request.GenerateQuestionsRequest;
import adaptivelearning.module.contentgen.dto.response.ContentGenerationResponse;
import adaptivelearning.module.contentgen.entity.ContentGeneration;
import adaptivelearning.module.contentgen.repository.ContentGenerationRepository;
import adaptivelearning.module.contentgen.service.ContentGenerationService;
import adaptivelearning.module.questions.entity.Question;
import adaptivelearning.module.questions.repository.QuestionRepository;
import adaptivelearning.module.rag.dto.response.KnowledgeContextResponse;
import adaptivelearning.module.rag.service.RagService;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.shared.enums.ContentStatus;
import adaptivelearning.shared.enums.GenerationStatus;
import adaptivelearning.shared.enums.QuestionDifficulty;
import adaptivelearning.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ContentGenerationServiceImpl implements ContentGenerationService {

    private static final int MAX_QUESTION_LENGTH = 20_000;

    private final ContentGenerationRepository repository;
    private final QuestionRepository questions;
    private final RagService ragService;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;
    private final CertificateRepository certificates;
    private final TopicRepository topics;

    @Value("${gemini.model:${ai.model:gemini-2.5-flash}}")
    private String model;

    @Override
    public ContentGenerationResponse submit(Long userId, GenerateQuestionsRequest request) {
        validateRequest(request);
        if (!certificates.existsById(request.getCertificateId())) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND);
        }
        Topic topic = topics.findById(request.getTopicId())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (!topic.isActive() || !Objects.equals(topic.getCertificateId(), request.getCertificateId())) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }

        ContentGeneration generation = new ContentGeneration();
        generation.setRequestedBy(userId);
        generation.setCertificateId(request.getCertificateId());
        generation.setTopicId(request.getTopicId());
        generation.setGenerationType("QUESTIONS");
        generation.setPrompt(request.getInstructions());
        generation.setParametersJson(toJson(request));
        generation.setStatus(GenerationStatus.PENDING);
        generation.setModel(model);
        generation.setItemCount(request.getCount());
        ContentGeneration saved = repository.save(generation);

        // This MVP performs retrieval and provider generation in the request thread.
        processOne(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ContentGenerationResponse get(Long userId, Long id) {
        ContentGeneration generation = findOwned(userId, id);
        return toResponse(generation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentGenerationResponse> list(Long userId) {
        return repository.findByRequestedByOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Override
    public ContentGenerationResponse apply(Long id, Long userId) {
        ContentGeneration generation = findOwned(userId, id);
        if (generation.getStatus() != GenerationStatus.READY) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }

        JsonNode generated;
        try {
            generated = objectMapper.readTree(generation.getResultJson());
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        GenerateQuestionsRequest request = fromParameters(generation);
        validateGeneratedBatch(generated, request, embeddedSourceIds(generated));

        List<Question> drafts = new ArrayList<>();
        for (JsonNode item : generated) {
            JsonNode answer = item.path("correctAnswer");
            drafts.add(Question.builder()
                    .certificateId(generation.getCertificateId())
                    .topicId(generation.getTopicId())
                    .questionType(item.path("questionType").asText())
                    .difficulty(parseDifficulty(request.getDifficulty()))
                    .content(item.path("content").asText().trim())
                    .contentJson(item.path("sourceChunkIds").toString())
                    .optionsJson(item.path("options").toString())
                    .correctAnswer(answer.isTextual()
                            ? objectMapper.valueToTree(answer.asText()).toString() : answer.toString())
                    .explanation(item.path("explanation").asText(null))
                    .points(1.0)
                    .estimatedTimeSeconds(60)
                    .status(ContentStatus.DRAFT)
                    .version(1)
                    .createdBy(SecurityUtils.getCurrentUsername())
                    .tags("AI_GENERATED")
                    .build());
        }
        questions.saveAll(drafts);
        generation.setStatus(GenerationStatus.APPLIED);
        return toResponse(repository.save(generation));
    }

    @Override
    public void processPending() {
        repository.findByStatus(GenerationStatus.PENDING).forEach(this::processOne);
    }

    private void processOne(ContentGeneration generation) {
        generation.setStatus(GenerationStatus.PROCESSING);
        generation.setStartedAt(LocalDateTime.now());
        repository.save(generation);
        try {
            GenerateQuestionsRequest request = fromParameters(generation);
            String query = request.getQuestionType() + " " + request.getDifficulty() + " " + request.getInstructions();
            List<KnowledgeContextResponse> sources = ragService.retrieveKnowledge(
                    generation.getCertificateId(), generation.getTopicId(), query, 6);
            if (sources.isEmpty()) throw new IllegalStateException("No relevant knowledge chunks");

            String context = sources.stream()
                    .map(source -> "[chunkId=" + source.getChunkId() + "; title=" + source.getTitle()
                            + "; chunk=" + source.getChunkIndex() + "]\n" + source.getContent())
                    .collect(Collectors.joining("\n\n"));
            String systemPrompt = "Create original practice questions grounded ONLY in the provided knowledge excerpts. "
                    + "Do not add facts absent from them or claim questions are official. Return ONLY a JSON array. "
                    + "Each object must contain questionType, content, options (array of {label,text}, or [] for non-choice), "
                    + "correctAnswer, explanation, and sourceChunkIds copied from the excerpts. No markdown.";
            String userPrompt = "Exam: " + generation.getCertificateId() + "\nTopic: " + generation.getTopicId()
                    + "\nQuestion type: " + request.getQuestionType() + "\nCount: " + request.getCount()
                    + "\nDifficulty: " + request.getDifficulty() + "\nAdmin instructions: " + request.getInstructions()
                    + "\n\nKnowledge excerpts:\n" + context;
            String raw = aiProvider.chatCompletionSync(systemPrompt, userPrompt, model, 4000);
            Set<Long> allowedSources = sources.stream().map(KnowledgeContextResponse::getChunkId)
                    .collect(Collectors.toSet());
            JsonNode parsed = validateGeneratedBatch(objectMapper.readTree(raw), request, allowedSources);
            generation.setResultJson(parsed.toString());
            generation.setStatus(GenerationStatus.READY);
            generation.setItemCount(parsed.size());
        } catch (Exception exception) {
            generation.setStatus(GenerationStatus.FAILED);
            generation.setErrorMessage("Generation failed (" + exception.getClass().getSimpleName() + ")");
            log.warn("Content generation {} failed with {}",
                    generation.getGenerationId(), exception.getClass().getSimpleName());
        } finally {
            generation.setCompletedAt(LocalDateTime.now());
            repository.save(generation);
        }
    }

    private JsonNode validateGeneratedBatch(JsonNode batch, GenerateQuestionsRequest request,
                                            Set<Long> allowedSourceIds) {
        if (batch == null || !batch.isArray() || batch.isEmpty() || batch.size() > request.getCount()) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        for (JsonNode item : batch) validateGeneratedQuestion(item, request, allowedSourceIds);
        return batch;
    }

    private void validateGeneratedQuestion(JsonNode item, GenerateQuestionsRequest request,
                                           Set<Long> allowedSourceIds) {
        JsonNode content = item.path("content");
        JsonNode questionType = item.path("questionType");
        JsonNode options = item.path("options");
        JsonNode answer = item.path("correctAnswer");
        JsonNode explanation = item.path("explanation");
        JsonNode sourceIds = item.path("sourceChunkIds");
        if (!item.isObject() || !content.isTextual() || content.asText().isBlank()
                || content.asText().length() > MAX_QUESTION_LENGTH || !questionType.isTextual()
                || !questionType.asText().equalsIgnoreCase(request.getQuestionType())
                || !options.isArray() || answer.isMissingNode() || answer.isNull()
                || !explanation.isTextual() || !sourceIds.isArray() || sourceIds.isEmpty()) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        for (JsonNode sourceId : sourceIds) {
            if (!sourceId.canConvertToLong() || !allowedSourceIds.contains(sourceId.asLong())) {
                throw new CommonException(ErrorCode.BAD_REQUEST);
            }
        }

        if ("MULTIPLE_CHOICE".equalsIgnoreCase(request.getQuestionType())) {
            if (options.isEmpty() || !answer.isTextual()) throw new CommonException(ErrorCode.BAD_REQUEST);
            boolean answerExists = false;
            for (JsonNode option : options) {
                if (!option.isObject() || !option.path("label").isTextual()
                        || option.path("label").asText().isBlank()
                        || !option.path("text").isTextual() || option.path("text").asText().isBlank()) {
                    throw new CommonException(ErrorCode.BAD_REQUEST);
                }
                if (answer.asText().equals(option.path("label").asText())) answerExists = true;
            }
            if (!answerExists) throw new CommonException(ErrorCode.BAD_REQUEST);
        } else if (!options.isEmpty()) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private Set<Long> embeddedSourceIds(JsonNode generated) {
        if (generated == null || !generated.isArray()) throw new CommonException(ErrorCode.BAD_REQUEST);
        Set<Long> ids = new java.util.HashSet<>();
        for (JsonNode item : generated) {
            JsonNode sourceIds = item.path("sourceChunkIds");
            if (!sourceIds.isArray()) throw new CommonException(ErrorCode.BAD_REQUEST);
            for (JsonNode sourceId : sourceIds) {
                if (!sourceId.canConvertToLong()) throw new CommonException(ErrorCode.BAD_REQUEST);
                ids.add(sourceId.asLong());
            }
        }
        return ids;
    }

    private void validateRequest(GenerateQuestionsRequest request) {
        if (request == null || request.getCertificateId() == null || request.getTopicId() == null
                || request.getCount() < 1 || request.getCount() > 20
                || request.getQuestionType() == null || request.getQuestionType().isBlank()
                || !Set.of("MULTIPLE_CHOICE", "TRUE_FALSE", "TRUE_FALSE_NOT_GIVEN", "FILL_BLANK", "ESSAY")
                        .contains(request.getQuestionType().trim().toUpperCase(Locale.ROOT))) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        parseDifficulty(request.getDifficulty());
    }

    private QuestionDifficulty parseDifficulty(String value) {
        if (value == null || value.isBlank()) return QuestionDifficulty.MEDIUM;
        try {
            return QuestionDifficulty.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private ContentGeneration findOwned(Long userId, Long id) {
        ContentGeneration generation = repository.findById(id)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        if (!Objects.equals(generation.getRequestedBy(), userId)) {
            throw new CommonException(ErrorCode.FORBIDDEN_ERROR);
        }
        return generation;
    }

    private String toJson(GenerateQuestionsRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private GenerateQuestionsRequest fromParameters(ContentGeneration generation) {
        try {
            return objectMapper.readValue(generation.getParametersJson(), GenerateQuestionsRequest.class);
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
    }

    private ContentGenerationResponse toResponse(ContentGeneration generation) {
        return ContentGenerationResponse.builder()
                .generationId(generation.getGenerationId()).requestedBy(generation.getRequestedBy())
                .certificateId(generation.getCertificateId()).topicId(generation.getTopicId())
                .generationType(generation.getGenerationType()).prompt(generation.getPrompt())
                .parametersJson(generation.getParametersJson()).resultJson(generation.getResultJson())
                .status(generation.getStatus()).model(generation.getModel()).itemCount(generation.getItemCount())
                .errorMessage(generation.getErrorMessage()).startedAt(generation.getStartedAt())
                .completedAt(generation.getCompletedAt()).build();
    }
}
