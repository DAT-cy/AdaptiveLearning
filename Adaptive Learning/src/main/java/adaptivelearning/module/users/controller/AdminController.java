package adaptivelearning.module.users.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.config.response.ResponseMessage;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.module.users.service.UserService;
import adaptivelearning.shared.enums.PlanType;
import adaptivelearning.module.analytics.dto.response.ProgressResponse;
import adaptivelearning.module.analytics.dto.response.AdminAnalyticsResponse;
import adaptivelearning.module.analytics.service.AnalyticsService;
import adaptivelearning.module.billing.service.BillingProperties;
import adaptivelearning.module.attempts.dto.response.AttemptResponse;
import adaptivelearning.module.attempts.service.AttemptService;
import adaptivelearning.utils.ClientUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ClientUtils.VERSION + "/admin/users")
@Tag(name = "03. Users management")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final UserService userService;
    private final AttemptService attemptService;
    private final AnalyticsService analyticsService;
    private final BillingProperties billingProperties;

    @GetMapping
    public DefaultRes<List<AccountSummary>> listUsers() {
        List<AccountSummary> users = userMapper.findAllForAdmin().stream()
                .map(user -> toSummary(user, billingProperties.freeExamLimit()))
                .toList();
        return DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.GET_LIST, users);
    }

    @GetMapping("/analytics")
    public DefaultRes<AdminAnalyticsResponse> analytics(
            @RequestParam(defaultValue = "30d") String period,
            @RequestParam(required = false) Long certificateId) {
        return DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.GET_ONE,
                analyticsService.getAdminOverview(period, certificateId));
    }

    @GetMapping("/{userId}/attempts")
    public DefaultRes<List<AttemptResponse>> listUserAttempts(@PathVariable Long userId) {
        return DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.GET_LIST,
                attemptService.getHistory(userId));
    }

    @GetMapping("/{userId}/progress")
    public DefaultRes<ProgressResponse> userProgress(
            @PathVariable Long userId, @RequestParam Long certificateId) {
        return DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.GET_ONE,
                analyticsService.getProgressOverview(userId, certificateId));
    }

    @PostMapping
    public ResponseEntity<DefaultRes<AccountSummary>> createUser(
            @Valid @RequestBody CreateAccountRequest request) {
        User user = userService.createManagedUser(request.getUsername(), request.getPassword(), request.getRole());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(HttpStatus.CREATED.value(), ResponseMessage.CREATED,
                        toSummary(user, billingProperties.freeExamLimit())));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<DefaultRes<Void>> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        if (userMapper.updateRole(userId, request.getRole()) == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.UPDATED));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<DefaultRes<Void>> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateStatusRequest request) {
        if (userMapper.updateEnabled(userId, request.getEnabled()) == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.UPDATED));
    }

    private static AccountSummary toSummary(User user, int freeExamLimit) {
        return new AccountSummary(user.getUserId(), user.getUsername(), user.getRole(),
                Boolean.TRUE.equals(user.getEnable()), user.isEmailVerified(),
                user.getPlan() == null ? PlanType.FREE : user.getPlan(), user.getFreeExamsUsed(), freeExamLimit);
    }

    public record AccountSummary(Long userId, String username, Role role, boolean enabled,
                                 boolean emailVerified, PlanType plan, int freeExamsUsed, int freeExamLimit) {}

    @Getter
    @Setter
    public static class CreateAccountRequest {
        @NotBlank
        @Size(max = 50)
        private String username;

        @NotBlank
        @Size(min = 8, max = 72)
        private String password;

        @NotNull
        private Role role;
    }

    @Getter
    @Setter
    public static class UpdateRoleRequest {
        @NotNull
        private Role role;
    }

    @Getter
    @Setter
    public static class UpdateStatusRequest {
        @NotNull
        private Boolean enabled;
    }
}
