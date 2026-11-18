package com.gcp.concurrency;

import com.gcp.dto.ApplicationRequest;
import com.gcp.dto.DecisionRequest;
import com.gcp.dto.RedemptionRequest;
import com.gcp.enums.*;
import com.gcp.exception.BadRequestException;
import com.gcp.exception.ConflictException;
import com.gcp.model.*;
import com.gcp.repository.*;
import com.gcp.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IdempotencyAndStateTransitionTests {

    @Mock
    private LandApplicationRepository applicationRepo;

    @Mock
    private LandParcelRepository parcelRepo;

    @Mock
    private ParcelAllocationRepository allocationRepo;

    @Mock
    private CompanyService companyService;

    @Mock
    private ParcelService parcelService;

    @Mock
    private AuditService auditService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private RedemptionRepository redemptionRepo;

    @Mock
    private CreditService creditService;

    @Mock
    private CompanyRepository companyRepo;

    @Mock
    private CreditLedgerMovementRepository ledgerRepo;

    private ApplicationService applicationService;
    private RedemptionService redemptionService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                applicationRepo,
                parcelRepo,
                allocationRepo,
                companyService,
                parcelService,
                auditService,
                outboxService,
                mongoTemplate
        );

        redemptionService = new RedemptionService(
                redemptionRepo,
                creditService,
                companyService,
                companyRepo,
                ledgerRepo,
                auditService
        );
    }

    // =========================================================================
    // 1. Idempotency Tests
    // =========================================================================
    @Test
    @DisplayName("Repeated application submission with identical idempotencyKey returns existing application without duplicate insertion")
    void testIdempotentApplicationSubmission() {
        String idempKey = "idemp-app-12345";
        LandApplication existingApp = LandApplication.builder()
                .id(99L)
                .companyId(1L)
                .parcelId("GCP-MH-2024-001")
                .status(ApplicationStatus.PENDING_REVIEW)
                .idempotencyKey(idempKey)
                .submittedAt(LocalDateTime.now())
                .build();

        when(applicationRepo.findByIdempotencyKey(idempKey)).thenReturn(Optional.of(existingApp));

        ApplicationRequest req = new ApplicationRequest(1L, "GCP-MH-2024-001", idempKey);
        LandApplication result = applicationService.apply(req);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals(idempKey, result.getIdempotencyKey());

        // Verify save was NOT called again
        verify(applicationRepo, never()).save(any());
        verifyNoInteractions(parcelService);
    }

    @Test
    @DisplayName("Repeated redemption request with identical idempotencyKey returns existing redemption without duplicate hold")
    void testIdempotentRedemptionSubmission() {
        String idempKey = "idemp-red-54321";
        Redemption existingRedemption = Redemption.builder()
                .id(42L)
                .companyId(1L)
                .redemptionOption(RedemptionOption.CARBON_OFFSET_CERTIFICATE)
                .credits(100.0)
                .status(RedemptionStatus.REQUESTED)
                .idempotencyKey(idempKey)
                .requestedAt(LocalDateTime.now())
                .build();

        when(redemptionRepo.findByIdempotencyKey(idempKey)).thenReturn(Optional.of(existingRedemption));

        RedemptionRequest req = new RedemptionRequest(1L, RedemptionOption.CARBON_OFFSET_CERTIFICATE, idempKey);
        Redemption result = redemptionService.request(req);

        assertNotNull(result);
        assertEquals(42L, result.getId());
        verify(redemptionRepo, never()).save(any());
        verifyNoInteractions(companyRepo);
        verifyNoInteractions(ledgerRepo);
    }

    @Test
    @DisplayName("Repeated application approval returns existing approved state idempotently without duplicating allocation")
    void testIdempotentApplicationApprovalReplay() {
        LandApplication approvedApp = LandApplication.builder()
                .id(50L)
                .companyId(1L)
                .parcelId("GCP-MH-2024-001")
                .status(ApplicationStatus.APPROVED)
                .decidedAt(LocalDateTime.now())
                .decidedBy("Dr. Arvind Sharma")
                .build();

        when(applicationRepo.findByIdForUpdate(50L)).thenReturn(Optional.of(approvedApp));

        DecisionRequest req = new DecisionRequest(true, "Dr. Arvind Sharma", "Approved");
        LandApplication result = applicationService.decide(50L, req);

        assertEquals(ApplicationStatus.APPROVED, result.getStatus());
        // Verify allocationRepo.save was NOT called again
        verify(allocationRepo, never()).save(any());
        verify(outboxService, never()).recordEvent(any(), any(), any(), any());
    }

    // =========================================================================
    // 2. State Transition Tests
    // =========================================================================
    @Test
    @DisplayName("Attempting to reject an already APPROVED application throws ConflictException")
    void testInvalidTransitionApprovedToRejected() {
        LandApplication approvedApp = LandApplication.builder()
                .id(60L)
                .companyId(1L)
                .parcelId("GCP-MH-2024-001")
                .status(ApplicationStatus.APPROVED)
                .build();

        when(applicationRepo.findByIdForUpdate(60L)).thenReturn(Optional.of(approvedApp));

        DecisionRequest req = new DecisionRequest(false, "Officer", "Attempt to reverse approval");
        ConflictException ex = assertThrows(ConflictException.class, () -> applicationService.decide(60L, req));
        assertTrue(ex.getMessage().contains("already been decided with status: APPROVED"));
    }

    @Test
    @DisplayName("Attempting to approve an already REJECTED redemption throws ConflictException")
    void testInvalidTransitionRejectedRedemptionToApproved() {
        Redemption rejectedRedemption = Redemption.builder()
                .id(70L)
                .companyId(1L)
                .status(RedemptionStatus.REJECTED)
                .build();

        when(redemptionRepo.findByIdForUpdate(70L)).thenReturn(Optional.of(rejectedRedemption));

        DecisionRequest req = new DecisionRequest(true, "Officer", "Approve rejected");
        ConflictException ex = assertThrows(ConflictException.class, () -> redemptionService.decide(70L, req));
        assertTrue(ex.getMessage().contains("already been decided with status: REJECTED"));
    }

    @Test
    @DisplayName("Rejecting an application without a reason throws BadRequestException")
    void testRejectApplicationWithoutReasonFails() {
        LandApplication app = LandApplication.builder()
                .id(80L)
                .status(ApplicationStatus.PENDING_REVIEW)
                .build();

        when(applicationRepo.findByIdForUpdate(80L)).thenReturn(Optional.of(app));

        DecisionRequest req = new DecisionRequest(false, "Officer", "   ");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> applicationService.decide(80L, req));
        assertTrue(ex.getMessage().contains("reason is required"));
    }
}
