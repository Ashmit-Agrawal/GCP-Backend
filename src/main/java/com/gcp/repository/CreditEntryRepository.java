package com.gcp.repository;

import com.gcp.enums.CreditStatus;
import com.gcp.model.CreditEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CreditEntryRepository extends JpaRepository<CreditEntry, Long> {
    List<CreditEntry> findByCompanyId(Long companyId);

    List<CreditEntry> findByParcelId(String parcelId);

    List<CreditEntry> findByStatus(CreditStatus status);

    List<CreditEntry> findByStatusIn(List<CreditStatus> statuses);

    long countByStatusIn(List<CreditStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CreditEntry c WHERE c.id = :id")
    Optional<CreditEntry> findByIdForUpdate(@Param("id") Long id);
}
