package adaptivelearning.module.billing.service;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.billing.dto.EntitlementsResponse;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.shared.enums.PlanType;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EntitlementService {
    private static final List<String> FREE_SKILLS = List.of("LISTENING", "READING", "WRITING");
    private static final List<String> PREMIUM_SKILLS = List.of("LISTENING", "READING", "WRITING", "SPEAKING");

    private final UserMapper userMapper;
    private final TopicRepository topicRepository;
    private final BillingProperties billingProperties;
    private final PaymentStrategyRegistry paymentStrategyRegistry;

    @Transactional(readOnly = true)
    public EntitlementsResponse getEntitlements(Long userId) {
        User user = requireUser(userId);
        boolean premium = isPremiumOrAdmin(user);
        int limit = billingProperties.freeExamLimit();
        int used = Math.max(0, user.getFreeExamsUsed());
        var price = billingProperties.premiumPrice();
        List<adaptivelearning.shared.enums.PaymentProvider> methods = paymentStrategyRegistry.configuredProviders();
        return new EntitlementsResponse(
                premium ? PlanType.PREMIUM : PlanType.FREE,
                premium ? PREMIUM_SKILLS : FREE_SKILLS,
                premium,
                premium ? -1 : limit,
                used,
                premium ? -1 : Math.max(0, limit - used),
                price,
                billingProperties.premiumCurrency(),
                price != null && !methods.isEmpty(),
                methods,
                user.getPremiumActivatedAt());
    }

    public void requireAi(Long userId) {
        if (!isPremiumOrAdmin(requireUser(userId))) throw new CommonException(ErrorCode.PREMIUM_REQUIRED);
    }

    public boolean hasPremiumAccess(Long userId) {
        return isPremiumOrAdmin(requireUser(userId));
    }

    public void requireSkill(Long userId, String code, String name) {
        if (isPremiumOrAdmin(requireUser(userId))) return;
        if (isSpeaking(code) || isSpeaking(name)) throw new CommonException(ErrorCode.PREMIUM_REQUIRED);
    }

    public void requireTopicAccess(Long userId, Long topicId) {
        User user = requireUser(userId);
        if (topicId == null || isPremiumOrAdmin(user)) return;
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic != null && (isSpeaking(topic.getCode()) || isSpeaking(topic.getName()))) {
            throw new CommonException(ErrorCode.PREMIUM_REQUIRED);
        }
    }

    public void consumeExamQuota(Long userId) {
        User user = requireUser(userId);
        if (isPremiumOrAdmin(user)) return;
        int limit = billingProperties.freeExamLimit();
        if (userMapper.consumeFreeExamQuota(userId, limit) == 1) return;
        User latest = requireUser(userId);
        if (isPremiumOrAdmin(latest)) return;
        throw new CommonException(ErrorCode.FREE_LIMIT_REACHED);
    }

    public void grantPremium(Long userId) {
        if (userMapper.updatePlan(userId, PlanType.PREMIUM, LocalDateTime.now()) == 0) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }

    public void revokePremium(Long userId) {
        if (userMapper.downgradePremiumAndSyncQuota(userId, billingProperties.freeExamLimit()) == 0) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }

    private User requireUser(Long userId) {
        return userMapper.findById(userId).orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private boolean isPremiumOrAdmin(User user) {
        return user.getRole() == Role.ROLE_ADMIN || isPremium(user);
    }

    private boolean isPremium(User user) {
        return user.getPlan() == PlanType.PREMIUM;
    }

    private boolean isSpeaking(String value) {
        if (value == null || value.isBlank()) return false;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT);
        return normalized.contains("SPEAKING") || normalized.equals("NOI") || normalized.contains("KY NANG NOI");
    }
}
