package com.gcp.controller;

import com.gcp.dto.CreditCalculationRequest;
import com.gcp.dto.CreditDecisionRequest;
import com.gcp.model.CreditEntry;
import com.gcp.model.CreditStatus;
import com.gcp.service.CreditService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private final CreditService service;

    public CreditController(CreditService service) {
        this.service = service;
    }

    /** Creates a DRAFT credit entry for an allotted parcel (Area x Canopy Growth %). */
    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.CREATED)
    public CreditEntry calculate(@Valid @RequestBody CreditCalculationRequest req) {
        return service.createDraft(req);
    }

    /** Officer queue: /api/credits?status=DRAFT */
    @GetMapping
    public List<CreditEntry> list(@RequestParam(required = false) CreditStatus status) {
        return service.find(status);
    }

    @GetMapping("/{id}")
    public CreditEntry one(@PathVariable Long id) {
        return service.get(id);
    }

    /** Officer approves (optionally with a revised amount) or rejects the draft. */
    @PutMapping("/{id}/decision")
    public CreditEntry decide(@PathVariable Long id, @Valid @RequestBody CreditDecisionRequest req) {
        return service.decide(id, req);
    }
}
