package com.gcp.service;

import com.gcp.dto.CreditAccount;
import com.gcp.dto.CreditCalculationRequest;
import com.gcp.dto.CreditDecisionRequest;
import com.gcp.enums.CreditMovementType;
import com.gcp.enums.CreditStatus;
import com.gcp.enums.ParcelStatus;
import com.gcp.enums.RedemptionStatus;
import com.gcp.exception.BadRequestException;
import com.gcp.exception.ConflictException;
import com.gcp.exception.NotFoundException;
import com.gcp.model.Company;
import com.gcp.model.CreditEntry;
import com.gcp.model.CreditLedgerMovement;
import com.gcp.model.LandParcel;
import com.gcp.model.Redemption;
import com.gcp.repository.CreditEntryRepository;
import com.gcp.repository.CreditLedgerMovementRepository;
import com.gcp.repository.RedemptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CreditService {

    public static final String POLICY_VERSION =
            "GCP-PILOT-2026-V1: Area(ha) x CanopyGrowth(%) [Simulated]";

    private final CreditEntryRepository repo;
    private final RedemptionRepository redemptionRepo;
    private final ParcelService parcels;
    private final CompanyService companies;
    private final AuditService audit;
    private final CreditLedgerMovementRepository ledgerRepo;

    @Autowired
    public CreditService(CreditEntryRepository repo, RedemptionRepository redemptionRepo,
            ParcelService parcels, CompanyService companies, AuditService audit,
            @Autowired(required = false) CreditLedgerMovementRepository ledgerRepo) {
        this.repo = repo;
        this.redemptionRepo = redemptionRepo;
        this.parcels = parcels;
        this.companies = companies;
        this.audit = audit;
        this.ledgerRepo = ledgerRepo;
    }

    public CreditService(CreditEntryRepository repo, RedemptionRepository redemptionRepo,
            ParcelService parcels, CompanyService companies, AuditService audit) {
        this(repo, redemptionRepo, parcels, companies, audit, null);
    }

    /**
     * Demo model: Credits = Parcel Area (ha) x Canopy Growth Improvement (%).
     * Keep the formula in this one method so it can be swapped for the notified
     * tree-based rule later without touching anything else.
     */
    double calculate(double areaHectares, double canopyGrowthPercent) {
        return Math.round(areaHectares * canopyGrowthPercent * 100.0) / 100.0;
    }

    /**
     * Creates a DRAFT credit entry. It only counts as "in pipeline" until an
     * officer approves it. Prevents duplicate pending drafts on the same parcel.
     */
    @Transactional
    public CreditEntry createDraft(CreditCalculationRequest req) {
        LandParcel parcel = parcels.get(req.parcelId());
        if (parcel.getStatus() != ParcelStatus.ALLOTTED || parcel.getAllottedCompanyId() == null) {
            throw new BadRequestException("Credits can only be calculated for an allotted parcel");
        }

        List<CreditEntry> existingForParcel = repo.findByParcelId(parcel.getId());
        if (existingForParcel != null) {
            boolean hasPending = existingForParcel.stream()
                    .anyMatch(e -> e.getStatus() == CreditStatus.DRAFT
                            || e.getStatus() == CreditStatus.SUBMITTED
                            || e.getStatus() == CreditStatus.IN_REVIEW);
            if (hasPending) {
                throw new ConflictException(
                        "A credit entry is already pending officer approval for parcel " + parcel.getId());
            }
        }

        double calculatedCredits = calculate(parcel.getAreaHectares(), req.canopyGrowthPercent());

        CreditEntry e = repo.save(
                CreditEntry.builder()
                        .companyId(parcel.getAllottedCompanyId())
                        .parcelId(parcel.getId())
                        .areaHectares(parcel.getAreaHectares())
                        .canopyGrowthPercent(req.canopyGrowthPercent())
                        .credits(calculatedCredits)
                        .verificationMethod(req.verificationMethod())
                        .status(CreditStatus.DRAFT)
                        .policyVersion(POLICY_VERSION)
                        .createdAt(LocalDateTime.now())
                        .build());

        audit.log("System", "CREDIT_CALCULATED", "CreditEntry", e.getId(),
                parcel.getAreaHectares() + " ha x " + req.canopyGrowthPercent() + "% = " + e.getCredits()
                        + " GC [" + POLICY_VERSION + "]");

        return enrich(e);
    }

    /** Officer approves (optionally revising the amount) or rejects the draft with pessimistic locking. */
    @Transactional
    public CreditEntry decide(Long id, CreditDecisionRequest req) {
        CreditEntry e = repo.findByIdForUpdate(id).orElseGet(() -> get(id));
        if (e.getStatus() == CreditStatus.APPROVED || e.getStatus() == CreditStatus.REJECTED) {
            throw new BadRequestException("Credit entry " + id + " has already been decided");
        }
        e.setDecidedAt(LocalDateTime.now());
        e.setDecidedBy(req.officerName());
        e.setRemarks(req.remarks());
        if (e.getPolicyVersion() == null) {
            e.setPolicyVersion(POLICY_VERSION);
        }
        if (Boolean.TRUE.equals(req.approve())) {
            if (req.revisedCredits() != null) {
                e.setCredits(req.revisedCredits());
            }
            e.setStatus(CreditStatus.APPROVED);
            if (ledgerRepo != null) {
                ledgerRepo.save(CreditLedgerMovement.builder()
                        .transactionId(UUID.randomUUID().toString())
                        .companyId(e.getCompanyId())
                        .amount(BigDecimal.valueOf(e.getCredits()).setScale(4, RoundingMode.HALF_UP))
                        .type(CreditMovementType.CREDIT_ISSUANCE)
                        .referenceType("CREDIT_ENTRY")
                        .referenceId(id)
                        .note("Credit approved and issued by " + req.officerName() + " [" + e.getPolicyVersion() + "]")
                        .timestamp(LocalDateTime.now())
                        .build());
            }
            audit.log(req.officerName(), "CREDIT_APPROVED", "CreditEntry", id, e.getCredits() + " GC issued");
        } else {
            e.setStatus(CreditStatus.REJECTED);
            audit.log(req.officerName(), "CREDIT_REJECTED", "CreditEntry", id, req.remarks());
        }
        return enrich(repo.save(e));
    }

    public CreditEntry get(Long id) {
        return enrich(repo.findById(id).orElseThrow(() -> new NotFoundException("Credit entry " + id + " not found")));
    }

    public List<CreditEntry> find(CreditStatus status) {
        List<CreditEntry> list = status == null ? repo.findAll() : repo.findByStatus(status);
        return list.stream().map(this::enrich).toList();
    }

    public List<CreditEntry> ledger(Long companyId) {
        companies.get(companyId);
        return repo.findByCompanyId(companyId).stream().map(this::enrich).toList();
    }

    /**
     * Total earned, redeemed, available (subtracting both APPROVED redemptions and REQUESTED holds),
     * and in-pipeline credits for one company.
     */
    public CreditAccount account(Long companyId) {
        companies.get(companyId);
        double earned = sumCredits(companyId, CreditStatus.APPROVED);
        double pipeline = repo.findByCompanyId(companyId).stream()
                .filter(e -> e.getStatus() == CreditStatus.DRAFT
                        || e.getStatus() == CreditStatus.SUBMITTED
                        || e.getStatus() == CreditStatus.IN_REVIEW)
                .mapToDouble(CreditEntry::getCredits).sum();
        List<Redemption> companyRedemptions = redemptionRepo.findByCompanyId(companyId);
        double redeemed = companyRedemptions.stream()
                .filter(r -> r.getStatus() == RedemptionStatus.APPROVED)
                .mapToDouble(Redemption::getCredits).sum();
        double held = companyRedemptions.stream()
                .filter(r -> r.getStatus() == RedemptionStatus.REQUESTED)
                .mapToDouble(Redemption::getCredits).sum();
        double available = Math.max(0.0, Math.round((earned - redeemed - held) * 100.0) / 100.0);
        return new CreditAccount(companyId, earned, redeemed, available, pipeline);
    }

    private double sumCredits(Long companyId, CreditStatus status) {
        return repo.findByCompanyId(companyId).stream()
                .filter(e -> e.getStatus() == status)
                .mapToDouble(CreditEntry::getCredits).sum();
    }

    private CreditEntry enrich(CreditEntry e) {
        if (e == null) {
            return null;
        }
        if (e.getPolicyVersion() == null) {
            e.setPolicyVersion(POLICY_VERSION);
        }
        try {
            if (e.getCompanyId() != null && companies != null) {
                Company c = companies.get(e.getCompanyId());
                if (c != null) {
                    e.setCompanyName(c.getName());
                }
            }
        } catch (Exception ignored) {
        }
        try {
            if (e.getParcelId() != null && parcels != null) {
                LandParcel p = parcels.get(e.getParcelId());
                if (p != null) {
                    e.setParcelLocation(p.getLocation());
                    e.setParcelState(p.getState());
                }
            }
        } catch (Exception ignored) {
        }
        return e;
    }
}
