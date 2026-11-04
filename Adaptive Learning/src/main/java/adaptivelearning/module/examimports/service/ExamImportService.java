package adaptivelearning.module.examimports.service;

import adaptivelearning.module.examimports.dto.ExamImportQuestion;
import adaptivelearning.module.examimports.dto.ExamImportResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ExamImportService {
    ExamImportResponse create(Long userId, String title, Long certificateId, Long topicId,
                              MultipartFile examFile, MultipartFile answerFile);
    List<ExamImportResponse> pending();
    ExamImportResponse get(Long importId);
    ExamImportResponse updateQuestions(Long importId, List<ExamImportQuestion> questions);
    ExamImportResponse confirm(Long importId);
}
