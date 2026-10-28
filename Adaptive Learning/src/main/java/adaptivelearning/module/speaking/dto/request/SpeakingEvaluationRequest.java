package adaptivelearning.module.speaking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SpeakingEvaluationRequest {

    @NotNull(message = "submissionId is required") @Positive
    private Long submissionId;

    @Positive
    @Positive
    private Long rubricId;
}
