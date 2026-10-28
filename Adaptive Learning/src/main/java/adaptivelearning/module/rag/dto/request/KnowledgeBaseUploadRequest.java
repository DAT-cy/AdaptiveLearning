package adaptivelearning.module.rag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class KnowledgeBaseUploadRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 255)
    private String fileName;

    @NotNull
    @Positive
    private Long certificateId;

    @Positive
    private Long topicId;

    @NotBlank
    @Size(max = 500000)
    private String content;
}
