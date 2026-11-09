package adaptivelearning.module.examimports.entity;

import adaptivelearning.config.app.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "exam_import_resources", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exam_import_resource_file", columnNames = {"import_id", "storage_key"})
})
@Getter
@Setter
public class ExamImportResource extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resourceId;

    @Column(name = "import_id", nullable = false)
    private Long importId;

    @Column(name = "section_key", nullable = false, length = 64)
    private String sectionKey;

    @Column(name = "section_title", nullable = false, length = 120)
    private String sectionTitle;

    @Column(name = "resource_type", nullable = false, length = 24)
    private String resourceType = "AUDIO";

    @Column(name = "storage_key", nullable = false, length = 36)
    private String storageKey;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
