package adaptivelearning.config.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminApiRejectsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void adminApiRejectsRegularUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminApiAllowsAdministrators() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    void nestedNonAuthNamespaceIsNotPubliclyAllowlisted() throws Exception {
        mockMvc.perform(get("/api/v1/admin/auth/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailVerificationEndpointRemainsPublicForSignupFlow() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void authenticatedUserCanReadCertificateCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/certificates"))
                .andExpect(status().isOk());
    }

    @Test
    void notificationInboxRejectsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void notificationStreamRejectsAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/stream"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsRejectsUnconfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/v1/certificates")
                        .header("Origin", "https://untrusted.invalid")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void corsAllowsConfiguredFrontendOriginWithoutCredentials() throws Exception {
        mockMvc.perform(options("/api/v1/certificates")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }
}
