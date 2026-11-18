package com.gcp.controller;

import com.gcp.dto.DecisionRequest;
import com.gcp.dto.RedemptionOptionInfo;
import com.gcp.dto.RedemptionRequest;
import com.gcp.enums.RedemptionStatus;
import com.gcp.exception.BadRequestException;
import com.gcp.model.Redemption;
import com.gcp.security.SecurityUtils;
import com.gcp.security.TenantSecurityService;
import com.gcp.service.RedemptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/redemptions")
public class RedemptionController {

    private final RedemptionService service;
    private final TenantSecurityService tenantSecurity;

    public RedemptionController(RedemptionService service, TenantSecurityService tenantSecurity) {
        this.service = service;
        this.tenantSecurity = tenantSecurity;
    }

    /** The four options and what each costs. */
    @GetMapping("/options")
    public List<RedemptionOptionInfo> options() {
        return service.options();
    }

    /** Company requests credit redemption. Company identity is derived from authentication. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Redemption request(@Valid @RequestBody RedemptionRequest req) {
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
                throw new AccessDeniedException("Cannot request redemption for another company");
            }
            targetCompanyId = authCompanyId;
        }

        RedemptionRequest secureReq = new RedemptionRequest(targetCompanyId, req.option(), req.idempotencyKey());
        return service.request(secureReq);
    }

    /** Redemptions list: Admin sees all; company users see only their company's redemptions. */
    @GetMapping
    public List<Redemption> list(@RequestParam(required = false) RedemptionStatus status) {
        if (SecurityUtils.isAdmin()) {
            return service.find(status);
        }
        Long companyId = tenantSecurity.resolveUserCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException("No company associated with account");
        }
        return service.findForCompany(companyId).stream()
                .filter(r -> status == null || r.getStatus() == status)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tenantSecurity.canAccessRedemption(#id)")
    public Redemption one(@PathVariable Long id) {
        return service.get(id);
    }

    /** Nodal officer approves or rejects. Admin only. */
    @PutMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Redemption decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return service.decide(id, req);
    }
}
