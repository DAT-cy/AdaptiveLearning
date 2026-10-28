package adaptivelearning.module.users.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.app.JwtService;
import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.users.dto.request.GoogleLoginRequest;
import adaptivelearning.module.users.dto.request.LoginRequest;
import adaptivelearning.module.users.dto.request.SignUpRequest;
import adaptivelearning.module.users.dto.response.EmailVerificationResponse;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.module.users.service.EmailVerificationService;
import adaptivelearning.module.users.service.GoogleTokenVerifier;
import adaptivelearning.module.users.service.impl.UserServiceImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {

    private UserMapper users;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private GoogleTokenVerifier googleTokenVerifier;
    private EmailVerificationService emailVerificationService;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        users = mock(UserMapper.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        googleTokenVerifier = mock(GoogleTokenVerifier.class);
        emailVerificationService = mock(EmailVerificationService.class);
        service = new UserServiceImpl(users, authenticationManager, jwtService, passwordEncoder,
                googleTokenVerifier, emailVerificationService);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(jwtService.generateToken(any(User.class))).thenReturn("signed-token");
    }

    @Test
    void passwordLoginNormalizesEmailAndReturnsServerIssuedRoleAndToken() {
        User user = activeUser("student@example.com", Role.ROLE_USER);
        when(users.findByUsername("student@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        LoginRequest request = new LoginRequest();
        request.setUsername(" Student@Example.com ");
        request.setPassword("correct-password");

        var result = service.login(request);

        assertEquals("student@example.com", result.getUsername());
        assertEquals(Role.ROLE_USER, result.getRole());
        assertEquals("signed-token", result.getToken());
        verify(authenticationManager).authenticate(any(Authentication.class));
    }

    @Test
    void badPasswordUsesExpectedAuthenticationError() {
        when(users.findByUsername("student@example.com"))
                .thenReturn(Optional.of(activeUser("student@example.com", Role.ROLE_USER)));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));
        LoginRequest request = new LoginRequest();
        request.setUsername("student@example.com");
        request.setPassword("wrong-password");

        CommonException exception = assertThrows(CommonException.class, () -> service.login(request));

        assertEquals(ErrorCode.WRONG_PASSWORD, exception.getErrorCode());
    }

    @Test
    void unknownUsernameUsesSameAuthenticationErrorAsBadPassword() {
        when(users.findByUsername("unknown@example.com")).thenReturn(Optional.empty());
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));
        LoginRequest request = new LoginRequest();
        request.setUsername("unknown@example.com");
        request.setPassword("any-password");

        CommonException exception = assertThrows(CommonException.class, () -> service.login(request));

        assertEquals(ErrorCode.WRONG_PASSWORD, exception.getErrorCode());
    }

    @Test
    void unverifiedAccountCannotReceivePasswordLoginToken() {
        User user = activeUser("student@example.com", Role.ROLE_USER);
        user.setEmailVerified(false);
        when(users.findByUsername("student@example.com")).thenReturn(Optional.of(user));
        LoginRequest request = new LoginRequest();
        request.setUsername("student@example.com");
        request.setPassword("correct-password");

        CommonException exception = assertThrows(CommonException.class, () -> service.login(request));

        assertEquals(ErrorCode.EMAIL_VERIFICATION_REQUIRED, exception.getErrorCode());
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void disabledAccountCannotReceivePasswordLoginToken() {
        User user = activeUser("student@example.com", Role.ROLE_USER);
        user.setEnable(false);
        when(users.findByUsername("student@example.com")).thenReturn(Optional.of(user));
        LoginRequest request = new LoginRequest();
        request.setUsername("student@example.com");
        request.setPassword("correct-password");

        CommonException exception = assertThrows(CommonException.class, () -> service.login(request));

        assertEquals(ErrorCode.ACCOUNT_DEACTIVATE, exception.getErrorCode());
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    void signupCreatesUnverifiedUserAsUserAndSendsVerification() {
        when(users.findByUsernameForUpdate("student@example.com")).thenReturn(Optional.empty());
        when(users.insertUnverifiedUserIfAbsent(any(User.class))).thenReturn(1);
        when(emailVerificationService.issue("student@example.com")).thenReturn(
                new EmailVerificationResponse("student@example.com", LocalDateTime.now().plusMinutes(5),
                        LocalDateTime.now().plusSeconds(60), false));
        SignUpRequest request = SignUpRequest.builder()
                .email(" Student@Example.com ").password("correct-password").build();

        var result = service.signUp(request);

        assertEquals("student@example.com", result.email());
        assertEquals(false, result.verified());
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).insertUnverifiedUserIfAbsent(captor.capture());
        assertEquals(Role.ROLE_USER, captor.getValue().getRole());
        assertEquals(false, captor.getValue().isEmailVerified());
        assertEquals("encoded-password", captor.getValue().getPassword());
        verify(emailVerificationService).issue("student@example.com");
    }

    @Test
    void verifiedDuplicateEmailIsRejected() {
        when(users.findByUsernameForUpdate("student@example.com"))
                .thenReturn(Optional.of(activeUser("student@example.com", Role.ROLE_USER)));
        SignUpRequest request = SignUpRequest.builder()
                .email("student@example.com").password("correct-password").build();

        CommonException exception = assertThrows(CommonException.class, () -> service.signUp(request));

        assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS, exception.getErrorCode());
        verify(users, never()).insertUnverifiedUserIfAbsent(any(User.class));
        verify(emailVerificationService, never()).issue(anyString());
    }

    @Test
    void duplicateUnverifiedEmailResendsInsteadOfCreatingAnotherUser() {
        User user = activeUser("student@example.com", Role.ROLE_USER);
        user.setEmailVerified(false);
        when(users.findByUsernameForUpdate("student@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationService.issue("student@example.com")).thenReturn(
                new EmailVerificationResponse("student@example.com", LocalDateTime.now().plusMinutes(5),
                        LocalDateTime.now().plusSeconds(60), false));
        SignUpRequest request = SignUpRequest.builder()
                .email("student@example.com").password("correct-password").build();

        service.signUp(request);

        verify(emailVerificationService).issue("student@example.com");
        verify(users, never()).insertUnverifiedUserIfAbsent(any(User.class));
    }

    @Test
    void concurrentSignupLosingUniqueInsertReusesTheExistingVerificationFlow() {
        when(users.findByUsernameForUpdate("student@example.com")).thenReturn(Optional.empty());
        when(users.insertUnverifiedUserIfAbsent(any(User.class))).thenReturn(0);
        when(emailVerificationService.issue("student@example.com")).thenReturn(
                new EmailVerificationResponse("student@example.com", LocalDateTime.now().plusMinutes(5),
                        LocalDateTime.now().plusSeconds(60), false));
        SignUpRequest request = SignUpRequest.builder()
                .email("student@example.com").password("correct-password").build();

        service.signUp(request);

        verify(users).insertUnverifiedUserIfAbsent(any(User.class));
        verify(users, never()).insertUser(any(User.class));
        verify(emailVerificationService).issue("student@example.com");
    }

    @Test
    void googleLoginCreatesOnlyUserRoleFromVerifiedGoogleIdentity() {
        GoogleTokenVerifier.GoogleProfile googleProfile =
                new GoogleTokenVerifier.GoogleProfile("google-subject", "student@example.com", "Student");
        when(googleTokenVerifier.verify("google-id-token")).thenReturn(googleProfile);
        when(users.findByUsername("student@example.com"))
                .thenReturn(Optional.empty(), Optional.of(activeUser("student@example.com", Role.ROLE_USER)));
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("google-id-token");

        var result = service.loginWithGoogle(request);

        assertEquals(Role.ROLE_USER, result.getRole());
        assertNotNull(result.getToken());
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).insertUser(captor.capture());
        assertEquals(Role.ROLE_USER, captor.getValue().getRole());
        assertEquals(true, captor.getValue().isEmailVerified());
    }

    @Test
    void googleLoginDoesNotReactivateDisabledAccount() {
        User user = activeUser("student@example.com", Role.ROLE_USER);
        user.setEnable(false);
        when(googleTokenVerifier.verify("google-id-token"))
                .thenReturn(new GoogleTokenVerifier.GoogleProfile("google-subject", "student@example.com", "Student"));
        when(users.findByUsername("student@example.com")).thenReturn(Optional.of(user));
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("google-id-token");

        CommonException exception = assertThrows(CommonException.class, () -> service.loginWithGoogle(request));

        assertEquals(ErrorCode.ACCOUNT_DEACTIVATE, exception.getErrorCode());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void invalidGoogleTokenDoesNotCreateAccountOrJwt() {
        when(googleTokenVerifier.verify("invalid-google-id-token"))
                .thenThrow(new CommonException(ErrorCode.GOOGLE_TOKEN_INVALID));
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken("invalid-google-id-token");

        CommonException exception = assertThrows(CommonException.class, () -> service.loginWithGoogle(request));

        assertEquals(ErrorCode.GOOGLE_TOKEN_INVALID, exception.getErrorCode());
        verify(users, never()).insertUser(any(User.class));
        verify(jwtService, never()).generateToken(any(User.class));
    }

    private User activeUser(String email, Role role) {
        User user = new User();
        user.setUsername(email);
        user.setPassword("encoded-password");
        user.setEnable(true);
        user.setEmailVerified(true);
        user.setIsDeleted(false);
        user.setRole(role);
        return user;
    }
}
