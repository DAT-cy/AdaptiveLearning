package adaptivelearning.module.ai.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class AiJobSchedulerTest {

    @Test
    void scheduledPollDelegatesToAiJobProcessorAndContainsPollFailure() {
        AiService aiService = mock(AiService.class);
        AiJobScheduler scheduler = new AiJobScheduler(aiService);
        doThrow(new IllegalStateException("database failure")).when(aiService).processPendingJobs();

        assertDoesNotThrow(scheduler::pollPendingJobs);

        verify(aiService).processPendingJobs();
    }
}
