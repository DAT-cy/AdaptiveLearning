package adaptivelearning.module.users.entity;

import adaptivelearning.config.app.BaseEntity;
import adaptivelearning.shared.enums.PlanType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tbl_users")
public class User extends BaseEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 254)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "enable")
    private Boolean enable;

    @Column(name = "email_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean emailVerified = true;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 16, columnDefinition = "VARCHAR(16) DEFAULT 'FREE'")
    private PlanType plan = PlanType.FREE;

    @Column(name = "premium_activated_at")
    private LocalDateTime premiumActivatedAt;

    @Column(name = "free_exams_used", nullable = false, columnDefinition = "INT DEFAULT 0")
    private int freeExamsUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enable) && emailVerified && !Boolean.TRUE.equals(isDeleted);
    }
}
