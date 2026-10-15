package com.gcp.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "land_parcels")
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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAreaHectares() { return areaHectares; }
    public void setAreaHectares(double areaHectares) { this.areaHectares = areaHectares; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getForestType() { return forestType; }
    public void setForestType(String forestType) { this.forestType = forestType; }

    public DegradationLevel getDegradationLevel() { return degradationLevel; }
    public void setDegradationLevel(DegradationLevel degradationLevel) { this.degradationLevel = degradationLevel; }

    public ParcelStatus getStatus() { return status; }
    public void setStatus(ParcelStatus status) { this.status = status; }

    public Long getAllottedCompanyId() { return allottedCompanyId; }
    public void setAllottedCompanyId(Long allottedCompanyId) { this.allottedCompanyId = allottedCompanyId; }

    public LocalDate getAllotmentDate() { return allotmentDate; }
    public void setAllotmentDate(LocalDate allotmentDate) { this.allotmentDate = allotmentDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
