package adaptivelearning.module.examimports.repository;

import adaptivelearning.module.examimports.entity.ExamImport;
import adaptivelearning.module.examimports.entity.ExamImportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamImportRepository extends JpaRepository<ExamImport, Long> {
    List<ExamImport> findByStatusOrderByCreatedAtDesc(ExamImportStatus status);
}
