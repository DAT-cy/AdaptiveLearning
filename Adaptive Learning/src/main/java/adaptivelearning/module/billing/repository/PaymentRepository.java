package adaptivelearning.module.billing.repository;

import adaptivelearning.module.billing.entity.Payment;
import adaptivelearning.shared.enums.PaymentProvider;
import adaptivelearning.shared.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentIdAndUserId(Long paymentId, Long userId);
    Optional<Payment> findByProviderAndProviderPaymentId(PaymentProvider provider, String providerPaymentId);
    Optional<Payment> findByProviderAndProviderTransactionId(PaymentProvider provider, String transactionId);
    List<Payment> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
    List<Payment> findAllByOrderByCreatedAtDesc();
    Optional<Payment> findTopByUserIdAndProviderAndStatusOrderByCreatedAtDesc(
            Long userId, PaymentProvider provider, PaymentStatus status);
}
