package adaptivelearning.module.rag.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KnowledgeDocumentDetailResponse {
    Long documentId;
    String title;
    String fileName;
    String content;
    List<String> chunks;
    Long certificateId;
    Long topicId;
    int chunkCount;
    boolean active;
    LocalDateTime createdAt;
}
