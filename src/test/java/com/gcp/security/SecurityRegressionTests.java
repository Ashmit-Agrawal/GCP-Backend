package com.gcp.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcp.controller.*;
import com.gcp.dto.*;
import com.gcp.enums.*;
import com.gcp.model.*;
import com.gcp.repository.*;
import com.gcp.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        CompanyController.class,
        ApplicationController.class,
        CreditController.class,
        RedemptionController.class,
        ParcelController.class,
        AuditController.class
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtTokenProvider.class,
        TenantSecurityService.class
})
@TestPropertySource(properties = {
        "jwt.secret=test-suite-secure-signing-key-for-gcp-spring-security-at-least-256-bits-length!",
        "jwt.expiration-ms=3600000",
        "gcp.seed-demo-data=false"
})
public class SecurityRegressionTests {

    private static final String VALID_TEST_SECRET =
            "test-suite-secure-signing-key-for-gcp-spring-security-at-least-256-bits-length!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private LandApplicationRepository applicationRepository;

    @MockBean
    private CreditEntryRepository creditRepository;

    @MockBean
    private RedemptionRepository redemptionRepository;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private CompanyVerificationService companyVerificationService;

    @MockBean
    private ApplicationService applicationService;

    @MockBean
    private ParcelService parcelService;

    @MockBean
    private CreditService creditService;

    @MockBean
    private RedemptionService redemptionService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private MonitoringService monitoringService;

    private String companyAToken;
    private String companyBToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        // Users
        UserPrincipal companyAUser = new UserPrincipal(1L, "companyA", "pass", Role.ROLE_COMPANY, 1L, "Company A", true);
        UserPrincipal companyBUser = new UserPrincipal(2L, "companyB", "pass", Role.ROLE_COMPANY, 2L, "Company B", true);
        UserPrincipal adminUser = new UserPrincipal(99L, "admin_user", "pass", Role.ROLE_ADMIN, null, "Admin Officer", true);

        when(userDetailsService.loadUserByUsername("companyA")).thenReturn(companyAUser);
        when(userDetailsService.loadUserByUsername("companyB")).thenReturn(companyBUser);
        when(userDetailsService.loadUserByUsername("admin_user")).thenReturn(adminUser);

        companyAToken = tokenProvider.generateToken("companyA", Role.ROLE_COMPANY, 1L, "Company A");
        companyBToken = tokenProvider.generateToken("companyB", Role.ROLE_COMPANY, 2L, "Company B");
        adminToken = tokenProvider.generateToken("admin_user", Role.ROLE_ADMIN, null, "Admin Officer");

        // Seed mock company entities
        Company c1 = new Company();
        c1.setId(1L);
        c1.setName("Company A");
        when(companyService.get(1L)).thenReturn(c1);

        Company c2 = new Company();
        c2.setId(2L);
        c2.setName("Company B");
        when(companyService.get(2L)).thenReturn(c2);

        // Seed mock applications
        LandApplication app1 = new LandApplication();
        app1.setId(1L);
        app1.setCompanyId(1L);
        app1.setParcelId("PARCEL-001");
        app1.setStatus(ApplicationStatus.PENDING_REVIEW);

