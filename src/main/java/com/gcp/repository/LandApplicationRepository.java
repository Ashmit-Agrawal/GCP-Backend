package com.gcp.repository;

import com.gcp.enums.ApplicationStatus;
import com.gcp.model.LandApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LandApplicationRepository extends JpaRepository<LandApplication, Long> {
    List<LandApplication> findByCompanyId(Long companyId);

    List<LandApplication> findByParcelId(String parcelId);

    List<LandApplication> findByStatus(ApplicationStatus status);

    long countByStatus(ApplicationStatus status);

    java.util.Optional<LandApplication> findByIdempotencyKey(String idempotencyKey);

    java.util.Optional<LandApplication> findByParcelIdAndStatus(String parcelId, ApplicationStatus status);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT a FROM LandApplication a WHERE a.id = :id")
    java.util.Optional<LandApplication> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
