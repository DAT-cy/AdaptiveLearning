package adaptivelearning.module.adaptive.service;

import adaptivelearning.module.adaptive.service.AdaptiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdaptiveUpdateDispatcher {
    private final AdaptiveService adaptiveService;

    @Async("adaptiveTaskExecutor")
    public void updateAfterAttempt(Long userId, Long attemptId) {
        try {
            adaptiveService.updateProfile(userId, attemptId);
        } catch (Exception error) {
            log.warn("Adaptive update failed for attempt {}: {}", attemptId, error.getMessage());
        }
    }
}
