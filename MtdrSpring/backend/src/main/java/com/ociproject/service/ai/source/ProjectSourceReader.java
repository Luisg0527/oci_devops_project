package com.ociproject.service.ai.source;

import com.ociproject.model.Project;
import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProjectSourceReader implements SourceReader {

    private final ProjectRepository projectRepository;

    @Override
    public SourceType type() { return SourceType.PROJECT; }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> readAll() {
        List<Project> projects = projectRepository.findAllByDeletedFalse();
        List<DocumentChunk> out = new ArrayList<>(projects.size());
        for (Project p : projects) {
            StringBuilder sb = new StringBuilder();
            sb.append("Proyecto: ").append(safe(p.getName())).append('\n');
            sb.append("Estado: ").append(p.getStatus() != null ? p.getStatus().name() : "N/A").append('\n');
            if (p.getManager() != null) {
                sb.append("Manager: ").append(safe(p.getManager().getFullName()))
                  .append(" (id=").append(p.getManager().getUserId()).append(")\n");
            }
            if (p.getTotalHours() != null) {
                sb.append("Horas totales acumuladas: ").append(p.getTotalHours()).append('\n');
            }
            if (p.getDescription() != null && !p.getDescription().isBlank()) {
                sb.append("Descripcion: ").append(p.getDescription().trim()).append('\n');
            }
            out.add(new DocumentChunk(SourceType.PROJECT, p.getProjectId(), sb.toString()));
        }
        return out;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
