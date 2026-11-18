package com.gcp.model;

import com.gcp.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "parcel_allocations", indexes = {
        @Index(name = "idx_parcel_alloc_parcel_id", columnList = "parcelId"),
        @Index(name = "idx_parcel_alloc_company_id", columnList = "companyId"),
        @Index(name = "idx_parcel_alloc_status", columnList = "status"),
        @Index(name = "idx_parcel_alloc_idemp", columnList = "idempotencyKey", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String parcelId;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false, unique = true)
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AllocationStatus status;

    @Column(nullable = false)
    private LocalDate allotmentDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime releasedAt;

    @Column(length = 64)
    private String idempotencyKey;

    @Version
    private Long version;
}
