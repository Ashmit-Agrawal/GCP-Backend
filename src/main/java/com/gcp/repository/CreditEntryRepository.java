package com.gcp.repository;

import com.gcp.enums.CreditStatus;
import com.gcp.model.CreditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditEntryRepository extends JpaRepository<CreditEntry, Long> {
    List<CreditEntry> findByCompanyId(Long companyId);

    List<CreditEntry> findByStatus(CreditStatus status);

    List<CreditEntry> findByStatusIn(List<CreditStatus> statuses);

    long countByStatusIn(List<CreditStatus> statuses);
}
