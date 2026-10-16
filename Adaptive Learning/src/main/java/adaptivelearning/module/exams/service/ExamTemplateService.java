package adaptivelearning.module.exams.service;

import adaptivelearning.module.exams.dto.request.ExamTemplateRequest;
import adaptivelearning.module.exams.dto.request.GenerateExamFromTemplateRequest;
import adaptivelearning.module.exams.dto.response.ExamResponse;
import adaptivelearning.module.exams.dto.response.ExamTemplateResponse;
import java.util.List;

public interface ExamTemplateService {
    List<ExamTemplateResponse> getActiveTemplates();
    ExamTemplateResponse createTemplate(ExamTemplateRequest request);
    void deactivateTemplate(Long templateId);
    ExamResponse generateExam(Long templateId, Long userId, GenerateExamFromTemplateRequest request);
}
