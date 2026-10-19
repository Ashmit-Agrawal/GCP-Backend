package com.gcp.repository;

import com.gcp.model.Company;
import com.gcp.model.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByCin(String cin);
    Optional<Company> findByGstin(String gstin);
    Optional<Company> findByUsername(String username);
    List<Company> findByStatus(CompanyStatus status);
    long countByStatus(CompanyStatus status);
}
