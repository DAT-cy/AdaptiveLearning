package adaptivelearning.module.examimports.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.module.examimports.dto.ExamImportResponse;
import adaptivelearning.module.examimports.dto.ExamImportAnalysisProgress;
import adaptivelearning.module.examimports.dto.ExamImportReviewRequest;
import adaptivelearning.module.examimports.dto.ExamImportAnswerKeyResult;
import adaptivelearning.module.examimports.dto.ExamImportResourceResponse;
import adaptivelearning.module.examimports.service.impl.ExamImportAnalysisJobService;
import adaptivelearning.module.examimports.service.ExamImportService;
import adaptivelearning.utils.ClientUtils;
import adaptivelearning.utils.SecurityUtils;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final ExamImportAnalysisJobService analysisJobs;

    @PostMapping(value = "/analysis", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DefaultRes<ExamImportAnalysisProgress>> startAnalysis(
            @RequestParam String title,
            @RequestParam Long certificateId,
            @RequestParam(required = false) Long topicId,
            @RequestParam("examFile") MultipartFile examFile,
            @RequestParam(value = "answerFile", required = false) MultipartFile answerFile,
            @RequestParam(defaultValue = "false") boolean answerInExam) {
        ExamImportAnalysisProgress progress = analysisJobs.start(SecurityUtils.getCurrentUserId(), title,
                certificateId, topicId, examFile, answerFile, answerInExam);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(DefaultRes.res(HttpStatus.ACCEPTED.value(), "Exam analysis accepted", progress));
    }

    @GetMapping("/analysis/{jobId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DefaultRes<ExamImportAnalysisProgress> analysisProgress(@PathVariable String jobId) {
        return DefaultRes.res(HttpStatus.OK.value(), "Exam analysis progress",
                analysisJobs.get(jobId, SecurityUtils.getCurrentUserId()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DefaultRes<ExamImportResponse>> create(
            @RequestParam String title,
            @RequestParam Long certificateId,
            @RequestParam(required = false) Long topicId,
            @RequestParam("examFile") MultipartFile examFile,
            @RequestParam(value = "answerFile", required = false) MultipartFile answerFile,
            @RequestParam(defaultValue = "false") boolean answerInExam) {
        ExamImportResponse imported = service.create(SecurityUtils.getCurrentUserId(), title,
                certificateId, topicId, examFile, answerFile, answerInExam);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(HttpStatus.CREATED.value(), "Exam imported for review", imported));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<List<ExamImportResponse>> pending() {
        return DefaultRes.res(HttpStatus.OK.value(), "Pending exam imports retrieved",
                service.pending(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/sets")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DefaultRes<List<ExamImportResponse>> confirmedSets() {
        return DefaultRes.res(HttpStatus.OK.value(), "Confirmed exam sets retrieved", service.confirmed());
    }

    @PatchMapping("/sets/{id}/active")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DefaultRes<ExamImportResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return DefaultRes.res(HttpStatus.OK.value(), "Exam set activity updated", service.setActive(id, active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<ExamImportResponse> get(@PathVariable Long id) {
        return DefaultRes.res(HttpStatus.OK.value(), "Exam import retrieved",
                service.get(id, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/{id}/source")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public ResponseEntity<byte[]> sourceFile(@PathVariable Long id) {
        var file = service.getSourceFile(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDisposition(ContentDisposition.inline().filename(file.fileName(), StandardCharsets.UTF_8).build());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setCacheControl("private, no-store");
        return ResponseEntity.ok().headers(headers).body(file.content());
    }

    @PostMapping(value = "/{id}/resources/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public ResponseEntity<DefaultRes<ExamImportResourceResponse>> uploadSectionAudio(
            @PathVariable Long id,
            @RequestParam String sectionKey,
            @RequestParam String sectionTitle,
            @RequestParam("audioFile") MultipartFile audioFile) {
        ExamImportResourceResponse resource = service.uploadSectionAudio(
                id, sectionKey, sectionTitle, audioFile, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(HttpStatus.CREATED.value(), "Section audio uploaded", resource));
    }

    @PostMapping(value = "/{id}/resources/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public ResponseEntity<DefaultRes<ExamImportResourceResponse>> uploadSectionImage(
            @PathVariable Long id,
            @RequestParam String sectionKey,
            @RequestParam String sectionTitle,
            @RequestParam("imageFile") MultipartFile imageFile) {
        ExamImportResourceResponse resource = service.uploadSectionImage(
                id, sectionKey, sectionTitle, imageFile, SecurityUtils.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(HttpStatus.CREATED.value(), "Section image uploaded", resource));
    }

    @GetMapping("/{id}/resources/{resourceId}/file")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public ResponseEntity<byte[]> sectionAudio(@PathVariable Long id, @PathVariable Long resourceId) {
        var file = service.getResourceFile(id, resourceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDisposition(ContentDisposition.inline().filename(file.fileName(), StandardCharsets.UTF_8).build());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setCacheControl("private, no-store");
        return ResponseEntity.ok().headers(headers).body(file.content());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<Void> delete(@PathVariable Long id) {
        service.deleteOwned(id, SecurityUtils.getCurrentUserId());
        return DefaultRes.res(HttpStatus.OK.value(), "Owned pending exam import deleted");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<ExamImportResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody ExamImportReviewRequest request) {
        return DefaultRes.res(HttpStatus.OK.value(), "Exam import review saved",
                service.updateQuestions(id, request.getQuestions()));
    }

    @PostMapping("/{id}/rescan-missing")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<ExamImportResponse> rescanMissingQuestions(@PathVariable Long id) {
        return DefaultRes.res(HttpStatus.OK.value(), "Missing exam questions rescanned",
                service.rescanMissingQuestions(id));
    }

    @PostMapping(value = "/{id}/answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<ExamImportAnswerKeyResult> importAnswerKey(
            @PathVariable Long id, @RequestParam("answerFile") MultipartFile answerFile) {
        return DefaultRes.res(HttpStatus.OK.value(), "Answer key imported",
                service.importAnswerKey(id, answerFile));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'EXAM_APPROVER')")
    public DefaultRes<ExamImportResponse> confirm(@PathVariable Long id) {
        return DefaultRes.res(HttpStatus.OK.value(), "Exam import confirmed", service.confirm(id));
    }
}
