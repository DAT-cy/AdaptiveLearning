package adaptivelearning.module.users.repository;

import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.entity.Role;
import adaptivelearning.shared.enums.PlanType;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;
import java.util.List;

@Mapper
public interface UserMapper {
    Optional<User> findByUsername(String username);
    Optional<User> findById(@Param("userId") Long userId);
    List<User> findAllForAdmin();
    int countAdminUsers();
    void insertUser(User user);
    int updateRole(@Param("userId") Long userId, @Param("role") Role role);
    int updateEnabled(@Param("userId") Long userId, @Param("enabled") boolean enabled);
    int updateEmailVerified(@Param("username") String username, @Param("verified") boolean verified);
    int updatePlan(@Param("userId") Long userId, @Param("plan") PlanType plan,
                   @Param("premiumActivatedAt") LocalDateTime premiumActivatedAt);
    int downgradePremiumAndSyncQuota(@Param("userId") Long userId, @Param("limit") int limit);
    int consumeFreeExamQuota(@Param("userId") Long userId, @Param("limit") int limit);
    int initializeLegacyFreeExamUsage(@Param("limit") int limit);
}
