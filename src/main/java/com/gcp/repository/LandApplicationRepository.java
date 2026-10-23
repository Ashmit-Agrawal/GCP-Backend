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
}
