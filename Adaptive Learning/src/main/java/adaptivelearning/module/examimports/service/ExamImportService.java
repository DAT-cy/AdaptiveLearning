package adaptivelearning.module.examimports.service;

import adaptivelearning.module.examimports.dto.ExamImportQuestion;
import adaptivelearning.module.examimports.dto.ExamImportResponse;
import adaptivelearning.module.examimports.dto.ExamImportSourceFile;
import adaptivelearning.module.examimports.dto.ExamImportResourceResponse;
import adaptivelearning.module.examimports.dto.ExamImportAnswerKeyResult;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.springframework.web.multipart.MultipartFile;

public interface ExamImportService {
    ExamImportResponse create(Long userId, String title, Long certificateId, Long topicId,
                              MultipartFile examFile, MultipartFile answerFile);

    default ExamImportResponse create(Long userId, String title, Long certificateId, Long topicId,
                                      MultipartFile examFile, MultipartFile answerFile, boolean answerInExam) {
        return create(userId, title, certificateId, topicId, examFile, answerFile);
    }

    default ExamImportResponse createWithProgress(Long userId, String title, Long certificateId, Long topicId,
                                                  MultipartFile examFile, MultipartFile answerFile,
                                                  boolean answerInExam, BiConsumer<Integer, Integer> progress) {
        return create(userId, title, certificateId, topicId, examFile, answerFile, answerInExam);
    }

    default ExamImportResponse createWithProgress(Long userId, String title, Long certificateId, Long topicId,
                                                  MultipartFile examFile, MultipartFile answerFile,
                                                  boolean answerInExam, BiConsumer<Integer, Integer> progress,
                                                  Consumer<List<ExamImportQuestion>> partialQuestions) {
        return createWithProgress(userId, title, certificateId, topicId, examFile, answerFile,
                answerInExam, progress);
    }
    List<ExamImportResponse> pending();
    List<ExamImportResponse> pending(Long viewerId);
    ExamImportResponse get(Long importId);
    ExamImportResponse get(Long importId, Long viewerId);

    ExamImportSourceFile getSourceFile(Long importId);
    ExamImportResourceResponse uploadSectionAudio(Long importId, String sectionKey, String sectionTitle,
                                                  MultipartFile audioFile, Long userId);
    ExamImportResourceResponse uploadSectionImage(Long importId, String sectionKey, String sectionTitle,
                                                  MultipartFile imageFile, Long userId);
    ExamImportSourceFile getResourceFile(Long importId, Long resourceId);
    void deleteOwned(Long importId, Long viewerId);
    ExamImportResponse updateQuestions(Long importId, List<ExamImportQuestion> questions);
    ExamImportResponse rescanMissingQuestions(Long importId);
    ExamImportAnswerKeyResult importAnswerKey(Long importId, MultipartFile answerFile);
    ExamImportResponse confirm(Long importId);
}
