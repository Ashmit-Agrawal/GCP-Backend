package com.gcp.controller;

import com.gcp.dto.DecisionRequest;
import com.gcp.dto.RedemptionOptionInfo;
import com.gcp.dto.RedemptionRequest;
import com.gcp.model.Redemption;
import com.gcp.enums.RedemptionStatus;
import com.gcp.service.RedemptionService;
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
@RequestMapping("/api/redemptions")
public class RedemptionController {

    private final RedemptionService service;

    public RedemptionController(RedemptionService service) {
        this.service = service;
    }

    /** The four options and what each costs. */
    @GetMapping("/options")
    public List<RedemptionOptionInfo> options() {
        return service.options();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Redemption request(@Valid @RequestBody RedemptionRequest req) {
        return service.request(req);
    }

    @GetMapping
    public List<Redemption> list(@RequestParam(required = false) RedemptionStatus status) {
        return service.find(status);
    }

    @GetMapping("/{id}")
    public Redemption one(@PathVariable Long id) {
        return service.get(id);
    }

    /** Nodal officer approves or rejects. */
    @PutMapping("/{id}/decision")
    public Redemption decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
        return service.decide(id, req);
    }
}
