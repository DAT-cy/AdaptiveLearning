package adaptivelearning.module.examimports.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import adaptivelearning.module.examimports.dto.ExamImportResponse;
import adaptivelearning.module.examimports.dto.ExamImportQuestion;
import adaptivelearning.module.examimports.service.ExamImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class ExamImportAnalysisJobServiceTest {
    private final ExamImportService examImportService = org.mockito.Mockito.mock(ExamImportService.class);
    private final ExamImportAnalysisJobService jobs = new ExamImportAnalysisJobService(examImportService, new ObjectMapper());
    private final CountDownLatch progressReported = new CountDownLatch(1);
    private final CountDownLatch finishJob = new CountDownLatch(1);

    @AfterEach
    void stopJobService() {
        finishJob.countDown();
        jobs.shutdown();
    }

    @Test
    void startReturnsJobAndProgressIsVisibleOnlyToOwner() throws Exception {
        when(examImportService.createWithProgress(eq(7L), eq("Exam"), eq(1L), eq(null),
                any(MultipartFile.class), eq(null), eq(false), any(BiConsumer.class), any(Consumer.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    BiConsumer<Integer, Integer> progress = invocation.getArgument(7);
                    @SuppressWarnings("unchecked")
                    Consumer<List<ExamImportQuestion>> partial = invocation.getArgument(8);
                    progress.accept(0, 2);
                    ExamImportQuestion question = new ExamImportQuestion();
                    question.setSourceQuestionNumber("1");
                    question.setContent("First recognized question");
                    partial.accept(List.of(question));
                    progressReported.countDown();
                    finishJob.await(2, TimeUnit.SECONDS);
                    progress.accept(1, 2);
                    return ExamImportResponse.builder().importId(55L).build();
                });

        var started = jobs.start(7L, "Exam", 1L, null,
                new MockMultipartFile("examFile", "exam.md", "text/markdown", "question".getBytes()),
                null, false);

        assertEquals("PROCESSING", started.getStatus());
        assertEquals(true, progressReported.await(2, TimeUnit.SECONDS));
        var current = jobs.get(started.getJobId(), 7L);
        assertEquals("PROCESSING", current.getStatus());
        assertEquals(0, current.getCompletedChunks());
        assertEquals(2, current.getTotalChunks());
        assertEquals(1, current.getQuestions().size());
        assertEquals("First recognized question", current.getQuestions().get(0).getContent());
        assertThrows(ResponseStatusException.class, () -> jobs.get(started.getJobId(), 8L));

        finishJob.countDown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        do {
            current = jobs.get(started.getJobId(), 7L);
            if ("COMPLETED".equals(current.getStatus())) break;
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        assertEquals("COMPLETED", current.getStatus());
        assertEquals(55L, current.getImportId());
        assertEquals(100, current.getProgress());
    }
}
