package com.ociproject.repository;

import com.ociproject.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByTelegramIdAndDeletedFalse(String telegramId);
    List<User> findAllByDeletedFalse();
    List<User> findByRoleRoleIdAndDeletedFalse(Long roleId);
    List<User> findByTeamTeamIdAndDeletedFalse(Long teamId);
    List<User> findByStatusAndDeletedFalse(User.Status status);
}
