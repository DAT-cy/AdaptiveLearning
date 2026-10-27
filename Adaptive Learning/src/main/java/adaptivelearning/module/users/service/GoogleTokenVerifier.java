package adaptivelearning.module.users.service;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoogleTokenVerifier {
    private final JwtDecoder jwtDecoder;
    private final String clientId;

    public GoogleTokenVerifier(@Value("${google.client-id:}") String clientId) {
        this.clientId = clientId;
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("https://accounts.google.com"));
        this.jwtDecoder = decoder;
    }

    public GoogleProfile verify(String idToken) {
        if (clientId.isBlank()) {
            throw new IllegalStateException("Google client ID is not configured");
        }

        try {
            Jwt jwt = jwtDecoder.decode(idToken);
            List<String> audiences = jwt.getAudience();
            if (!audiences.contains(clientId)) {
                throw new CommonException(ErrorCode.GOOGLE_TOKEN_INVALID);
            }

            String email = jwt.getClaimAsString("email");
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            if (email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerified)) {
                throw new CommonException(ErrorCode.GOOGLE_TOKEN_INVALID);
            }
            return new GoogleProfile(jwt.getSubject(), email);
        } catch (CommonException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CommonException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }
    }

    public String getClientId() {
        return clientId;
    }

    @Getter
    public static class GoogleProfile {
        private final String subject;
        private final String email;

        public GoogleProfile(String subject, String email) {
            this.subject = subject;
            this.email = email;
        }
    }
}
