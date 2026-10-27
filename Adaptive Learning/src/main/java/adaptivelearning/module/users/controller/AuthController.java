package adaptivelearning.module.users.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.config.response.ResponseMessage;
import adaptivelearning.config.response.StatusCode;
import adaptivelearning.module.users.dto.request.LoginRequest;
import adaptivelearning.module.users.dto.request.GoogleLoginRequest;
import adaptivelearning.module.users.dto.request.ResendVerificationRequest;
import adaptivelearning.module.users.dto.request.SignUpRequest;
import adaptivelearning.module.users.dto.request.VerifyEmailRequest;
import adaptivelearning.module.users.dto.response.EmailVerificationResponse;
import adaptivelearning.module.users.dto.response.GoogleConfigResponse;
import adaptivelearning.module.users.dto.response.LoginResponse;
import adaptivelearning.module.users.dto.response.ProfileUser;
import adaptivelearning.module.users.service.UserService;
import adaptivelearning.module.users.service.GoogleTokenVerifier;
import adaptivelearning.module.users.service.EmailVerificationService;
import adaptivelearning.utils.ClientUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ClientUtils.VERSION + "/auth")
@Tag(name = "01. Authentication", description = "Authentication APIs")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/login")
    @Operation(summary = "Login to get JWT token")
    public ResponseEntity<DefaultRes<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return new ResponseEntity<>(DefaultRes.res(StatusCode.OK, ResponseMessage.SUCCESS, userService.login(request)), HttpStatus.OK);

    }

    @PostMapping("/google")
    @Operation(summary = "Sign in with a Google ID token and get an application JWT")
    public ResponseEntity<DefaultRes<LoginResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return new ResponseEntity<>(DefaultRes.res(StatusCode.OK, ResponseMessage.SUCCESS,
                userService.loginWithGoogle(request)), HttpStatus.OK);
    }

    @GetMapping("/google/config")
    public ResponseEntity<DefaultRes<GoogleConfigResponse>> googleConfig() {
        String clientId = googleTokenVerifier.getClientId();
        return ResponseEntity.ok(DefaultRes.res(StatusCode.OK, ResponseMessage.SUCCESS,
                new GoogleConfigResponse(clientId, clientId != null && !clientId.isBlank())));
    }

    @PostMapping("/signup")
    public ResponseEntity<DefaultRes<EmailVerificationResponse>> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        EmailVerificationResponse response = userService.signUp(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(StatusCode.CREATED, ResponseMessage.CREATED, response));
    }

    @PostMapping("/signup/verify")
    public ResponseEntity<DefaultRes<EmailVerificationResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(DefaultRes.res(StatusCode.OK, ResponseMessage.SUCCESS,
                emailVerificationService.verify(request.getEmail(), request.getCode())));
    }

    @PostMapping("/signup/resend")
    public ResponseEntity<DefaultRes<EmailVerificationResponse>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(DefaultRes.res(StatusCode.OK, ResponseMessage.SUCCESS,
                emailVerificationService.resend(request.getEmail())));
    }

    @GetMapping("/profile")
    public ResponseEntity<DefaultRes<ProfileUser>> getProfile() {
        return new ResponseEntity<>(DefaultRes.res(StatusCode.OK, ResponseMessage.SUCCESS, userService.getProfile()), HttpStatus.OK);
    }
}
