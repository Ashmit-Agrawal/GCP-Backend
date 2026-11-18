package com.gcp.service;

import com.gcp.dto.DecisionRequest;
import com.gcp.dto.RedemptionOptionInfo;
import com.gcp.dto.RedemptionRequest;
import com.gcp.enums.CreditMovementType;
import com.gcp.enums.RedemptionOption;
import com.gcp.enums.RedemptionStatus;
import com.gcp.exception.BadRequestException;
import com.gcp.exception.ConflictException;
import com.gcp.exception.NotFoundException;
import com.gcp.model.Company;
import com.gcp.model.CreditLedgerMovement;
import com.gcp.model.Redemption;
import com.gcp.repository.CompanyRepository;
import com.gcp.repository.CreditLedgerMovementRepository;
import com.gcp.repository.RedemptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RedemptionService {

    private static final Logger log = LoggerFactory.getLogger(RedemptionService.class);

    private final RedemptionRepository repo;
    private final CreditService credits;
    private final CompanyService companies;
    private final CompanyRepository companyRepo;
    private final CreditLedgerMovementRepository ledgerRepo;
    private final AuditService audit;

    public RedemptionService(RedemptionRepository repo,
                             CreditService credits,
                             CompanyService companies,
                             CompanyRepository companyRepo,
                             CreditLedgerMovementRepository ledgerRepo,
                             AuditService audit) {
        this.repo = repo;
        this.credits = credits;
        this.companies = companies;
        this.companyRepo = companyRepo;
        this.ledgerRepo = ledgerRepo;
        this.audit = audit;
    }

    public List<RedemptionOptionInfo> options() {
        return Arrays.stream(RedemptionOption.values())
                .map(o -> new RedemptionOptionInfo(o.name(), o.getCost(), o.getPurpose()))
                .toList();
    }

    /**
     * Concurrency-safe credit redemption request protected by database-level pessimistic row locking
     * on the company record to strictly prevent double-spending race conditions.
     */
    @Transactional
    public Redemption request(RedemptionRequest req) {
        // 1. Idempotency Check: if request with idempotency key already exists, return existing
        if (req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            Optional<Redemption> existing = repo.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing Redemption for idempotencyKey={}", req.idempotencyKey());
                return existing.get();
            }
        }

        // 2. Acquire PESSIMISTIC_WRITE lock on Company record in PostgreSQL
        Company company = companyRepo.findByIdForUpdate(req.companyId())
                .orElseThrow(() -> new NotFoundException("Company " + req.companyId() + " not found"));

        // 3. Atomically calculate available credits with lock held
        double earned = credits.account(company.getId()).totalEarned();
        double alreadyRedeemed = repo.findByCompanyId(company.getId()).stream()
                .filter(r -> r.getStatus() == RedemptionStatus.APPROVED)
                .mapToDouble(Redemption::getCredits).sum();
        double available = Math.max(0.0, earned - alreadyRedeemed);

        double reservedPending = repo.findByCompanyId(company.getId()).stream()
                .filter(r -> r.getStatus() == RedemptionStatus.REQUESTED)
                .mapToDouble(Redemption::getCredits).sum();

        double freeBalance = Math.max(0.0, available - reservedPending);
        int cost = req.option().getCost();

        if (cost > freeBalance) {
            throw new BadRequestException(String.format(
                    "Insufficient available credits: %.2f GC free (%.2f earned, %.2f redeemed, %.2f pending hold), but %d GC required for %s",
                    freeBalance, earned, alreadyRedeemed, reservedPending, cost, req.option().name()
            ));
        }

        // 4. Persist Redemption record in REQUESTED status
        Redemption r = Redemption.builder()
                .companyId(company.getId())
                .redemptionOption(req.option())
                .credits(cost)
                .status(RedemptionStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .idempotencyKey(req.idempotencyKey())
                .build();
        r = repo.save(r);

        // 5. Create immutable CreditLedgerMovement entry (REDEMPTION_HOLD)
        BigDecimal costBd = BigDecimal.valueOf(cost).setScale(4, RoundingMode.HALF_UP);
        BigDecimal balanceAfter = BigDecimal.valueOf(freeBalance - cost).setScale(4, RoundingMode.HALF_UP);

        CreditLedgerMovement holdMovement = CreditLedgerMovement.builder()
                .transactionId(UUID.randomUUID().toString())
                .companyId(company.getId())
                .amount(costBd)
                .type(CreditMovementType.REDEMPTION_HOLD)
                .referenceType("REDEMPTION")
                .referenceId(r.getId())
                .balanceAfter(balanceAfter)
                .note("Credit hold for redemption request: " + req.option().name())
                .timestamp(LocalDateTime.now())
                .build();
        ledgerRepo.save(holdMovement);

        audit.log("Company " + req.companyId(), "REDEMPTION_REQUESTED", "Redemption", r.getId(),
                req.option() + " (" + cost + " GC hold, balance after: " + balanceAfter + ")");

        return r;
    }

    /**
     * Officer decision with pessimistic locking and immutable ledger settlement/release.
     */
    @Transactional
    public Redemption decide(Long id, DecisionRequest req) {
        // Lock redemption record
        Redemption r = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Redemption " + id + " not found"));

        // Idempotency check: if already decided
        if (r.getStatus() != RedemptionStatus.REQUESTED) {
            boolean requestedApproval = Boolean.TRUE.equals(req.approve());
            boolean currentApproval = r.getStatus() == RedemptionStatus.APPROVED;
            if (requestedApproval == currentApproval) {
                log.info("Idempotent replay: Redemption {} is already decided with status {}", id, r.getStatus());
                return r;
            }
            throw new ConflictException("Redemption " + id + " has already been decided with status: " + r.getStatus());
        }

        // Lock company record
        companyRepo.findByIdForUpdate(r.getCompanyId());

        boolean approve = Boolean.TRUE.equals(req.approve());
        r.setStatus(approve ? RedemptionStatus.APPROVED : RedemptionStatus.REJECTED);
        r.setDecidedAt(LocalDateTime.now());
        r.setDecidedBy(req.officerName());
        r.setRemarks(req.reason());

        // Create immutable CreditLedgerMovement (SETTLED or RELEASED)
        CreditMovementType movementType = approve
                ? CreditMovementType.REDEMPTION_SETTLED
                : CreditMovementType.REDEMPTION_RELEASED;

        CreditLedgerMovement decisionMovement = CreditLedgerMovement.builder()
                .transactionId(UUID.randomUUID().toString())
                .companyId(r.getCompanyId())
                .amount(BigDecimal.valueOf(r.getCredits()).setScale(4, RoundingMode.HALF_UP))
                .type(movementType)
                .referenceType("REDEMPTION")
                .referenceId(r.getId())
                .note(approve ? "Redemption approved and settled by " + req.officerName()
                              : "Redemption rejected by " + req.officerName() + ": " + req.reason())
                .timestamp(LocalDateTime.now())
                .build();
        ledgerRepo.save(decisionMovement);

        audit.log(req.officerName(), approve ? "REDEMPTION_APPROVED" : "REDEMPTION_REJECTED",
                "Redemption", id, r.getRedemptionOption() + " (" + r.getCredits() + " GC)");

        return repo.save(r);
    }

    public Redemption get(Long id) {
        return enrich(repo.findById(id).orElseThrow(() -> new NotFoundException("Redemption " + id + " not found")));
    }

    public List<Redemption> find(RedemptionStatus status) {
        List<Redemption> list = status == null ? repo.findAll() : repo.findByStatus(status);
        return list.stream().map(this::enrich).toList();
    }

    public List<Redemption> findForCompany(Long companyId) {
        companies.get(companyId);
        return repo.findByCompanyId(companyId).stream().map(this::enrich).toList();
    }

    private Redemption enrich(Redemption r) {
        if (r == null) {
            return null;
        }
        if (r.getRedemptionOption() != null) {
            r.setPurpose(r.getRedemptionOption().getPurpose());
        }
        try {
            if (r.getCompanyId() != null && companies != null) {
                Company c = companies.get(r.getCompanyId());
                if (c != null) {
                    r.setCompanyName(c.getName());
                }
            }
        } catch (Exception ignored) {
        }
        return r;
    }
}
