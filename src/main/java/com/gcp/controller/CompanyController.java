package com.gcp.controller;

import com.gcp.dto.CompanyRequest;
import com.gcp.dto.CreditAccount;
import com.gcp.model.Company;
import com.gcp.model.CreditEntry;
import com.gcp.model.LandApplication;
import com.gcp.model.LandParcel;
import com.gcp.model.Redemption;
import com.gcp.service.ApplicationService;
import com.gcp.service.CompanyService;
import com.gcp.service.CreditService;
import com.gcp.service.ParcelService;
import com.gcp.service.RedemptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Everything a company's dashboard needs, grouped under /api/companies/{id}/... */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companies;
    private final ApplicationService applications;
    private final ParcelService parcels;
    private final CreditService credits;
    private final RedemptionService redemptions;

    public CompanyController(CompanyService companies, ApplicationService applications,
                             ParcelService parcels, CreditService credits, RedemptionService redemptions) {
        this.companies = companies;
        this.applications = applications;
        this.parcels = parcels;
        this.credits = credits;
        this.redemptions = redemptions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Company register(@Valid @RequestBody CompanyRequest req) {
        return companies.register(req);
    }

    @GetMapping
    public List<Company> all() {
        return companies.findAll();
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
