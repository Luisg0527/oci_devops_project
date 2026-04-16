package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.User;
import com.ociproject.repository.UserRepository;
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
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService service;

    @Test
    void findAll_returnsNonDeletedUsers() {
        User u = TestFixtures.user1();
        when(userRepository.findAllByDeletedFalse()).thenReturn(List.of(u));

        assertThat(service.findAll()).containsExactly(u);
    }

    @Test
    void findById_found_returnsUser() {
        User u = TestFixtures.user1();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        assertThat(service.findById(1L)).isPresent().contains(u);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void findByEmail_found_returnsUser() {
        User u = TestFixtures.user1();
        when(userRepository.findByEmailAndDeletedFalse("a01723229@tec.mx")).thenReturn(Optional.of(u));

        assertThat(service.findByEmail("a01723229@tec.mx")).isPresent().contains(u);
    }

    @Test
    void findByTelegramId_found_returnsUser() {
        User u = TestFixtures.user1();
        when(userRepository.findByTelegramIdAndDeletedFalse("@inigogonzalez")).thenReturn(Optional.of(u));

        assertThat(service.findByTelegramId("@inigogonzalez")).isPresent().contains(u);
    }

    @Test
    void save_persistsUser() {
        User u = TestFixtures.user1();
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.save(u)).isSameAs(u);
        verify(userRepository).save(u);
    }

    @Test
    void updateStatus_changesStatusAndSaves() {
        User u = TestFixtures.user1();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = service.updateStatus(1L, User.Status.LOCKED);

        assertThat(result.getStatus()).isEqualTo(User.Status.LOCKED);
        verify(userRepository).save(u);
    }

    @Test
    void updateStatus_notFound_throwsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.updateStatus(99L, User.Status.LOCKED));
        verify(userRepository, never()).save(any());
    }

    @Test
    void softDelete_setsDeletedFlagTimestampAndInactivatesStatus() {
        User u = TestFixtures.user1();
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(1L);

        verify(userRepository).save(argThat(user ->
                Boolean.TRUE.equals(user.getDeleted())
                && user.getDeletedAt() != null
                && user.getStatus() == User.Status.INACTIVE
        ));
    }

    @Test
    void softDelete_notFound_throwsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.softDelete(99L));
        verify(userRepository, never()).save(any());
    }

}
