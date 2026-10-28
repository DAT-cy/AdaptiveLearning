package adaptivelearning.module.planner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import adaptivelearning.module.planner.dto.request.CreatePlanRequest;
import adaptivelearning.module.planner.dto.request.UpdateTaskRequest;
import adaptivelearning.shared.enums.PlanStatus;
import adaptivelearning.shared.enums.PlanTaskStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class PlannerFlowIntegrationTest {
    @Autowired
    private PlannerService plannerService;

    @Test
    @Transactional
    void learnerCanCreateReviewTasksUpdateThemAndCompleteThePlan() {
        Long userId = System.currentTimeMillis();
        CreatePlanRequest request = new CreatePlanRequest();
        request.setTitle("Integration planner flow");
        request.setExamDate(LocalDate.now().plusDays(7));
        request.setHoursPerWeek(2);
        request.setTargetScore(7.0);

        var plan = plannerService.createPlan(userId, request);
        var tasks = plannerService.getTasks(userId, plan.getPlanId());

        assertTrue(tasks.size() > 0);
        assertEquals(tasks.size(), plan.getTaskCount());
        for (int index = 0; index < tasks.size(); index++) {
            UpdateTaskRequest update = new UpdateTaskRequest();
            update.setStatus(index == 0 ? PlanTaskStatus.COMPLETED : PlanTaskStatus.SKIPPED);
            plannerService.updateTask(userId, tasks.get(index).getTaskId(), update);
        }

        var completed = plannerService.completePlan(userId, plan.getPlanId());

        assertEquals(PlanStatus.COMPLETED, completed.getStatus());
        TestTransaction.flagForRollback();
    }
}
