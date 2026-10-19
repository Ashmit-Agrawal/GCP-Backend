package com.gcp.service;

import com.gcp.dto.CompanyDecisionRequest;
import com.gcp.dto.CompanyRegistrationRequest;
import com.gcp.dto.CompanyRequest;
import com.gcp.dto.CompanyVerificationResult;
import com.gcp.dto.RankUpdateRequest;
import com.gcp.exception.BadRequestException;
import com.gcp.exception.NotFoundException;
import com.gcp.model.Company;
import com.gcp.model.CompanyRank;
import com.gcp.model.CompanyStatus;
import com.gcp.model.Role;
import com.gcp.model.User;
import com.gcp.repository.CompanyRepository;
import com.gcp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompanyService {

    private final CompanyRepository repo;
    private final UserRepository userRepo;
    private final CompanyVerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public CompanyService(CompanyRepository repo,
                          UserRepository userRepo,
                          CompanyVerificationService verificationService,
                          PasswordEncoder passwordEncoder,
                          AuditService audit) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.verificationService = verificationService;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    /**
     * Full company registration with automated statutory checks (GSTIN, PAN, CIN)
     * and automatic rank suggestion based on turnover.
     */
    @Transactional
    public Company registerFull(CompanyRegistrationRequest req) {
        String cin = req.cin().trim().toUpperCase();
        String gstin = req.gstin().trim().toUpperCase();
        String pan = req.pan().trim().toUpperCase();
        String username = req.username().trim();

        // 1. Uniqueness checks
        if (repo.findByCin(cin).isPresent()) {
            throw new BadRequestException("A company with CIN " + cin + " is already registered.");
        }
        if (repo.findByGstin(gstin).isPresent()) {
            throw new BadRequestException("A company with GSTIN " + gstin + " is already registered.");
        }
        if (userRepo.existsByUsername(username)) {
            throw new BadRequestException("Username '" + username + "' is already taken.");
        }

        // 2. Automated Regex & Consistency Verification
        CompanyVerificationResult verification = verificationService.verify(
                cin, gstin, pan, req.annualTurnoverCrores()
        );

        if (!verification.validOverall()) {
            throw new BadRequestException(verification.summaryMessage());
        }

        // 3. Create Company entity in PENDING_VERIFICATION status
        Company c = new Company();
        c.setName(req.name().trim());
        c.setSector(req.sector().trim());
        c.setCin(cin);
        c.setGstin(gstin);
        c.setPan(pan);
        c.setState(req.state().trim());
        c.setContactPerson(req.contactPerson().trim());
        c.setEmail(req.email().trim());
        c.setPhone(req.phone().trim());
        c.setAnnualTurnoverCrores(req.annualTurnoverCrores());
        c.setRank(verification.suggestedRank());
        c.setStatus(CompanyStatus.PENDING_VERIFICATION);
        c.setUsername(username);
        c.setRegisteredAt(LocalDateTime.now());
        c = repo.save(c);

        // 4. Create User credentials (disabled until officer manual approval)
        User user = new User(
                username,
                passwordEncoder.encode(req.password()),
                Role.ROLE_COMPANY,
                c.getId(),
                c.getName(),
                c.getEmail(),
                false // disabled until approved
        );
        userRepo.save(user);

        audit.log(c.getName(), "COMPANY_REGISTRATION_SUBMITTED", "Company", c.getId(),
                "Turnover ₹" + c.getAnnualTurnoverCrores() + " Cr, Suggested Rank: " + c.getRank()
                        + ", Max Limit: " + c.getRank().getMaxAreaHectares() + " ha. Awaiting officer verification.");

        return c;
    }

    /**
     * Backward-compatible registration method for simple requests / demo seeder.
     */
    @Transactional
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
        c.setStatus(CompanyStatus.APPROVED);
        c.setRank(CompanyRank.GOLD);
        c = repo.save(c);
        audit.log(c.getName(), "COMPANY_REGISTERED", "Company", c.getId(), "CIN " + c.getCin());
        return c;
    }

    /**
     * Officer decision on pending company registration.
     */
    @Transactional
    public Company decide(Long id, CompanyDecisionRequest req) {
        Company company = get(id);
        if (company.getStatus() != CompanyStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Company " + id + " has already been decided (Status: " + company.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        company.setVerifiedAt(now);
        company.setVerifiedBy(req.officerName());

        if (req.approve()) {
            company.setStatus(CompanyStatus.APPROVED);
            if (req.rank() != null) {
                company.setRank(req.rank());
            }
            company.setRejectionReason(null);

            // Enable user credentials
            userRepo.findByCompanyId(id).ifPresent(u -> {
                u.setEnabled(true);
                userRepo.save(u);
            });

            audit.log(req.officerName(), "COMPANY_APPROVED", "Company", id,
                    "Approved with Rank " + company.getRank().name()
                            + " (Max parcel area: " + company.getRank().getMaxAreaHectares() + " ha)");
        } else {
            if (req.reason() == null || req.reason().isBlank()) {
                throw new BadRequestException("Rejection reason is required");
            }
            company.setStatus(CompanyStatus.REJECTED);
            company.setRejectionReason(req.reason());

            // Ensure user stays disabled
            userRepo.findByCompanyId(id).ifPresent(u -> {
                u.setEnabled(false);
                userRepo.save(u);
            });

            audit.log(req.officerName(), "COMPANY_REJECTED", "Company", id, "Reason: " + req.reason());
        }

        return repo.save(company);
    }

    /**
     * Officer updates company rank.
     */
    @Transactional
    public Company updateRank(Long id, RankUpdateRequest req) {
        Company company = get(id);
        CompanyRank oldRank = company.getRank();
        company.setRank(req.rank());
        company = repo.save(company);

        String actor = (req.officerName() != null && !req.officerName().isBlank()) ? req.officerName() : "Admin Officer";
        audit.log(actor, "COMPANY_RANK_UPDATED", "Company", id,
                "Rank changed from " + oldRank + " to " + req.rank()
                        + " (Max limit: " + req.rank().getMaxAreaHectares() + " ha). Reason: " + req.reason());

        return company;
    }

    public List<Company> findAll() {
        return repo.findAll();
    }

    public List<Company> findByStatus(CompanyStatus status) {
        return status == null ? repo.findAll() : repo.findByStatus(status);
    }

    public Company get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Company " + id + " not found"));
    }
}
