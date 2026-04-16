package com.ociproject.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateIncidentRequest {
    private String type;
    private String description;
    private String severity;
    private LocalDateTime resolvedAt;
}
