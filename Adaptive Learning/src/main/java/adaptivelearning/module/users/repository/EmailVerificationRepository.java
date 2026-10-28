package adaptivelearning.module.users.repository;

import adaptivelearning.module.users.entity.EmailVerification;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select verification from EmailVerification verification where lower(verification.email) = lower(:email)")
    Optional<EmailVerification> findByEmailIgnoreCaseForUpdate(@Param("email") String email);
}
