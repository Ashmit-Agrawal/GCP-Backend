package com.gcp.model;

import com.gcp.enums.CompanyRank;
import com.gcp.enums.CompanyStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String sector;

    @Column(unique = true, nullable = false)
    private String cin;

    @Column(unique = true)
    private String gstin;

    private String pan;

    private String state;

    private String contactPerson;

    private String email;

    private String phone;

    private Double annualTurnoverCrores;

    @Enumerated(EnumType.STRING)
    private CompanyRank rank;

    @Enumerated(EnumType.STRING)
    private CompanyStatus status;

    private String rejectionReason;

    private String username;

    private LocalDateTime registeredAt;

    private LocalDateTime verifiedAt;

    private String verifiedBy;
}
