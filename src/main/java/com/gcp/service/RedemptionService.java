package com.gcp.service;

import com.gcp.dto.DecisionRequest;
import com.gcp.dto.RedemptionOptionInfo;
import com.gcp.dto.RedemptionRequest;
import com.gcp.exception.BadRequestException;
import com.gcp.exception.NotFoundException;
import com.gcp.model.Redemption;
import com.gcp.model.RedemptionOption;
import com.gcp.model.RedemptionStatus;
import com.gcp.repository.RedemptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class RedemptionService {

    private final RedemptionRepository repo;
    private final CreditService credits;
    private final CompanyService companies;
    private final AuditService audit;

    public RedemptionService(RedemptionRepository repo, CreditService credits,
                             CompanyService companies, AuditService audit) {
        this.repo = repo;
        this.credits = credits;
        this.companies = companies;
        this.audit = audit;
    }

    public List<RedemptionOptionInfo> options() {
        return Arrays.stream(RedemptionOption.values())
                .map(o -> new RedemptionOptionInfo(o.name(), o.getCost(), o.getPurpose()))
                .toList();
    }

    /** The balance must cover this request on top of any requests that are still waiting for review. */
    @Transactional
    public Redemption request(RedemptionRequest req) {
        companies.get(req.companyId());
        double available = credits.account(req.companyId()).available();
        double reserved = repo.findByCompanyId(req.companyId()).stream()
                .filter(r -> r.getStatus() == RedemptionStatus.REQUESTED)
                .mapToDouble(Redemption::getCredits).sum();
        int cost = req.option().getCost();
        if (cost > available - reserved) {
            throw new BadRequestException("Not enough available credits: " + (available - reserved)
                    + " GC free, " + cost + " GC needed");
        }

        Redemption r = new Redemption();
        r.setCompanyId(req.companyId());
        r.setRedemptionOption(req.option());
        r.setCredits(cost);
        r.setStatus(RedemptionStatus.REQUESTED);
        r.setRequestedAt(LocalDateTime.now());
        r = repo.save(r);
        audit.log("Company " + req.companyId(), "REDEMPTION_REQUESTED", "Redemption", r.getId(),
                req.option() + " (" + cost + " GC)");
        return r;
    }

    @Transactional
    public Redemption decide(Long id, DecisionRequest req) {
        Redemption r = get(id);
        if (r.getStatus() != RedemptionStatus.REQUESTED) {
            throw new BadRequestException("Redemption " + id + " has already been decided");
        }
        boolean approve = Boolean.TRUE.equals(req.approve());
        r.setStatus(approve ? RedemptionStatus.APPROVED : RedemptionStatus.REJECTED);
        r.setDecidedAt(LocalDateTime.now());
        r.setDecidedBy(req.officerName());
        r.setRemarks(req.reason());
        audit.log(req.officerName(), approve ? "REDEMPTION_APPROVED" : "REDEMPTION_REJECTED",
                "Redemption", id, r.getRedemptionOption() + " (" + r.getCredits() + " GC)");
        return repo.save(r);
    }

    public Redemption get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Redemption " + id + " not found"));
    }

    public List<Redemption> find(RedemptionStatus status) {
        return status == null ? repo.findAll() : repo.findByStatus(status);
    }

    public List<Redemption> findForCompany(Long companyId) {
        companies.get(companyId);
        return repo.findByCompanyId(companyId);
    }
}
