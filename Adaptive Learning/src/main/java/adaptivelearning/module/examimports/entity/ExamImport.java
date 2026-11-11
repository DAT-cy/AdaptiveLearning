package adaptivelearning.module.examimports.entity;

import adaptivelearning.config.app.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "exam_imports")
@Getter
@Setter
public class ExamImport extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long importId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private Long certificateId;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String sourceContent;

    @Column(columnDefinition = "LONGTEXT")
    private String answerContent;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String questionsJson;

    @Column(name = "source_file_key", length = 1000)
    private String sourceFileKey;

    @Column(name = "source_file_name", length = 255)
    private String sourceFileName;

    @Column(name = "source_file_content_type", length = 120)
    private String sourceFileContentType;

    @Column(name = "source_file_size")
    private Long sourceFileSize;

    @Column(name = "source_file_sha256", length = 64)
    private String sourceFileSha256;

    @Column(nullable = false)
    private boolean answerGenerated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ExamImportStatus status = ExamImportStatus.PENDING_REVIEW;

    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false)
    private Long uploadedBy;
}
