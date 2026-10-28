package adaptivelearning.module.ai.repository;

import adaptivelearning.module.ai.entity.AiJob;
import adaptivelearning.shared.enums.AiJobStatus;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {

    Optional<AiJob> findByIdempotencyKey(String key);

    List<AiJob> findByStatus(AiJobStatus status);

    List<AiJob> findByStatusInOrderByJobIdAsc(List<AiJobStatus> statuses);

    @Modifying
    @Query("update AiJob job set job.status = :claimedStatus, job.startedAt = :startedAt "
            + "where job.jobId = :jobId and job.status in :claimableStatuses")
    int claimIfStatusMatches(@Param("jobId") Long jobId,
                             @Param("claimableStatuses") List<AiJobStatus> claimableStatuses,
                             @Param("claimedStatus") AiJobStatus claimedStatus,
                             @Param("startedAt") LocalDateTime startedAt);

    List<AiJob> findByInputRefTypeAndInputRefId(String type, Long id);
}
