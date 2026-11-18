package com.gcp.service;

import com.gcp.dto.InspectionDecisionRequest;
import com.gcp.dto.MonitoringRequest;
import com.gcp.enums.RiskLevel;
import com.gcp.exception.NotFoundException;
import com.gcp.model.Company;
import com.gcp.model.LandParcel;
import com.gcp.model.MonitoringRecord;
import com.gcp.repository.CompanyRepository;
import com.gcp.repository.MonitoringRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class MonitoringService {

    private final MonitoringRecordRepository repo;
    private final ParcelService parcels;
    private final AuditService audit;
    private final SatelliteMonitoringProvider satelliteProvider;
    private final CompanyRepository companyRepo;

    @Autowired
    public MonitoringService(MonitoringRecordRepository repo,
                             ParcelService parcels,
                             AuditService audit,
                             @Autowired(required = false) SatelliteMonitoringProvider satelliteProvider,
                             @Autowired(required = false) CompanyRepository companyRepo) {
        this.repo = repo;
        this.parcels = parcels;
        this.audit = audit;
        this.satelliteProvider = satelliteProvider != null ? satelliteProvider : new MockSatelliteMonitoringProvider();
        this.companyRepo = companyRepo;
    }

    public MonitoringService(MonitoringRecordRepository repo, ParcelService parcels, AuditService audit) {
        this(repo, parcels, audit, new MockSatelliteMonitoringProvider(), null);
    }

    @Transactional
    public MonitoringRecord add(String parcelId, MonitoringRequest req) {
        parcels.get(parcelId); // 404 if the parcel does not exist

        MonitoringRecord r = new MonitoringRecord();
        r.setParcelId(parcelId);
        r.setRecordedOn(req.recordedOn() != null ? req.recordedOn() : LocalDate.now());
        r.setCanopyPercent(req.canopyPercent());
        r.setBiomassIndex(req.biomassIndex() != null ? req.biomassIndex() : 0);
        r.setMoistureStability(req.moistureStability() != null ? req.moistureStability() : 0);
        r.setRiskLevel(req.riskLevel() != null ? req.riskLevel() : RiskLevel.LOW);
        r.setNote(req.note());
        r.setProviderName(req.providerName() != null ? req.providerName() : "MOCK_ISRO_SENTINEL_V1");
        r.setModelVersion(req.modelVersion() != null ? req.modelVersion() : "canopy-ai-sim-1.0");
        r.setConfidenceScore(req.confidenceScore() != null ? req.confidenceScore() : 0.88);
        r.setIsSimulated(req.isSimulated() != null ? req.isSimulated() : true);
        r.setInspectionStatus(req.inspectionStatus() != null ? req.inspectionStatus() : "AI_ADVISORY_ONLY");
        r = repo.save(r);
        audit.log("Monitoring System", "MONITORING_RECORDED", "LandParcel", parcelId,
                "Canopy " + r.getCanopyPercent() + "% [" + r.getProviderName() + "]");
        return enrich(r);
    }

    /**
     * Triggers the pluggable Satellite & AI provider (MockSatelliteMonitoringProvider)
     * to generate and store a new monitoring pass for the given parcel.
     */
    @Transactional
    public MonitoringRecord triggerMockMonitoring(String parcelId) {
        LandParcel parcel = parcels.get(parcelId);
        List<MonitoringRecord> existing = repo.findByParcelIdOrderByRecordedOnAsc(parcelId);
        Double prevCanopy = existing.isEmpty() ? null : existing.get(existing.size() - 1).getCanopyPercent();

        MonitoringRecord generated = satelliteProvider.analyzeParcel(parcel, prevCanopy);
        MonitoringRecord saved = repo.save(generated);

        audit.log(satelliteProvider.getProviderName(), "MOCK_SATELLITE_PASS_COMPLETED", "LandParcel", parcelId,
                String.format("Simulated pass recorded: Canopy %.1f%%, Confidence %.0f%%",
                        saved.getCanopyPercent(),
                        (saved.getConfidenceScore() != null ? saved.getConfidenceScore() * 100.0 : 88.0)));
        return enrich(saved);
    }

    /** Oldest first, so the frontend can plot the canopy trend directly. */
    public List<MonitoringRecord> history(String parcelId) {
        parcels.get(parcelId);
        return repo.findByParcelIdOrderByRecordedOnAsc(parcelId).stream()
                .map(this::enrich)
                .toList();
    }

    /** All monitoring records enriched with parcel and company metadata (newest first). */
    public List<MonitoringRecord> findAllEnriched() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(
                        (MonitoringRecord r) -> r.getRecordedOn() != null ? r.getRecordedOn() : LocalDate.MIN)
                        .reversed()
                        .thenComparing(MonitoringRecord::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::enrich)
                .toList();
    }

    /** Officer updates field inspection status and notes on a monitoring record. */
    @Transactional
    public MonitoringRecord updateInspection(Long id, InspectionDecisionRequest req, String actor) {
        MonitoringRecord r = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Monitoring record " + id + " not found"));

        String officer = (actor != null && !actor.isBlank()) ? actor
                : (req.officerName() != null && !req.officerName().isBlank() ? req.officerName() : "Dr. Arvind Sharma");

        r.setInspectionStatus(req.inspectionStatus().trim().toUpperCase());
        r.setInspectorNotes(req.inspectorNotes());
        r.setInspectedBy(officer);
        r.setInspectedAt(LocalDateTime.now());
        r = repo.save(r);

        audit.log(officer, "MONITORING_INSPECTION_UPDATED", "LandParcel", r.getParcelId(),
                "Record #" + id + " -> " + r.getInspectionStatus()
                        + (r.getInspectorNotes() != null ? " (" + r.getInspectorNotes() + ")" : ""));
        return enrich(r);
    }

    private MonitoringRecord enrich(MonitoringRecord r) {
        if (r == null) {
            return null;
        }
        if (r.getProviderName() == null) {
            r.setProviderName("MOCK_SENTINEL2_AI");
        }
        if (r.getModelVersion() == null) {
            r.setModelVersion("canopy-ai-sim-1.0");
        }
        if (r.getConfidenceScore() == null) {
            r.setConfidenceScore(0.88);
        }
        if (r.getIsSimulated() == null) {
            r.setIsSimulated(true);
        }
        if (r.getInspectionStatus() == null) {
            r.setInspectionStatus("AI_ADVISORY_ONLY");
        }
        try {
            if (r.getParcelId() != null && parcels != null) {
                LandParcel p = parcels.get(r.getParcelId());
                if (p != null) {
                    r.setParcelLocation(p.getLocation());
                    r.setParcelState(p.getState());
                    r.setParcelAreaHectares(p.getAreaHectares());
                    r.setCompanyId(p.getAllottedCompanyId());
                    if (p.getAllottedCompanyId() != null && companyRepo != null) {
                        companyRepo.findById(p.getAllottedCompanyId())
                                .map(Company::getName)
                                .ifPresent(r::setCompanyName);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return r;
    }
}
