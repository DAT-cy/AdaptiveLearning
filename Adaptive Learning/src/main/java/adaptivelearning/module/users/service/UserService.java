package adaptivelearning.module.users.service;

import adaptivelearning.module.users.dto.request.LoginRequest;
import adaptivelearning.module.users.dto.request.GoogleLoginRequest;
import adaptivelearning.module.users.dto.request.SignUpRequest;
import adaptivelearning.module.users.dto.response.LoginResponse;
import adaptivelearning.module.users.dto.response.ProfileUser;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.module.users.entity.User;

public interface UserService {
    LoginResponse login(LoginRequest request);
    LoginResponse loginWithGoogle(GoogleLoginRequest request);
    void initAdminUser();
    Boolean signUp(SignUpRequest request);
    User createManagedUser(String username, String password, Role role);
    ProfileUser getProfile();
}
