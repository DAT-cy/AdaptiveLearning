package adaptivelearning.module.attempts.repository;

import adaptivelearning.module.attempts.entity.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    List<AttemptAnswer> findByAttemptId(Long attemptId);

    List<AttemptAnswer> findByAttemptIdIn(Collection<Long> attemptIds);

    Optional<AttemptAnswer> findByAttemptIdAndQuestionId(Long attemptId, Long questionId);

    long countByAttemptIdAndIsCorrect(Long attemptId, Boolean isCorrect);
}
