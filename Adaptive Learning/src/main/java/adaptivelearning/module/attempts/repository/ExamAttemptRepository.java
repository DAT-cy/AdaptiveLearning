package adaptivelearning.module.attempts.repository;

import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.shared.enums.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from ExamAttempt attempt where attempt.attemptId = :attemptId")
    Optional<ExamAttempt> findByIdForUpdate(@Param("attemptId") Long attemptId);

    Optional<ExamAttempt> findByUserIdAndExamId(Long userId, Long examId);

    List<ExamAttempt> findByUserId(Long userId);

    List<ExamAttempt> findByStatus(AttemptStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from ExamAttempt attempt where attempt.status = :status and attempt.expiresAt < :now")
    List<ExamAttempt> findOverdueForUpdate(@Param("status") AttemptStatus status,
                                           @Param("now") LocalDateTime now);

    List<ExamAttempt> findByStatusIn(Collection<AttemptStatus> statuses);

    List<ExamAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    boolean existsByUserIdAndExamIdAndSubmittedAtIsNotNull(Long userId, Long examId);

    List<ExamAttempt> findByStartedAtGreaterThanEqualAndStartedAtLessThan(
            LocalDateTime from, LocalDateTime until);
}
