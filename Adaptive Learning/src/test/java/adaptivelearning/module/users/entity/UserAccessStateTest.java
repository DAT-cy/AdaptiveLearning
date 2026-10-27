package adaptivelearning.module.users.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserAccessStateTest {

    @Test
    void anAccountRequiresEmailVerificationAndMustNotBeSoftLocked() {
        User user = new User();
        user.setEnable(true);
        user.setIsDeleted(false);
        user.setEmailVerified(false);
        assertFalse(user.isEnabled());

        user.setEmailVerified(true);
        assertTrue(user.isEnabled());

        user.setEnable(false);
        assertFalse(user.isEnabled());
    }
}
