package com.gcp.security;

import com.gcp.model.User;
import com.gcp.repository.CreditEntryRepository;
import com.gcp.repository.LandApplicationRepository;
import com.gcp.repository.RedemptionRepository;
import com.gcp.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component("tenantSecurity")
public class TenantSecurityService {

    private final UserRepository userRepository;
    private final LandApplicationRepository applicationRepository;
    private final CreditEntryRepository creditRepository;
    private final RedemptionRepository redemptionRepository;

    public TenantSecurityService(UserRepository userRepository,
                                 LandApplicationRepository applicationRepository,
                                 CreditEntryRepository creditRepository,
                                 RedemptionRepository redemptionRepository) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.creditRepository = creditRepository;
        this.redemptionRepository = redemptionRepository;
    }

    public Long resolveUserCompanyId() {
        Long cid = SecurityUtils.getCurrentCompanyId();
        if (cid != null) {
            return cid;
        }
        return SecurityUtils.getCurrentUsername()
                .flatMap(userRepository::findByUsername)
                .map(User::getCompanyId)
                .orElse(null);
    }

    public boolean isCompanyOwner(Long companyId) {
        if (companyId == null) {
            return false;
        }
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        Long userCompanyId = resolveUserCompanyId();
        return Objects.equals(companyId, userCompanyId);
    }

    public boolean canAccessApplication(Long applicationId) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        if (applicationId == null) {
            return false;
        }
        Long userCompanyId = resolveUserCompanyId();
        if (userCompanyId == null) {
            return false;
        }
        return applicationRepository.findById(applicationId)
                .map(app -> Objects.equals(app.getCompanyId(), userCompanyId))
                .orElse(false);
    }

    public boolean canAccessCredit(Long creditId) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        if (creditId == null) {
            return false;
        }
        Long userCompanyId = resolveUserCompanyId();
        if (userCompanyId == null) {
            return false;
        }
        return creditRepository.findById(creditId)
                .map(credit -> Objects.equals(credit.getCompanyId(), userCompanyId))
                .orElse(false);
    }

    public boolean canAccessRedemption(Long redemptionId) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        if (redemptionId == null) {
            return false;
        }
        Long userCompanyId = resolveUserCompanyId();
        if (userCompanyId == null) {
            return false;
        }
        return redemptionRepository.findById(redemptionId)
                .map(redemption -> Objects.equals(redemption.getCompanyId(), userCompanyId))
                .orElse(false);
    }
}
