package com.gcp.service;

import com.gcp.enums.AllocationStatus;
import com.gcp.enums.ApplicationStatus;
import com.gcp.enums.OutboxStatus;
import com.gcp.enums.ParcelStatus;
import com.gcp.model.LandApplication;
import com.gcp.model.LandParcel;
import com.gcp.model.OutboxEvent;
import com.gcp.model.ParcelAllocation;
import com.gcp.repository.LandApplicationRepository;
import com.gcp.repository.LandParcelRepository;
import com.gcp.repository.OutboxEventRepository;
import com.gcp.repository.ParcelAllocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final ParcelAllocationRepository allocationRepo;
    private final LandApplicationRepository applicationRepo;
    private final LandParcelRepository mongoParcelRepo;
    private final OutboxEventRepository outboxRepo;
    private final OutboxService outboxService;

    public ReconciliationService(ParcelAllocationRepository allocationRepo,
                                 LandApplicationRepository applicationRepo,
                                 LandParcelRepository mongoParcelRepo,
                                 OutboxEventRepository outboxRepo,
                                 OutboxService outboxService) {
        this.allocationRepo = allocationRepo;
        this.applicationRepo = applicationRepo;
        this.mongoParcelRepo = mongoParcelRepo;
        this.outboxRepo = outboxRepo;
        this.outboxService = outboxService;
    }

    public record ReconciliationSummary(
            int stuckOutboxEventsRecovered,
            int mongoParcelsSynced,
            int inconsistenciesFound,
            List<String> details
    ) {}

    /**
     * Idempotently scans for inconsistencies between PostgreSQL and MongoDB and heals them.
     */
    @Transactional
    public ReconciliationSummary reconcile() {
        List<String> details = new ArrayList<>();
        int stuckEventsRecovered = 0;
        int mongoSynced = 0;
        int inconsistencies = 0;

        // 1. Recover stuck outbox events (PROCESSING for > 2 minutes)
        LocalDateTime stuckThreshold = LocalDateTime.now().minusMinutes(2);
        List<OutboxEvent> processingEvents = outboxRepo.findByStatusIn(List.of(OutboxStatus.PROCESSING));
        for (OutboxEvent event : processingEvents) {
            if (event.getCreatedAt().isBefore(stuckThreshold)) {
                log.warn("Reconciliation: Resetting stuck outbox event {}", event.getEventId());
                event.setStatus(OutboxStatus.PENDING);
                outboxRepo.save(event);
                stuckEventsRecovered++;
                details.add("Reset stuck outbox event: " + event.getEventId());
            }
        }

        // 2. Scan active PostgreSQL parcel allocations vs MongoDB land_parcels
        List<ParcelAllocation> activeAllocations = allocationRepo.findAll().stream()
                .filter(a -> a.getStatus() == AllocationStatus.ALLOCATED)
                .toList();

        for (ParcelAllocation alloc : activeAllocations) {
            LandParcel mongoParcel = mongoParcelRepo.findById(alloc.getParcelId()).orElse(null);
            if (mongoParcel == null) {
                inconsistencies++;
                details.add(String.format("CRITICAL: Allocation %d references missing MongoDB parcel %s", alloc.getId(), alloc.getParcelId()));
            } else if (mongoParcel.getStatus() != ParcelStatus.ALLOTTED || !alloc.getCompanyId().equals(mongoParcel.getAllottedCompanyId())) {
                inconsistencies++;
                log.warn("Reconciliation: Syncing MongoDB parcel {} with active PostgreSQL allocation for company {}", alloc.getParcelId(), alloc.getCompanyId());
                mongoParcel.setStatus(ParcelStatus.ALLOTTED);
                mongoParcel.setAllottedCompanyId(alloc.getCompanyId());
                mongoParcel.setAllotmentDate(alloc.getAllotmentDate());
                mongoParcelRepo.save(mongoParcel);
                mongoSynced++;
                details.add(String.format("Synced parcel %s to company %d", alloc.getParcelId(), alloc.getCompanyId()));
            }
        }

        // 3. Scan approved LandApplications vs ParcelAllocations
        List<LandApplication> approvedApps = applicationRepo.findByStatus(ApplicationStatus.APPROVED);
        for (LandApplication app : approvedApps) {
            if (allocationRepo.findByApplicationId(app.getId()).isEmpty()) {
                inconsistencies++;
                log.warn("Reconciliation: Approved application {} lacks a corresponding ParcelAllocation. Healing...", app.getId());
                ParcelAllocation healedAlloc = ParcelAllocation.builder()
                        .parcelId(app.getParcelId())
                        .companyId(app.getCompanyId())
                        .applicationId(app.getId())
                        .status(AllocationStatus.ALLOCATED)
                        .allotmentDate(app.getDecidedAt() != null ? app.getDecidedAt().toLocalDate() : app.getSubmittedAt().toLocalDate())
                        .createdAt(LocalDateTime.now())
                        .build();
                allocationRepo.save(healedAlloc);
                details.add(String.format("Healed missing ParcelAllocation for approved application %d", app.getId()));
            }
        }

        // 4. Trigger outbox processing for any remaining events
        outboxService.processOutboxQueue();

        return new ReconciliationSummary(stuckEventsRecovered, mongoSynced, inconsistencies, details);
    }
}
