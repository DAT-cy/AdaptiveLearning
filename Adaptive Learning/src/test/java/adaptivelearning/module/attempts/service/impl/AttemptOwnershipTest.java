package adaptivelearning.module.attempts.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.attempts.dto.request.ViolationRequest;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.AttemptAnswerRepository;
import adaptivelearning.module.attempts.repository.AttemptEventRepository;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.billing.service.EntitlementService;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import adaptivelearning.module.questions.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AttemptOwnershipTest {

    @Test
    void userCannotRecordViolationAgainstAnotherUsersAttempt() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        when(attempts.findByIdForUpdate(9L)).thenReturn(Optional.of(ExamAttempt.builder().attemptId(9L).userId(2L).build()));
        AttemptServiceImpl service = new AttemptServiceImpl(attempts, mock(AttemptAnswerRepository.class),
                mock(AttemptEventRepository.class), mock(ExamRepository.class), mock(ExamQuestionRepository.class),
                mock(QuestionRepository.class), new ObjectMapper(), mock(EntitlementService.class));

        assertThrows(CommonException.class, () -> service.recordViolation(9L, 1L, false, new ViolationRequest()));
    }
}
