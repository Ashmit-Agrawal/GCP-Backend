package com.gcp.concurrency;

import com.gcp.dto.DecisionRequest;
import com.gcp.enums.AllocationStatus;
import com.gcp.enums.ApplicationStatus;
import com.gcp.enums.ParcelStatus;
import com.gcp.exception.ConflictException;
import com.gcp.model.Company;
import com.gcp.model.LandApplication;
import com.gcp.model.LandParcel;
import com.gcp.model.ParcelAllocation;
import com.gcp.repository.LandApplicationRepository;
import com.gcp.repository.LandParcelRepository;
import com.gcp.repository.ParcelAllocationRepository;
import com.gcp.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParcelAllocationConcurrencyTests {

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

    private ApplicationService applicationService;

    private final String testParcelId = "GCP-MH-2024-CONC-001";

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
    }

    @Test
    @DisplayName("Simultaneous allocation requests for the same parcel: exactly one succeeds, other receives 409 Conflict")
    void testConcurrentParcelAllocationRaceCondition() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // App 1 for Company 1, App 2 for Company 2
        LandApplication app1 = LandApplication.builder()
                .id(101L)
                .companyId(1L)
                .parcelId(testParcelId)
                .status(ApplicationStatus.PENDING_REVIEW)
                .submittedAt(LocalDateTime.now())
                .build();

        LandApplication app2 = LandApplication.builder()
                .id(102L)
                .companyId(2L)
                .parcelId(testParcelId)
                .status(ApplicationStatus.PENDING_REVIEW)
                .submittedAt(LocalDateTime.now())
                .build();

        when(applicationRepo.findByIdForUpdate(101L)).thenReturn(Optional.of(app1));
        when(applicationRepo.findByIdForUpdate(102L)).thenReturn(Optional.of(app2));
        when(applicationRepo.save(any(LandApplication.class))).thenAnswer(i -> i.getArgument(0));

        // Atomic simulate: Only the first findAndModify call returns the modified parcel. The second returns null.
        AtomicInteger mongoCallCount = new AtomicInteger(0);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(LandParcel.class)))
                .thenAnswer(invocation -> {
                    int count = mongoCallCount.incrementAndGet();
                    if (count == 1) {
                        return LandParcel.builder()
                                .id(testParcelId)
                                .status(ParcelStatus.ALLOTTED)
                                .allottedCompanyId(1L)
                                .allotmentDate(LocalDate.now())
                                .build();
                    } else {
                        // Parcel no longer AVAILABLE
                        return null;
                    }
                });

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        DecisionRequest req = new DecisionRequest(true, "Dr. Arvind Sharma", "Approved restoration proposal");

        executor.submit(() -> {
            try {
                startLatch.await();
                applicationService.decide(101L, req);
                successCount.incrementAndGet();
            } catch (ConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                applicationService.decide(102L, req);
                successCount.incrementAndGet();
            } catch (ConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                doneLatch.countDown();
            }
        });

        // Trigger simultaneous start
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent test execution timed out");
        executor.shutdown();

        assertEquals(0, errors.size(), "Unexpected errors encountered: " + errors);
        assertEquals(1, successCount.get(), "Exactly one allocation request must succeed");
        assertEquals(1, conflictCount.get(), "The competing request must receive HTTP 409 ConflictException");

        // Verify that ParcelAllocation was persisted exactly once in PostgreSQL
        verify(allocationRepo, times(1)).save(any(ParcelAllocation.class));
        // Verify that outbox event was recorded exactly once
        verify(outboxService, times(1)).recordEvent(eq("PARCEL_ALLOCATED"), eq("LandParcel"), eq(testParcelId), any());
    }

    @Test
    @DisplayName("Pre-existing active allocation in PostgreSQL prevents any new allocation")
    void testPreExistingAllocationRejection() {
        LandApplication app = LandApplication.builder()
                .id(105L)
                .companyId(1L)
                .parcelId(testParcelId)
                .status(ApplicationStatus.PENDING_REVIEW)
                .submittedAt(LocalDateTime.now())
                .build();

        when(applicationRepo.findByIdForUpdate(105L)).thenReturn(Optional.of(app));
        when(allocationRepo.existsByParcelIdAndStatus(testParcelId, AllocationStatus.ALLOCATED)).thenReturn(true);

        DecisionRequest req = new DecisionRequest(true, "Officer", "Approve");

        ConflictException ex = assertThrows(ConflictException.class, () -> applicationService.decide(105L, req));
        assertTrue(ex.getMessage().contains("already allocated"), "Expected conflict message for already allocated parcel");

        // Ensure MongoDB is never called if PostgreSQL active allocation already exists
        verifyNoInteractions(mongoTemplate);
    }
}
