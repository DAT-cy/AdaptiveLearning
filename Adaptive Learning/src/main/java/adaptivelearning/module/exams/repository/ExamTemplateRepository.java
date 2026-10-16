package adaptivelearning.module.exams.repository;

import adaptivelearning.module.exams.entity.ExamTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamTemplateRepository extends JpaRepository<ExamTemplate, Long> {
    List<ExamTemplate> findByActiveTrueOrderByCreatedAtDesc();
}
