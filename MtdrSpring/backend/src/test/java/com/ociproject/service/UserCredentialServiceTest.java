package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.UserCredential;
import com.ociproject.repository.UserCredentialRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCredentialServiceTest {

    @Mock
    UserCredentialRepository credentialRepository;

    @InjectMocks
    UserCredentialService service;

    @Test
    void findByUsername_found_returnsCredential() {
        UserCredential cred = TestFixtures.credential1(0, false);
        when(credentialRepository.findByUsernameAndDeletedFalse("inigo.gonzalez")).thenReturn(Optional.of(cred));

        assertThat(service.findByUsername("inigo.gonzalez")).isPresent().contains(cred);
    }

    @Test
    void findByUserId_found_returnsCredential() {
        UserCredential cred = TestFixtures.credential1(0, false);
        when(credentialRepository.findByUserUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(cred));

        assertThat(service.findByUserId(1L)).isPresent().contains(cred);
    }

    @Test
    void save_persistsCredential() {
        UserCredential cred = TestFixtures.credential1(0, false);
        when(credentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.save(cred)).isSameAs(cred);
    }

    @Test
    void recordFailedLogin_incrementsCounter_doesNotLockBelowThreshold() {
        UserCredential cred = TestFixtures.credential1(3, false);
        when(credentialRepository.findByUserUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(cred));
        when(credentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordFailedLogin(1L);

        verify(credentialRepository).save(argThat(c ->
                c.getFailedAttempts() == 4 && Boolean.FALSE.equals(c.getAccountLocked())
        ));
    }

    @Test
    void recordFailedLogin_locksAccountOnFifthFailure() {
        UserCredential cred = TestFixtures.credential1(4, false);
        when(credentialRepository.findByUserUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(cred));
        when(credentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordFailedLogin(1L);

        verify(credentialRepository).save(argThat(c ->
                c.getFailedAttempts() == 5 && Boolean.TRUE.equals(c.getAccountLocked())
        ));
    }

    @Test
    void recordFailedLogin_notFound_throwsEntityNotFoundException() {
        when(credentialRepository.findByUserUserIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.recordFailedLogin(99L));
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void recordSuccessfulLogin_resetsCounterAndSetsLastLogin() {
        UserCredential cred = TestFixtures.credential1(3, false);
        when(credentialRepository.findByUserUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(cred));
        when(credentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordSuccessfulLogin(1L);

        verify(credentialRepository).save(argThat(c ->
                c.getFailedAttempts() == 0 && c.getLastLogin() != null
        ));
    }

    @Test
    void recordSuccessfulLogin_notFound_throwsEntityNotFoundException() {
        when(credentialRepository.findByUserUserIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.recordSuccessfulLogin(99L));
    }

    @Test
    void softDelete_setsDeletedFlagAndTimestamp() {
        UserCredential cred = TestFixtures.credential1(0, false);
        when(credentialRepository.findByUserUserIdAndDeletedFalse(1L)).thenReturn(Optional.of(cred));
        when(credentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(1L);

        verify(credentialRepository).save(argThat(c ->
                Boolean.TRUE.equals(c.getDeleted()) && c.getDeletedAt() != null
        ));
    }
}
