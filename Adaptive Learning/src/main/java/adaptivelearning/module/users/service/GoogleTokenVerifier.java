package adaptivelearning.module.users.service;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
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
        OAuth2TokenValidator<Jwt> issuerValidator = token -> {
            String issuer = token.getIssuer() != null ? token.getIssuer().toString() : "";
            if ("https://accounts.google.com".equals(issuer) || "accounts.google.com".equals(issuer)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_issuer", "Invalid Google token issuer: " + issuer, null));
        };
        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestampValidator, issuerValidator));
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
            String name = jwt.getClaimAsString("name");
            return new GoogleProfile(jwt.getSubject(), email, name);
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
        private final String name;

        public GoogleProfile(String subject, String email, String name) {
            this.subject = subject;
            this.email = email;
            this.name = name;
        }
    }
}
