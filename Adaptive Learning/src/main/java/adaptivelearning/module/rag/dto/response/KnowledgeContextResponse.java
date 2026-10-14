package adaptivelearning.module.rag.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KnowledgeContextResponse {
    Long chunkId;
    Long documentId;
    String title;
    int chunkIndex;
    String content;
}
