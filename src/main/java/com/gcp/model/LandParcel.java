package com.gcp.model;

import com.gcp.enums.DegradationLevel;
import com.gcp.enums.ParcelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "land_parcels")
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

    private DegradationLevel degradationLevel;

    private ParcelStatus status;

    private Long allottedCompanyId;

    private LocalDate allotmentDate;

    private LocalDateTime createdAt;
}
