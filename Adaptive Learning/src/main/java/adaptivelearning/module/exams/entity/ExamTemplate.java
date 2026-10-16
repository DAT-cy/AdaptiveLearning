package adaptivelearning.module.exams.entity;

import adaptivelearning.config.app.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.OrderBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "exam_templates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exam_template_title_topic", columnNames = {"topic_id", "title"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExamTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;

    @Column(name = "certificate_id", nullable = false)
    private Long certificateId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "target_score_min")
    private Double targetScoreMin;

    @Column(name = "target_score_max")
    private Double targetScoreMax;

    @Column(name = "target_score_unit", length = 30)
    private String targetScoreUnit;

    @Column(name = "passing_score", nullable = false)
    @Builder.Default
    private double passingScore = 0;

    @Builder.Default
    @OrderBy("sectionOrder ASC")
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ExamTemplateSection> sections = new ArrayList<>();

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;
}
