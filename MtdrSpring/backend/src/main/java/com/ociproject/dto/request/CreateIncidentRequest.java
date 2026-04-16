package com.ociproject.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateIncidentRequest {
    @NotNull private Long projectId;
    private String type;
    private String description;
    private String severity;
    private LocalDateTime occurredAt;
}
