package com.gcp.model;

import com.gcp.enums.CreditMovementType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_ledger_movements", indexes = {
        @Index(name = "idx_ledger_company_id", columnList = "companyId"),
        @Index(name = "idx_ledger_tx_id", columnList = "transactionId", unique = true),
        @Index(name = "idx_ledger_ref", columnList = "referenceType, referenceId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditLedgerMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CreditMovementType type;

    @Column(length = 32)
    private String referenceType;

    private Long referenceId;

    @Column(precision = 18, scale = 4)
    private BigDecimal balanceAfter;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
