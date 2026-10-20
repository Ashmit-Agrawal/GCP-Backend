package com.gcp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "redemptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
