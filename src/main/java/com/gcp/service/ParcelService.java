package com.gcp.service;

import com.gcp.dto.ParcelRequest;
import com.gcp.exception.NotFoundException;
import com.gcp.enums.DegradationLevel;
import com.gcp.model.LandParcel;
import com.gcp.enums.ParcelStatus;
import com.gcp.repository.LandParcelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
public class ParcelService {

    private final LandParcelRepository repo;
    private final AuditService audit;

    public ParcelService(LandParcelRepository repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    /**
     * Registers a parcel and gives it an ID like GCP-MH-2024-001 (scheme, state
     * code, year, serial).
     */
    @Transactional
    public LandParcel create(ParcelRequest req) {
        String prefix = "GCP-" + req.stateCode().trim().toUpperCase() + "-" + Year.now().getValue() + "-";
        long serial = repo.countByIdStartingWith(prefix) + 1;

        LandParcel p = new LandParcel();
        p.setId(prefix + String.format("%03d", serial));
        p.setState(req.state());
        p.setStateCode(req.stateCode().trim().toUpperCase());
        p.setLocation(req.location());
        p.setLatitude(req.latitude());
        p.setLongitude(req.longitude());
        p.setAreaHectares(req.areaHectares());
        p.setForestType(req.forestType());
        p.setDegradationLevel(req.degradationLevel());
        p.setStatus(ParcelStatus.AVAILABLE);
        p.setCreatedAt(LocalDateTime.now());
        p = repo.save(p);
        audit.log("Government", "PARCEL_UPLOADED", "LandParcel", p.getId(),
                p.getAreaHectares() + " ha, " + p.getState());
        return p;
    }

    /**
     * Browse/filter. Any filter left null is ignored. Area range is min inclusive,
     * max exclusive.
     */
    public List<LandParcel> search(String state, Double minArea, Double maxArea,
            DegradationLevel degradation, ParcelStatus status) {
        return repo.findAll().stream()
                .filter(p -> state == null || p.getState().equalsIgnoreCase(state))
                .filter(p -> minArea == null || p.getAreaHectares() >= minArea)
                .filter(p -> maxArea == null || p.getAreaHectares() < maxArea)
                .filter(p -> degradation == null || p.getDegradationLevel() == degradation)
                .filter(p -> status == null || p.getStatus() == status)
                .toList();
    }

    public LandParcel get(String id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Parcel " + id + " not found"));
    }

    public List<LandParcel> findAllottedTo(Long companyId) {
        return repo.findByAllottedCompanyId(companyId);
    }
}
