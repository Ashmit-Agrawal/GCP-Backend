package com.gcp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_entries")
public class CreditEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    private String parcelId;

    private double areaHectares;

    private double canopyGrowthPercent;

    private double credits;

    @Enumerated(EnumType.STRING)
    private VerificationMethod verificationMethod;

    @Enumerated(EnumType.STRING)
    private CreditStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime decidedAt;

    private String decidedBy;

    private String remarks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public String getParcelId() { return parcelId; }
    public void setParcelId(String parcelId) { this.parcelId = parcelId; }

    public double getAreaHectares() { return areaHectares; }
    public void setAreaHectares(double areaHectares) { this.areaHectares = areaHectares; }

    public double getCanopyGrowthPercent() { return canopyGrowthPercent; }
    public void setCanopyGrowthPercent(double canopyGrowthPercent) { this.canopyGrowthPercent = canopyGrowthPercent; }

    public double getCredits() { return credits; }
    public void setCredits(double credits) { this.credits = credits; }

    public VerificationMethod getVerificationMethod() { return verificationMethod; }
    public void setVerificationMethod(VerificationMethod verificationMethod) { this.verificationMethod = verificationMethod; }

    public CreditStatus getStatus() { return status; }
    public void setStatus(CreditStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }

    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
