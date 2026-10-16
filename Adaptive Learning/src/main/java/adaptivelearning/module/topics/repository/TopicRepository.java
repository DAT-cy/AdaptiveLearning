package adaptivelearning.module.topics.repository;

import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.shared.enums.TopicType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findByCertificateIdAndType(Long certificateId, TopicType type);

    List<Topic> findByParentId(Long parentId);

    List<Topic> findByCertificateId(Long certificateId);

    boolean existsByNameAndCertificateId(String name, Long certificateId);

    Optional<Topic> findByCodeAndCertificateId(String code, Long certificateId);
}
