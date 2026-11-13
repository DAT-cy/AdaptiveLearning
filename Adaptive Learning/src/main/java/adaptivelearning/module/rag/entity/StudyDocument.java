package adaptivelearning.module.rag.entity;

import adaptivelearning.config.app.BaseEntity;
import adaptivelearning.shared.enums.DocumentStatus;
import adaptivelearning.shared.enums.KnowledgeDocumentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "study_documents") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StudyDocument extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long documentId;
    @Column(nullable = false) private Long userId;
    private Long certificateId;
    private Long topicId;
    /** Optional exam/question-set scope for a scoring-criteria document. */
    private Long examId;
    @Enumerated(EnumType.STRING) @Builder.Default
    @Column(name = "document_type", nullable = false, length = 40)
    private KnowledgeDocumentType documentType = KnowledgeDocumentType.KNOWLEDGE;
    @Column(length = 1000) private String description;
    @Builder.Default
    @Column(name = "is_shared", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean shared = false;
    @Builder.Default
    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean active = true;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200) private String title;
    private String fileName; @Column(length = 1000) private String storagePath; private String mimeType;
    @Column(name = "source_content", columnDefinition = "LONGTEXT")
    private String sourceContent;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(nullable = false) private DocumentStatus status = DocumentStatus.UPLOADED;
    @Builder.Default @Column(name = "chunk_count") private int chunkCount = 0;
    @Column(name = "doc_created_at", nullable = false) @Builder.Default private LocalDateTime docCreatedAt = LocalDateTime.now();
}
