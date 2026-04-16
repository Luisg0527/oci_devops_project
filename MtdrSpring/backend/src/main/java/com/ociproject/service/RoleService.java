package com.ociproject.service;

import com.ociproject.model.Role;
import com.ociproject.repository.RoleRepository;
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
public class RoleService {

    private final RoleRepository roleRepository;

    public List<Role> findAll() {
        return roleRepository.findAllByDeletedFalse();
    }

    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id);
    }

    public Optional<Role> findByName(String name) {
        return roleRepository.findByRoleNameAndDeletedFalse(name);
    }

    @Transactional
    public Role save(Role role) {
        return roleRepository.save(role);
    }

    @Transactional
    public void softDelete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + id));
        role.setDeleted(true);
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);
    }
}
