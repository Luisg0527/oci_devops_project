package com.ociproject.service;

import com.ociproject.model.Sprint;
import com.ociproject.repository.SprintRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SprintServiceTest {

    @Mock
    SprintRepository sprintRepository;

    @InjectMocks
    SprintService service;

    private Sprint plannedSprint() {
        // Uses seed Sprint 1 dates/name but keeps PLANNED status for the transition test.
        return Sprint.builder()
                .sprintId(1L)
                .name("Sprint 1 - Desarrollo base del sistema")
                .startDate(LocalDate.of(2025, 1, 6))
                .endDate(LocalDate.of(2025, 1, 24))
                .status(Sprint.Status.PLANNED)
                .deleted(false)
                .build();
    }

    @Test
    void findAll_returnsNonDeletedSprints() {
        Sprint s = plannedSprint();
        when(sprintRepository.findAllByDeletedFalse()).thenReturn(List.of(s));

        assertThat(service.findAll()).containsExactly(s);
    }

    @Test
    void findById_found_returnsSprint() {
        Sprint s = plannedSprint();
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThat(service.findById(1L)).isPresent().contains(s);
    }

    @Test
    void findByStatus_returnsMatchingList() {
        Sprint s = plannedSprint();
        s.setStatus(Sprint.Status.ACTIVE);
        when(sprintRepository.findByStatusAndDeletedFalse(Sprint.Status.ACTIVE)).thenReturn(List.of(s));

        assertThat(service.findByStatus(Sprint.Status.ACTIVE)).containsExactly(s);
    }

    @Test
    void save_persistsSprint() {
        Sprint s = plannedSprint();
        when(sprintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.save(s)).isSameAs(s);
    }

    @Test
    void updateStatus_changesStatusAndSaves() {
        Sprint s = plannedSprint();
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(s));
        when(sprintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Sprint result = service.updateStatus(1L, Sprint.Status.ACTIVE);

        assertThat(result.getStatus()).isEqualTo(Sprint.Status.ACTIVE);
    }

    @Test
    void updateStatus_notFound_throwsEntityNotFoundException() {
        when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.updateStatus(99L, Sprint.Status.ACTIVE));
        verify(sprintRepository, never()).save(any());
    }

    @Test
    void softDelete_setsDeletedFlagAndTimestamp() {
        Sprint s = plannedSprint();
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(s));
        when(sprintRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(1L);

        verify(sprintRepository).save(argThat(sprint ->
                Boolean.TRUE.equals(sprint.getDeleted()) && sprint.getDeletedAt() != null
        ));
    }

    @Test
    void softDelete_notFound_throwsEntityNotFoundException() {
        when(sprintRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.softDelete(99L));
        verify(sprintRepository, never()).save(any());
    }
}
