package adaptivelearning.module.exams.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.module.exams.dto.request.ExamTemplateRequest;
import adaptivelearning.module.exams.dto.request.GenerateExamFromTemplateRequest;
import adaptivelearning.module.exams.dto.response.ExamResponse;
import adaptivelearning.module.exams.dto.response.ExamTemplateResponse;
import adaptivelearning.module.exams.service.ExamTemplateService;
import adaptivelearning.utils.ClientUtils;
import adaptivelearning.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ClientUtils.VERSION + "/exam-templates")
@RequiredArgsConstructor
@Tag(name = "Exam templates")
public class ExamTemplateController {

    private final ExamTemplateService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public DefaultRes<List<ExamTemplateResponse>> list() {
        return DefaultRes.res(HttpStatus.OK.value(), "Exam templates retrieved", service.getActiveTemplates());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DefaultRes<ExamTemplateResponse>> create(
            @Valid @RequestBody ExamTemplateRequest request) {
        ExamTemplateResponse response = service.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(HttpStatus.CREATED.value(), "Exam template created", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DefaultRes<Void> deactivate(@PathVariable Long id) {
        service.deactivateTemplate(id);
        return DefaultRes.res(HttpStatus.OK.value(), "Exam template deactivated");
    }

    @PostMapping("/{id}/generate")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<DefaultRes<ExamResponse>> generateExam(
            @PathVariable Long id,
            @Valid @RequestBody GenerateExamFromTemplateRequest request) {
        Long ownerUserId = SecurityUtils.hasRole(adaptivelearning.module.users.entity.Role.ROLE_ADMIN)
                ? null
                : SecurityUtils.getCurrentUserId();
        ExamResponse response = service.generateExam(id, ownerUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(HttpStatus.CREATED.value(), "Exam generated and published", response));
    }
}
