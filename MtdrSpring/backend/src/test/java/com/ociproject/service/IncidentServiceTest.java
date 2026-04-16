package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.Incident;
import com.ociproject.repository.IncidentRepository;
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
class IncidentServiceTest {

    @Mock
    IncidentRepository incidentRepository;

    @InjectMocks
    IncidentService service;

    private Incident openIncident() {
        return TestFixtures.incident1();
    }

    @Test
    void findById_found() {
        Incident i = openIncident();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(i));

        assertThat(service.findById(1L)).isPresent().contains(i);
    }

    @Test
    void findByProject_returnsNonDeletedList() {
        when(incidentRepository.findByProjectProjectIdAndDeletedFalse(1L)).thenReturn(List.of(openIncident()));

        assertThat(service.findByProject(1L)).hasSize(1);
    }

    @Test
    void findBySeverity_returnsList() {
        when(incidentRepository.findBySeverityAndDeletedFalse(Incident.Severity.CRITICAL))
                .thenReturn(List.of(openIncident()));

        assertThat(service.findBySeverity(Incident.Severity.CRITICAL)).hasSize(1);
    }

    @Test
    void findUnresolved_returnsOpenIncidents() {
        when(incidentRepository.findByResolvedAtIsNullAndDeletedFalse()).thenReturn(List.of(openIncident()));

        assertThat(service.findUnresolved()).hasSize(1);
    }

    @Test
    void save_persistsIncident() {
        Incident i = openIncident();
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.save(i)).isSameAs(i);
    }

    @Test
    void resolve_setsResolvedAt() {
        Incident i = openIncident();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(i));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resolve(1L);

        verify(incidentRepository).save(argThat(inc -> inc.getResolvedAt() != null));
    }

    @Test
    void resolve_notFound_throwsEntityNotFoundException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.resolve(99L));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void softDelete_setsDeletedFlagAndTimestamp() {
        Incident i = openIncident();
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(i));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(1L);

        verify(incidentRepository).save(argThat(inc ->
                Boolean.TRUE.equals(inc.getDeleted()) && inc.getDeletedAt() != null
        ));
    }

    @Test
    void softDelete_notFound_throwsEntityNotFoundException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.softDelete(99L));
        verify(incidentRepository, never()).save(any());
    }
}
