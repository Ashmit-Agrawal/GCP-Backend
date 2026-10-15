package com.gcp.service;

import com.gcp.dto.CreditAccount;
import com.gcp.dto.CreditCalculationRequest;
import com.gcp.dto.CreditDecisionRequest;
import com.gcp.exception.BadRequestException;
import com.gcp.exception.NotFoundException;
import com.gcp.model.CreditEntry;
import com.gcp.model.CreditStatus;
import com.gcp.model.LandParcel;
import com.gcp.model.ParcelStatus;
import com.gcp.model.Redemption;
import com.gcp.model.RedemptionStatus;
import com.gcp.repository.CreditEntryRepository;
import com.gcp.repository.RedemptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreditService {

    private final CreditEntryRepository repo;
    private final RedemptionRepository redemptionRepo;
    private final ParcelService parcels;
    private final CompanyService companies;
    private final AuditService audit;

    public CreditService(CreditEntryRepository repo, RedemptionRepository redemptionRepo,
                         ParcelService parcels, CompanyService companies, AuditService audit) {
        this.repo = repo;
        this.redemptionRepo = redemptionRepo;
        this.parcels = parcels;
        this.companies = companies;
        this.audit = audit;
    }

    /**
     * Demo model: Credits = Parcel Area (ha) x Canopy Growth Improvement (%).
     * Keep the formula in this one method so it can be swapped for the notified
     * tree-based rule later without touching anything else.
     */
    double calculate(double areaHectares, double canopyGrowthPercent) {
        return Math.round(areaHectares * canopyGrowthPercent * 100.0) / 100.0;
    }

    /** Creates a DRAFT credit entry. It only counts as "in pipeline" until an officer approves it. */
    @Transactional
    public CreditEntry createDraft(CreditCalculationRequest req) {
        LandParcel parcel = parcels.get(req.parcelId());
        if (parcel.getStatus() != ParcelStatus.ALLOTTED || parcel.getAllottedCompanyId() == null) {
            throw new BadRequestException("Credits can only be calculated for an allotted parcel");
        }
        CreditEntry e = new CreditEntry();
        e.setCompanyId(parcel.getAllottedCompanyId());
        e.setParcelId(parcel.getId());
        e.setAreaHectares(parcel.getAreaHectares());
        e.setCanopyGrowthPercent(req.canopyGrowthPercent());
        e.setCredits(calculate(parcel.getAreaHectares(), req.canopyGrowthPercent()));
        e.setVerificationMethod(req.verificationMethod());
        e.setStatus(CreditStatus.DRAFT);
        e.setCreatedAt(LocalDateTime.now());
        e = repo.save(e);
        audit.log("System", "CREDIT_CALCULATED", "CreditEntry", e.getId(),
                parcel.getAreaHectares() + " ha x " + req.canopyGrowthPercent() + "% = " + e.getCredits() + " GC");
        return e;
    }

    /** Officer approves (optionally revising the amount) or rejects the draft. */
    @Transactional
    public CreditEntry decide(Long id, CreditDecisionRequest req) {
        CreditEntry e = get(id);
        if (e.getStatus() != CreditStatus.DRAFT) {
            throw new BadRequestException("Credit entry " + id + " has already been decided");
        }
        e.setDecidedAt(LocalDateTime.now());
        e.setDecidedBy(req.officerName());
        e.setRemarks(req.remarks());
        if (Boolean.TRUE.equals(req.approve())) {
            if (req.revisedCredits() != null) {
                e.setCredits(req.revisedCredits());
            }
            e.setStatus(CreditStatus.APPROVED);
            audit.log(req.officerName(), "CREDIT_APPROVED", "CreditEntry", id, e.getCredits() + " GC issued");
        } else {
            e.setStatus(CreditStatus.REJECTED);
            audit.log(req.officerName(), "CREDIT_REJECTED", "CreditEntry", id, req.remarks());
        }
        return repo.save(e);
    }

    public CreditEntry get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Credit entry " + id + " not found"));
    }

    public List<CreditEntry> find(CreditStatus status) {
        return status == null ? repo.findAll() : repo.findByStatus(status);
    }

    public List<CreditEntry> ledger(Long companyId) {
        companies.get(companyId);
        return repo.findByCompanyId(companyId);
    }

    /** Total earned, redeemed, available and in-pipeline credits for one company. */
    public CreditAccount account(Long companyId) {
        companies.get(companyId);
        double earned = sumCredits(companyId, CreditStatus.APPROVED);
        double pipeline = sumCredits(companyId, CreditStatus.DRAFT);
        double redeemed = redemptionRepo.findByCompanyId(companyId).stream()
                .filter(r -> r.getStatus() == RedemptionStatus.APPROVED)
                .mapToDouble(Redemption::getCredits).sum();
        return new CreditAccount(companyId, earned, redeemed, earned - redeemed, pipeline);
    }

    private double sumCredits(Long companyId, CreditStatus status) {
        return repo.findByCompanyId(companyId).stream()
                .filter(e -> e.getStatus() == status)
                .mapToDouble(CreditEntry::getCredits).sum();
    }
}
