package com.ociproject.service;

import com.ociproject.model.Incident;
import com.ociproject.repository.IncidentRepository;
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
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public Optional<Incident> findById(Long id) {
        return incidentRepository.findById(id);
    }

    public List<Incident> findByProject(Long projectId) {
        return incidentRepository.findByProjectProjectIdAndDeletedFalse(projectId);
    }

    public List<Incident> findBySeverity(Incident.Severity severity) {
        return incidentRepository.findBySeverityAndDeletedFalse(severity);
    }

    public List<Incident> findUnresolved() {
        return incidentRepository.findByResolvedAtIsNullAndDeletedFalse();
    }

    @Transactional
    public Incident save(Incident incident) {
        return incidentRepository.save(incident);
    }

    @Transactional
    public Incident resolve(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incident not found: " + id));
        incident.setResolvedAt(LocalDateTime.now());
        return incidentRepository.save(incident);
    }

    @Transactional
    public void softDelete(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Incident not found: " + id));
        incident.setDeleted(true);
        incident.setDeletedAt(LocalDateTime.now());
        incidentRepository.save(incident);
    }
}
