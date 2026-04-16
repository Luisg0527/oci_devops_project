package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.Deployment;
import com.ociproject.repository.DeploymentRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceTest {

    @Mock
    DeploymentRepository deploymentRepository;

    @InjectMocks
    DeploymentService service;

    private Deployment deployment() {
        return TestFixtures.deployment1();
    }

    @Test
    void findById_found() {
        Deployment d = deployment();
        when(deploymentRepository.findById(1L)).thenReturn(Optional.of(d));

        assertThat(service.findById(1L)).isPresent().contains(d);
    }

    @Test
    void findByProject_returnsList() {
        when(deploymentRepository.findByProjectProjectId(1L)).thenReturn(List.of(deployment()));

        assertThat(service.findByProject(1L)).hasSize(1);
    }

    @Test
    void findByStatus_returnsList() {
        when(deploymentRepository.findByStatus(Deployment.Status.SUCCESS)).thenReturn(List.of(deployment()));

        assertThat(service.findByStatus(Deployment.Status.SUCCESS)).hasSize(1);
    }

    @Test
    void findByEnvironment_returnsList() {
        when(deploymentRepository.findByEnvironment(Deployment.Environment.PRODUCTION)).thenReturn(List.of(deployment()));

        assertThat(service.findByEnvironment(Deployment.Environment.PRODUCTION)).hasSize(1);
    }

    @Test
    void findByProjectAndStatus_returnsList() {
        when(deploymentRepository.findByProjectProjectIdAndStatus(1L, Deployment.Status.FAILED))
                .thenReturn(List.of(deployment()));

        assertThat(service.findByProjectAndStatus(1L, Deployment.Status.FAILED)).hasSize(1);
    }

    @Test
    void save_persistsDeployment() {
        Deployment d = deployment();
        when(deploymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.save(d)).isSameAs(d);
    }

    @Test
    void updateStatus_changesStatusAndSaves() {
        Deployment d = deployment();
        when(deploymentRepository.findById(1L)).thenReturn(Optional.of(d));
        when(deploymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Deployment result = service.updateStatus(1L, Deployment.Status.SUCCESS);

        assertThat(result.getStatus()).isEqualTo(Deployment.Status.SUCCESS);
    }

    @Test
    void updateStatus_notFound_throwsEntityNotFoundException() {
        when(deploymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.updateStatus(99L, Deployment.Status.SUCCESS));
        verify(deploymentRepository, never()).save(any());
    }
}
