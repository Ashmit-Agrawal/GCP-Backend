package com.gcp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemptions")
public class Redemption {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Enumerated(EnumType.STRING)
    private RedemptionOption redemptionOption;

    private double credits;

    @Enumerated(EnumType.STRING)
    private RedemptionStatus status;

    private LocalDateTime requestedAt;

    private LocalDateTime decidedAt;

    private String decidedBy;

    private String remarks;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public RedemptionOption getRedemptionOption() { return redemptionOption; }
    public void setRedemptionOption(RedemptionOption redemptionOption) { this.redemptionOption = redemptionOption; }

    public double getCredits() { return credits; }
    public void setCredits(double credits) { this.credits = credits; }

    public RedemptionStatus getStatus() { return status; }
    public void setStatus(RedemptionStatus status) { this.status = status; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }

    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
