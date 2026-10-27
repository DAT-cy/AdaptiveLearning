package adaptivelearning.module.billing.repository;

import adaptivelearning.module.billing.entity.PaymentWebhookEvent;
import adaptivelearning.shared.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    boolean existsByProviderAndEventId(PaymentProvider provider, String eventId);
    java.util.Optional<PaymentWebhookEvent> findByProviderAndEventId(PaymentProvider provider, String eventId);
}
