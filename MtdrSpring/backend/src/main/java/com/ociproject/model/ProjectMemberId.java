package com.ociproject.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ProjectMemberId implements Serializable {

    @Column(name = "PROJECT_ID")
    private Long projectId;

    @Column(name = "USER_ID")
    private Long userId;
}
