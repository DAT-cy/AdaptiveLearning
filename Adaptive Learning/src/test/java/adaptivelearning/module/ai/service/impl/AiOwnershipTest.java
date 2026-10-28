package adaptivelearning.module.ai.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.ai.dto.request.AiGradeRequest;
import adaptivelearning.module.ai.entity.AiEvaluation;
import adaptivelearning.module.ai.gateway.AiProvider;
import adaptivelearning.module.ai.repository.AiEvaluationRepository;
import adaptivelearning.module.ai.repository.AiJobRepository;
import adaptivelearning.module.ai.repository.RubricRepository;
import adaptivelearning.module.attempts.entity.ExamAttempt;
import adaptivelearning.module.attempts.repository.ExamAttemptRepository;
import adaptivelearning.module.exams.repository.ExamQuestionRepository;
import adaptivelearning.module.exams.repository.ExamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiOwnershipTest {

    @Test
    void userCannotRequestGradingForAnotherUsersAttempt() {
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        when(attempts.findById(9L)).thenReturn(Optional.of(ExamAttempt.builder().attemptId(9L).userId(2L).build()));
        AiServiceImpl service = new AiServiceImpl(mock(AiEvaluationRepository.class), mock(AiJobRepository.class),
                mock(RubricRepository.class), mock(AiProvider.class), new ObjectMapper(), attempts,
                mock(ExamQuestionRepository.class), mock(ExamRepository.class));
        AiGradeRequest request = AiGradeRequest.builder()
                .attemptId(9L).questionId(3L).userAnswerText("answer").build();

        assertThrows(CommonException.class, () -> service.submitGradeJob(request, 1L, false));
    }

    @Test
    void userCannotReadAnotherUsersAiEvaluation() {
        AiEvaluationRepository evaluations = mock(AiEvaluationRepository.class);
        ExamAttemptRepository attempts = mock(ExamAttemptRepository.class);
        when(evaluations.findById(4L)).thenReturn(Optional.of(AiEvaluation.builder().evaluationId(4L).attemptId(9L).build()));
        when(attempts.findById(9L)).thenReturn(Optional.of(ExamAttempt.builder().attemptId(9L).userId(2L).build()));
        AiServiceImpl service = new AiServiceImpl(evaluations, mock(AiJobRepository.class),
                mock(RubricRepository.class), mock(AiProvider.class), new ObjectMapper(), attempts,
                mock(ExamQuestionRepository.class), mock(ExamRepository.class));

        assertThrows(CommonException.class, () -> service.getEvaluation(4L, 1L, false));
    }
}
