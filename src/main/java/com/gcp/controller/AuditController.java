package com.gcp.controller;

import com.gcp.model.AuditLog;
import com.gcp.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    /** All entries (newest first), or one entity's history: /api/audit?entityType=LandParcel&entityId=GCP-MH-2026-001 */
    @GetMapping
    public List<AuditLog> list(@RequestParam(required = false) String entityType,
                               @RequestParam(required = false) String entityId) {
        return service.find(entityType, entityId);
    }
}
