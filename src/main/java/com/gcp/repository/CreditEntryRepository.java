package com.gcp.repository;

import com.gcp.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditEntryRepository extends JpaRepository<CreditEntry, Long> {
    List<CreditEntry> findByCompanyId(Long companyId);
    List<CreditEntry> findByStatus(CreditStatus status);
}
