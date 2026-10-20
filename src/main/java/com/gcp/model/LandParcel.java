package com.gcp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "land_parcels")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandParcel {

    @Id
    private String id;

    private double latitude;

    private double longitude;

    private double areaHectares;

    private String state;

    private String stateCode;

    private String location;

    private String forestType;

    @Enumerated(EnumType.STRING)
    private DegradationLevel degradationLevel;

    @Enumerated(EnumType.STRING)
    private ParcelStatus status;

    private Long allottedCompanyId;

    private LocalDate allotmentDate;

    private LocalDateTime createdAt;
}
