package com.ociproject.service;

import com.ociproject.model.Deployment;
import com.ociproject.repository.DeploymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;

    public Optional<Deployment> findById(Long id) {
        return deploymentRepository.findById(id);
    }

    public List<Deployment> findByProject(Long projectId) {
        return deploymentRepository.findByProjectProjectId(projectId);
    }

    public List<Deployment> findByStatus(Deployment.Status status) {
        return deploymentRepository.findByStatus(status);
    }

    public List<Deployment> findByEnvironment(Deployment.Environment environment) {
        return deploymentRepository.findByEnvironment(environment);
    }

    public List<Deployment> findByProjectAndStatus(Long projectId, Deployment.Status status) {
        return deploymentRepository.findByProjectProjectIdAndStatus(projectId, status);
    }

    @Transactional
    public Deployment save(Deployment deployment) {
        return deploymentRepository.save(deployment);
    }

    @Transactional
    public Deployment updateStatus(Long id, Deployment.Status newStatus) {
        Deployment deployment = deploymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Deployment not found: " + id));
        deployment.setStatus(newStatus);
        return deploymentRepository.save(deployment);
    }
}
