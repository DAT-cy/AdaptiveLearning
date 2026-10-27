package adaptivelearning.module.billing.service;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.billing.payment.PaymentStrategy;
import adaptivelearning.shared.enums.PaymentProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyRegistry {
    private final Map<PaymentProvider, PaymentStrategy> strategies = new EnumMap<>(PaymentProvider.class);

    public PaymentStrategyRegistry(List<PaymentStrategy> strategies) {
        strategies.forEach(strategy -> this.strategies.put(strategy.provider(), strategy));
    }

    public PaymentStrategy get(PaymentProvider provider) {
        PaymentStrategy strategy = strategies.get(provider);
        if (strategy == null || !strategy.isConfigured()) {
            throw new CommonException(ErrorCode.PAYMENT_CONFIGURATION_MISSING);
        }
        return strategy;
    }

    public List<PaymentProvider> configuredProviders() {
        return strategies.values().stream().filter(PaymentStrategy::isConfigured)
                .map(PaymentStrategy::provider).sorted().toList();
    }
}
