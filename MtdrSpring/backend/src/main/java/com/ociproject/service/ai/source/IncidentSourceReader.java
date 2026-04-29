package com.ociproject.service.ai.source;

import com.ociproject.model.Incident;
import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class IncidentSourceReader implements SourceReader {

    private final IncidentRepository incidentRepository;

    @Override
    public SourceType type() { return SourceType.INCIDENT; }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> readAll() {
        List<Incident> incidents = incidentRepository.findAll();
        List<DocumentChunk> out = new ArrayList<>(incidents.size());
        for (Incident i : incidents) {
            if (Boolean.TRUE.equals(i.getDeleted())) continue;
            StringBuilder sb = new StringBuilder();
            sb.append("Incidente: ").append(i.getType() == null ? "(sin tipo)" : i.getType()).append('\n');
            if (i.getProject() != null) {
                sb.append("Proyecto: ").append(i.getProject().getName())
                  .append(" (id=").append(i.getProject().getProjectId()).append(")\n");
            }
            sb.append("Severidad: ").append(i.getSeverity() != null ? i.getSeverity().name() : "N/A").append('\n');
            if (i.getOccurredAt() != null) {
                sb.append("Ocurrido: ").append(i.getOccurredAt()).append('\n');
            }
            if (i.getResolvedAt() != null) {
                sb.append("Resuelto: ").append(i.getResolvedAt()).append('\n');
            } else {
                sb.append("Estado: abierto\n");
            }
            if (i.getDescription() != null && !i.getDescription().isBlank()) {
                sb.append("Descripcion: ").append(i.getDescription().trim()).append('\n');
            }
            out.add(new DocumentChunk(SourceType.INCIDENT, i.getIncidentId(), sb.toString()));
        }
        return out;
    }
}
