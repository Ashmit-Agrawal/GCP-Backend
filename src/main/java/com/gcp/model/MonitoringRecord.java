package com.gcp.model;

import com.gcp.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitoring_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String parcelId;

    private LocalDate recordedOn;

    private double canopyPercent;

    private double biomassIndex;

    private double moistureStability;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(length = 1000)
    private String note;

    private String providerName;

    private String modelVersion;

    private Double confidenceScore;

    private Boolean isSimulated;

    private String inspectionStatus;

    @Column(length = 1000)
    private String inspectorNotes;

    private String inspectedBy;

    private LocalDateTime inspectedAt;

    @Transient
    private String parcelLocation;

    @Transient
    private String parcelState;

    @Transient
    private Double parcelAreaHectares;

    @Transient
    private Long companyId;

    @Transient
    private String companyName;
}
