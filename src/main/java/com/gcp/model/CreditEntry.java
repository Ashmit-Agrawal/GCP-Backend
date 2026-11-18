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
@Table(name = "credit_entries", indexes = {
        @Index(name = "idx_credit_company_status", columnList = "companyId, status"),
        @Index(name = "idx_credit_parcel_id", columnList = "parcelId")
})
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CreditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false, length = 64)
    private String parcelId;

    @Column(nullable = false)
    private double areaHectares;

    @Column(nullable = false)
    private double canopyGrowthPercent;

    @Column(nullable = false)
    private double credits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VerificationMethod verificationMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CreditStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime decidedAt;

    private String decidedBy;

    private String remarks;

    private String policyVersion;

    @Transient
    private String companyName;

    @Transient
    private String parcelLocation;

    @Transient
    private String parcelState;

    @Version
    private Long version;
}
