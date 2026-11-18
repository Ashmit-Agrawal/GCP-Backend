package com.gcp.service;

import com.gcp.dto.ApplicationRequest;
import com.gcp.dto.DecisionRequest;
import com.gcp.dto.PlantationActivityRequest;
import com.gcp.enums.AllocationStatus;
import com.gcp.enums.ApplicationStatus;
import com.gcp.enums.CompanyStatus;
import com.gcp.enums.ParcelStatus;
import com.gcp.exception.BadRequestException;
import com.gcp.exception.ConflictException;
import com.gcp.exception.NotFoundException;
import com.gcp.model.Company;
import com.gcp.model.LandApplication;
import com.gcp.model.LandParcel;
import com.gcp.model.ParcelAllocation;
import com.gcp.repository.LandApplicationRepository;
import com.gcp.repository.LandParcelRepository;
import com.gcp.repository.ParcelAllocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final LandApplicationRepository repo;
    private final LandParcelRepository parcelRepo;
    private final ParcelAllocationRepository allocationRepo;
    private final CompanyService companies;
    private final ParcelService parcels;
    private final AuditService audit;
    private final OutboxService outboxService;
    private final MongoTemplate mongoTemplate;

    public ApplicationService(LandApplicationRepository repo,
                              LandParcelRepository parcelRepo,
                              ParcelAllocationRepository allocationRepo,
                              CompanyService companies,
                              ParcelService parcels,
                              AuditService audit,
                              OutboxService outboxService,
                              MongoTemplate mongoTemplate) {
        this.repo = repo;
        this.parcelRepo = parcelRepo;
        this.allocationRepo = allocationRepo;
        this.companies = companies;
        this.parcels = parcels;
        this.audit = audit;
        this.outboxService = outboxService;
        this.mongoTemplate = mongoTemplate;
    }

    @Transactional
    public LandApplication apply(ApplicationRequest req) {
        // 1. Idempotency Check: if request with idempotency key already exists, return existing
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            Optional<LandApplication> existing = repo.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing LandApplication for idempotencyKey={}", req.idempotencyKey());
                return existing.get();
            }
        }

        Company company = companies.get(req.companyId());
        LandParcel parcel = parcels.get(req.parcelId());

        if (company.getStatus() != null && company.getStatus() != CompanyStatus.APPROVED) {
            throw new BadRequestException("Company registration status is " + company.getStatus()
                    + ". Only verified companies with APPROVED status can apply for land parcels.");
        }

        if (company.getRank() != null && parcel.getAreaHectares() > company.getRank().getMaxAreaHectares()) {
            throw new BadRequestException(String.format(
                    "Area limit exceeded: Company rank '%s' permits parcels up to %.1f ha, but parcel %s is %.1f ha.",
                    company.getRank().name(),
                    company.getRank().getMaxAreaHectares(),
                    parcel.getId(),
                    parcel.getAreaHectares()));
        }

        // Verify parcel is not already allotted in PostgreSQL authoritative allocation table
        if (allocationRepo.existsByParcelIdAndStatus(parcel.getId(), AllocationStatus.ALLOCATED)) {
            throw new ConflictException("Parcel " + parcel.getId() + " is already allocated to another company.");
        }

        if (parcel.getStatus() != ParcelStatus.AVAILABLE) {
            throw new ConflictException("Parcel " + parcel.getId() + " is not available for applications");
        }

        boolean alreadyPending = repo.findByParcelId(parcel.getId()).stream()
                .anyMatch(a -> a.getCompanyId().equals(company.getId())
                        && a.getStatus() == ApplicationStatus.PENDING_REVIEW);
        if (alreadyPending) {
            throw new BadRequestException("This company already has a pending application for this parcel");
        }

        String species = (req.proposedSpecies() != null && !req.proposedSpecies().isBlank())
                ? req.proposedSpecies().trim()
                : "Native Teak, Bamboo, Neem";
        Integer saplings = (req.saplingCount() != null && req.saplingCount() > 0)
                ? req.saplingCount()
                : (int) Math.max(500, Math.round(parcel.getAreaHectares() * 400));
        String plan = (req.plantingPlanSummary() != null && !req.plantingPlanSummary().isBlank())
                ? req.plantingPlanSummary().trim()
                : "Staggered monsoon pit-planting (400 saplings/ha) with 3-year maintenance and moisture conservation.";
        String docRef = (req.supportingDocRef() != null && !req.supportingDocRef().isBlank())
                ? req.supportingDocRef().trim()
                : "GCP-AFF-PLAN-" + parcel.getId() + ".pdf";

        LandApplication a = LandApplication.builder()
                .companyId(company.getId())
                .parcelId(parcel.getId())
                .status(ApplicationStatus.PENDING_REVIEW)
                .submittedAt(LocalDateTime.now())
                .idempotencyKey(req.idempotencyKey())
                .proposedSpecies(species)
                .saplingCount(saplings)
                .plantingPlanSummary(plan)
                .supportingDocRef(docRef)
                .plantationStatus("NOT_STARTED")
                .lastActivityNote("Application submitted with afforestation plan")
                .build();

        a = repo.save(a);
        audit.log(company.getName(), "APPLICATION_SUBMITTED", "LandApplication", a.getId(),
                "Parcel " + parcel.getId() + " (" + parcel.getAreaHectares() + " ha) | Species: " + species);
        return enrich(a);
    }

    /**
     * Officer approves or rejects with atomic concurrency control, state transition validation,
     * and PostgreSQL-MongoDB consistency via authoritative allocation and transactional outbox.
     */
    @Transactional
    public LandApplication decide(Long id, DecisionRequest req) {
        // 1. Pessimistic lock row in PostgreSQL to prevent concurrent decision race
        LandApplication a = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Application " + id + " not found"));

        // Idempotency: if already decided (APPROVED or REJECTED)
        if (a.getStatus() == ApplicationStatus.APPROVED || a.getStatus() == ApplicationStatus.REJECTED) {
            boolean requestedApproval = Boolean.TRUE.equals(req.approve()) && !Boolean.TRUE.equals(req.requestChanges());
            boolean currentApproval = a.getStatus() == ApplicationStatus.APPROVED;
            if (requestedApproval == currentApproval && !Boolean.TRUE.equals(req.requestChanges())) {
                log.info("Idempotent replay: Application {} is already decided with status {}", id, a.getStatus());
                return enrich(a);
            }
            throw new ConflictException("Application " + id + " has already been decided with status: " + a.getStatus());
        }

        if (Boolean.TRUE.equals(req.requestChanges())) {
            if (req.reason() == null || req.reason().isBlank()) {
                throw new BadRequestException("A reason is required when requesting changes on an application");
            }
            LocalDateTime now = LocalDateTime.now();
            a.setStatus(ApplicationStatus.CHANGES_REQUESTED);
            a.setDecidedAt(now);
            a.setDecidedBy(req.officerName());
            a.setDecisionReason(req.reason());
            audit.log(req.officerName(), "APPLICATION_CHANGES_REQUESTED", "LandApplication", id, req.reason());
            return enrich(repo.save(a));
        }

        boolean approve = Boolean.TRUE.equals(req.approve());
        if (!approve && (req.reason() == null || req.reason().isBlank())) {
            throw new BadRequestException("A reason is required when rejecting an application");
        }

        LocalDateTime now = LocalDateTime.now();
        a.setDecidedAt(now);
        a.setDecidedBy(req.officerName());
        a.setDecisionReason(req.reason());

        if (approve) {
            String parcelId = a.getParcelId();

            // 2. Concurrency Safety: Check if parcel is already allocated in PostgreSQL authoritative table
            if (allocationRepo.existsByParcelIdAndStatus(parcelId, AllocationStatus.ALLOCATED)) {
                throw new ConflictException("Parcel " + parcelId + " is already allocated to another company.");
            }

            // 3. Concurrency Safety: Atomic conditional update in MongoDB via findAndModify
            // Only succeeds if MongoDB parcel status is currently AVAILABLE
            Query query = new Query(Criteria.where("_id").is(parcelId).and("status").is(ParcelStatus.AVAILABLE.name()));
            Update update = new Update()
                    .set("status", ParcelStatus.ALLOTTED.name())
                    .set("allottedCompanyId", a.getCompanyId())
                    .set("allotmentDate", LocalDate.now());

            LandParcel modifiedParcel = mongoTemplate.findAndModify(
                    query,
                    update,
                    FindAndModifyOptions.options().returnNew(true),
                    LandParcel.class
            );

            if (modifiedParcel == null) {
                // Another thread or operation already allotted or modified this parcel
                log.warn("Atomic check failed: Parcel {} is no longer AVAILABLE in MongoDB", parcelId);
                throw new ConflictException("Parcel " + parcelId + " has already been allocated or is no longer available.");
            }

            // 4. Create authoritative ParcelAllocation record in PostgreSQL
            ParcelAllocation allocation = ParcelAllocation.builder()
                    .parcelId(parcelId)
                    .companyId(a.getCompanyId())
                    .applicationId(a.getId())
                    .status(AllocationStatus.ALLOCATED)
                    .allotmentDate(LocalDate.now())
                    .createdAt(now)
                    .idempotencyKey(req.idempotencyKey())
                    .build();
            allocationRepo.save(allocation);

            a.setStatus(ApplicationStatus.APPROVED);
            if (a.getPlantationStatus() == null || "NOT_STARTED".equals(a.getPlantationStatus())) {
                a.setPlantationStatus("PLANTING_IN_PROGRESS");
            }
            if (a.getLastActivityNote() == null || a.getLastActivityNote().isBlank()) {
                a.setLastActivityNote("Land parcel allotted; restoration project initiated.");
            }

            // 5. Persist durable Outbox event for cross-database auditability and reconciliation
            outboxService.recordEvent(
                    "PARCEL_ALLOCATED",
                    "LandParcel",
                    parcelId,
                    Map.of(
                            "companyId", a.getCompanyId(),
                            "applicationId", a.getId(),
                            "allotmentDate", LocalDate.now().toString()
                    )
            );

            audit.log(req.officerName(), "APPLICATION_APPROVED", "LandApplication", id,
                    "Parcel " + parcelId + " allotted to company " + a.getCompanyId());

            // 6. Automatically close and reject other pending applications for this parcel
            for (LandApplication other : repo.findByParcelId(parcelId)) {
                if (!Objects.equals(other.getId(), id) && other.getStatus() == ApplicationStatus.PENDING_REVIEW) {
                    other.setStatus(ApplicationStatus.REJECTED);
                    other.setDecidedAt(now);
                    other.setDecidedBy(req.officerName());
                    other.setDecisionReason("Parcel allotted to another applicant (Application #" + id + ")");
                    repo.save(other);
                    audit.log(req.officerName(), "APPLICATION_REJECTED", "LandApplication", other.getId(),
                            "Parcel allotted to another applicant");
                }
            }
        } else {
            a.setStatus(ApplicationStatus.REJECTED);
            audit.log(req.officerName(), "APPLICATION_REJECTED", "LandApplication", id, req.reason());
        }

        return enrich(repo.save(a));
    }

    @Transactional
    public LandApplication updateActivity(Long id, PlantationActivityRequest req, String actor) {
        LandApplication a = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Application " + id + " not found"));

        if (a.getStatus() == ApplicationStatus.CHANGES_REQUESTED) {
            if (req.proposedSpecies() != null && !req.proposedSpecies().isBlank()) {
                a.setProposedSpecies(req.proposedSpecies().trim());
            }
            if (req.saplingCount() != null && req.saplingCount() > 0) {
                a.setSaplingCount(req.saplingCount());
            }
            if (req.supportingDocRef() != null && !req.supportingDocRef().isBlank()) {
                a.setSupportingDocRef(req.supportingDocRef().trim());
            }
            if (req.lastActivityNote() != null && !req.lastActivityNote().isBlank()) {
                a.setPlantingPlanSummary(req.lastActivityNote().trim());
                a.setLastActivityNote("Resubmitted planting plan: " + req.lastActivityNote().trim());
            }
            a.setStatus(ApplicationStatus.PENDING_REVIEW);
            a = repo.save(a);
            audit.log(actor, "APPLICATION_RESUBMITTED", "LandApplication", id,
                    "Updated afforestation plan resubmitted for review");
            return enrich(a);
        }

        if (a.getStatus() != ApplicationStatus.APPROVED) {
            throw new BadRequestException(
                    "Plantation activities can only be recorded for APPROVED applications (current status: "
                            + a.getStatus() + ")");
        }

        if (req.plantationStatus() != null && !req.plantationStatus().isBlank()) {
            a.setPlantationStatus(req.plantationStatus().trim());
        }
        if (req.saplingCount() != null && req.saplingCount() > 0) {
            a.setSaplingCount(req.saplingCount());
        }
        if (req.proposedSpecies() != null && !req.proposedSpecies().isBlank()) {
            a.setProposedSpecies(req.proposedSpecies().trim());
        }
        if (req.supportingDocRef() != null && !req.supportingDocRef().isBlank()) {
            a.setSupportingDocRef(req.supportingDocRef().trim());
        }
        if (req.lastActivityNote() != null && !req.lastActivityNote().isBlank()) {
            a.setLastActivityNote(req.lastActivityNote().trim());
        }

        a = repo.save(a);
        audit.log(actor, "PLANTATION_ACTIVITY_LOGGED", "LandApplication", id,
                "Status: " + a.getPlantationStatus() + " | Saplings: " + a.getSaplingCount()
                        + (a.getLastActivityNote() != null ? " | " + a.getLastActivityNote() : ""));
        return enrich(a);
    }

    public LandApplication get(Long id) {
        return enrich(repo.findById(id).orElseThrow(() -> new NotFoundException("Application " + id + " not found")));
    }

    public List<LandApplication> find(ApplicationStatus status) {
        List<LandApplication> list = status == null ? repo.findAll() : repo.findByStatus(status);
        return list.stream().map(this::enrich).toList();
    }

    public List<LandApplication> findForCompany(Long companyId) {
        companies.get(companyId);
        return repo.findByCompanyId(companyId).stream().map(this::enrich).toList();
    }

    private LandApplication enrich(LandApplication app) {
        if (app == null) {
            return null;
        }
        try {
            if (app.getCompanyId() != null && companies != null) {
                Company c = companies.get(app.getCompanyId());
                if (c != null) {
                    app.setCompanyName(c.getName());
                    app.setCompanySector(c.getSector());
                    app.setCompanyRank(c.getRank() != null ? c.getRank().name() : null);
                }
            }
        } catch (Exception ignored) {
        }
        try {
            if (app.getParcelId() != null && parcelRepo != null) {
                Optional<LandParcel> optParcel = parcelRepo.findById(app.getParcelId());
                if (optParcel != null && optParcel.isPresent()) {
                    LandParcel p = optParcel.get();
                    app.setParcelLocation(p.getLocation());
                    app.setParcelState(p.getState());
                    app.setParcelAreaHectares(p.getAreaHectares());
                }
            }
        } catch (Exception ignored) {
        }
        if (app.getProposedSpecies() == null) {
            app.setProposedSpecies("Native Teak, Bamboo, Neem");
        }
        if (app.getSaplingCount() == null) {
            double area = app.getParcelAreaHectares() != null ? app.getParcelAreaHectares() : 10.0;
            app.setSaplingCount((int) Math.round(area * 400));
        }
        if (app.getPlantationStatus() == null) {
            app.setPlantationStatus(app.getStatus() == ApplicationStatus.APPROVED ? "PLANTING_IN_PROGRESS" : "NOT_STARTED");
        }
        return app;
    }
}
