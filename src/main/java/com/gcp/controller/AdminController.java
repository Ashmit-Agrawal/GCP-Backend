package com.gcp.controller;

import com.gcp.dto.*;
import com.gcp.enums.*;
import com.gcp.model.*;
import com.gcp.repository.CompanyRepository;
import com.gcp.repository.CreditEntryRepository;
import com.gcp.repository.LandApplicationRepository;
import com.gcp.repository.LandParcelRepository;
import com.gcp.repository.RedemptionRepository;
import com.gcp.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Dedicated backend endpoints for Government / Admin Control.
 * Protected with ROLE_ADMIN authorization.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final CompanyService companyService;
    private final CompanyVerificationService verificationService;
    private final ApplicationService applicationService;
    private final ParcelService parcelService;
    private final CreditService creditService;
    private final RedemptionService redemptionService;
    private final AuditService auditService;

    private final CompanyRepository companyRepo;
    private final LandApplicationRepository applicationRepo;
    private final CreditEntryRepository creditRepo;
    private final LandParcelRepository parcelRepo;
    private final RedemptionRepository redemptionRepo;

    public AdminController(CompanyService companyService,
            CompanyVerificationService verificationService,
            ApplicationService applicationService,
            ParcelService parcelService,
            CreditService creditService,
            RedemptionService redemptionService,
            AuditService auditService,
            CompanyRepository companyRepo,
            LandApplicationRepository applicationRepo,
            CreditEntryRepository creditRepo,
            LandParcelRepository parcelRepo,
            RedemptionRepository redemptionRepo) {
        this.companyService = companyService;
        this.verificationService = verificationService;
        this.applicationService = applicationService;
        this.parcelService = parcelService;
        this.creditService = creditService;
        this.redemptionService = redemptionService;
        this.auditService = auditService;
        this.companyRepo = companyRepo;
        this.applicationRepo = applicationRepo;
        this.creditRepo = creditRepo;
        this.parcelRepo = parcelRepo;
        this.redemptionRepo = redemptionRepo;
    }

    /** Real-time administrative dashboard KPI metrics */
    @GetMapping("/dashboard/stats")
    public AdminDashboardStats getDashboardStats() {
        long totalCompanies = companyRepo.count();
        long pendingVerifications = companyRepo.countByStatus(CompanyStatus.PENDING_VERIFICATION);
        long pendingApps = applicationRepo.countByStatus(ApplicationStatus.PENDING_REVIEW);
        long pendingCredits = creditRepo.countByStatusIn(
                List.of(CreditStatus.DRAFT, CreditStatus.SUBMITTED, CreditStatus.IN_REVIEW));

        double totalCreditsIssued = creditRepo.findByStatus(CreditStatus.APPROVED).stream()
                .mapToDouble(CreditEntry::getCredits).sum();

        double totalCreditsRedeemed = redemptionRepo.findByStatus(RedemptionStatus.APPROVED).stream()
                .mapToDouble(r -> r.getCredits() > 0 ? r.getCredits()
                        : (r.getRedemptionOption() != null ? r.getRedemptionOption().getCost() : 0.0))
                .sum();

        double totalLandAllocated = parcelRepo.findAll().stream()
                .filter(p -> p.getStatus() == ParcelStatus.ALLOTTED)
                .mapToDouble(LandParcel::getAreaHectares).sum();

        long availableParcels = parcelRepo.findAll().stream()
                .filter(p -> p.getStatus() == ParcelStatus.AVAILABLE)
                .count();

        return new AdminDashboardStats(
                totalCompanies,
                pendingVerifications,
                pendingApps,
                pendingCredits,
                Math.round(totalCreditsIssued * 100.0) / 100.0,
                Math.round(totalCreditsRedeemed * 100.0) / 100.0,
                Math.round(totalLandAllocated * 10.0) / 10.0,
                availableParcels);
    }

    // ==================== COMPANY REGISTRATION & RANK CONTROL ====================

    /**
     * List companies with optional status filter (e.g.
     * ?status=PENDING_VERIFICATION)
     */
    @GetMapping("/companies")
    public List<Company> listCompanies(@RequestParam(required = false) CompanyStatus status) {
        return companyService.findByStatus(status);
    }

    /** Detailed view of a company with automated verification diagnostics */
    @GetMapping("/companies/{id}")
    public AdminCompanyDetails getCompanyDetails(@PathVariable Long id) {
        Company company = companyService.get(id);
        CompanyVerificationResult report = verificationService.verify(
                company.getCin(), company.getGstin(), company.getPan(), company.getAnnualTurnoverCrores());
        return new AdminCompanyDetails(company, report);
    }

    /** Officer approval or rejection of company registration */
    @PutMapping("/companies/{id}/decision")
    public Company decideCompanyRegistration(@PathVariable Long id, @Valid @RequestBody CompanyDecisionRequest req) {
        return companyService.decide(id, req);
    }

    /** Officer updates/adjusts the assigned rank of a company */
    @PutMapping("/companies/{id}/rank")
    public Company updateCompanyRank(@PathVariable Long id, @Valid @RequestBody RankUpdateRequest req) {
        return companyService.updateRank(id, req);
    }

    // ==================== APPLICATION MANAGEMENT ====================

    @GetMapping("/applications")
    public List<LandApplication> listApplications(@RequestParam(required = false) ApplicationStatus status) {
        return applicationService.find(status);
    }

    @PutMapping("/applications/{id}/decision")
    public LandApplication decideApplication(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return applicationService.decide(id, req);
    }

    // ==================== CREDIT MANAGEMENT ====================

    @GetMapping("/credits")
    public List<CreditEntry> listCredits(@RequestParam(required = false) CreditStatus status) {
        return creditService.find(status);
    }

    @PostMapping("/credits/calculate")
    @ResponseStatus(HttpStatus.CREATED)
    public CreditEntry calculateCredits(@Valid @RequestBody CreditCalculationRequest req) {
        return creditService.createDraft(req);
    }

    @PutMapping("/credits/{id}/decision")
    public CreditEntry decideCredits(@PathVariable Long id, @Valid @RequestBody CreditDecisionRequest req) {
        return creditService.decide(id, req);
    }

    // ==================== LAND PARCEL MANAGEMENT ====================

    @GetMapping("/parcels")
    public List<LandParcel> listParcels() {
        return parcelRepo.findAll();
    }

    @PostMapping("/parcels")
    @ResponseStatus(HttpStatus.CREATED)
    public LandParcel createParcel(@Valid @RequestBody ParcelRequest req) {
        return parcelService.create(req);
    }

    // ==================== REDEMPTION MANAGEMENT ====================

    @GetMapping("/redemptions")
    public List<Redemption> listRedemptions(@RequestParam(required = false) RedemptionStatus status) {
        return redemptionService.find(status);
    }

    @PutMapping("/redemptions/{id}/decision")
    public Redemption decideRedemption(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return redemptionService.decide(id, req);
    }

    // ==================== AUDIT TRAIL ====================

    @GetMapping("/audit")
    public List<AuditLog> listAudits(@RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId) {
        return auditService.find(entityType, entityId);
    }
}
