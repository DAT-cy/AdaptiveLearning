package adaptivelearning.module.billing.dto;

import adaptivelearning.shared.enums.PlanType;
import adaptivelearning.shared.enums.PaymentProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EntitlementsResponse(
        PlanType plan,
        List<String> skills,
        boolean aiEnabled,
        int examLimit,
        int examsUsed,
        int examsRemaining,
        BigDecimal premiumPrice,
        String premiumCurrency,
        boolean premiumAvailable,
        List<PaymentProvider> paymentMethods,
        LocalDateTime premiumActivatedAt) {}
