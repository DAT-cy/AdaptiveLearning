package adaptivelearning.module.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.billing.dto.EntitlementsResponse;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.shared.enums.PlanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EntitlementServiceTest {
    private UserMapper userMapper;
    private EntitlementService service;
    private User user;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        BillingProperties properties = new BillingProperties();
        ReflectionTestUtils.setField(properties, "premiumPriceValue", "120000");
        ReflectionTestUtils.setField(properties, "premiumCurrencyValue", "VND");
        ReflectionTestUtils.setField(properties, "freeExamLimitValue", 5);
        service = new EntitlementService(userMapper, mock(TopicRepository.class), properties,
                mock(PaymentStrategyRegistry.class));
        user = new User();
        user.setUserId(7L);
        user.setUsername("student@example.test");
        user.setRole(Role.ROLE_USER);
        user.setEnable(true);
        user.setEmailVerified(true);
        user.setIsDeleted(false);
        user.setPlan(PlanType.FREE);
        when(userMapper.findById(7L)).thenReturn(java.util.Optional.of(user));
    }

    @Test
    void freeQuotaUsesAtomicCounterAndRejectsWhenLimitReached() {
        when(userMapper.consumeFreeExamQuota(7L, 5)).thenReturn(1, 1, 1, 1, 1, 0);
        for (int index = 0; index < 5; index++) service.consumeExamQuota(7L);

        assertThrows(CommonException.class, () -> service.consumeExamQuota(7L));
        verify(userMapper, org.mockito.Mockito.times(6)).consumeFreeExamQuota(7L, 5);
    }

    @Test
    void premiumHasUnlimitedQuotaAndSpeakingAndAiAccess() {
        user.setPlan(PlanType.PREMIUM);

        service.consumeExamQuota(7L);
        service.requireSkill(7L, "SPEAKING", "Nói");
        service.requireAi(7L);

        verify(userMapper, never()).consumeFreeExamQuota(7L, 5);
    }

    @Test
    void freePlanCannotAccessSpeakingOrTutor() {
        assertThrows(CommonException.class, () -> service.requireSkill(7L, "SPEAKING", "Speaking"));
        assertThrows(CommonException.class, () -> service.requireSkill(7L, "NOI", "Nói"));
        assertThrows(CommonException.class, () -> service.requireAi(7L));
    }

    @Test
    void premiumEntitlementIsPermanentAndUsesConfiguredPrice() {
        user.setPlan(PlanType.PREMIUM);
        var entitlements = service.getEntitlements(7L);

        assertEquals(PlanType.PREMIUM, entitlements.plan());
        assertEquals(-1, entitlements.examLimit());
        assertEquals(-1, entitlements.examsRemaining());
        assertEquals("120000", entitlements.premiumPrice().toPlainString());
        assertEquals("VND", entitlements.premiumCurrency());
    }
}
