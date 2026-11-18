package com.gcp.repository;

import com.gcp.enums.RedemptionStatus;
import com.gcp.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {
    List<Redemption> findByCompanyId(Long companyId);

    List<Redemption> findByStatus(RedemptionStatus status);

    Optional<Redemption> findByIdempotencyKey(String idempotencyKey);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Redemption r WHERE r.id = :id")
    Optional<Redemption> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
