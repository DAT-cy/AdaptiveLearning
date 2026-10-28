package adaptivelearning.module.rag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentUploadRequest {
    @NotBlank @Size(max = 200) private String title;
    @Positive private Long certificateId;
    @NotBlank @Size(max = 500000) private String content;
}
