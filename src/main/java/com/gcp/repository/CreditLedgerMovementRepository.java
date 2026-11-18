package com.gcp.repository;

import com.gcp.model.CreditLedgerMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditLedgerMovementRepository extends JpaRepository<CreditLedgerMovement, Long> {

    List<CreditLedgerMovement> findByCompanyIdOrderByTimestampDesc(Long companyId);

    Optional<CreditLedgerMovement> findByTransactionId(String transactionId);

    List<CreditLedgerMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    boolean existsByTransactionId(String transactionId);
}
