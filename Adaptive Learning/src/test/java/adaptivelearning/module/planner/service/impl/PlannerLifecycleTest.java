package adaptivelearning.module.planner.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.planner.dto.request.CreatePlanRequest;
import adaptivelearning.module.planner.dto.request.UpdateTaskRequest;
import adaptivelearning.module.planner.entity.PlanTask;
import adaptivelearning.module.planner.entity.StudyPlan;
import adaptivelearning.module.planner.repository.PlanTaskRepository;
import adaptivelearning.module.planner.repository.StudyPlanRepository;
import adaptivelearning.shared.enums.PlanStatus;
import adaptivelearning.shared.enums.PlanTaskStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlannerLifecycleTest {
    private StudyPlanRepository plans;
    private PlanTaskRepository tasks;
    private CertificateRepository certificates;
    private PlannerServiceImpl service;

    @BeforeEach
    void setUp() {
        plans = mock(StudyPlanRepository.class);
        tasks = mock(PlanTaskRepository.class);
        certificates = mock(CertificateRepository.class);
        service = new PlannerServiceImpl(plans, tasks, certificates);
    }

    @Test
    void rejectsPastExamDateAndInvalidHours() {
        CreatePlanRequest request = validRequest();
        request.setExamDate(LocalDate.now().minusDays(1));
        assertThrows(CommonException.class, () -> service.createPlan(1L, request));
        request.setExamDate(LocalDate.now().plusDays(30));
        request.setHoursPerWeek(81);
        assertThrows(CommonException.class, () -> service.createPlan(1L, request));
        verify(plans, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsUnknownCertificate() {
        CreatePlanRequest request = validRequest();
        request.setCertificateId(99L);
        when(certificates.existsById(99L)).thenReturn(false);
        assertThrows(CommonException.class, () -> service.createPlan(1L, request));
    }

    @Test
    void taskReadRequiresOwningPlan() {
        when(plans.findById(10L)).thenReturn(Optional.of(StudyPlan.builder().planId(10L).userId(2L).build()));
        assertThrows(CommonException.class, () -> service.getTasks(1L, 10L));
        verify(tasks, never()).findByPlanIdOrderByScheduledDateAsc(10L);
    }

    @Test
    void activeOwnerCanUpdateTask() {
        PlanTask task = PlanTask.builder().taskId(20L).planId(10L).status(PlanTaskStatus.PENDING).description("Study").build();
        when(tasks.findById(20L)).thenReturn(Optional.of(task));
        when(plans.findById(10L)).thenReturn(Optional.of(StudyPlan.builder().planId(10L).userId(1L).status(PlanStatus.ACTIVE).build()));
        when(tasks.save(task)).thenReturn(task);
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setStatus(PlanTaskStatus.COMPLETED);
        request.setNotes("Reviewed");
        assertEquals(PlanTaskStatus.COMPLETED, service.updateTask(1L, 20L, request).getStatus());
    }

    @Test
    void closedPlanCannotUpdateTasksOrBeCompletedAgain() {
        PlanTask task = PlanTask.builder().taskId(20L).planId(10L).status(PlanTaskStatus.PENDING).build();
        StudyPlan plan = StudyPlan.builder().planId(10L).userId(1L).status(PlanStatus.ARCHIVED).build();
        when(tasks.findById(20L)).thenReturn(Optional.of(task));
        when(plans.findById(10L)).thenReturn(Optional.of(plan));
        assertThrows(CommonException.class, () -> service.updateTask(1L, 20L, new UpdateTaskRequest()));
        assertThrows(CommonException.class, () -> service.completePlan(1L, 10L));
        verify(tasks, never()).save(task);
    }

    private CreatePlanRequest validRequest() {
        CreatePlanRequest request = new CreatePlanRequest();
        request.setTitle("My plan");
        request.setExamDate(LocalDate.now().plusDays(30));
        request.setHoursPerWeek(5);
        return request;
    }
}
