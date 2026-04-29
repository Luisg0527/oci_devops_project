package com.ociproject.service.ai.source;

import com.ociproject.model.Deployment;
import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.repository.DeploymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DeploymentSourceReader implements SourceReader {

    private final DeploymentRepository deploymentRepository;

    @Override
    public SourceType type() { return SourceType.DEPLOYMENT; }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> readAll() {
        List<Deployment> deployments = deploymentRepository.findAll();
        List<DocumentChunk> out = new ArrayList<>(deployments.size());
        for (Deployment d : deployments) {
            StringBuilder sb = new StringBuilder();
            sb.append("Deploy version: ").append(d.getVersion() == null ? "(sin version)" : d.getVersion()).append('\n');
            if (d.getProject() != null) {
                sb.append("Proyecto: ").append(d.getProject().getName())
                  .append(" (id=").append(d.getProject().getProjectId()).append(")\n");
            }
            sb.append("Entorno: ").append(d.getEnvironment() != null ? d.getEnvironment().name() : "N/A").append('\n');
            sb.append("Estado: ").append(d.getStatus() != null ? d.getStatus().name() : "N/A").append('\n');
            if (d.getDeployedAt() != null) {
                sb.append("Fecha: ").append(d.getDeployedAt()).append('\n');
            }
            if (d.getRecoveryTimeMin() != null) {
                sb.append("Tiempo de recuperacion (min): ").append(d.getRecoveryTimeMin()).append('\n');
            }
            out.add(new DocumentChunk(SourceType.DEPLOYMENT, d.getDeploymentId(), sb.toString()));
        }
        return out;
    }
}
