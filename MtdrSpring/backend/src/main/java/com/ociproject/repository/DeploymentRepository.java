package com.ociproject.repository;

import com.ociproject.model.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {
    List<Deployment> findByProjectProjectId(Long projectId);
    List<Deployment> findByStatus(Deployment.Status status);
    List<Deployment> findByEnvironment(Deployment.Environment environment);
    List<Deployment> findByProjectProjectIdAndStatus(Long projectId, Deployment.Status status);
}
