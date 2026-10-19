package com.gcp.controller;

import com.gcp.dto.CompanyRegistrationRequest;
import com.gcp.dto.CompanyRequest;
import com.gcp.dto.CompanyVerificationResult;
import com.gcp.dto.CreditAccount;
import com.gcp.model.Company;
import com.gcp.model.CompanyStatus;
import com.gcp.model.CreditEntry;
import com.gcp.model.LandApplication;
import com.gcp.model.LandParcel;
import com.gcp.model.Redemption;
import com.gcp.service.ApplicationService;
import com.gcp.service.CompanyService;
import com.gcp.service.CompanyVerificationService;
import com.gcp.service.CreditService;
import com.gcp.service.ParcelService;
import com.gcp.service.RedemptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Everything a company's dashboard needs, grouped under /api/companies/{id}/... */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companies;
    private final CompanyVerificationService verificationService;
    private final ApplicationService applications;
    private final ParcelService parcels;
    private final CreditService credits;
    private final RedemptionService redemptions;

    public CompanyController(CompanyService companies,
                             CompanyVerificationService verificationService,
                             ApplicationService applications,
                             ParcelService parcels,
                             CreditService credits,
                             RedemptionService redemptions) {
        this.companies = companies;
        this.verificationService = verificationService;
        this.applications = applications;
        this.parcels = parcels;
        this.credits = credits;
        this.redemptions = redemptions;
    }

    /** Full company registration with GSTIN/PAN/CIN validation and rank calculation */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Company registerFull(@Valid @RequestBody CompanyRegistrationRequest req) {
        return companies.registerFull(req);
    }

    /** Pre-registration verification check (preview without persisting) */
    @PostMapping("/verify-check")
    public CompanyVerificationResult checkVerification(@RequestBody VerifyCheckRequest req) {
        return verificationService.verify(req.cin(), req.gstin(), req.pan(), req.annualTurnoverCrores());
    }

    public record VerifyCheckRequest(String cin, String gstin, String pan, Double annualTurnoverCrores) {}

    /** Legacy register endpoint */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Company register(@Valid @RequestBody CompanyRequest req) {
        return companies.register(req);
    }

    @GetMapping
    public List<Company> all(@RequestParam(required = false) CompanyStatus status) {
        return companies.findByStatus(status);
    }

    @GetMapping("/{id}")
    public Company one(@PathVariable Long id) {
        return companies.get(id);
    }

    /** "Applications Under Review" and decided applications. */
    @GetMapping("/{id}/applications")
    public List<LandApplication> applications(@PathVariable Long id) {
        return applications.findForCompany(id);
    }

    /** "Allocated Land Parcels". */
    @GetMapping("/{id}/parcels")
    public List<LandParcel> allottedParcels(@PathVariable Long id) {
        companies.get(id);
        return parcels.findAllottedTo(id);
    }

    /** Credits Dashboard: total earned, redeemed, available, in pipeline. */
    @GetMapping("/{id}/credits")
    public CreditAccount account(@PathVariable Long id) {
        return credits.account(id);
    }

    @GetMapping("/{id}/credits/ledger")
    public List<CreditEntry> ledger(@PathVariable Long id) {
        return credits.ledger(id);
    }

    @GetMapping("/{id}/redemptions")
    public List<Redemption> redemptions(@PathVariable Long id) {
        return redemptions.findForCompany(id);
    }
}
