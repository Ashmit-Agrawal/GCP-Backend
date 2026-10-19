package com.gcp.config;

import com.gcp.dto.ParcelRequest;
import com.gcp.model.*;
import com.gcp.repository.*;
import com.gcp.service.ParcelService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "gcp.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;
    private final CompanyRepository companyRepo;
    private final LandParcelRepository parcelRepo;
    private final LandApplicationRepository applicationRepo;
    private final CreditEntryRepository creditRepo;
    private final RedemptionRepository redemptionRepo;
    private final AuditLogRepository auditRepo;
    private final ParcelService parcelService;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepo,
                          CompanyRepository companyRepo,
                          LandParcelRepository parcelRepo,
                          LandApplicationRepository applicationRepo,
                          CreditEntryRepository creditRepo,
                          RedemptionRepository redemptionRepo,
                          AuditLogRepository auditRepo,
                          ParcelService parcelService,
                          PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.companyRepo = companyRepo;
        this.parcelRepo = parcelRepo;
        this.applicationRepo = applicationRepo;
        this.creditRepo = creditRepo;
        this.redemptionRepo = redemptionRepo;
        this.auditRepo = auditRepo;
        this.parcelService = parcelService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) {
            return;
        }

        // 1. Seed Admin User
        User admin = new User(
                "admin_gcp",
                passwordEncoder.encode("admin@2024"),
                Role.ROLE_ADMIN,
                null,
                "Dr. Arvind Sharma",
                "arvind.sharma@moefcc.gov.in",
                true
        );
        userRepo.save(admin);

        // 2. Seed Company 1: Tata Power Ltd (PLATINUM - max 50 ha)
        Company tata = new Company();
        tata.setName("Tata Power Ltd.");
        tata.setSector("Energy");
        tata.setCin("L12345MH1919PLC000001");
        tata.setGstin("27AAACT2727Q1ZB");
        tata.setPan("AAACT2727Q");
        tata.setState("Maharashtra");
        tata.setContactPerson("Rohan Mehta");
        tata.setEmail("rohan.mehta@tatapower.com");
        tata.setPhone("9820011223");
        tata.setAnnualTurnoverCrores(12000.0);
        tata.setRank(CompanyRank.PLATINUM);
        tata.setStatus(CompanyStatus.APPROVED);
        tata.setUsername("tata_power");
        tata.setRegisteredAt(LocalDateTime.now().minusMonths(6));
        tata.setVerifiedAt(LocalDateTime.now().minusMonths(6));
        tata.setVerifiedBy("Dr. Arvind Sharma");
        tata = companyRepo.save(tata);

        User tataUser = new User("tata_power", passwordEncoder.encode("tata@123"),
                Role.ROLE_COMPANY, tata.getId(), tata.getName(), tata.getEmail(), true);
        userRepo.save(tataUser);

        // 3. Seed Company 2: Infosys Ltd (PLATINUM - max 50 ha)
        Company infy = new Company();
        infy.setName("Infosys Ltd.");
        infy.setSector("IT Services");
        infy.setCin("L85110KA1981PLC013115");
        infy.setGstin("29AAACI4747L1Z8");
        infy.setPan("AAACI4747L");
        infy.setState("Karnataka");
        infy.setContactPerson("Priya Nair");
        infy.setEmail("priya.nair@infosys.com");
        infy.setPhone("9845012345");
        infy.setAnnualTurnoverCrores(15000.0);
        infy.setRank(CompanyRank.PLATINUM);
        infy.setStatus(CompanyStatus.APPROVED);
        infy.setUsername("infosys_ltd");
        infy.setRegisteredAt(LocalDateTime.now().minusMonths(5));
        infy.setVerifiedAt(LocalDateTime.now().minusMonths(5));
        infy.setVerifiedBy("Dr. Arvind Sharma");
        infy = companyRepo.save(infy);

        User infyUser = new User("infosys_ltd", passwordEncoder.encode("infy@123"),
                Role.ROLE_COMPANY, infy.getId(), infy.getName(), infy.getEmail(), true);
        userRepo.save(infyUser);

        // 4. Seed Company 3: Mahindra & Mahindra Ltd (GOLD - max 25 ha)
        Company mahindra = new Company();
        mahindra.setName("Mahindra & Mahindra Ltd.");
        mahindra.setSector("Automotive");
        mahindra.setCin("L65990MH1945PLC004558");
        mahindra.setGstin("27AAACM3333F1Z4");
        mahindra.setPan("AAACM3333F");
        mahindra.setState("Maharashtra");
        mahindra.setContactPerson("Vikram Deshmukh");
        mahindra.setEmail("deshmukh.vikram@mahindra.com");
        mahindra.setPhone("9822099887");
        mahindra.setAnnualTurnoverCrores(450.0);
        mahindra.setRank(CompanyRank.GOLD);
        mahindra.setStatus(CompanyStatus.APPROVED);
        mahindra.setUsername("mahindra_auto");
        mahindra.setRegisteredAt(LocalDateTime.now().minusMonths(4));
        mahindra.setVerifiedAt(LocalDateTime.now().minusMonths(4));
        mahindra.setVerifiedBy("Dr. Arvind Sharma");
        mahindra = companyRepo.save(mahindra);

        User mahindraUser = new User("mahindra_auto", passwordEncoder.encode("mahindra@123"),
                Role.ROLE_COMPANY, mahindra.getId(), mahindra.getName(), mahindra.getEmail(), true);
        userRepo.save(mahindraUser);

        // 5. Seed Company 4: EcoGreen Ventures Pvt Ltd (PENDING VERIFICATION - SILVER - max 15 ha)
        // Ready for admin manual verification test!
        Company eco = new Company();
        eco.setName("EcoGreen Ventures Pvt Ltd");
        eco.setSector("Renewable Energy");
        eco.setCin("U40106GJ2021PTC123456");
        eco.setGstin("24AABCE1234F1Z8");
        eco.setPan("AABCE1234F");
        eco.setState("Gujarat");
        eco.setContactPerson("Ananya Patel");
        eco.setEmail("ananya@ecogreen.in");
        eco.setPhone("9712345678");
        eco.setAnnualTurnoverCrores(45.0);
        eco.setRank(CompanyRank.SILVER);
        eco.setStatus(CompanyStatus.PENDING_VERIFICATION);
        eco.setUsername("ecogreen");
        eco.setRegisteredAt(LocalDateTime.now().minusDays(2));
        eco = companyRepo.save(eco);

        User ecoUser = new User("ecogreen", passwordEncoder.encode("eco@123"),
                Role.ROLE_COMPANY, eco.getId(), eco.getName(), eco.getEmail(), false); // disabled until verified
        userRepo.save(ecoUser);

        // 6. Seed Land Parcels
        // Allotted parcels
        LandParcel p1 = new LandParcel();
        p1.setId("GCP-MH-2024-001");
        p1.setLocation("Palghar Forest Belt, Maharashtra");
        p1.setState("Maharashtra");
        p1.setStateCode("MH");
        p1.setLatitude(19.6967);
        p1.setLongitude(72.7697);
        p1.setAreaHectares(18.5);
        p1.setForestType("Tropical Moist");
        p1.setDegradationLevel(DegradationLevel.MEDIUM);
        p1.setStatus(ParcelStatus.ALLOTTED);
        p1.setAllottedCompanyId(tata.getId());
        p1.setAllotmentDate(LocalDate.now().minusMonths(5));
        p1.setCreatedAt(LocalDateTime.now().minusMonths(6));
        parcelRepo.save(p1);

        LandParcel p2 = new LandParcel();
        p2.setId("GCP-KA-2024-022");
        p2.setLocation("Bandipur Buffer Zone, Karnataka");
        p2.setState("Karnataka");
        p2.setStateCode("KA");
        p2.setLatitude(11.6854);
        p2.setLongitude(76.6348);
        p2.setAreaHectares(12.0);
        p2.setForestType("Tropical Dry");
        p2.setDegradationLevel(DegradationLevel.LOW);
        p2.setStatus(ParcelStatus.ALLOTTED);
        p2.setAllottedCompanyId(infy.getId());
        p2.setAllotmentDate(LocalDate.now().minusMonths(4));
        p2.setCreatedAt(LocalDateTime.now().minusMonths(5));
        parcelRepo.save(p2);

        // Available parcels with varied area sizes to showcase Rank-based area limits
        parcelService.create(new ParcelRequest("Karnataka", "KA", "Bandipur Buffer Sector 4",
                11.6854, 76.6348, 7.5, "Tropical Dry", DegradationLevel.MEDIUM));

        parcelService.create(new ParcelRequest("Rajasthan", "RJ", "Aravalli Scrubland Zone",
                25.1462, 73.7124, 15.8, "Subtropical Thorn", DegradationLevel.HIGH));

        parcelService.create(new ParcelRequest("Madhya Pradesh", "MP", "Satpura Buffer Zone",
                22.3372, 78.2115, 13.0, "Deciduous", DegradationLevel.LOW));

        parcelService.create(new ParcelRequest("West Bengal", "WB", "Sundarbans Fringe Sector 9",
                21.9497, 88.9000, 24.0, "Mangrove Swamp", DegradationLevel.HIGH));

        parcelService.create(new ParcelRequest("Uttarakhand", "UK", "Doon Valley Foothills",
                30.3165, 78.0322, 9.0, "Subtropical Pine", DegradationLevel.LOW));

        parcelService.create(new ParcelRequest("Odisha", "OD", "Similipal Peripheral Corridor",
                21.9333, 86.3333, 35.0, "Semi-Evergreen", DegradationLevel.HIGH));

        // 7. Seed Applications
        LandApplication a1 = new LandApplication();
        a1.setCompanyId(tata.getId());
        a1.setParcelId(p1.getId());
        a1.setStatus(ApplicationStatus.APPROVED);
        a1.setSubmittedAt(LocalDateTime.now().minusMonths(5).minusDays(10));
        a1.setDecidedAt(LocalDateTime.now().minusMonths(5));
        a1.setDecidedBy("Dr. Arvind Sharma");
        applicationRepo.save(a1);

        LandApplication a2 = new LandApplication();
        a2.setCompanyId(infy.getId());
        a2.setParcelId(p2.getId());
        a2.setStatus(ApplicationStatus.APPROVED);
        a2.setSubmittedAt(LocalDateTime.now().minusMonths(4).minusDays(8));
        a2.setDecidedAt(LocalDateTime.now().minusMonths(4));
        a2.setDecidedBy("Dr. Arvind Sharma");
        applicationRepo.save(a2);

        LandApplication pendingApp = new LandApplication();
        pendingApp.setCompanyId(mahindra.getId());
        pendingApp.setParcelId("GCP-KA-2024-022"); // or available parcel
        pendingApp.setStatus(ApplicationStatus.PENDING_REVIEW);
        pendingApp.setSubmittedAt(LocalDateTime.now().minusDays(5));
        applicationRepo.save(pendingApp);

        // 8. Seed Credits
        CreditEntry c1 = new CreditEntry();
        c1.setCompanyId(tata.getId());
        c1.setParcelId(p1.getId());
        c1.setAreaHectares(18.5);
        c1.setCanopyGrowthPercent(20.0);
        c1.setCredits(370.0);
        c1.setVerificationMethod(VerificationMethod.AI_OFFICER_VERIFIED);
        c1.setStatus(CreditStatus.APPROVED);
        c1.setCreatedAt(LocalDateTime.now().minusMonths(2));
        c1.setDecidedAt(LocalDateTime.now().minusMonths(2));
        c1.setDecidedBy("Dr. Arvind Sharma");
        creditRepo.save(c1);

        CreditEntry cDraft = new CreditEntry();
        cDraft.setCompanyId(tata.getId());
        cDraft.setParcelId(p1.getId());
        cDraft.setAreaHectares(18.5);
        cDraft.setCanopyGrowthPercent(15.5);
        cDraft.setCredits(286.75);
        cDraft.setVerificationMethod(VerificationMethod.AI_VERIFIED);
        cDraft.setStatus(CreditStatus.DRAFT);
        cDraft.setCreatedAt(LocalDateTime.now().minusDays(3));
        creditRepo.save(cDraft);

        // 9. Seed Redemptions
        Redemption r1 = new Redemption();
        r1.setCompanyId(tata.getId());
        r1.setRedemptionOption(RedemptionOption.CARBON_OFFSET_CERTIFICATE);
        r1.setCredits(RedemptionOption.CARBON_OFFSET_CERTIFICATE.getCost());
        r1.setStatus(RedemptionStatus.APPROVED);
        r1.setRequestedAt(LocalDateTime.now().minusMonths(1));
        r1.setDecidedAt(LocalDateTime.now().minusMonths(1));
        r1.setDecidedBy("Dr. Arvind Sharma");
        redemptionRepo.save(r1);

        // 10. Seed Audits
        auditRepo.save(new AuditLog("System", "SEED_INIT", "Database", 1L, "PostgreSQL schema and demo seed initialized"));
        auditRepo.save(new AuditLog("Dr. Arvind Sharma", "COMPANY_APPROVED", "Company", tata.getId(), "Approved Tata Power Ltd (PLATINUM rank, max 50 ha)"));
        auditRepo.save(new AuditLog("Dr. Arvind Sharma", "COMPANY_APPROVED", "Company", infy.getId(), "Approved Infosys Ltd (PLATINUM rank, max 50 ha)"));
        auditRepo.save(new AuditLog("Dr. Arvind Sharma", "COMPANY_APPROVED", "Company", mahindra.getId(), "Approved Mahindra & Mahindra Ltd (GOLD rank, max 25 ha)"));
    }
}
