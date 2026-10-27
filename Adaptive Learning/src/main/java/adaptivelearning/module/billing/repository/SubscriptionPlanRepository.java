package adaptivelearning.module.billing.repository;

import adaptivelearning.module.billing.entity.SubscriptionPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByCode(String code);
    List<SubscriptionPlan> findAllByOrderByPlanIdAsc();
}
