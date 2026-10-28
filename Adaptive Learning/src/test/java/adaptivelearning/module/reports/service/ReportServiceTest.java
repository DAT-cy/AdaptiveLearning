package adaptivelearning.module.reports.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ReportServiceTest {
    @Test void completionRateUsesGradedAttemptsOverAllAttempts() {
        assertEquals(60d, ReportService.completionRate(3, 5));
        assertEquals(0d, ReportService.completionRate(0, 0));
    }

    @Test void passRateUsesPassedAttemptsOverGradedAttempts() {
        assertEquals(75d, ReportService.passRate(3, 4));
        assertEquals(0d, ReportService.passRate(0, 0));
    }
}
