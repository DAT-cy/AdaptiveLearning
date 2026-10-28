package adaptivelearning.module.speaking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SpeakingSubmissionRequest {

    @NotNull @Positive
    private Long attemptId;

    @NotNull(message = "questionId is required") @Positive
    private Long questionId;

    @NotBlank(message = "transcriptText is required for MVP") @Size(max = 20000)
    private String transcriptText;
}
