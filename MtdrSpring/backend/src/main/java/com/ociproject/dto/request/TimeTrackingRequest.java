package com.ociproject.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TimeTrackingRequest {
    @NotNull private Long taskId;
}
