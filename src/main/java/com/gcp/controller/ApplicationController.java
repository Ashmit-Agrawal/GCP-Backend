package com.gcp.controller;

import com.gcp.dto.ApplicationRequest;
import com.gcp.dto.DecisionRequest;
import com.gcp.dto.PlantationActivityRequest;
import com.gcp.enums.ApplicationStatus;
import com.gcp.exception.BadRequestException;
import com.gcp.model.LandApplication;
import com.gcp.security.SecurityUtils;
import com.gcp.security.TenantSecurityService;
import com.gcp.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;
    private final TenantSecurityService tenantSecurity;

    public ApplicationController(ApplicationService service, TenantSecurityService tenantSecurity) {
        this.service = service;
        this.tenantSecurity = tenantSecurity;
    }

    /** Company applies for a parcel. Company identity is derived from authentication. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LandApplication apply(@Valid @RequestBody ApplicationRequest req) {
        Long targetCompanyId;
        if (SecurityUtils.isAdmin()) {
            if (req.companyId() == null) {
                throw new BadRequestException("companyId is required for admin submission");
            }
            targetCompanyId = req.companyId();
        } else {
            Long authCompanyId = tenantSecurity.resolveUserCompanyId();
            if (authCompanyId == null) {
                throw new AccessDeniedException("User is not associated with any company");
            }
            if (req.companyId() != null && !req.companyId().equals(authCompanyId)) {
                throw new AccessDeniedException("Cannot submit application for another company");
            }
            targetCompanyId = authCompanyId;
        }

        ApplicationRequest secureReq = new ApplicationRequest(
                targetCompanyId,
                req.parcelId(),
                req.idempotencyKey(),
                req.proposedSpecies(),
                req.saplingCount(),
                req.plantingPlanSummary(),
                req.supportingDocRef());
        return service.apply(secureReq);
    }

    /** Applications list: Admin sees all; company users see only their company's applications. */
    @GetMapping
    public List<LandApplication> list(@RequestParam(required = false) ApplicationStatus status) {
        if (SecurityUtils.isAdmin()) {
            return service.find(status);
        }
        Long companyId = tenantSecurity.resolveUserCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException("No company associated with account");
        }
        return service.findForCompany(companyId).stream()
                .filter(a -> status == null || a.getStatus() == status)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tenantSecurity.canAccessApplication(#id)")
    public LandApplication one(@PathVariable Long id) {
        return service.get(id);
    }

    /** Officer approves or rejects (reason required on rejection). Admin only. */
    @PutMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public LandApplication decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return service.decide(id, req);
    }

    /** Company owner or Admin records plantation execution activity or resubmits requested changes. */
    @PutMapping("/{id}/activity")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tenantSecurity.canAccessApplication(#id)")
    public LandApplication updateActivity(@PathVariable Long id, @RequestBody PlantationActivityRequest req) {
        String actor = SecurityUtils.getCurrentUsername().orElse("Company User");
        return service.updateActivity(id, req, actor);
    }
}
