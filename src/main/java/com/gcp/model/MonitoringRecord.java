package com.gcp.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "monitoring_records")
public class MonitoringRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String parcelId;

    private LocalDate recordedOn;

    private double canopyPercent;

    private double biomassIndex;

    private double moistureStability;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getParcelId() { return parcelId; }
    public void setParcelId(String parcelId) { this.parcelId = parcelId; }

    public LocalDate getRecordedOn() { return recordedOn; }
    public void setRecordedOn(LocalDate recordedOn) { this.recordedOn = recordedOn; }

    public double getCanopyPercent() { return canopyPercent; }
    public void setCanopyPercent(double canopyPercent) { this.canopyPercent = canopyPercent; }

    public double getBiomassIndex() { return biomassIndex; }
    public void setBiomassIndex(double biomassIndex) { this.biomassIndex = biomassIndex; }

    public double getMoistureStability() { return moistureStability; }
    public void setMoistureStability(double moistureStability) { this.moistureStability = moistureStability; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
