package com.ociproject.dto.response;

import com.ociproject.model.Incident;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IncidentResponse {
    private Long incidentId;
    private Long projectId;
    private String projectName;
    private String type;
    private String description;
    private String severity;
    private LocalDateTime occurredAt;
    private LocalDateTime resolvedAt;
    private Boolean isOpen;
    private Boolean isDeleted;

    public static IncidentResponse from(Incident i) {
        return IncidentResponse.builder()
                .incidentId(i.getIncidentId())
                .projectId(i.getProject() != null ? i.getProject().getProjectId() : null)
                .projectName(i.getProject() != null ? i.getProject().getName() : null)
                .type(i.getType())
                .description(i.getDescription())
                .severity(i.getSeverity() != null ? i.getSeverity().name() : null)
                .occurredAt(i.getOccurredAt())
                .resolvedAt(i.getResolvedAt())
                .isOpen(i.getResolvedAt() == null)
                .isDeleted(i.getDeleted())
                .build();
    }
}
