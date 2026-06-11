package com.ociproject.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateSprintRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    /** Proyecto al que pertenece el sprint; sincroniza el flag activo por proyecto. */
    private Long projectId;
}
