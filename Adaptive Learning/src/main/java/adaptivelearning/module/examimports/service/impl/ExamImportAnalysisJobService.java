package adaptivelearning.module.examimports.service.impl;

import adaptivelearning.module.examimports.dto.ExamImportAnalysisProgress;
import adaptivelearning.module.examimports.dto.ExamImportResponse;
import adaptivelearning.module.examimports.service.ExamImportService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamImportAnalysisJobService {
    private static final long JOB_TTL_MILLIS = TimeUnit.HOURS.toMillis(3);

    private final ExamImportService examImportService;
    private final ConcurrentHashMap<String, Job> jobs = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 2, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(4), runnable -> {
                Thread thread = new Thread(runnable, "exam-import-analysis");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public ExamImportAnalysisProgress start(Long userId, String title, Long certificateId, Long topicId,
                                            MultipartFile examFile, MultipartFile answerFile,
                                            boolean answerInExam) {
        purgeExpiredJobs();
        Upload exam = snapshot(examFile, true);
        Upload answer = answerFile == null || answerFile.isEmpty() ? null : snapshot(answerFile, false);
        String jobId = UUID.randomUUID().toString();
        Job job = new Job(jobId, userId);
        jobs.put(jobId, job);
        try {
            executor.execute(() -> run(job, title, certificateId, topicId, exam, answer, answerInExam));
        } catch (RejectedExecutionException exception) {
            jobs.remove(jobId);
            throw new IllegalStateException("Đang có nhiều đề được phân tích. Vui lòng đợi một lúc rồi thử lại.");
        }
        return job.snapshot();
    }

    public ExamImportAnalysisProgress get(String jobId, Long userId) {
        purgeExpiredJobs();
        Job job = jobs.get(jobId);
        if (job == null || !job.ownerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tiến độ phân tích.");
        }
        return job.snapshot();
    }

    private void run(Job job, String title, Long certificateId, Long topicId,
                     Upload exam, Upload answer, boolean answerInExam) {
        try {
            ExamImportResponse result = examImportService.createWithProgress(
                    job.ownerId, title, certificateId, topicId,
                    exam.asMultipartFile("examFile"), answer == null ? null : answer.asMultipartFile("answerFile"),
                    answerInExam, job::updateProgress);
            job.complete(result.getImportId());
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? "Không thể phân tích bộ đề." : exception.getMessage();
            log.error("Exam import analysis job {} failed: {}", job.id, message, exception);
            job.fail(message);
        }
    }

    private Upload snapshot(MultipartFile file, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) throw new IllegalArgumentException("File đề bắt buộc.");
            return null;
        }
        try {
            return new Upload(file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc file tải lên.", exception);
        }
    }

    private void purgeExpiredJobs() {
        long cutoff = System.currentTimeMillis() - JOB_TTL_MILLIS;
        jobs.entrySet().removeIf(entry -> entry.getValue().finishedAt > 0 && entry.getValue().finishedAt < cutoff);
    }

    @jakarta.annotation.PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    private record Upload(String filename, String contentType, byte[] bytes) {
        MultipartFile asMultipartFile(String name) {
            return new ByteArrayMultipartFile(name, filename, contentType, bytes);
        }
    }

    private static final class Job {
        private final String id;
        private final Long ownerId;
        private volatile String status = "PROCESSING";
        private volatile int progress = 1;
        private volatile int completedChunks;
        private volatile int totalChunks;
        private volatile Long importId;
        private volatile String message = "Đang chuẩn bị file...";
        private volatile String errorMessage;
        private volatile long finishedAt;

        private Job(String id, Long ownerId) {
            this.id = id;
            this.ownerId = ownerId;
        }

        private void updateProgress(int completed, int total) {
            totalChunks = total;
            completedChunks = completed;
            progress = total == 0 ? 3 : 5 + (int) (85.0 * completed / total);
            message = completed >= total ? "Đang lưu bộ đề..."
                    : "Đang phân tích phần " + Math.min(completed + 1, total) + " / " + total + "...";
        }

        private void complete(Long createdImportId) {
            importId = createdImportId;
            progress = 100;
            status = "COMPLETED";
            message = "Phân tích hoàn tất.";
            finishedAt = Instant.now().toEpochMilli();
        }

        private void fail(String error) {
            status = "FAILED";
            errorMessage = error;
            message = "Phân tích thất bại.";
            finishedAt = Instant.now().toEpochMilli();
        }

        private ExamImportAnalysisProgress snapshot() {
            return ExamImportAnalysisProgress.builder().jobId(id).status(status).progress(progress)
                    .completedChunks(completedChunks).totalChunks(totalChunks).importId(importId)
                    .message(message).errorMessage(errorMessage).build();
        }
    }

    private static final class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String filename;
        private final String contentType;
        private final byte[] bytes;

        private ByteArrayMultipartFile(String name, String filename, String contentType, byte[] bytes) {
            this.name = name;
            this.filename = filename;
            this.contentType = contentType;
            this.bytes = bytes;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return filename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes.clone(); }
        @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
        @Override public void transferTo(File dest) throws IOException { java.nio.file.Files.write(dest.toPath(), bytes); }
        @Override public void transferTo(Path dest) throws IOException { java.nio.file.Files.write(dest, bytes); }
    }
}
