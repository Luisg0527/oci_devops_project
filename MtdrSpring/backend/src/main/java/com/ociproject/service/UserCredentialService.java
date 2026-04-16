package com.ociproject.service;

import com.ociproject.model.UserCredential;
import com.ociproject.repository.UserCredentialRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCredentialService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserCredentialRepository credentialRepository;

    public Optional<UserCredential> findByUsername(String username) {
        return credentialRepository.findByUsernameAndDeletedFalse(username);
    }

    public Optional<UserCredential> findByUserId(Long userId) {
        return credentialRepository.findByUserUserIdAndDeletedFalse(userId);
    }

    @Transactional
    public UserCredential save(UserCredential credential) {
        return credentialRepository.save(credential);
    }

    @Transactional
    public void recordFailedLogin(Long userId) {
        UserCredential credential = credentialRepository.findByUserUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new EntityNotFoundException("Credentials not found for user: " + userId));
        credential.setFailedAttempts(credential.getFailedAttempts() + 1);
        if (credential.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            credential.setAccountLocked(true);
        }
        credentialRepository.save(credential);
    }

    @Transactional
    public void recordSuccessfulLogin(Long userId) {
        UserCredential credential = credentialRepository.findByUserUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new EntityNotFoundException("Credentials not found for user: " + userId));
        credential.setFailedAttempts(0);
        credential.setLastLogin(LocalDateTime.now());
        credentialRepository.save(credential);
    }

    @Transactional
    public void softDelete(Long userId) {
        UserCredential credential = credentialRepository.findByUserUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new EntityNotFoundException("Credentials not found for user: " + userId));
        credential.setDeleted(true);
        credential.setDeletedAt(LocalDateTime.now());
        credentialRepository.save(credential);
    }
}
