package adaptivelearning.module.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.users.entity.EmailVerification;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.EmailVerificationRepository;
import adaptivelearning.module.users.repository.UserMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

class EmailVerificationServiceTest {

    private EmailVerificationRepository verificationRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JavaMailSender mailSender;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        verificationRepository = mock(EmailVerificationRepository.class);
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        mailSender = mock(JavaMailSender.class);
        service = new EmailVerificationService(verificationRepository, userMapper, passwordEncoder, mailSender);
        ReflectionTestUtils.setField(service, "mailHost", "smtp.example.test");
        ReflectionTestUtils.setField(service, "mailUsername", "sender@example.test");
        ReflectionTestUtils.setField(service, "configuredFrom", "sender@example.test");
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hashed:" + invocation.getArgument(0));
        User user = new User();
        user.setEmailVerified(false);
        when(userMapper.findByUsernameForUpdate(anyString())).thenReturn(Optional.of(user));
    }

    @Test
    void verificationLookupUsesExclusiveDatabaseLock() throws Exception {
        Lock lock = EmailVerificationRepository.class
                .getMethod("findByEmailIgnoreCaseForUpdate", String.class)
                .getAnnotation(Lock.class);

        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }

    @Test
    void issueSendsSixDigitCodeAndStoresOnlyItsHashForFiveMinutes() {
        User user = new User();
        user.setEmailVerified(false);
        when(verificationRepository.findByEmailIgnoreCaseForUpdate("student@example.com"))
                .thenReturn(Optional.empty());
        when(verificationRepository.saveAndFlush(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        var response = service.issue(" Student@Example.com ");
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        Matcher codeMatcher = Pattern.compile("(?m)Mã xác minh của bạn là: ([0-9]{6})").matcher(messageCaptor.getValue().getText());
        assertTrue(codeMatcher.find());
        String code = codeMatcher.group(1);
        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(verificationRepository).saveAndFlush(verificationCaptor.capture());
        assertEquals("hashed:" + code, verificationCaptor.getValue().getCodeHash());
        assertFalse(verificationCaptor.getValue().getCodeHash().equals(code));
        assertEquals("student@example.com", response.email());
        assertFalse(response.verified());
        assertTrue(Duration.between(before.plusMinutes(5), response.expiresAt()).abs().toSeconds() <= 1
                || Duration.between(after.plusMinutes(5), response.expiresAt()).abs().toSeconds() <= 1);
        verify(passwordEncoder).encode(code);
    }

    @Test
    void generatedOtpPreservesLeadingZeros() {
        ReflectionTestUtils.setField(service, "secureRandom", new SecureRandom() {
            @Override
            public int nextInt(int bound) {
                return 7;
            }
        });
        when(verificationRepository.findByEmailIgnoreCaseForUpdate("student@example.com"))
                .thenReturn(Optional.empty());
        when(verificationRepository.saveAndFlush(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.issue("student@example.com");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertTrue(messageCaptor.getValue().getText().contains("Mã xác minh của bạn là: 000007"));
    }

    @Test
    void validCodeVerifiesTheAccountAndCannotBeReused() {
        EmailVerification verification = activeChallenge("123456");
        when(verificationRepository.findByEmailIgnoreCaseForUpdate("student@example.com"))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed:123456")).thenReturn(true);
        when(userMapper.updateEmailVerified("student@example.com", true)).thenReturn(1);

        var result = service.verify("student@example.com", "123456");

        assertTrue(result.verified());
        assertNotNull(verification.getVerifiedAt());
        verify(userMapper).updateEmailVerified("student@example.com", true);
        var lockOrder = inOrder(userMapper, verificationRepository);
        lockOrder.verify(userMapper).findByUsernameForUpdate("student@example.com");
        lockOrder.verify(verificationRepository).findByEmailIgnoreCaseForUpdate("student@example.com");
        assertThrows(CommonException.class, () -> service.verify("student@example.com", "123456"));
    }

    @Test
    void invalidCodeIncrementsAttemptCount() {
        EmailVerification verification = activeChallenge("123456");
        when(verificationRepository.findByEmailIgnoreCaseForUpdate("student@example.com"))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("000000", "hashed:123456")).thenReturn(false);

        assertThrows(CommonException.class, () -> service.verify("student@example.com", "000000"));

        assertEquals(1, verification.getFailedAttempts());
        verify(verificationRepository).save(verification);
    }

    @Test
    void expiredCodeCannotVerifyAccount() {
        EmailVerification verification = activeChallenge("123456");
        verification.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(verificationRepository.findByEmailIgnoreCaseForUpdate("student@example.com"))
                .thenReturn(Optional.of(verification));

        CommonException exception = assertThrows(CommonException.class,
                () -> service.verify("student@example.com", "123456"));

        assertEquals(adaptivelearning.config.response.ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED,
                exception.getErrorCode());
        verify(userMapper, org.mockito.Mockito.never()).updateEmailVerified(
                anyString(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void resendIssuesNewCodeAfterCooldown() {
        ReflectionTestUtils.setField(service, "secureRandom", new SecureRandom() {
            @Override
            public int nextInt(int bound) {
                return 7;
            }
        });
        User user = new User();
        user.setEmailVerified(false);
        EmailVerification verification = activeChallenge("123456");
        verification.setLastSentAt(LocalDateTime.now().minusMinutes(2));
        verification.setResendWindowStartedAt(LocalDateTime.now().minusMinutes(2));
        verification.setResendCount(1);
        when(userMapper.findByUsernameForUpdate("student@example.com")).thenReturn(Optional.of(user));
        when(verificationRepository.findByEmailIgnoreCaseForUpdate("student@example.com"))
                .thenReturn(Optional.of(verification));
        when(verificationRepository.saveAndFlush(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.resend("student@example.com");

        assertFalse(response.verified());
        assertTrue(response.expiresAt().isAfter(LocalDateTime.now()));
        verify(mailSender).send(any(SimpleMailMessage.class));
        assertEquals("hashed:000007", verification.getCodeHash());
        assertEquals(2, verification.getResendCount());
    }

    @Test
    void resendDuringCooldownReturnsRateLimitError() {
        User user = new User();
        user.setEmailVerified(false);
        EmailVerification verification = activeChallenge("123456");
        verification.setLastSentAt(LocalDateTime.now().minusSeconds(20));
        when(userMapper.findByUsernameForUpdate("student@example.com")).thenReturn(Optional.of(user));
        when(verificationRepository.findByEmailIgnoreCaseForUpdate("student@example.com"))
                .thenReturn(Optional.of(verification));

        CommonException exception = assertThrows(CommonException.class,
                () -> service.resend("student@example.com"));

        assertEquals(adaptivelearning.config.response.ErrorCode.EMAIL_VERIFICATION_RATE_LIMITED,
                exception.getErrorCode());
        verify(mailSender, org.mockito.Mockito.never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void verificationStopsAfterMaximumFailedAttempts() {
        EmailVerification verification = activeChallenge("123456");
        verification.setFailedAttempts(5);
        when(verificationRepository.findByEmailIgnoreCaseForUpdate("student@example.com"))
                .thenReturn(Optional.of(verification));

        CommonException exception = assertThrows(CommonException.class,
                () -> service.verify("student@example.com", "000000"));

        assertEquals(adaptivelearning.config.response.ErrorCode.EMAIL_VERIFICATION_RATE_LIMITED,
                exception.getErrorCode());
        verify(passwordEncoder, org.mockito.Mockito.never()).matches(anyString(), anyString());
    }

    @Test
    void resendReportsUnavailableWhenMailIsNotConfigured() {
        ReflectionTestUtils.setField(service, "mailHost", " ");

        CommonException exception = assertThrows(CommonException.class,
                () -> service.resend("student@example.com"));

        assertEquals(adaptivelearning.config.response.ErrorCode.EMAIL_SERVICE_UNAVAILABLE,
                exception.getErrorCode());
    }

    @Test
    void smtpFailureReturnsUnavailableAndNeverVerifiesTheAccount() {
        when(verificationRepository.findByEmailIgnoreCaseForUpdate("student@example.com"))
                .thenReturn(Optional.empty());
        when(verificationRepository.saveAndFlush(any(EmailVerification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.doThrow(new MailSendException("smtp unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        CommonException error = assertThrows(CommonException.class,
                () -> service.issue("student@example.com"));

        assertEquals(adaptivelearning.config.response.ErrorCode.EMAIL_SERVICE_UNAVAILABLE,
                error.getErrorCode());
        verify(userMapper, org.mockito.Mockito.never()).updateEmailVerified(anyString(),
                org.mockito.ArgumentMatchers.anyBoolean());
    }

    private EmailVerification activeChallenge(String code) {
        EmailVerification verification = new EmailVerification();
        verification.setEmail("student@example.com");
        verification.setCodeHash("hashed:" + code);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verification.setLastSentAt(LocalDateTime.now().minusMinutes(1));
        verification.setResendWindowStartedAt(LocalDateTime.now().minusMinutes(1));
        verification.setResendCount(1);
        verification.setFailedAttempts(0);
        return verification;
    }
}
