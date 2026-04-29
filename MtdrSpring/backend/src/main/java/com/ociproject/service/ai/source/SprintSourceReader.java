package com.ociproject.service.ai.source;

import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.model.Sprint;
import com.ociproject.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SprintSourceReader implements SourceReader {

    private final SprintRepository sprintRepository;

    @Override
    public SourceType type() { return SourceType.SPRINT; }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> readAll() {
        List<Sprint> sprints = sprintRepository.findAllByDeletedFalse();
        List<DocumentChunk> out = new ArrayList<>(sprints.size());
        for (Sprint s : sprints) {
            StringBuilder sb = new StringBuilder();
            sb.append("Sprint: ").append(s.getName() == null ? "" : s.getName()).append('\n');
            sb.append("Estado: ").append(s.getStatus() != null ? s.getStatus().name() : "N/A").append('\n');
            sb.append("Inicio: ").append(s.getStartDate()).append('\n');
            sb.append("Fin: ").append(s.getEndDate()).append('\n');
            if (s.getTotalHours() != null) {
                sb.append("Horas totales: ").append(s.getTotalHours()).append('\n');
            }
            out.add(new DocumentChunk(SourceType.SPRINT, s.getSprintId(), sb.toString()));
        }
        return out;
    }
}
