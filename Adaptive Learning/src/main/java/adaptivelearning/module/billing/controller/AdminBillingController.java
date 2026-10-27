package adaptivelearning.module.billing.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.config.response.ResponseMessage;
import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.module.billing.entity.SubscriptionPlan;
import adaptivelearning.module.billing.repository.PaymentRepository;
import adaptivelearning.module.billing.repository.SubscriptionPlanRepository;
import adaptivelearning.module.billing.service.BillingProperties;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.shared.enums.PaymentProvider;
import adaptivelearning.shared.enums.PaymentStatus;
import adaptivelearning.shared.enums.PlanType;
import adaptivelearning.utils.ClientUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ClientUtils.VERSION + "/admin")
@Tag(name = "04. Admin Billing and Plans management")
@RequiredArgsConstructor
public class AdminBillingController {

    private final SubscriptionPlanRepository planRepository;
    private final PaymentRepository paymentRepository;
    private final UserMapper userMapper;
    private final BillingProperties billingProperties;

    // --- 1. QUẢN LÝ CÁC GÓI CƯỚC (SUBSCRIPTION PLANS) ---

    @GetMapping("/plans")
    public DefaultRes<List<SubscriptionPlan>> listPlans() {
        return DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.GET_LIST,
                planRepository.findAllByOrderByPlanIdAsc());
    }

    @PostMapping("/plans")
    public ResponseEntity<DefaultRes<SubscriptionPlan>> createPlan(
            @Valid @RequestBody PlanUpsertRequest request) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(request.getCode().trim().toUpperCase());
        plan.setName(request.getName().trim());
        plan.setDescription(request.getDescription());
        plan.setPrice(request.getPrice());
        plan.setCurrency(request.getCurrency() == null ? "USD" : request.getCurrency().trim().toUpperCase());
        plan.setBillingCycle(request.getBillingCycle() == null ? "LIFETIME" : request.getBillingCycle());
        plan.setExamLimit(request.getExamLimit() == null ? -1 : request.getExamLimit());
        plan.setFeaturesJson(request.getFeaturesJson());
        plan.setIsActive(request.getIsActive() == null ? true : request.getIsActive());

        SubscriptionPlan saved = planRepository.save(plan);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(HttpStatus.CREATED.value(), ResponseMessage.CREATED, saved));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<DefaultRes<SubscriptionPlan>> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody PlanUpsertRequest request) {
        SubscriptionPlan plan = planRepository.findById(id).orElse(null);
        if (plan == null) return ResponseEntity.notFound().build();

        if (request.getName() != null && !request.getName().isBlank()) plan.setName(request.getName().trim());
        if (request.getDescription() != null) plan.setDescription(request.getDescription().trim());
        if (request.getPrice() != null) plan.setPrice(request.getPrice());
        if (request.getCurrency() != null) plan.setCurrency(request.getCurrency().trim().toUpperCase());
        if (request.getBillingCycle() != null) plan.setBillingCycle(request.getBillingCycle());
        if (request.getExamLimit() != null) plan.setExamLimit(request.getExamLimit());
        if (request.getFeaturesJson() != null) plan.setFeaturesJson(request.getFeaturesJson());
        if (request.getIsActive() != null) plan.setIsActive(request.getIsActive());

        SubscriptionPlan updated = planRepository.save(plan);
        return ResponseEntity.ok(DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.UPDATED, updated));
    }

    @PatchMapping("/plans/{id}/toggle")
    public ResponseEntity<DefaultRes<SubscriptionPlan>> togglePlan(@PathVariable Long id) {
        SubscriptionPlan plan = planRepository.findById(id).orElse(null);
        if (plan == null) return ResponseEntity.notFound().build();

        plan.setIsActive(!Boolean.TRUE.equals(plan.getIsActive()));
        SubscriptionPlan updated = planRepository.save(plan);
        return ResponseEntity.ok(DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.UPDATED, updated));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<DefaultRes<Void>> deletePlan(@PathVariable Long id) {
        SubscriptionPlan plan = planRepository.findById(id).orElse(null);
        if (plan == null) return ResponseEntity.notFound().build();
        if ("FREE".equalsIgnoreCase(plan.getCode()) || "PREMIUM".equalsIgnoreCase(plan.getCode())) {
            return ResponseEntity.badRequest().body(DefaultRes.res(HttpStatus.BAD_REQUEST.value(), "Cannot delete default plans"));
        }
        planRepository.delete(plan);
        return ResponseEntity.ok(DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.DELETED));
    }

    // --- 2. QUẢN LÝ USER THANH TOÁN (USER BILLING & SUBSCRIPTIONS) ---

    @GetMapping("/billing/users")
    public DefaultRes<List<UserBillingSummary>> listUserBillings() {
        List<User> users = userMapper.findAllForAdmin();
        List<Payment> allPayments = paymentRepository.findAllByOrderByCreatedAtDesc();

        Map<Long, List<Payment>> paymentsByUser = allPayments.stream()
                .collect(Collectors.groupingBy(Payment::getUserId));

        List<UserBillingSummary> summaries = users.stream().map(user -> {
            List<Payment> userPayments = paymentsByUser.getOrDefault(user.getUserId(), List.of());
            BigDecimal totalSpent = userPayments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int paymentCount = userPayments.size();
            LocalDateTime lastPaymentDate = userPayments.stream()
                    .map(Payment::getCreatedAt)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            return new UserBillingSummary(
                    user.getUserId(),
                    user.getUsername(),
                    user.getRole(),
                    Boolean.TRUE.equals(user.getEnable()),
                    user.isEmailVerified(),
                    user.getPlan() == null ? PlanType.FREE : user.getPlan(),
                    user.getPremiumActivatedAt(),
                    user.getFreeExamsUsed(),
                    billingProperties.freeExamLimit(),
                    totalSpent,
                    paymentCount,
                    lastPaymentDate
            );
        }).toList();

        return DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.GET_LIST, summaries);
    }

    @GetMapping("/billing/users/{userId}/payments")
    public DefaultRes<List<UserPaymentDetail>> getUserPayments(@PathVariable Long userId) {
        List<UserPaymentDetail> payments = paymentRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(p -> new UserPaymentDetail(
                        p.getPaymentId(),
                        p.getUserId(),
                        p.getProvider(),
                        p.getProviderPaymentId(),
                        p.getProviderTransactionId(),
                        p.getAmount(),
                        p.getCurrency(),
                        p.getStatus(),
                        p.getCreatedAt()
                ))
                .toList();
        return DefaultRes.res(HttpStatus.OK.value(), ResponseMessage.GET_LIST, payments);
    }

    @PostMapping("/billing/users/{userId}/reset-quota")
    public ResponseEntity<DefaultRes<Void>> resetUserQuota(@PathVariable Long userId) {
        userMapper.resetFreeExamQuota(userId);
        return ResponseEntity.ok(DefaultRes.res(HttpStatus.OK.value(), "Free exam quota reset successfully"));
    }

    // --- DTOs ---

    public record UserBillingSummary(
            Long userId,
            String username,
            Role role,
            boolean enabled,
            boolean emailVerified,
            PlanType plan,
            LocalDateTime premiumActivatedAt,
            int freeExamsUsed,
            int freeExamLimit,
            BigDecimal totalSpent,
            int paymentCount,
            LocalDateTime lastPaymentDate
    ) {}

    public record UserPaymentDetail(
            Long paymentId,
            Long userId,
            PaymentProvider provider,
            String providerPaymentId,
            String providerTransactionId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            LocalDateTime createdAt
    ) {}

    @Getter
    @Setter
    public static class PlanUpsertRequest {
        @NotBlank
        private String code;
        @NotBlank
        private String name;
        private String description;
        @NotNull
        private BigDecimal price;
        private String currency;
        private String billingCycle;
        private Integer examLimit;
        private String featuresJson;
        private Boolean isActive;
    }
}
