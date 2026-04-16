package com.ociproject.dto.request;

import lombok.Data;

@Data
public class UpdateProjectRequest {
    private String name;
    private String description;
    private Long managerId;
    private String status;
}
