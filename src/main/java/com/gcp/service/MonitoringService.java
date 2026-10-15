package com.gcp.service;

import com.gcp.dto.MonitoringRequest;
import com.gcp.model.MonitoringRecord;
import com.gcp.model.RiskLevel;
import com.gcp.repository.MonitoringRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class MonitoringService {

    private final MonitoringRecordRepository repo;
    private final ParcelService parcels;
    private final AuditService audit;

    public MonitoringService(MonitoringRecordRepository repo, ParcelService parcels, AuditService audit) {
        this.repo = repo;
        this.parcels = parcels;
        this.audit = audit;
    }

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
        r = repo.save(r);
        audit.log("Monitoring System", "MONITORING_RECORDED", "LandParcel", parcelId,
                "Canopy " + r.getCanopyPercent() + "%");
        return r;
    }

    /** Oldest first, so the frontend can plot the canopy trend directly. */
    public List<MonitoringRecord> history(String parcelId) {
        parcels.get(parcelId);
        return repo.findByParcelIdOrderByRecordedOnAsc(parcelId);
    }
}
