package adaptivelearning.module.adaptive.service;

import adaptivelearning.module.adaptive.dto.request.BktEstimateRequest;
import adaptivelearning.module.adaptive.dto.response.BktEstimateResponse;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class BktClient {
    private final WebClient webClient;
    private final Duration timeout;

    @Autowired
    public BktClient(
            WebClient.Builder builder,
            @Value("${adaptive.bkt.base-url:http://localhost:8001}") String baseUrl,
            @Value("${adaptive.bkt.timeout-ms:30000}") long timeoutMs) {
        this(builder, baseUrl, Duration.ofMillis(timeoutMs));
    }

    public BktClient(WebClient.Builder builder, String baseUrl, Duration timeout) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.timeout = timeout;
    }

    public Optional<BktEstimateResponse> estimate(BktEstimateRequest request) {
        try {
            BktEstimateResponse response = webClient.post()
                    .uri("/v1/mastery/estimate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(BktEstimateResponse.class)
                    .block(timeout);
            return Optional.ofNullable(response);
        } catch (Exception error) {
            log.warn("pyBKT service unavailable; adaptive heuristic remains active: {}",
                    error.getMessage());
            return Optional.empty();
        }
    }
}
