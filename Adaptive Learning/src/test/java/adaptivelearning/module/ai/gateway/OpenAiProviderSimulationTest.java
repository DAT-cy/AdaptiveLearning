package adaptivelearning.module.ai.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class OpenAiProviderSimulationTest {

    private String baseUrl;

    @BeforeEach
    void requireSimulationUrl() {
        baseUrl = System.getenv("AI_SIM_BASE_URL");
        Assumptions.assumeTrue(baseUrl != null && !baseUrl.isBlank(),
                "Set AI_SIM_BASE_URL when running against the local Postman AI simulation");
    }

    @Test
    void providerMapsCompatibleChatCompletionResponse() {
        OpenAiProvider provider = provider(null);

        String result = provider.chatCompletionSync("system prompt", "user prompt", "test-model", 128);

        assertEquals("{\"result\":\"ok\"}", result);
    }

    @Test
    void providerErrorIsSurfacedWithoutLeakingConfiguredApiKey() {
        OpenAiProvider provider = provider("provider-error");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> provider.chatCompletionSync("system prompt", "user prompt", "test-model", 128));

        assertTrue(exception.getMessage().contains("400"));
        assertTrue(!exception.getMessage().contains("test-only-key"));
    }

    private OpenAiProvider provider(String responseMode) {
        WebClient.Builder builder = WebClient.builder();
        if (responseMode != null) builder.defaultHeader("x-test-response", responseMode);
        return new OpenAiProvider(builder, new ObjectMapper(), "test-only-key", baseUrl, "test-model");
    }
}
