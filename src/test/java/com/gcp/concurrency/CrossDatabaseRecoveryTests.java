package com.gcp.concurrency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcp.enums.AllocationStatus;
import com.gcp.enums.ApplicationStatus;
import com.gcp.enums.OutboxStatus;
import com.gcp.enums.ParcelStatus;
import com.gcp.model.LandApplication;
import com.gcp.model.LandParcel;
import com.gcp.model.OutboxEvent;
import com.gcp.model.ParcelAllocation;
import com.gcp.repository.LandApplicationRepository;
import com.gcp.repository.LandParcelRepository;
import com.gcp.repository.OutboxEventRepository;
import com.gcp.repository.ParcelAllocationRepository;
import com.gcp.service.OutboxService;
import com.gcp.service.ReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CrossDatabaseRecoveryTests {

    @Mock
    private OutboxEventRepository outboxRepo;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private ParcelAllocationRepository allocationRepo;

    @Mock
    private LandApplicationRepository applicationRepo;

    @Mock
    private LandParcelRepository mongoParcelRepo;

    private ObjectMapper objectMapper;
    private OutboxService outboxService;
    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outboxService = new OutboxService(outboxRepo, mongoTemplate, objectMapper);
        reconciliationService = new ReconciliationService(
                allocationRepo,
                applicationRepo,
                mongoParcelRepo,
                outboxRepo,
                outboxService
        );
    }

    @Test
    @DisplayName("Cross-DB: Mongo failure triggers retry with backoff, subsequent success marks event COMPLETED")
    void testPostgresAllocationSucceeds_MongoFails_OutboxRetriesAndSucceeds() {
        Long eventId = 100L;
        Map<String, Object> payload = Map.of("companyId", 1L, "allotmentDate", "2026-10-30");
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .eventId(UUID.randomUUID().toString())
                .eventType("PARCEL_ALLOCATED")
                .aggregateType("LandParcel")
                .aggregateId("PARCEL-TEST-001")
                .payload(jsonPayload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .maxRetries(5)
                .createdAt(LocalDateTime.now())
                .build();

        when(outboxRepo.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));

        // First attempt: MongoTemplate throws exception simulating network/timeout failure
        doThrow(new RuntimeException("MongoDB connection timed out"))
                .when(mongoTemplate).upsert(any(Query.class), any(Update.class), eq(LandParcel.class));

        outboxService.processSingleEvent(eventId);

        // Verification after first failure:
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(1, event.getRetryCount());
        assertNotNull(event.getNextRetryAt());
        assertTrue(event.getLastError().contains("MongoDB connection timed out"));
        verify(outboxRepo, atLeastOnce()).save(event);

        // Second attempt: MongoTemplate succeeds
        doReturn(null).when(mongoTemplate).upsert(any(Query.class), any(Update.class), eq(LandParcel.class));

        outboxService.processSingleEvent(eventId);

        // Verification after retry success:
        assertEquals(OutboxStatus.COMPLETED, event.getStatus());
        assertNotNull(event.getProcessedAt());
        assertNull(event.getLastError());
    }

    @Test
    @DisplayName("Cross-DB: Repeated failures exhaust retries and mark event FAILED; replay resets to PENDING")
    void testOutboxPermanentFailure_AndReplay() {
        Long eventId = 200L;
        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .eventId(UUID.randomUUID().toString())
                .eventType("PARCEL_ALLOCATED")
                .aggregateType("LandParcel")
                .aggregateId("PARCEL-TEST-002")
                .payload("{\"companyId\":2}")
                .status(OutboxStatus.PENDING)
                .retryCount(4)
                .maxRetries(5)
                .createdAt(LocalDateTime.now())
                .build();

        when(outboxRepo.findByIdForUpdate(eventId)).thenReturn(Optional.of(event));
        doThrow(new RuntimeException("Fatal Mongo write concern error"))
                .when(mongoTemplate).upsert(any(Query.class), any(Update.class), eq(LandParcel.class));

        outboxService.processSingleEvent(eventId);

        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(5, event.getRetryCount());

        // Admin replays failed event
        outboxService.replayFailedEvent(eventId);
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(0, event.getRetryCount());
        assertNull(event.getNextRetryAt());
    }

    @Test
    @DisplayName("Reconciliation: Automatically resets stuck PROCESSING outbox events after app crash")
    void testReconciliationHealsStuckProcessingEvents() {
        OutboxEvent stuckEvent = OutboxEvent.builder()
                .id(300L)
                .eventId(UUID.randomUUID().toString())
                .eventType("PARCEL_ALLOCATED")
                .aggregateId("P-STUCK")
                .status(OutboxStatus.PROCESSING)
                .createdAt(LocalDateTime.now().minusMinutes(5)) // stuck for 5 mins
                .build();

        when(outboxRepo.findByStatusIn(anyList())).thenReturn(List.of(stuckEvent));
        when(allocationRepo.findAll()).thenReturn(Collections.emptyList());
        when(applicationRepo.findByStatus(ApplicationStatus.APPROVED)).thenReturn(Collections.emptyList());

        ReconciliationService.ReconciliationSummary summary = reconciliationService.reconcile();

        assertEquals(1, summary.stuckOutboxEventsRecovered());
        assertEquals(OutboxStatus.PENDING, stuckEvent.getStatus());
        verify(outboxRepo).save(stuckEvent);
    }

    @Test
    @DisplayName("Reconciliation: Restores MongoDB parcel state when out of sync with authoritative PostgreSQL allocation")
    void testReconciliationHealsMongoDesyncFromAuthoritativePostgres() {
        String parcelId = "PARCEL-DESYNC-01";
        Long companyId = 15L;

        ParcelAllocation alloc = ParcelAllocation.builder()
                .id(1L)
                .parcelId(parcelId)
                .companyId(companyId)
                .status(AllocationStatus.ALLOCATED)
                .allotmentDate(LocalDate.now())
                .build();

        LandParcel staleMongoParcel = LandParcel.builder()
                .id(parcelId)
                .status(ParcelStatus.AVAILABLE)
                .allottedCompanyId(null)
                .build();

        when(outboxRepo.findByStatusIn(anyList())).thenReturn(Collections.emptyList());
        when(allocationRepo.findAll()).thenReturn(List.of(alloc));
        when(mongoParcelRepo.findById(parcelId)).thenReturn(Optional.of(staleMongoParcel));
        when(applicationRepo.findByStatus(ApplicationStatus.APPROVED)).thenReturn(Collections.emptyList());

        ReconciliationService.ReconciliationSummary summary = reconciliationService.reconcile();

        assertEquals(1, summary.mongoParcelsSynced());
        assertEquals(1, summary.inconsistenciesFound());
        assertEquals(ParcelStatus.ALLOTTED, staleMongoParcel.getStatus());
        assertEquals(companyId, staleMongoParcel.getAllottedCompanyId());
        verify(mongoParcelRepo).save(staleMongoParcel);
    }

    @Test
    @DisplayName("Reconciliation: Creates missing ParcelAllocation for approved applications")
    void testReconciliationHealsApprovedApplicationWithoutAllocation() {
        Long appId = 50L;
        String parcelId = "P-APP-50";
        Long companyId = 8L;

        LandApplication app = LandApplication.builder()
                .id(appId)
                .parcelId(parcelId)
                .companyId(companyId)
                .status(ApplicationStatus.APPROVED)
                .submittedAt(LocalDateTime.now().minusDays(1))
                .decidedAt(LocalDateTime.now())
                .build();

        when(outboxRepo.findByStatusIn(anyList())).thenReturn(Collections.emptyList());
        when(allocationRepo.findAll()).thenReturn(Collections.emptyList());
        when(applicationRepo.findByStatus(ApplicationStatus.APPROVED)).thenReturn(List.of(app));
        when(allocationRepo.findByApplicationId(appId)).thenReturn(Optional.empty());

        ReconciliationService.ReconciliationSummary summary = reconciliationService.reconcile();

        assertEquals(1, summary.inconsistenciesFound());
        verify(allocationRepo).save(argThat(alloc ->
                alloc.getApplicationId().equals(appId) &&
                alloc.getParcelId().equals(parcelId) &&
                alloc.getCompanyId().equals(companyId) &&
                alloc.getStatus() == AllocationStatus.ALLOCATED
        ));
    }
}
