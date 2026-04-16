package com.ociproject.repository;

import com.ociproject.model.ProjectSprint;
import com.ociproject.model.ProjectSprintId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectSprintRepository extends JpaRepository<ProjectSprint, ProjectSprintId> {
    List<ProjectSprint> findByIdProjectId(Long projectId);
    Optional<ProjectSprint> findByIdProjectIdAndActiveTrue(Long projectId);
    boolean existsByIdProjectIdAndSprintNumber(Long projectId, Integer sprintNumber);
}
