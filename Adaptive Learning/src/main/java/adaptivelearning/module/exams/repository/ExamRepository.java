package adaptivelearning.module.exams.repository;

import adaptivelearning.module.exams.entity.Exam;
import adaptivelearning.shared.enums.ContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select exam from Exam exam where exam.examId = :examId")
    Optional<Exam> findByIdForUpdate(@Param("examId") Long examId);
    List<Exam> findByCertificateIdAndStatus(Long certificateId, ContentStatus status);
    List<Exam> findByStatus(ContentStatus status);

    @Query("select e from Exam e where e.status = :status " +
            "and (:certificateId is null or e.certificateId = :certificateId) " +
            "and (e.ownerUserId is null or e.ownerUserId = :userId)")
    List<Exam> findAvailableToUser(@Param("status") ContentStatus status,
                                   @Param("certificateId") Long certificateId,
                                   @Param("userId") Long userId);
}
