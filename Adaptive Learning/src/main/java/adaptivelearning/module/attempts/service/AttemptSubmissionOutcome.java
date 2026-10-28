package adaptivelearning.module.attempts.service;

import adaptivelearning.module.attempts.dto.response.AttemptResultResponse;

public record AttemptSubmissionOutcome(AttemptResultResponse result, boolean newlySubmitted) {}
