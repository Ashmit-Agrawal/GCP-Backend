package com.gcp.repository;

import com.gcp.enums.AllocationStatus;
import com.gcp.model.ParcelAllocation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParcelAllocationRepository extends JpaRepository<ParcelAllocation, Long> {

    Optional<ParcelAllocation> findByParcelIdAndStatus(String parcelId, AllocationStatus status);

    Optional<ParcelAllocation> findByApplicationId(Long applicationId);

    List<ParcelAllocation> findByCompanyId(Long companyId);

    List<ParcelAllocation> findByParcelId(String parcelId);

    Optional<ParcelAllocation> findByIdempotencyKey(String idempotencyKey);

    boolean existsByParcelIdAndStatus(String parcelId, AllocationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ParcelAllocation p WHERE p.parcelId = :parcelId AND p.status = :status")
    Optional<ParcelAllocation> findByParcelIdAndStatusForUpdate(
            @Param("parcelId") String parcelId,
            @Param("status") AllocationStatus status);
}
