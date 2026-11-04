package adaptivelearning.module.examimports.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.module.examimports.dto.ExamImportResponse;
import adaptivelearning.module.examimports.dto.ExamImportReviewRequest;
import adaptivelearning.module.examimports.service.ExamImportService;
import adaptivelearning.utils.ClientUtils;
import adaptivelearning.utils.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ClientUtils.VERSION + "/exam-imports")
@RequiredArgsConstructor
public class ExamImportController {
    private final ExamImportService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DefaultRes<ExamImportResponse>> create(
            @RequestParam String title,
            @RequestParam Long certificateId,
            @RequestParam Long topicId,
            @RequestParam("examFile") MultipartFile examFile,
            @RequestParam(value = "answerFile", required = false) MultipartFile answerFile) {
        ExamImportResponse imported = service.create(SecurityUtils.getCurrentUserId(), title,
                certificateId, topicId, examFile, answerFile);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(HttpStatus.CREATED.value(), "Exam imported for review", imported));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<List<ExamImportResponse>> pending() {
        return DefaultRes.res(HttpStatus.OK.value(), "Pending exam imports retrieved", service.pending());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<ExamImportResponse> get(@PathVariable Long id) {
        return DefaultRes.res(HttpStatus.OK.value(), "Exam import retrieved", service.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<ExamImportResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody ExamImportReviewRequest request) {
        return DefaultRes.res(HttpStatus.OK.value(), "Exam import review saved",
                service.updateQuestions(id, request.getQuestions()));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<ExamImportResponse> confirm(@PathVariable Long id) {
        return DefaultRes.res(HttpStatus.OK.value(), "Exam import confirmed", service.confirm(id));
    }
}
