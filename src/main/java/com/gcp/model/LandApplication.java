package com.gcp.model;

import com.gcp.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "land_applications", indexes = {
        @Index(name = "idx_land_app_parcel_status", columnList = "parcelId, status"),
        @Index(name = "idx_land_app_company_status", columnList = "companyId, status"),
        @Index(name = "idx_land_app_idemp", columnList = "idempotencyKey", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false, length = 64)
    private String parcelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime decidedAt;

    private String decidedBy;

    private String decisionReason;

    @Column(length = 64)
    private String idempotencyKey;

    private String proposedSpecies;

    private Integer saplingCount;

    @Column(length = 1000)
    private String plantingPlanSummary;

    private String supportingDocRef;

    private String plantationStatus;

    @Column(length = 1000)
    private String lastActivityNote;

    @Transient
    private String companyName;

    @Transient
    private String companySector;

    @Transient
    private String companyRank;

    @Transient
    private String parcelLocation;

    @Transient
    private String parcelState;

    @Transient
    private Double parcelAreaHectares;

    @Version
    private Long version;
}
