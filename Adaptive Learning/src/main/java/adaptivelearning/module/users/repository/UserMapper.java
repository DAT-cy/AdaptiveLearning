package adaptivelearning.module.users.repository;

import adaptivelearning.module.users.entity.User;
import adaptivelearning.module.users.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;
import java.util.List;

@Mapper
public interface UserMapper {
    Optional<User> findByUsername(String username);
    List<User> findAllForAdmin();
    int countAdminUsers();
    void insertUser(User user);
    int updateRole(@Param("userId") Long userId, @Param("role") Role role);
    int updateEnabled(@Param("userId") Long userId, @Param("enabled") boolean enabled);
}
