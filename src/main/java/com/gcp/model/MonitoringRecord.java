package com.gcp.model;

import com.gcp.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    private String note;
}
