package com.ociproject.service;

import com.ociproject.model.User;
import com.ociproject.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAllByDeletedFalse();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailAndDeletedFalse(email);
    }

    public Optional<User> findByTelegramId(String telegramId) {
        return userRepository.findByTelegramIdAndDeletedFalse(telegramId);
    }

    public List<User> findByTeam(Long teamId) {
        return userRepository.findByTeamTeamIdAndDeletedFalse(teamId);
    }

    public List<User> findByRole(Long roleId) {
        return userRepository.findByRoleRoleIdAndDeletedFalse(roleId);
    }

    public List<User> findByStatus(User.Status status) {
        return userRepository.findByStatusAndDeletedFalse(status);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public User updateStatus(Long id, User.Status status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public void softDelete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(User.Status.INACTIVE);
        userRepository.save(user);
    }
}
