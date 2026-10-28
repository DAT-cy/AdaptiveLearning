package adaptivelearning.config.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void generatedJwtResolvesToTheAuthenticatedUsername() {
        JwtService service = service(60_000);
        UserDetails principal = User.withUsername("student@example.com")
                .password("encoded").authorities("ROLE_USER").build();

        String token = service.generateToken(principal);

        assertEquals("student@example.com", service.extractUsername(token));
        assertEquals(false, service.isTokenExpired(token));
    }

    @Test
    void expiredJwtIsRejectedDuringClaimParsing() {
        JwtService service = service(-1_000);
        UserDetails principal = User.withUsername("student@example.com")
                .password("encoded").authorities("ROLE_USER").build();
        String token = service.generateToken(principal);

        assertThrows(ExpiredJwtException.class, () -> service.extractUsername(token));
    }

    private JwtService service(long expiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(service, "jwtExpiration", expiration);
        service.validateSecret();
        return service;
    }
}
