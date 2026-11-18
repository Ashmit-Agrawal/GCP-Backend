package com.gcp.config;

import com.gcp.dto.ParcelRequest;
import com.gcp.enums.*;
import com.gcp.model.*;
import com.gcp.repository.*;
import com.gcp.service.CreditService;
import com.gcp.service.MockSatelliteMonitoringProvider;
import com.gcp.service.ParcelService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "gcp.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

        private final UserRepository userRepo;
        private final CompanyRepository companyRepo;
        private final LandParcelRepository parcelRepo;
        private final LandApplicationRepository applicationRepo;
        private final ParcelAllocationRepository allocationRepo;
        private final MonitoringRecordRepository monitoringRepo;
        private final CreditEntryRepository creditRepo;
        private final CreditLedgerMovementRepository ledgerRepo;
        private final RedemptionRepository redemptionRepo;
        private final AuditLogRepository auditRepo;
        private final ParcelService parcelService;
        private final PasswordEncoder passwordEncoder;

        public DemoDataSeeder(UserRepository userRepo,
                        CompanyRepository companyRepo,
                        LandParcelRepository parcelRepo,
                        LandApplicationRepository applicationRepo,
                        ParcelAllocationRepository allocationRepo,
                        MonitoringRecordRepository monitoringRepo,
                        CreditEntryRepository creditRepo,
                        CreditLedgerMovementRepository ledgerRepo,
                        RedemptionRepository redemptionRepo,
                        AuditLogRepository auditRepo,
                        ParcelService parcelService,
                        PasswordEncoder passwordEncoder) {
                this.userRepo = userRepo;
                this.companyRepo = companyRepo;
                this.parcelRepo = parcelRepo;
                this.applicationRepo = applicationRepo;
                this.allocationRepo = allocationRepo;
                this.monitoringRepo = monitoringRepo;
                this.creditRepo = creditRepo;
                this.ledgerRepo = ledgerRepo;
                this.redemptionRepo = redemptionRepo;
                this.auditRepo = auditRepo;
                this.parcelService = parcelService;
                this.passwordEncoder = passwordEncoder;
        }

        @Override
        @Transactional
        public void run(String... args) {
                if (userRepo.count() > 0 && monitoringRepo.count() > 0) {
                        return;
                }

                // Clear any legacy pre-Phase-1 demo rows so Phase 1 enriched demo dataset seeds cleanly
                if (userRepo.count() > 0) {
                        auditRepo.deleteAll();
                        redemptionRepo.deleteAll();
                        ledgerRepo.deleteAll();
                        creditRepo.deleteAll();
                        monitoringRepo.deleteAll();
                        allocationRepo.deleteAll();
                        applicationRepo.deleteAll();
                        companyRepo.deleteAll();
                        userRepo.deleteAll();
                }

                // Ensure MongoDB parcels are clean when seeding a fresh PostgreSQL database
                parcelRepo.deleteAll();

                // 1. Seed Admin User
                User admin = new User(
                                "admin_gcp",
                                passwordEncoder.encode("admin@2024"),
                                Role.ROLE_ADMIN,
                                null,
                                "Dr. Arvind Sharma",
                                "arvind.sharma@moefcc.gov.in",
                                true);
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
                                Role.ROLE_COMPANY, eco.getId(), eco.getName(), eco.getEmail(), false);
                userRepo.save(ecoUser);

                // 6. Seed Land Parcels
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

                LandParcel availMp = parcelService.create(new ParcelRequest("Madhya Pradesh", "MP", "Satpura Buffer Zone",
                                22.3372, 78.2115, 13.0, "Deciduous", DegradationLevel.LOW));

                parcelService.create(new ParcelRequest("West Bengal", "WB", "Sundarbans Fringe Sector 9",
                                21.9497, 88.9000, 24.0, "Mangrove Swamp", DegradationLevel.HIGH));

                parcelService.create(new ParcelRequest("Uttarakhand", "UK", "Doon Valley Foothills",
                                30.3165, 78.0322, 9.0, "Subtropical Pine", DegradationLevel.LOW));

                parcelService.create(new ParcelRequest("Odisha", "OD", "Similipal Peripheral Corridor",
                                21.9333, 86.3333, 35.0, "Semi-Evergreen", DegradationLevel.HIGH));

                // 7. Seed Applications & Authoritative ParcelAllocations
                LandApplication a1 = new LandApplication();
                a1.setCompanyId(tata.getId());
                a1.setParcelId(p1.getId());
                a1.setStatus(ApplicationStatus.APPROVED);
                a1.setSubmittedAt(LocalDateTime.now().minusMonths(5).minusDays(10));
                a1.setDecidedAt(LocalDateTime.now().minusMonths(5));
                a1.setDecidedBy("Dr. Arvind Sharma");
                a1.setDecisionReason("Approved under MoEFCC Western Ghats Restoration Scheme");
                a1.setProposedSpecies("Native Teak, Bamboo, Indian Rosewood");
                a1.setSaplingCount(7400);
                a1.setPlantingPlanSummary("Monsoon staggered pit-planting across 18.5 ha with drip-mulch retention and fire-line trenching.");
                a1.setSupportingDocRef("GCP-AFF-PLAN-MH-001.pdf");
                a1.setPlantationStatus("UNDER_MONITORING");
                a1.setLastActivityNote("7,400 native saplings planted; 92% survival verified at 6-month milestone.");
                a1 = applicationRepo.save(a1);

                allocationRepo.save(ParcelAllocation.builder()
                                .parcelId(p1.getId())
                                .companyId(tata.getId())
                                .applicationId(a1.getId())
                                .status(AllocationStatus.ALLOCATED)
                                .allotmentDate(p1.getAllotmentDate())
                                .createdAt(LocalDateTime.now().minusMonths(5))
                                .build());

                LandApplication a2 = new LandApplication();
                a2.setCompanyId(infy.getId());
                a2.setParcelId(p2.getId());
                a2.setStatus(ApplicationStatus.APPROVED);
                a2.setSubmittedAt(LocalDateTime.now().minusMonths(4).minusDays(8));
                a2.setDecidedAt(LocalDateTime.now().minusMonths(4));
                a2.setDecidedBy("Dr. Arvind Sharma");
                a2.setDecisionReason("Approved for Nilgiri Biosphere buffer afforestation");
                a2.setProposedSpecies("Sandalwood, Malabar Neem, Pongamia");
                a2.setSaplingCount(4800);
                a2.setPlantingPlanSummary("Buffer zone native species restoration across 12.0 ha with rainwater harvesting trenches.");
                a2.setSupportingDocRef("GCP-AFF-PLAN-KA-022.pdf");
                a2.setPlantationStatus("PLANTED");
                a2.setLastActivityNote("4,800 saplings planted across Bandipur Buffer sectors A-C; mulching completed.");
                a2 = applicationRepo.save(a2);

                allocationRepo.save(ParcelAllocation.builder()
                                .parcelId(p2.getId())
                                .companyId(infy.getId())
                                .applicationId(a2.getId())
                                .status(AllocationStatus.ALLOCATED)
                                .allotmentDate(p2.getAllotmentDate())
                                .createdAt(LocalDateTime.now().minusMonths(4))
                                .build());

                // Pending application pointing to an actually AVAILABLE parcel (Satpura Buffer Zone, 13.0 ha <= 25 ha GOLD limit)
                LandApplication pendingApp = new LandApplication();
                pendingApp.setCompanyId(mahindra.getId());
                pendingApp.setParcelId(availMp.getId());
                pendingApp.setStatus(ApplicationStatus.PENDING_REVIEW);
                pendingApp.setSubmittedAt(LocalDateTime.now().minusDays(5));
                pendingApp.setProposedSpecies("Sal, Mahua, Arjun, Bamboo");
                pendingApp.setSaplingCount(5200);
                pendingApp.setPlantingPlanSummary("Watershed-aligned native deciduous afforestation (400 saplings/ha) with community forest guards.");
                pendingApp.setSupportingDocRef("GCP-AFF-PLAN-MP-SATPURA.pdf");
                pendingApp.setPlantationStatus("NOT_STARTED");
                pendingApp.setLastActivityNote("Awaiting Nodal Officer review and parcel allocation.");
                applicationRepo.save(pendingApp);

                // 8. Seed Simulated Satellite & AI Monitoring Records
                monitoringRepo.save(MonitoringRecord.builder()
                                .parcelId(p1.getId())
                                .recordedOn(LocalDate.now().minusMonths(4))
                                .canopyPercent(22.0)
                                .biomassIndex(1.05)
                                .moistureStability(68.0)
                                .riskLevel(RiskLevel.MEDIUM)
                                .providerName(MockSatelliteMonitoringProvider.PROVIDER_NAME)
                                .modelVersion(MockSatelliteMonitoringProvider.MODEL_VERSION)
                                .confidenceScore(0.86)
                                .isSimulated(true)
                                .inspectionStatus("AI_ADVISORY_ONLY")
                                .note("[SIMULATED SATELLITE & AI PASS — MOCK_SENTINEL2_AI] Baseline post-plantation multi-spectral pass: 22.0% initial canopy cover.")
                                .build());

                monitoringRepo.save(MonitoringRecord.builder()
                                .parcelId(p1.getId())
                                .recordedOn(LocalDate.now().minusMonths(2))
                                .canopyPercent(33.5)
                                .biomassIndex(1.28)
                                .moistureStability(71.5)
                                .riskLevel(RiskLevel.LOW)
                                .providerName(MockSatelliteMonitoringProvider.PROVIDER_NAME)
                                .modelVersion(MockSatelliteMonitoringProvider.MODEL_VERSION)
                                .confidenceScore(0.89)
                                .isSimulated(true)
                                .inspectionStatus("AI_ADVISORY_ONLY")
                                .note("[SIMULATED SATELLITE & AI PASS — MOCK_SENTINEL2_AI] Mid-cycle pass shows +11.5% canopy expansion across northern ridge.")
                                .build());

                monitoringRepo.save(MonitoringRecord.builder()
                                .parcelId(p1.getId())
                                .recordedOn(LocalDate.now().minusDays(7))
                                .canopyPercent(42.0)
                                .biomassIndex(1.49)
                                .moistureStability(75.4)
                                .riskLevel(RiskLevel.LOW)
                                .providerName(MockSatelliteMonitoringProvider.PROVIDER_NAME)
                                .modelVersion(MockSatelliteMonitoringProvider.MODEL_VERSION)
                                .confidenceScore(0.92)
                                .isSimulated(true)
                                .inspectionStatus("FIELD_VERIFIED")
                                .inspectedBy("Dr. Arvind Sharma")
                                .inspectedAt(LocalDateTime.now().minusDays(5))
                                .inspectorNotes("Ground-truthing inspection confirmed >90% sapling survival and healthy canopy closure.")
                                .note("[SIMULATED SATELLITE & AI PASS — MOCK_SENTINEL2_AI] Current pass: 42.0% canopy density (+20.0% net growth over baseline).")
                                .build());

                monitoringRepo.save(MonitoringRecord.builder()
                                .parcelId(p2.getId())
                                .recordedOn(LocalDate.now().minusMonths(3))
                                .canopyPercent(19.5)
                                .biomassIndex(0.98)
                                .moistureStability(64.0)
                                .riskLevel(RiskLevel.MEDIUM)
                                .providerName(MockSatelliteMonitoringProvider.PROVIDER_NAME)
                                .modelVersion(MockSatelliteMonitoringProvider.MODEL_VERSION)
                                .confidenceScore(0.85)
                                .isSimulated(true)
                                .inspectionStatus("AI_ADVISORY_ONLY")
                                .note("[SIMULATED SATELLITE & AI PASS — MOCK_SENTINEL2_AI] Baseline pass for Bandipur Buffer Zone: 19.5% canopy density.")
                                .build());

                monitoringRepo.save(MonitoringRecord.builder()
                                .parcelId(p2.getId())
                                .recordedOn(LocalDate.now().minusDays(4))
                                .canopyPercent(34.0)
                                .biomassIndex(1.32)
                                .moistureStability(70.8)
                                .riskLevel(RiskLevel.LOW)
                                .providerName(MockSatelliteMonitoringProvider.PROVIDER_NAME)
                                .modelVersion(MockSatelliteMonitoringProvider.MODEL_VERSION)
                                .confidenceScore(0.90)
                                .isSimulated(true)
                                .inspectionStatus("INSPECTION_REQUESTED")
                                .inspectedBy("Dr. Arvind Sharma")
                                .inspectedAt(LocalDateTime.now().minusDays(2))
                                .inspectorNotes("Scheduled field verification team for Bandipur Sector B.")
                                .note("[SIMULATED SATELLITE & AI PASS — MOCK_SENTINEL2_AI] Latest pass: 34.0% canopy density (+14.5% growth). Ready for field inspection.")
                                .build());

                // 9. Seed Credits & Matching Immutable Ledger Movements
                CreditEntry c1 = new CreditEntry();
                c1.setCompanyId(tata.getId());
                c1.setParcelId(p1.getId());
                c1.setAreaHectares(18.5);
                c1.setCanopyGrowthPercent(20.0);
                c1.setCredits(370.0);
                c1.setVerificationMethod(VerificationMethod.AI_OFFICER_VERIFIED);
                c1.setStatus(CreditStatus.APPROVED);
                c1.setPolicyVersion(CreditService.POLICY_VERSION);
                c1.setCreatedAt(LocalDateTime.now().minusMonths(2));
                c1.setDecidedAt(LocalDateTime.now().minusMonths(2));
                c1.setDecidedBy("Dr. Arvind Sharma");
                c1.setRemarks("Verified via Sentinel-2 NDVI pass and field officer inspection.");
                c1 = creditRepo.save(c1);

                ledgerRepo.save(CreditLedgerMovement.builder()
                                .transactionId(UUID.randomUUID().toString())
                                .companyId(tata.getId())
                                .amount(BigDecimal.valueOf(370.0).setScale(4, RoundingMode.HALF_UP))
                                .type(CreditMovementType.CREDIT_ISSUANCE)
                                .referenceType("CREDIT_ENTRY")
                                .referenceId(c1.getId())
                                .balanceAfter(BigDecimal.valueOf(370.0).setScale(4, RoundingMode.HALF_UP))
                                .note("Credit approved and issued by Dr. Arvind Sharma [" + CreditService.POLICY_VERSION + "]")
                                .timestamp(LocalDateTime.now().minusMonths(2))
                                .build());

                CreditEntry cDraft = new CreditEntry();
                cDraft.setCompanyId(infy.getId());
                cDraft.setParcelId(p2.getId());
                cDraft.setAreaHectares(12.0);
                cDraft.setCanopyGrowthPercent(14.5);
                cDraft.setCredits(174.0);
                cDraft.setVerificationMethod(VerificationMethod.AI_VERIFIED);
                cDraft.setStatus(CreditStatus.DRAFT);
                cDraft.setPolicyVersion(CreditService.POLICY_VERSION);
                cDraft.setCreatedAt(LocalDateTime.now().minusDays(3));
                cDraft.setRemarks("Auto-calculated from simulated satellite pass; awaiting field inspection confirmation.");
                creditRepo.save(cDraft);

                // 10. Seed Redemptions & Matching Ledger Movements
                Redemption r1 = new Redemption();
                r1.setCompanyId(tata.getId());
                r1.setRedemptionOption(RedemptionOption.CARBON_OFFSET_CERTIFICATE);
                r1.setCredits(RedemptionOption.CARBON_OFFSET_CERTIFICATE.getCost());
                r1.setStatus(RedemptionStatus.APPROVED);
                r1.setRequestedAt(LocalDateTime.now().minusMonths(1));
                r1.setDecidedAt(LocalDateTime.now().minusMonths(1));
                r1.setDecidedBy("Dr. Arvind Sharma");
                r1.setRemarks("Issued Carbon Offset Certificate #MoEFCC-COC-2026-001");
                r1 = redemptionRepo.save(r1);

                ledgerRepo.save(CreditLedgerMovement.builder()
                                .transactionId(UUID.randomUUID().toString())
                                .companyId(tata.getId())
                                .amount(BigDecimal.valueOf(r1.getCredits()).setScale(4, RoundingMode.HALF_UP))
                                .type(CreditMovementType.REDEMPTION_HOLD)
                                .referenceType("REDEMPTION")
                                .referenceId(r1.getId())
                                .balanceAfter(BigDecimal.valueOf(270.0).setScale(4, RoundingMode.HALF_UP))
                                .note("Credit hold for redemption request: CARBON_OFFSET_CERTIFICATE")
                                .timestamp(LocalDateTime.now().minusMonths(1).minusHours(2))
                                .build());

                ledgerRepo.save(CreditLedgerMovement.builder()
                                .transactionId(UUID.randomUUID().toString())
                                .companyId(tata.getId())
                                .amount(BigDecimal.valueOf(r1.getCredits()).setScale(4, RoundingMode.HALF_UP))
                                .type(CreditMovementType.REDEMPTION_SETTLED)
                                .referenceType("REDEMPTION")
                                .referenceId(r1.getId())
                                .balanceAfter(BigDecimal.valueOf(270.0).setScale(4, RoundingMode.HALF_UP))
                                .note("Redemption approved and settled by Dr. Arvind Sharma")
                                .timestamp(LocalDateTime.now().minusMonths(1))
                                .build());

                // 11. Seed Audits
                auditRepo.save(
                                AuditLog.builder()
                                                .actor("System")
                                                .actionType("SEED_INIT")
                                                .entityType("Database")
                                                .entityId("1")
                                                .details("PostgreSQL schema, MongoDB parcels, and demo seed initialized")
                                                .loggedAt(LocalDateTime.now().minusMonths(6))
                                                .build());

                auditRepo.save(
                                AuditLog.builder()
                                                .actor("Dr. Arvind Sharma")
                                                .actionType("COMPANY_APPROVED")
                                                .entityType("Company")
                                                .entityId(String.valueOf(tata.getId()))
                                                .details("Approved Tata Power Ltd (PLATINUM rank, max 50 ha)")
                                                .loggedAt(LocalDateTime.now().minusMonths(6))
                                                .build());

                auditRepo.save(
                                AuditLog.builder()
                                                .actor("Dr. Arvind Sharma")
                                                .actionType("COMPANY_APPROVED")
                                                .entityType("Company")
                                                .entityId(String.valueOf(infy.getId()))
                                                .details("Approved Infosys Ltd (PLATINUM rank, max 50 ha)")
                                                .loggedAt(LocalDateTime.now().minusMonths(5))
                                                .build());

                auditRepo.save(
                                AuditLog.builder()
                                                .actor("Dr. Arvind Sharma")
                                                .actionType("COMPANY_APPROVED")
                                                .entityType("Company")
                                                .entityId(String.valueOf(mahindra.getId()))
                                                .details("Approved Mahindra & Mahindra Ltd (GOLD rank, max 25 ha)")
                                                .loggedAt(LocalDateTime.now().minusMonths(4))
                                                .build());
        }
}
