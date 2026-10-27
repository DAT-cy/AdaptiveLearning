package adaptivelearning.module.users.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.module.users.entity.EmailVerification;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.EmailVerificationRepository;
import adaptivelearning.module.users.repository.UserMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    }

    @Test
    void issueSendsSixDigitCodeAndStoresOnlyItsHashForFiveMinutes() {
        User user = new User();
        user.setEmailVerified(false);
        when(userMapper.findByUsername("student@example.com")).thenReturn(Optional.of(user));
        when(verificationRepository.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.empty());
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
        assertEquals("student@example.com", response.email());
        assertFalse(response.verified());
        assertTrue(Duration.between(before.plusMinutes(5), response.expiresAt()).abs().toSeconds() <= 1
                || Duration.between(after.plusMinutes(5), response.expiresAt()).abs().toSeconds() <= 1);
        verify(passwordEncoder).encode(code);
    }

    @Test
    void validCodeVerifiesTheAccountAndCannotBeReused() {
        EmailVerification verification = activeChallenge("123456");
        when(verificationRepository.findByEmailIgnoreCase("student@example.com"))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("123456", "hashed:123456")).thenReturn(true);
        when(userMapper.updateEmailVerified("student@example.com", true)).thenReturn(1);

        var result = service.verify("student@example.com", "123456");

        assertTrue(result.verified());
        assertNotNull(verification.getVerifiedAt());
        verify(userMapper).updateEmailVerified("student@example.com", true);
        assertThrows(CommonException.class, () -> service.verify("student@example.com", "123456"));
    }

    @Test
    void invalidCodeIncrementsAttemptCount() {
        EmailVerification verification = activeChallenge("123456");
        when(verificationRepository.findByEmailIgnoreCase("student@example.com"))
                .thenReturn(Optional.of(verification));
        when(passwordEncoder.matches("000000", "hashed:123456")).thenReturn(false);

        assertThrows(CommonException.class, () -> service.verify("student@example.com", "000000"));

        assertEquals(1, verification.getFailedAttempts());
        verify(verificationRepository).save(verification);
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
