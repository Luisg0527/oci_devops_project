package com.ociproject.repository;

import com.ociproject.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByProjectProjectIdAndDeletedFalse(Long projectId);
    List<Incident> findBySeverityAndDeletedFalse(Incident.Severity severity);
    List<Incident> findByResolvedAtIsNullAndDeletedFalse();
    List<Incident> findByProjectProjectIdAndSeverityAndDeletedFalse(Long projectId, Incident.Severity severity);
}
