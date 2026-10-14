package adaptivelearning.module.rag.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KnowledgeDocumentActiveRequest {
    @NotNull
    private Boolean active;
}
