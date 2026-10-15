package com.gcp.repository;

import com.gcp.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LandApplicationRepository extends JpaRepository<LandApplication, Long> {
    List<LandApplication> findByCompanyId(Long companyId);
    List<LandApplication> findByParcelId(String parcelId);
    List<LandApplication> findByStatus(ApplicationStatus status);
}
