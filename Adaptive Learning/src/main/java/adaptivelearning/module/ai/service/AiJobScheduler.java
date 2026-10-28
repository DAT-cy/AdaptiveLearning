package adaptivelearning.module.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiJobScheduler {
    private final AiService aiService;

    @Scheduled(fixedDelayString = "${adaptive.ai.jobs.poll-interval-ms:5000}")
    public void pollPendingJobs() {
        try {
            aiService.processPendingJobs();
        } catch (RuntimeException exception) {
            log.warn("AI job poll failed with {}", exception.getClass().getSimpleName());
        }
    }
}
