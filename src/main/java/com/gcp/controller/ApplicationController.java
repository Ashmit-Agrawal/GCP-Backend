package com.gcp.controller;

import com.gcp.dto.ApplicationRequest;
import com.gcp.dto.DecisionRequest;
import com.gcp.enums.ApplicationStatus;
import com.gcp.model.LandApplication;
import com.gcp.service.ApplicationService;
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
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }

    /** Company applies for a parcel. Status starts as PENDING_REVIEW. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LandApplication apply(@Valid @RequestBody ApplicationRequest req) {
        return service.apply(req);
    }

    /** Officer queue: /api/applications?status=PENDING_REVIEW */
    @GetMapping
    public List<LandApplication> list(@RequestParam(required = false) ApplicationStatus status) {
        return service.find(status);
    }

    @GetMapping("/{id}")
    public LandApplication one(@PathVariable Long id) {
        return service.get(id);
    }

    /** Officer approves or rejects (reason required on rejection). */
    @PutMapping("/{id}/decision")
    public LandApplication decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return service.decide(id, req);
    }
}
