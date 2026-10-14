package adaptivelearning.module.rag.repository;
import adaptivelearning.module.rag.entity.StudyDocument; import adaptivelearning.shared.enums.DocumentStatus; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface StudyDocumentRepository extends JpaRepository<StudyDocument,Long> { List<StudyDocument> findByUserIdAndSharedFalseOrderByDocCreatedAtDesc(Long userId); List<StudyDocument> findByCertificateIdAndSharedTrueOrderByDocCreatedAtDesc(Long certificateId); List<StudyDocument> findByStatus(DocumentStatus status); }
