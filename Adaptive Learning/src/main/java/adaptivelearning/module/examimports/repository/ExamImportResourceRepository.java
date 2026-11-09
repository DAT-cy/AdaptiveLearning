package adaptivelearning.module.examimports.repository;

import adaptivelearning.module.examimports.entity.ExamImportResource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamImportResourceRepository extends JpaRepository<ExamImportResource, Long> {
    List<ExamImportResource> findByImportIdOrderByUploadedAtAsc(Long importId);

    Optional<ExamImportResource> findByResourceIdAndImportId(Long resourceId, Long importId);

    void deleteByImportId(Long importId);
}
