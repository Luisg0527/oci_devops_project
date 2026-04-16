package com.ociproject.repository;

import com.ociproject.model.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByUsernameAndDeletedFalse(String username);
    Optional<UserCredential> findByUserUserIdAndDeletedFalse(Long userId);
}
