package adaptivelearning.module.users.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import adaptivelearning.module.users.dto.request.SignUpRequest;
import adaptivelearning.module.users.entity.EmailVerification;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.EmailVerificationRepository;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.config.response.CommonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "spring.mail.host=smtp.example.test",
        "spring.mail.username=no-reply@example.test",
        "MAIL_FROM=no-reply@example.test"
})
@AutoConfigureMockMvc
class EmailRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserMapper users;

    @Autowired
    private EmailVerificationRepository verifications;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private JavaMailSender mailSender;

    private String testEmail;

    @AfterEach
    void removeTestAccount() {
        if (testEmail != null) {
            jdbcTemplate.update("delete from email_verifications where email = ?", testEmail);
            jdbcTemplate.update("delete from tbl_users where username = ?", testEmail);
        }
    }

    @Test
    @Transactional
    void atomicUnverifiedInsertIsVisibleToLockingRead() {
        testEmail = "otp-mapper-" + UUID.randomUUID() + "@example.test";
        User user = new User();
        user.setUsername(testEmail);
        user.setFullName("Mapper Integration");
        user.setPassword("hashed-test-password");
        user.setEnable(true);
        user.setEmailVerified(false);
        user.setIsDeleted(false);
        user.setRole(Role.ROLE_USER);

        assertEquals(1, users.insertUnverifiedUserIfAbsent(user));
        assertTrue(users.findByUsernameForUpdate(testEmail).isPresent());
    }

    @Test
    void concurrentSignupsCreateOnlyOneAccountForAnEmail() throws Exception {
        testEmail = "otp-signup-race-" + UUID.randomUUID() + "@example.test";
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = executor.submit(() -> insertWhenReleased("First", ready, start));
            Future<Integer> second = executor.submit(() -> insertWhenReleased("Second", ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int insertedAccounts = first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS);
            assertEquals(1, insertedAccounts);
            assertTrue(users.findByUsernameForUpdate(testEmail).isPresent());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentVerificationConsumesOtpExactlyOnce() throws Exception {
        testEmail = "otp-race-" + UUID.randomUUID() + "@example.test";
        new TransactionTemplate(transactionManager).execute(status -> {
            User user = new User();
            user.setUsername(testEmail);
            user.setFullName("OTP Race");
            user.setPassword("hashed-test-password");
            user.setEnable(true);
            user.setEmailVerified(false);
            user.setIsDeleted(false);
            user.setRole(Role.ROLE_USER);
            assertEquals(1, users.insertUnverifiedUserIfAbsent(user));

            LocalDateTime now = LocalDateTime.now();
            EmailVerification verification = new EmailVerification();
            verification.setEmail(testEmail);
            verification.setCodeHash(passwordEncoder.encode("123456"));
            verification.setExpiresAt(now.plusMinutes(5));
            verification.setLastSentAt(now.minusMinutes(1));
            verification.setResendWindowStartedAt(now.minusMinutes(1));
            verification.setResendCount(1);
            verification.setFailedAttempts(0);
            verifications.saveAndFlush(verification);
            return null;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> first = executor.submit(() -> verifyWhenReleased(ready, start));
            Future<Boolean> second = executor.submit(() -> verifyWhenReleased(ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int successfulVerifications = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successfulVerifications);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean verifyWhenReleased(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            return emailVerificationService.verify(testEmail, "123456").verified();
        } catch (CommonException expected) {
            return false;
        }
    }

    private int insertWhenReleased(String fullName, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        User user = new User();
        user.setUsername(testEmail);
        user.setFullName(fullName);
        user.setPassword("hashed-test-password");
        user.setEnable(true);
        user.setEmailVerified(false);
        user.setIsDeleted(false);
        user.setRole(Role.ROLE_USER);
        ready.countDown();
        start.await();
        return users.insertUnverifiedUserIfAbsent(user);
    }

    @Test
    void signupVerificationLoginAndProfileUseTheExistingAuthApi() throws Exception {
        String email = "otp-" + UUID.randomUUID() + "@example.test";
        testEmail = email;
        SignUpRequest signup = SignUpRequest.builder()
                .email(email)
                .fullName("OTP Integration")
                .password("valid-password-123")
                .build();

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(signup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.verified").value(false))
                .andExpect(jsonPath("$.data.code").doesNotExist());

        var userBeforeVerification = users.findByUsername(email).orElseThrow();
        assertTrue(!userBeforeVerification.isEmailVerified());
        assertTrue(!"valid-password-123".equals(userBeforeVerification.getPassword()));

        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        Matcher codeMatcher = Pattern.compile("(?m)Mã xác minh của bạn là: ([0-9]{6})")
                .matcher(message.getValue().getText());
        assertTrue(codeMatcher.find());
        String code = codeMatcher.group(1);

        mockMvc.perform(post("/api/v1/auth/signup/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true));

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + email + "\",\"password\":\"valid-password-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsByteArray())
                .path("data").path("token").asText();

        MockHttpServletRequestBuilder profile = get("/api/v1/auth/profile")
                .header("Authorization", "Bearer " + token);
        mockMvc.perform(profile)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(email));

    }
}
