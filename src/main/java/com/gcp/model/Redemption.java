package com.gcp.model;

import com.gcp.enums.RedemptionOption;
import com.gcp.enums.RedemptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "redemptions", indexes = {
        @Index(name = "idx_redemption_company_status", columnList = "companyId, status"),
        @Index(name = "idx_redemption_idemp", columnList = "idempotencyKey", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Redemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private RedemptionOption redemptionOption;

    @Column(nullable = false)
    private double credits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RedemptionStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime decidedAt;

    private String decidedBy;

    private String remarks;

    @Column(length = 64)
    private String idempotencyKey;

    @Transient
    private String companyName;

    @Transient
    private String purpose;

    @Version
    private Long version;
}
