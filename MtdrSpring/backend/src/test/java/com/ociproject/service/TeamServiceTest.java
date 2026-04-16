package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.Team;
import com.ociproject.repository.TeamRepository;
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
class TeamServiceTest {

    @Mock
    TeamRepository teamRepository;

    @InjectMocks
    TeamService service;

    @Test
    void findAll_returnsNonDeletedTeams() {
        Team t = TestFixtures.team1();
        when(teamRepository.findAllByDeletedFalse()).thenReturn(List.of(t));

        assertThat(service.findAll()).containsExactly(t);
    }

    @Test
    void findById_found_returnsTeam() {
        Team t = TestFixtures.team1();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(t));

        assertThat(service.findById(1L)).isPresent().contains(t);
    }

    @Test
    void findById_notFound_returnsEmpty() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.findById(99L)).isEmpty();
    }

    @Test
    void save_persistsAndReturnsTeam() {
        Team t = TestFixtures.team2();
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.save(t)).isSameAs(t);
        verify(teamRepository).save(t);
    }

    @Test
    void softDelete_setsDeletedFlagAndTimestamp() {
        Team t = TestFixtures.team1();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(t));
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(1L);

        verify(teamRepository).save(argThat(team ->
                Boolean.TRUE.equals(team.getDeleted()) && team.getDeletedAt() != null
        ));
    }

    @Test
    void softDelete_notFound_throwsEntityNotFoundException() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.softDelete(99L));
        verify(teamRepository, never()).save(any());
    }
}
