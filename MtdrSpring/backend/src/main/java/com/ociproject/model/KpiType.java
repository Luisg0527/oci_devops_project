package com.ociproject.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "KPI_TYPES")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor 
@Builder
public class KpiType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "KPI_TYPE_ID")
    private Long kpiTypeId;

    @Column(name = "NAME", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "CATEGORY", length = 50)
    private String category;

    @Column(name = "UNIT", length = 50)
    private String unit;
}
