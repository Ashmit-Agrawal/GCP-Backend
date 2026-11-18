package com.gcp.controller;

import com.gcp.dto.MonitoringRequest;
import com.gcp.dto.ParcelRequest;
import com.gcp.enums.DegradationLevel;
import com.gcp.enums.ParcelStatus;
import com.gcp.model.LandParcel;
import com.gcp.model.MonitoringRecord;
import com.gcp.service.MonitoringService;
import com.gcp.service.ParcelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parcels")
public class ParcelController {

    private final ParcelService parcels;
    private final MonitoringService monitoring;

    public ParcelController(ParcelService parcels, MonitoringService monitoring) {
        this.parcels = parcels;
        this.monitoring = monitoring;
    }

    /**
     * Browse and filter. Example:
     * /api/parcels?state=Maharashtra&minArea=10&maxArea=20&degradation=HIGH&status=AVAILABLE
     * Area ranges from the UI: under 10 ha = maxArea=10, 10-20 ha =
     * minArea=10&maxArea=20, over 20 ha = minArea=20.
     */
    @GetMapping
    public List<LandParcel> search(@RequestParam(required = false) String state,
            @RequestParam(required = false) Double minArea,
            @RequestParam(required = false) Double maxArea,
            @RequestParam(required = false) DegradationLevel degradation,
            @RequestParam(required = false) ParcelStatus status) {
        return parcels.search(state, minArea, maxArea, degradation, status);
    }

    @GetMapping("/{id}")
    public LandParcel one(@PathVariable String id) {
        return parcels.get(id);
    }

    /** Government uploads a degraded parcel. Admin only. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public LandParcel create(@Valid @RequestBody ParcelRequest req) {
        return parcels.create(req);
    }

    /** Canopy trend for the parcel, oldest first. */
    @GetMapping("/{id}/monitoring")
    public List<MonitoringRecord> history(@PathVariable String id) {
        return monitoring.history(id);
    }

    /** Record monitoring measurement. Admin / authorized service only. */
    @PostMapping("/{id}/monitoring")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public MonitoringRecord addReading(@PathVariable String id, @Valid @RequestBody MonitoringRequest req) {
        return monitoring.add(id, req);
    }

    /** Trigger a simulated satellite & AI monitoring pass for the parcel. */
    @PostMapping({"/{id}/monitoring/trigger-mock", "/{id}/monitoring/Trigger-mock"})
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public MonitoringRecord triggerMockReading(@PathVariable String id) {
        return monitoring.triggerMockMonitoring(id);
    }
}
