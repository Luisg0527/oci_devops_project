package com.ociproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTeamRequest {
    @NotBlank private String name;
    private String description;
}
