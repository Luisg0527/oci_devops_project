package com.ociproject.service.ai.source;

import com.ociproject.model.KpiValue;
import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.repository.KpiValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KpiValueSourceReader implements SourceReader {

    private final KpiValueRepository kpiValueRepository;

    @Override
    public SourceType type() { return SourceType.KPI_VALUE; }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> readAll() {
        List<KpiValue> values = kpiValueRepository.findAll();
        List<DocumentChunk> out = new ArrayList<>(values.size());
        for (KpiValue v : values) {
            StringBuilder sb = new StringBuilder();
            sb.append("KPI: ");
            if (v.getKpiType() != null) {
                sb.append(v.getKpiType().getName());
                if (v.getKpiType().getUnit() != null) sb.append(" (").append(v.getKpiType().getUnit()).append(')');
            } else {
                sb.append("(sin tipo)");
            }
            sb.append('\n');
            sb.append("Valor: ").append(v.getValue()).append('\n');
            sb.append("Alcance: ").append(v.getScopeType() != null ? v.getScopeType().name() : "GLOBAL").append('\n');
            if (v.getProject() != null) {
                sb.append("Proyecto: ").append(v.getProject().getName())
                  .append(" (id=").append(v.getProject().getProjectId()).append(")\n");
            }
            if (v.getSprint() != null) {
                sb.append("Sprint: ").append(v.getSprint().getName())
                  .append(" (id=").append(v.getSprint().getSprintId()).append(")\n");
            }
            if (v.getUser() != null) {
                sb.append("Usuario: ").append(v.getUser().getFullName())
                  .append(" (id=").append(v.getUser().getUserId()).append(")\n");
            }
            if (v.getRecordedAt() != null) {
                sb.append("Registrado: ").append(v.getRecordedAt()).append('\n');
            }
            out.add(new DocumentChunk(SourceType.KPI_VALUE, v.getKpiValueId(), sb.toString()));
        }
        return out;
    }
}
