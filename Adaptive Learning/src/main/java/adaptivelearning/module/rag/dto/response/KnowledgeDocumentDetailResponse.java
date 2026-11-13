package adaptivelearning.module.rag.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import adaptivelearning.shared.enums.KnowledgeDocumentType;

@Value
@Builder
public class KnowledgeDocumentDetailResponse {
    Long documentId;
    String title;
    String fileName;
    String description;
    KnowledgeDocumentType documentType;
    String content;
    List<String> chunks;
    Long certificateId;
    Long topicId;
    Long examId;
    int chunkCount;
    boolean active;
    LocalDateTime createdAt;
}
