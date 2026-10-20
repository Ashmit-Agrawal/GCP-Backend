package com.gcp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "land_applications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandApplication {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    private String parcelId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private LocalDateTime submittedAt;

    private LocalDateTime decidedAt;

    private String decidedBy;

    private String decisionReason;
}
