package com.gcp.service;

import com.gcp.dto.ApplicationRequest;
import com.gcp.dto.DecisionRequest;
import com.gcp.exception.BadRequestException;
import com.gcp.exception.NotFoundException;
import com.gcp.model.ApplicationStatus;
import com.gcp.model.Company;
import com.gcp.model.LandApplication;
import com.gcp.model.LandParcel;
import com.gcp.model.ParcelStatus;
import com.gcp.repository.LandApplicationRepository;
import com.gcp.repository.LandParcelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    private final LandApplicationRepository repo;
    private final LandParcelRepository parcelRepo;
    private final CompanyService companies;
    private final ParcelService parcels;
    private final AuditService audit;

    public ApplicationService(LandApplicationRepository repo, LandParcelRepository parcelRepo,
                              CompanyService companies, ParcelService parcels, AuditService audit) {
        this.repo = repo;
        this.parcelRepo = parcelRepo;
        this.companies = companies;
        this.parcels = parcels;
        this.audit = audit;
    }

    @Transactional
    public LandApplication apply(ApplicationRequest req) {
        Company company = companies.get(req.companyId());
        LandParcel parcel = parcels.get(req.parcelId());

        if (parcel.getStatus() != ParcelStatus.AVAILABLE) {
            throw new BadRequestException("Parcel " + parcel.getId() + " is not available");
        }
        boolean alreadyPending = repo.findByParcelId(parcel.getId()).stream()
                .anyMatch(a -> a.getCompanyId().equals(company.getId())
                        && a.getStatus() == ApplicationStatus.PENDING_REVIEW);
        if (alreadyPending) {
            throw new BadRequestException("This company already has a pending application for this parcel");
        }

        LandApplication a = new LandApplication();
        a.setCompanyId(company.getId());
        a.setParcelId(parcel.getId());
        a.setStatus(ApplicationStatus.PENDING_REVIEW);
        a.setSubmittedAt(LocalDateTime.now());
        a = repo.save(a);
        audit.log(company.getName(), "APPLICATION_SUBMITTED", "LandApplication", a.getId(),
                "Parcel " + parcel.getId());
        return a;
    }

    /** Officer approves or rejects. Approval allots the parcel and records the allotment date. */
    @Transactional
    public LandApplication decide(Long id, DecisionRequest req) {
        LandApplication a = get(id);
        if (a.getStatus() != ApplicationStatus.PENDING_REVIEW) {
            throw new BadRequestException("Application " + id + " has already been decided");
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
            LandParcel parcel = parcels.get(a.getParcelId());
            if (parcel.getStatus() != ParcelStatus.AVAILABLE) {
                throw new BadRequestException("Parcel " + parcel.getId() + " has already been allotted");
            }
            a.setStatus(ApplicationStatus.APPROVED);
            parcel.setStatus(ParcelStatus.ALLOTTED);
            parcel.setAllottedCompanyId(a.getCompanyId());
            parcel.setAllotmentDate(LocalDate.now());
            parcelRepo.save(parcel);
            audit.log(req.officerName(), "APPLICATION_APPROVED", "LandApplication", id,
                    "Parcel " + parcel.getId() + " allotted to company " + a.getCompanyId());

            // Other companies waiting on the same parcel are closed automatically.
            for (LandApplication other : repo.findByParcelId(parcel.getId())) {
                if (!other.getId().equals(id) && other.getStatus() == ApplicationStatus.PENDING_REVIEW) {
                    other.setStatus(ApplicationStatus.REJECTED);
                    other.setDecidedAt(now);
                    other.setDecidedBy(req.officerName());
                    other.setDecisionReason("Parcel allotted to another applicant");
                    repo.save(other);
                    audit.log(req.officerName(), "APPLICATION_REJECTED", "LandApplication", other.getId(),
                            "Parcel allotted to another applicant");
                }
            }
        } else {
            a.setStatus(ApplicationStatus.REJECTED);
            audit.log(req.officerName(), "APPLICATION_REJECTED", "LandApplication", id, req.reason());
        }
        return repo.save(a);
    }

    public LandApplication get(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Application " + id + " not found"));
    }

    public List<LandApplication> find(ApplicationStatus status) {
        return status == null ? repo.findAll() : repo.findByStatus(status);
    }

    public List<LandApplication> findForCompany(Long companyId) {
        companies.get(companyId);
        return repo.findByCompanyId(companyId);
    }
}
