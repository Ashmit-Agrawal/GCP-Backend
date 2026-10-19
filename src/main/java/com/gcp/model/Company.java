package com.gcp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Double getAnnualTurnoverCrores() { return annualTurnoverCrores; }
    public void setAnnualTurnoverCrores(Double annualTurnoverCrores) { this.annualTurnoverCrores = annualTurnoverCrores; }

    public CompanyRank getRank() { return rank; }
    public void setRank(CompanyRank rank) { this.rank = rank; }

    public CompanyStatus getStatus() { return status; }
    public void setStatus(CompanyStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
}
