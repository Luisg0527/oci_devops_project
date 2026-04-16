package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.Role;
import com.ociproject.repository.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    RoleRepository roleRepository;

    @InjectMocks
    RoleService service;

    @Test
    void findAll_returnsNonDeletedRoles() {
        Role r = TestFixtures.role1();
        when(roleRepository.findAllByDeletedFalse()).thenReturn(List.of(r));

        assertThat(service.findAll()).containsExactly(r);
        verify(roleRepository).findAllByDeletedFalse();
    }

    @Test
    void findById_found_returnsRole() {
        Role r = TestFixtures.role1();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(r));

        assertThat(service.findById(1L)).isPresent().contains(r);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void findByName_found_returnsRole() {
        Role r = TestFixtures.role1();
        when(roleRepository.findByRoleNameAndDeletedFalse("MANAGER")).thenReturn(Optional.of(r));

        assertThat(service.findByName("MANAGER")).isPresent().contains(r);
    }

    @Test
    void save_persistsAndReturnsRole() {
        Role r = Role.builder().roleName("QA").build();
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Role saved = service.save(r);

        assertThat(saved).isSameAs(r);
        verify(roleRepository).save(r);
    }

    @Test
    void softDelete_setsDeletedFlagAndTimestamp() {
        Role r = TestFixtures.role1();
        when(roleRepository.findById(1L)).thenReturn(Optional.of(r));
        when(roleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(1L);

        verify(roleRepository).save(argThat(role ->
                Boolean.TRUE.equals(role.getDeleted()) && role.getDeletedAt() != null
        ));
    }

    @Test
    void softDelete_notFound_throwsEntityNotFoundException() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.softDelete(99L));
        verify(roleRepository, never()).save(any());
    }
}
