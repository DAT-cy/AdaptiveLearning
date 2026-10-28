package adaptivelearning.module.adaptive.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class AdaptiveUpdateDispatcherTest {

    @Test
    void dispatchInvokesProfileUpdateAndContainsSynchronousFailure() {
        AdaptiveService adaptiveService = mock(AdaptiveService.class);
        AdaptiveUpdateDispatcher dispatcher = new AdaptiveUpdateDispatcher(adaptiveService);
        when(adaptiveService.updateProfile(5L, 91L)).thenThrow(new IllegalStateException("profile store unavailable"));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> dispatcher.updateAfterAttempt(5L, 91L));

        verify(adaptiveService).updateProfile(5L, 91L);
    }
}
