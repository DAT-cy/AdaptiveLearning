package adaptivelearning.module.users.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.app.JwtService;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.module.users.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class UserBootstrapSecurityTest {

    @Test
    void startupDoesNotCreateKnownDefaultAccounts() {
        UserMapper users = mock(UserMapper.class);
        when(users.countAdminUsers()).thenReturn(0);
        UserServiceImpl service = service(users);

        service.initAdminUser();

        verify(users).countAdminUsers();
        verify(users, never()).insertUser(any(User.class));
    }

    @Test
    void configuredBootstrapCreatesOnlyExplicitAdmin() {
        UserMapper users = mock(UserMapper.class);
        when(users.countAdminUsers()).thenReturn(0);
        UserServiceImpl service = service(users);
        ReflectionTestUtils.setField(service, "bootstrapAdminUsername", "first-admin");
        ReflectionTestUtils.setField(service, "bootstrapAdminPassword", "a-secure-bootstrap-password");

        service.initAdminUser();

        verify(users).insertUser(org.mockito.ArgumentMatchers.argThat(user ->
                "first-admin".equals(user.getUsername()) && user.getPassword() != null
                        && !user.getPassword().equals("a-secure-bootstrap-password")));
    }

    private UserServiceImpl service(UserMapper users) {
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-bootstrap-password");
        return new UserServiceImpl(users, mock(AuthenticationManager.class), mock(JwtService.class),
                passwordEncoder, mock(GoogleTokenVerifier.class), mock(EmailVerificationService.class));
    }
}
