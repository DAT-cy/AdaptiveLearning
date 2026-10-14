package adaptivelearning.module.rag.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KnowledgeDocumentDetailResponse {
    Long documentId;
    String title;
    String fileName;
    String content;
    Long certificateId;
    Long topicId;
    int chunkCount;
    boolean active;
    LocalDateTime createdAt;
}
