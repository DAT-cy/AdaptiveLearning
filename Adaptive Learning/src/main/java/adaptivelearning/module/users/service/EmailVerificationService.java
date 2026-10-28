package adaptivelearning.module.users.service;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.users.dto.response.EmailVerificationResponse;
import adaptivelearning.module.users.entity.EmailVerification;
import adaptivelearning.module.users.repository.EmailVerificationRepository;
import adaptivelearning.module.users.repository.UserMapper;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {

    private static final int CODE_TTL_MINUTES = 5;
    private static final int RESEND_DELAY_SECONDS = 60;
    private static final int MAX_SENDS_PER_HOUR = 5;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final EmailVerificationRepository verificationRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${MAIL_FROM:}")
    private String configuredFrom;

    public EmailVerificationResponse issue(String rawEmail) {
        String email = normalize(rawEmail);
        UserIdentity identity = findIdentityForUpdate(email);
        if (identity == null) throw new CommonException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
        if (identity.emailVerified()) throw new CommonException(ErrorCode.USERNAME_ALREADY_EXISTS);
        return createAndSend(email);
    }

    public EmailVerificationResponse resend(String rawEmail) {
        String email = normalize(rawEmail);
        UserIdentity identity = findIdentityForUpdate(email);
        if (identity == null || identity.emailVerified()) {
            LocalDateTime now = LocalDateTime.now();
            return new EmailVerificationResponse(email, now.plusMinutes(CODE_TTL_MINUTES),
                    now.plusSeconds(RESEND_DELAY_SECONDS), false);
        }
        return createAndSend(email);
    }

    @Transactional(noRollbackFor = CommonException.class)
    public EmailVerificationResponse verify(String rawEmail, String code) {
        String email = normalize(rawEmail);
        UserIdentity identity = findIdentityForUpdate(email);
        if (identity == null || identity.emailVerified()) {
            throw new CommonException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
        }
        EmailVerification verification = verificationRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new CommonException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));
        if (verification.getVerifiedAt() != null) {
            throw new CommonException(ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (!verification.getExpiresAt().isAfter(now)) {
            throw new CommonException(ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
        }
        if (verification.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new CommonException(ErrorCode.EMAIL_VERIFICATION_RATE_LIMITED);
        }
        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.setFailedAttempts(verification.getFailedAttempts() + 1);
            verificationRepository.save(verification);
            throw new CommonException(ErrorCode.EMAIL_VERIFICATION_CODE_INVALID);
        }
        if (userMapper.updateEmailVerified(email, true) == 0) {
            throw new CommonException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
        }
        verification.setVerifiedAt(now);
        verificationRepository.save(verification);
        return new EmailVerificationResponse(email, verification.getExpiresAt(),
                verification.getLastSentAt().plusSeconds(RESEND_DELAY_SECONDS), true);
    }

    private EmailVerificationResponse createAndSend(String email) {
        requireMailConfiguration();
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = verificationRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseGet(() -> {
                    EmailVerification created = new EmailVerification();
                    created.setEmail(email);
                    created.setResendWindowStartedAt(now);
                    created.setResendCount(0);
                    created.setFailedAttempts(0);
                    return created;
                });

        if (verification.getVerifiedAt() != null) {
            throw new CommonException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (verification.getLastSentAt() != null
                && verification.getLastSentAt().plusSeconds(RESEND_DELAY_SECONDS).isAfter(now)) {
            throw new CommonException(ErrorCode.EMAIL_VERIFICATION_RATE_LIMITED);
        }
        if (verification.getResendWindowStartedAt() == null
                || !verification.getResendWindowStartedAt().plusHours(1).isAfter(now)) {
            verification.setResendWindowStartedAt(now);
            verification.setResendCount(0);
        }
        if (verification.getResendCount() >= MAX_SENDS_PER_HOUR) {
            throw new CommonException(ErrorCode.EMAIL_VERIFICATION_RATE_LIMITED);
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setExpiresAt(now.plusMinutes(CODE_TTL_MINUTES));
        verification.setLastSentAt(now);
        verification.setResendCount(verification.getResendCount() + 1);
        verification.setFailedAttempts(0);
        verificationRepository.saveAndFlush(verification);
        sendEmail(email, code);

        return new EmailVerificationResponse(email, verification.getExpiresAt(),
                now.plusSeconds(RESEND_DELAY_SECONDS), false);
    }

    private void requireMailConfiguration() {
        if (mailHost == null || mailHost.isBlank()
                || (configuredFrom == null || configuredFrom.isBlank())
                && (mailUsername == null || mailUsername.isBlank())) {
            throw new CommonException(ErrorCode.EMAIL_SERVICE_UNAVAILABLE);
        }
    }

    private void sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(configuredFrom == null || configuredFrom.isBlank() ? mailUsername : configuredFrom);
        message.setTo(email);
        message.setSubject("Mã xác minh tài khoản Adaptive Learning");
        message.setText("Mã xác minh của bạn là: " + code
                + "\nMã có hiệu lực trong 5 phút. Không chia sẻ mã này với bất kỳ ai.");
        try {
            mailSender.send(message);
        } catch (RuntimeException exception) {
            throw new CommonException(ErrorCode.EMAIL_SERVICE_UNAVAILABLE);
        }
    }

    private UserIdentity findIdentityForUpdate(String email) {
        return userMapper.findByUsernameForUpdate(email)
                .map(user -> new UserIdentity(user.isEmailVerified()))
                .orElse(null);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record UserIdentity(boolean emailVerified) {}
}
