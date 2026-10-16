package adaptivelearning.module.exams.repository;

import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.shared.enums.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByCertificateIdAndStatus(Long certificateId, ContentStatus status);
    List<Exam> findByStatus(ContentStatus status);

    @Query("select e from Exam e where e.status = :status " +
            "and (:certificateId is null or e.certificateId = :certificateId) " +
            "and (e.ownerUserId is null or e.ownerUserId = :userId)")
    List<Exam> findAvailableToUser(@Param("status") ContentStatus status,
                                   @Param("certificateId") Long certificateId,
                                   @Param("userId") Long userId);
}
