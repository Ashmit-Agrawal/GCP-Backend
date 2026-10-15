package com.gcp.service;

import com.gcp.dto.CompanyRequest;
import com.gcp.exception.BadRequestException;
import com.gcp.exception.NotFoundException;
import com.gcp.model.Company;
import com.gcp.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository repo;
    private final AuditService audit;

    public CompanyService(CompanyRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public Company register(CompanyRequest req) {
        if (repo.findByCin(req.cin()).isPresent()) {
            throw new BadRequestException("A company with CIN " + req.cin() + " is already registered");
        }
        Company c = new Company();
        c.setName(req.name());
        c.setSector(req.sector());
        c.setCin(req.cin());
        c.setEmail(req.email());
        c.setPhone(req.phone());
        c.setRegisteredAt(LocalDateTime.now());
        c = repo.save(c);
        audit.log(c.getName(), "COMPANY_REGISTERED", "Company", c.getId(), "CIN " + c.getCin());
        return c;
    }

    public List<Company> findAll() {
        return repo.findAll();
    }

    public Company get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Company " + id + " not found"));
    }
}
