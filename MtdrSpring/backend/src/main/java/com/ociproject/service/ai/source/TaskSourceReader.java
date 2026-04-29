package com.ociproject.service.ai.source;

import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.model.Task;
import com.ociproject.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TaskSourceReader implements SourceReader {

    private final TaskRepository taskRepository;

    @Override
    public SourceType type() { return SourceType.TASK; }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> readAll() {
        List<Task> tasks = taskRepository.findAllByDeletedFalse();
        List<DocumentChunk> out = new ArrayList<>(tasks.size());
        for (Task t : tasks) {
            StringBuilder sb = new StringBuilder();
            sb.append("Tarea: ").append(t.getTitle() == null ? "" : t.getTitle()).append('\n');
            if (t.getProject() != null) {
                sb.append("Proyecto: ").append(t.getProject().getName())
                  .append(" (id=").append(t.getProject().getProjectId()).append(")\n");
            }
            if (t.getSprint() != null) {
                sb.append("Sprint: ").append(t.getSprint().getName())
                  .append(" (id=").append(t.getSprint().getSprintId()).append(")\n");
            }
            sb.append("Estado: ").append(t.getStatus() != null ? t.getStatus().name() : "N/A").append('\n');
            sb.append("Etapa: ").append(t.getTaskStage() != null ? t.getTaskStage().name() : "N/A").append('\n');
            if (t.getPriority() != null) {
                sb.append("Prioridad: ").append(t.getPriority().name()).append('\n');
            }
            if (t.getAssignedTo() != null) {
                sb.append("Asignado a: ").append(t.getAssignedTo().getFullName())
                  .append(" (id=").append(t.getAssignedTo().getUserId()).append(")\n");
            }
            if (t.getEstimatedHours() != null) {
                sb.append("Horas estimadas: ").append(t.getEstimatedHours()).append('\n');
            }
            if (t.getDueDate() != null) {
                sb.append("Fecha limite: ").append(t.getDueDate()).append('\n');
            }
            if (t.getDescription() != null && !t.getDescription().isBlank()) {
                sb.append("Descripcion: ").append(t.getDescription().trim()).append('\n');
            }
            out.add(new DocumentChunk(SourceType.TASK, t.getTaskId(), sb.toString()));
        }
        return out;
    }
}