        LandApplication app2 = new LandApplication();
        app2.setId(2L);
        app2.setCompanyId(2L);
        app2.setParcelId("PARCEL-002");
        app2.setStatus(ApplicationStatus.PENDING_REVIEW);

        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app1));
        when(applicationRepository.findById(2L)).thenReturn(Optional.of(app2));
        when(applicationService.get(1L)).thenReturn(app1);
        when(applicationService.get(2L)).thenReturn(app2);

        // Seed mock credit entries
        CreditEntry cr1 = new CreditEntry();
        cr1.setId(1L);
        cr1.setCompanyId(1L);
        cr1.setCredits(100.0);
        cr1.setStatus(CreditStatus.DRAFT);

        CreditEntry cr2 = new CreditEntry();
        cr2.setId(2L);
        cr2.setCompanyId(2L);
        cr2.setCredits(200.0);
        cr2.setStatus(CreditStatus.DRAFT);

        when(creditRepository.findById(1L)).thenReturn(Optional.of(cr1));
        when(creditRepository.findById(2L)).thenReturn(Optional.of(cr2));
        when(creditService.get(1L)).thenReturn(cr1);
        when(creditService.get(2L)).thenReturn(cr2);

        // Seed mock redemptions
        Redemption r1 = new Redemption();
        r1.setId(1L);
        r1.setCompanyId(1L);
        r1.setStatus(RedemptionStatus.REQUESTED);

        Redemption r2 = new Redemption();
        r2.setId(2L);
        r2.setCompanyId(2L);
        r2.setStatus(RedemptionStatus.REQUESTED);

        when(redemptionRepository.findById(1L)).thenReturn(Optional.of(r1));
        when(redemptionRepository.findById(2L)).thenReturn(Optional.of(r2));
        when(redemptionService.get(1L)).thenReturn(r1);
        when(redemptionService.get(2L)).thenReturn(r2);
    }

    // =========================================================================
    // 1. Unauthenticated Request Access
    // =========================================================================
    @Nested
    @DisplayName("1. Unauthenticated Requests")
    class UnauthenticatedRequests {

        @Test
        @DisplayName("Unauthenticated request to protected company resource returns 401 Unauthorized")
        void unauthenticatedCompanyAccess() throws Exception {
            mockMvc.perform(get("/api/companies/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated request to applications returns 401 Unauthorized")
        void unauthenticatedApplicationsAccess() throws Exception {
            mockMvc.perform(post("/api/applications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"companyId\":1,\"parcelId\":\"PARCEL-001\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated request to credit decision returns 401 Unauthorized")
        void unauthenticatedCreditDecision() throws Exception {
            mockMvc.perform(put("/api/credits/1/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"approve\":true,\"officerName\":\"Hacker\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated request to parcel creation returns 401 Unauthorized")
        void unauthenticatedParcelCreation() throws Exception {
            mockMvc.perform(post("/api/parcels")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"state\":\"MH\",\"stateCode\":\"MH\",\"location\":\"Palghar\",\"latitude\":19.0,\"longitude\":72.0,\"areaHectares\":10.0,\"forestType\":\"Dry\",\"degradationLevel\":\"LOW\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated request to audit trail returns 401 Unauthorized")
        void unauthenticatedAuditAccess() throws Exception {
            mockMvc.perform(get("/api/audit"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 2. Company User Permitted Operations
    // =========================================================================
    @Nested
    @DisplayName("2. Company User Permitted Operations")
    class CompanyPermittedOperations {

        @Test
        @DisplayName("Company user can view own profile and sub-resources")
        void companyAccessOwnResources() throws Exception {
            when(companyService.get(1L)).thenReturn(new Company());
            when(applicationService.findForCompany(1L)).thenReturn(Collections.emptyList());
            when(parcelService.findAllottedTo(1L)).thenReturn(Collections.emptyList());
            when(creditService.account(1L)).thenReturn(new CreditAccount(1L, 100.0, 0.0, 100.0, 0.0));
            when(creditService.ledger(1L)).thenReturn(Collections.emptyList());
            when(redemptionService.findForCompany(1L)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/companies/1")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/companies/1/applications")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/companies/1/parcels")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/companies/1/credits")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/companies/1/credits/ledger")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/companies/1/redemptions")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Company user can view own individual application, credit and redemption")
        void companyAccessOwnEntities() throws Exception {
            mockMvc.perform(get("/api/applications/1")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/credits/1")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/redemptions/1")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 3. Tenant Isolation & Cross-Company Data Access Prevention
    // =========================================================================
    @Nested
    @DisplayName("3. Cross-Company Tenant Isolation")
    class TenantIsolation {

        @Test
        @DisplayName("Company A cannot view Company B profile (403 Forbidden)")
        void companyACannotViewCompanyBProfile() throws Exception {
            mockMvc.perform(get("/api/companies/2")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company A cannot view Company B applications (403 Forbidden)")
        void companyACannotViewCompanyBApplications() throws Exception {
            mockMvc.perform(get("/api/companies/2/applications")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company A cannot view Company B allotted parcels (403 Forbidden)")
        void companyACannotViewCompanyBParcels() throws Exception {
            mockMvc.perform(get("/api/companies/2/parcels")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company A cannot view Company B credit balance (403 Forbidden)")
        void companyACannotViewCompanyBCredits() throws Exception {
            mockMvc.perform(get("/api/companies/2/credits")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company A cannot view Company B credit ledger (403 Forbidden)")
        void companyACannotViewCompanyBLedger() throws Exception {
            mockMvc.perform(get("/api/companies/2/credits/ledger")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company A cannot view Company B redemptions (403 Forbidden)")
        void companyACannotViewCompanyBRedemptions() throws Exception {
            mockMvc.perform(get("/api/companies/2/redemptions")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company A cannot view Company B application by ID (403 Forbidden)")
        void companyACannotViewCompanyBApplicationById() throws Exception {
            mockMvc.perform(get("/api/applications/2")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company A cannot view Company B credit entry by ID (403 Forbidden)")
        void companyACannotViewCompanyBCreditById() throws Exception {
            mockMvc.perform(get("/api/credits/2")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company A cannot view Company B redemption by ID (403 Forbidden)")
        void companyACannotViewCompanyBRedemptionById() throws Exception {
            mockMvc.perform(get("/api/redemptions/2")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company user cannot enumerate all companies via GET /api/companies (403 Forbidden)")
        void companyUserCannotEnumerateAllCompanies() throws Exception {
            mockMvc.perform(get("/api/companies")
                            .header("Authorization", "Bearer " + companyAToken))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 4. Company User Cannot Approve/Reject Applications
    // =========================================================================
    @Nested
    @DisplayName("4. Application Decision Authorization")
    class ApplicationDecisionAuthorization {

        @Test
        @DisplayName("Company user cannot approve or reject an application (403 Forbidden)")
        void companyUserCannotDecideApplication() throws Exception {
            DecisionRequest req = new DecisionRequest(true, null, "Hacker User");
            mockMvc.perform(put("/api/applications/1/decision")
                            .header("Authorization", "Bearer " + companyAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 5. Company User Cannot Approve/Reject Credits
    // =========================================================================
    @Nested
    @DisplayName("5. Credit Decision Authorization")
    class CreditDecisionAuthorization {

        @Test
        @DisplayName("Company user cannot approve or reject credits (403 Forbidden)")
        void companyUserCannotDecideCredit() throws Exception {
            CreditDecisionRequest req = new CreditDecisionRequest(true, "Hacker User", 100.0, "Approved by hacker");
            mockMvc.perform(put("/api/credits/1/decision")
                            .header("Authorization", "Bearer " + companyAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company user cannot calculate or create draft credits (403 Forbidden)")
        void companyUserCannotCalculateCredits() throws Exception {
            CreditCalculationRequest req = new CreditCalculationRequest("PARCEL-001", 20.0, VerificationMethod.AI_VERIFIED);
            mockMvc.perform(post("/api/credits/calculate")
                            .header("Authorization", "Bearer " + companyAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 6. Company User Cannot Approve/Reject Redemptions
    // =========================================================================
    @Nested
    @DisplayName("6. Redemption Decision Authorization")
    class RedemptionDecisionAuthorization {

        @Test
        @DisplayName("Company user cannot approve or reject a redemption (403 Forbidden)")
        void companyUserCannotDecideRedemption() throws Exception {
            DecisionRequest req = new DecisionRequest(true, null, "Hacker User");
            mockMvc.perform(put("/api/redemptions/1/decision")
                            .header("Authorization", "Bearer " + companyAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 7. Company User Cannot Create Land Parcels
    // =========================================================================
    @Nested
    @DisplayName("7. Parcel Creation Authorization")
    class ParcelCreationAuthorization {

        @Test
        @DisplayName("Company user cannot create arbitrary land parcels (403 Forbidden)")
        void companyUserCannotCreateParcel() throws Exception {
            ParcelRequest req = new ParcelRequest("Maharashtra", "MH", "Palghar", 19.0, 72.0, 10.0, "Dry", DegradationLevel.LOW);
            mockMvc.perform(post("/api/parcels")
                            .header("Authorization", "Bearer " + companyAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 8. Company User Cannot Inject Monitoring Records
    // =========================================================================
    @Nested
    @DisplayName("8. Monitoring Injection Authorization")
    class MonitoringInjectionAuthorization {

        @Test
        @DisplayName("Company user cannot inject monitoring readings (403 Forbidden)")
        void companyUserCannotAddMonitoringRecord() throws Exception {
            MonitoringRequest req = new MonitoringRequest(null, 50.0, 1.2, 0.8, RiskLevel.LOW, "Fake canopy record");
            mockMvc.perform(post("/api/parcels/PARCEL-001/monitoring")
                            .header("Authorization", "Bearer " + companyAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 9. Derive Identity & Prevent Spoofed Company ID Submissions
    // =========================================================================
    @Nested
    @DisplayName("9. Derive Identity & Spoofed ID Prevention")
    class IdentityDerivationAndSpoofingPrevention {

        @Test
        @DisplayName("Company user cannot submit application with another company's ID (403 Forbidden)")
        void companyUserCannotSubmitAppForOtherCompany() throws Exception {
            ApplicationRequest spoofedReq = new ApplicationRequest(2L, "PARCEL-001");
            mockMvc.perform(post("/api/applications")
                            .header("Authorization", "Bearer " + companyAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(spoofedReq)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company user cannot submit redemption with another company's ID (403 Forbidden)")
        void companyUserCannotSubmitRedemptionForOtherCompany() throws Exception {
            RedemptionRequest spoofedReq = new RedemptionRequest(2L, RedemptionOption.CARBON_OFFSET_CERTIFICATE);
            mockMvc.perform(post("/api/redemptions")
                            .header("Authorization", "Bearer " + companyAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(spoofedReq)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Company user submitting valid application has authoritative companyId enforced")
        void companyUserValidApplicationEnforced() throws Exception {
            ApplicationRequest req = new ApplicationRequest(1L, "PARCEL-001");
            LandApplication created = new LandApplication();
            created.setId(10L);
            created.setCompanyId(1L);
            created.setParcelId("PARCEL-001");

            when(applicationService.apply(any(ApplicationRequest.class))).thenReturn(created);

            mockMvc.perform(post("/api/applications")
                            .header("Authorization", "Bearer " + companyAToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            verify(applicationService).apply(argThat(r -> r.companyId().equals(1L) && r.parcelId().equals("PARCEL-001")));
        }
    }

    // =========================================================================
    // 10. Administrator Authorized Operations
    // =========================================================================
    @Nested
    @DisplayName("10. Administrator Authorized Operations")
    class AdministratorOperations {

        @Test
        @DisplayName("Administrator can view all companies")
        void adminCanListCompanies() throws Exception {
            when(companyService.findByStatus(null)).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/companies")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Administrator can view any company's credits")
        void adminCanViewAnyCompanyCredits() throws Exception {
            when(creditService.account(1L)).thenReturn(new CreditAccount(1L, 100.0, 0.0, 100.0, 0.0));
            when(creditService.account(2L)).thenReturn(new CreditAccount(2L, 200.0, 0.0, 200.0, 0.0));

            mockMvc.perform(get("/api/companies/1/credits")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/companies/2/credits")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Administrator can approve an application")
        void adminCanDecideApplication() throws Exception {
            DecisionRequest req = new DecisionRequest(true, "Dr. Arvind Sharma", "Approved");
            LandApplication decided = new LandApplication();
            decided.setId(1L);
            decided.setStatus(ApplicationStatus.APPROVED);

            when(applicationService.decide(eq(1L), any(DecisionRequest.class))).thenReturn(decided);

            mockMvc.perform(put("/api/applications/1/decision")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Administrator can approve credits")
        void adminCanDecideCredit() throws Exception {
            CreditDecisionRequest req = new CreditDecisionRequest(true, "Dr. Arvind Sharma", 100.0, "Approved");
            CreditEntry decided = new CreditEntry();
            decided.setId(1L);
            decided.setStatus(CreditStatus.APPROVED);

            when(creditService.decide(eq(1L), any(CreditDecisionRequest.class))).thenReturn(decided);

            mockMvc.perform(put("/api/credits/1/decision")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Administrator can approve redemptions")
        void adminCanDecideRedemption() throws Exception {
            DecisionRequest req = new DecisionRequest(true, "Dr. Arvind Sharma", "Approved");
            Redemption decided = new Redemption();
            decided.setId(1L);
            decided.setStatus(RedemptionStatus.APPROVED);

            when(redemptionService.decide(eq(1L), any(DecisionRequest.class))).thenReturn(decided);

            mockMvc.perform(put("/api/redemptions/1/decision")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Administrator can create land parcels")
        void adminCanCreateParcel() throws Exception {
            ParcelRequest req = new ParcelRequest("Maharashtra", "MH", "Palghar", 19.0, 72.0, 10.0, "Dry", DegradationLevel.LOW);
            LandParcel created = new LandParcel();
            created.setId("GCP-MH-2024-001");

            when(parcelService.create(any(ParcelRequest.class))).thenReturn(created);

            mockMvc.perform(post("/api/parcels")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Administrator can record monitoring readings")
        void adminCanAddMonitoringRecord() throws Exception {
            MonitoringRequest req = new MonitoringRequest(null, 25.0, 1.0, 0.9, RiskLevel.LOW, "Verified by officer");
            MonitoringRecord record = new MonitoringRecord();
            record.setId(1L);

            when(monitoringService.add(eq("PARCEL-001"), any(MonitoringRequest.class))).thenReturn(record);

            mockMvc.perform(post("/api/parcels/PARCEL-001/monitoring")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Administrator can view audit logs")
        void adminCanViewAuditLogs() throws Exception {
            when(auditService.find(null, null)).thenReturn(Collections.emptyList());
            mockMvc.perform(get("/api/audit")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }

    // =========================================================================
    // 11. Insecure JWT Configuration Rejection
    // =========================================================================
    @Nested
    @DisplayName("11. JWT Configuration Validation & Rejection of Insecure Defaults")
    class JwtConfigValidation {

        @Test
        @DisplayName("Null or empty JWT secret halts startup")
        void emptySecretFailsStartup() {
            assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", 86400000));
            assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(null, 86400000));
            assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("   ", 86400000));
        }

        @Test
        @DisplayName("Short JWT secret (< 256 bits / 32 bytes) halts startup")
        void shortSecretFailsStartup() {
            assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("short-secret-16bytes", 86400000));
        }

        @Test
        @DisplayName("Publicly known / compromised fallback secret halts startup")
        void compromisedSecretFailsStartup() {
            assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(
                    "9a3f2c4e6b8a1d0f5e7c3b2a1f4e6d8c0b2a4e6f8a1c3e5d7b9a0c2e4f6a8b1d", 86400000));
        }

        @Test
        @DisplayName("Valid secure secret initializes, signs and validates tokens correctly")
        void validSecretOperatesCorrectly() {
            JwtTokenProvider provider = new JwtTokenProvider(VALID_TEST_SECRET, 86400000);
            String token = provider.generateToken("testUser", Role.ROLE_COMPANY, 42L, "Test Full Name");

            assertNotNull(token);
            assertTrue(provider.validateToken(token));
            assertEquals("testUser", provider.getUsernameFromToken(token));
            assertEquals("ROLE_COMPANY", provider.getRoleFromToken(token));
            assertEquals(42L, provider.getCompanyIdFromToken(token));
        }
    }
}
