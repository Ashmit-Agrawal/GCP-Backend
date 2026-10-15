package com.gcp.repository;

import com.gcp.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {
    List<Redemption> findByCompanyId(Long companyId);
    List<Redemption> findByStatus(RedemptionStatus status);
}
