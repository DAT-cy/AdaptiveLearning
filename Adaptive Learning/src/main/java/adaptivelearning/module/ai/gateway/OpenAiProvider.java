package adaptivelearning.module.ai.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@Slf4j
public class OpenAiProvider implements AiProvider {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final boolean geminiEndpoint;
    private final boolean geminiNativeEndpoint;
    private final String geminiModel;
    private final String baseUrl;

    public OpenAiProvider(WebClient.Builder webClientBuilder,
                          ObjectMapper objectMapper,
                          @Value("${gemini.api-key:${ai.api-key:}}") String apiKey,
                          @Value("${gemini.base-url:${ai.base-url:https://api.openai.com/v1}}") String baseUrl,
                          @Value("${gemini.model:${ai.model:gemini-2.5-flash}}") String geminiModel) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.geminiEndpoint = normalizedBaseUrl.contains("generativelanguage.googleapis.com/v1beta/openai/");
        this.geminiNativeEndpoint = normalizedBaseUrl.contains("generativelanguage.googleapis.com/v1beta/")
                && !geminiEndpoint;
        this.geminiModel = geminiModel;
        this.baseUrl = normalizedBaseUrl;
        this.webClient = webClientBuilder.baseUrl(normalizedBaseUrl).build();
        log.info("Gemini provider initialized: native={}, keyConfigured={}, baseUrl={}, model={}",
                geminiNativeEndpoint, apiKey != null && !apiKey.isBlank(), normalizedBaseUrl, geminiModel);
    }

    @Override
    public Mono<String> chatCompletion(String systemPrompt, String userPrompt, String model, int maxTokens) {
        return executeRequest(systemPrompt, userPrompt, model, maxTokens);
    }

    @Override
    public String chatCompletionSync(String systemPrompt, String userPrompt, String model, int maxTokens) {
        return executeRequest(systemPrompt, userPrompt, model, maxTokens)
                .block(Duration.ofSeconds(120));
    }

    private Mono<String> executeRequest(String systemPrompt, String userPrompt, String model, int maxTokens) {
        log.info("AI provider route: nativeGemini={}, compatibleGemini={}, keyConfigured={}, model={}, baseUrl={}",
                geminiNativeEndpoint, geminiEndpoint, apiKey != null && !apiKey.isBlank(),
                geminiNativeEndpoint ? geminiModel : model, baseUrl);
        if (geminiNativeEndpoint) {
            return executeGeminiRequest(systemPrompt, userPrompt, maxTokens);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (!geminiEndpoint) requestBody.put("max_tokens", maxTokens);

        return webClient.post()
                .uri("chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .map(response -> {
                    if (response.getChoices() == null || response.getChoices().isEmpty()) {
                        throw new RuntimeException("Empty response from AI provider");
                    }
                    return response.getChoices().get(0).getMessage().getContent();
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryable)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorMap(ex -> {
                    if (ex instanceof WebClientResponseException response) {
                        String message = providerErrorMessage(response);
                        log.error("AI provider call failed after retries: {}", message);
                        return new RuntimeException("AI provider error: " + message, ex);
                    }
                    log.error("AI provider call failed after retries: {}", ex.getMessage(), ex);
                    return new RuntimeException("AI provider error: " + ex.getMessage(), ex);
                });
    }

    private Mono<String> executeGeminiRequest(String systemPrompt, String userPrompt, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            return Mono.error(new IllegalStateException("GEMINI_API_KEY is not configured"));
        }
        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt)))),
                "generationConfig", Map.of(
                        "maxOutputTokens", maxTokens,
                        "responseMimeType", "application/json")
        );

        return webClient.post()
                .uri("models/" + geminiModel + ":generateContent")
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(response -> {
                    JsonNode candidates = response.path("candidates");
                    if (!candidates.isArray() || candidates.isEmpty()) {
                        String reason = response.path("promptFeedback").path("blockReason").asText();
                        throw new IllegalStateException(reason.isBlank()
                                ? "Gemini returned no candidates"
                                : "Gemini blocked the prompt: " + reason);
                    }
                    JsonNode parts = candidates.path(0).path("content").path("parts");
                    StringBuilder text = new StringBuilder();
                    for (JsonNode part : parts) {
                        if (part.hasNonNull("text")) text.append(part.path("text").asText());
                    }
                    if (text.isEmpty()) throw new IllegalStateException("Gemini returned no text content");
                    return text.toString();
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryable)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorMap(ex -> {
                    if (ex instanceof WebClientResponseException response) {
                        String message = providerErrorMessage(response);
                        log.error("AI provider call failed after retries: {}", message);
                        return new RuntimeException("AI provider error: " + message, ex);
                    }
                    log.error("AI provider call failed after retries: {}", ex.getMessage(), ex);
                    return new RuntimeException("AI provider error: " + ex.getMessage(), ex);
                });
    }

    private String providerErrorMessage(WebClientResponseException response) {
        String detail = "";
        try {
            JsonNode body = objectMapper.readTree(response.getResponseBodyAsString());
            detail = body.path("error").path("message").asText();
            if (detail.isBlank()) detail = body.path("message").asText();
        } catch (Exception ignored) {
            // Use the status when the provider response is not JSON.
        }
        detail = detail.replaceAll("AIza[0-9A-Za-z_-]{20,}", "[REDACTED]");
        String status = response.getStatusCode() + " " + response.getStatusText();
        return detail.isBlank() ? status : status + ": " + detail;
    }

    private boolean isRetryable(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("429")
                || message.contains("500")
                || message.contains("502")
                || message.contains("503")
                || message.contains("timeout");
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ChatCompletionResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class Choice {
        private int index;
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class Message {
        private String role;
        private String content;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
