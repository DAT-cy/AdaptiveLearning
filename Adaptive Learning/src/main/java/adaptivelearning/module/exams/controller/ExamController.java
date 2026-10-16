package adaptivelearning.module.exams.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.config.response.ResponseMessage;
import adaptivelearning.config.response.StatusCode;
import adaptivelearning.module.exams.dto.response.ExamDetailResponse;
import adaptivelearning.module.exams.dto.response.ExamResponse;
import adaptivelearning.module.exams.service.ExamService;
import adaptivelearning.utils.ClientUtils;
import adaptivelearning.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ClientUtils.VERSION + "/exams")
@Tag(name = "Exams", description = "Exam management")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping
    @Operation(summary = "List exams", description = "Optionally filters by certificateId. Without filter, returns all exams.")
    public ResponseEntity<DefaultRes<List<ExamResponse>>> list(@RequestParam(required = false) Long certificateId) {
        boolean admin = SecurityUtils.hasRole(adaptivelearning.module.users.entity.Role.ROLE_ADMIN);
        List<ExamResponse> data = admin
                ? examService.getAll(certificateId)
                : examService.getAvailableToUser(certificateId, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(DefaultRes.res(StatusCode.OK, ResponseMessage.GET_LIST, data));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exam summary by id")
    public ResponseEntity<DefaultRes<ExamResponse>> get(@PathVariable Long id) {
        ExamResponse data = examService.getVisibleById(id, SecurityUtils.getCurrentUserId(),
                SecurityUtils.hasRole(adaptivelearning.module.users.entity.Role.ROLE_ADMIN));
        return ResponseEntity.ok(DefaultRes.res(StatusCode.OK, ResponseMessage.GET_ONE, data));
    }

    @GetMapping("/{id}/detail")
    @Operation(summary = "Get exam detail including questions")
    public ResponseEntity<DefaultRes<ExamDetailResponse>> detail(@PathVariable Long id) {
        ExamDetailResponse data = examService.getVisibleDetail(id, SecurityUtils.getCurrentUserId(),
                SecurityUtils.hasRole(adaptivelearning.module.users.entity.Role.ROLE_ADMIN));
        return ResponseEntity.ok(DefaultRes.res(StatusCode.OK, ResponseMessage.GET_ONE, data));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an exam")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DefaultRes<Void>> delete(@PathVariable Long id) {
        examService.delete(id);
        return ResponseEntity.ok(DefaultRes.res(StatusCode.OK, ResponseMessage.DELETED));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive an exam (PUBLISHED → ARCHIVED)")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DefaultRes<ExamResponse>> archive(@PathVariable Long id) {
        ExamResponse data = examService.archive(id);
        return ResponseEntity.ok(DefaultRes.res(StatusCode.OK, ResponseMessage.UPDATED, data));
    }

}
