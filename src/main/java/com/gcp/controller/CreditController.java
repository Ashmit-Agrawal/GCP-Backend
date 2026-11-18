package com.gcp.controller;

import com.gcp.dto.CreditCalculationRequest;
import com.gcp.dto.CreditDecisionRequest;
import com.gcp.enums.CreditStatus;
import com.gcp.model.CreditEntry;
import com.gcp.security.SecurityUtils;
import com.gcp.security.TenantSecurityService;
import com.gcp.service.CreditService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final CreditService service;
    private final TenantSecurityService tenantSecurity;

    public CreditController(CreditService service, TenantSecurityService tenantSecurity) {
        this.service = service;
        this.tenantSecurity = tenantSecurity;
    }

    /** Creates a DRAFT credit entry for an allotted parcel (Area x Canopy Growth %). Admin only. */
    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CreditEntry calculate(@Valid @RequestBody CreditCalculationRequest req) {
        return service.createDraft(req);
    }

    /** Credits list: Admin sees all; company users see only their company's credit ledger. */
    @GetMapping
    public List<CreditEntry> list(@RequestParam(required = false) CreditStatus status) {
        if (SecurityUtils.isAdmin()) {
            return service.find(status);
        }
        Long companyId = tenantSecurity.resolveUserCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException("No company associated with account");
        }
        return service.ledger(companyId).stream()
                .filter(e -> status == null || e.getStatus() == status)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @tenantSecurity.canAccessCredit(#id)")
    public CreditEntry one(@PathVariable Long id) {
        return service.get(id);
    }

    /** Officer approves (optionally with a revised amount) or rejects the draft. Admin only. */
    @PutMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public CreditEntry decide(@PathVariable Long id, @Valid @RequestBody CreditDecisionRequest req) {
        return service.decide(id, req);
    }
}
