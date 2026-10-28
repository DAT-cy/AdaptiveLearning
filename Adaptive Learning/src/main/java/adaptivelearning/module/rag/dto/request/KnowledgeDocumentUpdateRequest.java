package adaptivelearning.module.rag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class KnowledgeDocumentUpdateRequest {
    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    @Positive
    private Long certificateId;

    @Positive
    private Long topicId;

    @NotBlank
    @Size(max = 500000)
    private String content;
}
