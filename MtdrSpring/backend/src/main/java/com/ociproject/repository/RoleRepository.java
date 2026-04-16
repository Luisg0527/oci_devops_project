package com.ociproject.repository;

import com.ociproject.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleNameAndDeletedFalse(String roleName);
    List<Role> findAllByDeletedFalse();
}
