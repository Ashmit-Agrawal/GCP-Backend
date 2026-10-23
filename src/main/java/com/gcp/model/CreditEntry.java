package com.gcp.model;

import com.gcp.enums.CreditStatus;
import com.gcp.enums.VerificationMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "credit_entries")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CreditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
}
