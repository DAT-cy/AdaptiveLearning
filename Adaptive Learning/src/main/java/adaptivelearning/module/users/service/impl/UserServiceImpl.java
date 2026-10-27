package adaptivelearning.module.users.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.config.app.JwtService;
import adaptivelearning.module.users.dto.request.LoginRequest;
import adaptivelearning.module.users.dto.request.GoogleLoginRequest;
import adaptivelearning.module.users.dto.request.SignUpRequest;
import adaptivelearning.module.users.dto.response.LoginResponse;
import adaptivelearning.module.users.dto.response.ProfileUser;
import adaptivelearning.module.users.dto.response.EmailVerificationResponse;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.repository.UserMapper;
import adaptivelearning.module.users.service.UserService;
import adaptivelearning.module.users.service.GoogleTokenVerifier;
import adaptivelearning.module.users.service.EmailVerificationService;
import adaptivelearning.utils.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailVerificationService emailVerificationService;



    @Override
    public LoginResponse login(LoginRequest request) {
        String username = normalizeLogin(request.getUsername());
        User storedUser = userMapper.findByUsername(username).orElse(null);
        if (storedUser != null && !Boolean.TRUE.equals(storedUser.getEnable())) {
            throw new CommonException(ErrorCode.ACCOUNT_DEACTIVATE);
        }
        if (storedUser != null && !storedUser.isEmailVerified()) {
            throw new CommonException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            username,
                            request.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();

            String jwtToken = jwtService.generateToken(user);

            return LoginResponse.builder()
                    .username(user.getUsername())
                    .token(jwtToken)
                    .role(user.getRole())
                    .build();
        } catch (AccountExpiredException | DisabledException e) {
            throw new CommonException(ErrorCode.ACCOUNT_DEACTIVATE);
        }
        catch (Exception e) {
            throw new CommonException(ErrorCode.WRONG_PASSWORD);
        }
    }

    @Override
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleProfile googleProfile = googleTokenVerifier.verify(request.getIdToken());
        User user = findUserByEmail(googleProfile.getEmail());

        if (user == null) {
            user = new User();
            user.setUsername(googleProfile.getEmail());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setRole(Role.ROLE_USER);
            user.setEnable(true);
            user.setEmailVerified(true);
            user.setIsDeleted(false);
            userMapper.insertUser(user);
            user = findUserByEmail(googleProfile.getEmail());
        }

        if (user == null) {
            throw new CommonException(ErrorCode.INTER_SERVER_ERROR);
        }
        if (!user.isEmailVerified()) {
            userMapper.updateEmailVerified(user.getUsername(), true);
            user.setEmailVerified(true);
        }
        if (!user.isEnabled()) {
            throw new CommonException(ErrorCode.ACCOUNT_DEACTIVATE);
        }

        return LoginResponse.builder()
                .username(user.getUsername())
                .token(jwtService.generateToken(user))
                .role(user.getRole())
                .build();
    }
    @Override
    public EmailVerificationResponse signUp(SignUpRequest request) {
        String email = normalizeEmail(request.getEmail());
        User existing = findUserByEmail(email);
        if (existing != null) {
            if (existing.isEmailVerified()) throw new CommonException(ErrorCode.USERNAME_ALREADY_EXISTS);
            return emailVerificationService.issue(email);
        }
        User user = saveUser(request, email);
        userMapper.insertUser(user);
        return emailVerificationService.issue(email);
    }

    @Override
    public User createManagedUser(String username, String password, Role role) {
        if (findUserByEmail(username) != null) {
            throw new CommonException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnable(true);
        user.setEmailVerified(true);
        user.setIsDeleted(false);
        userMapper.insertUser(user);
        return userMapper.findByUsername(username).orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    @Override
    public ProfileUser getProfile() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new CommonException(ErrorCode.FORBIDDEN_ERROR);
        }
        return ProfileUser.builder()
                .role(user.getRole())
                .username(user.getUsername())
                .build();

    }

    private User saveUser(SignUpRequest request, String email) {
        User user = new User();
        user.setUsername(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER);
        user.setEnable(true);
        user.setEmailVerified(false);
        user.setIsDeleted(false);
        return user;
    }

    private User findUserByEmail(String email) {
         return userMapper.findByUsername(normalizeEmail(email)).orElse(null);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeLogin(String username) {
        String trimmed = username.trim();
        return trimmed.contains("@") ? trimmed.toLowerCase(Locale.ROOT) : trimmed;
    }



    @Override
    public void initAdminUser() {
        if (userMapper.countAdminUsers() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEnable(true);
            admin.setEmailVerified(true);
            admin.setIsDeleted(false);
            admin.setRole(Role.ROLE_ADMIN);
            userMapper.insertUser(admin);
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user"));
            user.setEnable(true);
            user.setEmailVerified(true);
            user.setIsDeleted(false);
            user.setRole(Role.ROLE_USER);
            userMapper.insertUser(user);
        }

    }


}
