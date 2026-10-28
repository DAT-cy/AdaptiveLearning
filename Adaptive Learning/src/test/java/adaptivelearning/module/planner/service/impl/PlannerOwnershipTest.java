package adaptivelearning.module.planner.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.planner.dto.request.UpdateTaskRequest;
import adaptivelearning.module.planner.entity.PlanTask;
import adaptivelearning.module.planner.entity.StudyPlan;
import adaptivelearning.module.planner.repository.PlanTaskRepository;
import adaptivelearning.module.planner.repository.StudyPlanRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlannerOwnershipTest {

    @Test
    void userCannotUpdateAnotherUsersPlanTask() {
        PlanTaskRepository tasks = mock(PlanTaskRepository.class);
        StudyPlanRepository plans = mock(StudyPlanRepository.class);
        CertificateRepository certificates = mock(CertificateRepository.class);
        when(tasks.findById(20L)).thenReturn(Optional.of(PlanTask.builder().taskId(20L).planId(10L).build()));
        when(plans.findById(10L)).thenReturn(Optional.of(StudyPlan.builder().planId(10L).userId(2L).build()));

        PlannerServiceImpl service = new PlannerServiceImpl(plans, tasks, certificates);

        assertThrows(CommonException.class,
                () -> service.updateTask(1L, 20L, new UpdateTaskRequest()));
    }
}
