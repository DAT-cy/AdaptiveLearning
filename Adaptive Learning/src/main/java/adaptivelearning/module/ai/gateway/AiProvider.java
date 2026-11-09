package adaptivelearning.module.ai.gateway;

import reactor.core.publisher.Mono;

public interface AiProvider {

    Mono<String> chatCompletion(String systemPrompt, String userPrompt, String model, int maxTokens);

    String chatCompletionSync(String systemPrompt, String userPrompt, String model, int maxTokens);

    Mono<String> analyzeDocument(String systemPrompt, String userPrompt, byte[] documentBytes, String mimeType, int maxTokens);

    String analyzeDocumentSync(String systemPrompt, String userPrompt, byte[] documentBytes, String mimeType, int maxTokens);
}
