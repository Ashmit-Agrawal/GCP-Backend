package com.gcp.concurrency;

import com.gcp.dto.CreditAccount;
import com.gcp.dto.RedemptionOptionInfo;
import com.gcp.dto.RedemptionRequest;
import com.gcp.enums.CreditMovementType;
import com.gcp.enums.RedemptionOption;
import com.gcp.enums.RedemptionStatus;
import com.gcp.exception.BadRequestException;
import com.gcp.model.Company;
import com.gcp.model.CreditLedgerMovement;
import com.gcp.model.Redemption;
import com.gcp.repository.CompanyRepository;
import com.gcp.repository.CreditLedgerMovementRepository;
import com.gcp.repository.RedemptionRepository;
import com.gcp.service.AuditService;
import com.gcp.service.CompanyService;
import com.gcp.service.CreditService;
import com.gcp.service.RedemptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditRedemptionConcurrencyTests {

    @Mock
    private RedemptionRepository redemptionRepo;

    @Mock
    private CreditService creditService;

    @Mock
    private CompanyService companyService;

    @Mock
    private CompanyRepository companyRepo;

    @Mock
    private CreditLedgerMovementRepository ledgerRepo;

    @Mock
    private AuditService auditService;

    private RedemptionService redemptionService;

    private final Long testCompanyId = 1L;

    @BeforeEach
    void setUp() {
        redemptionService = new RedemptionService(
                redemptionRepo,
                creditService,
                companyService,
                companyRepo,
                ledgerRepo,
                auditService
        );
    }

    @Test
    @DisplayName("Concurrent redemptions exceeding balance (70 + 60 on 100 balance): exactly one succeeds, preventing double-spending")
    void testConcurrentRedemptionDoubleSpendingPrevention() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        Company company = new Company();
        company.setId(testCompanyId);
        company.setName("Tata Power Ltd.");

        // Simulate database-level row lock (PESSIMISTIC_WRITE) using ReentrantLock
        ReentrantLock dbRowLock = new ReentrantLock();

        // Thread-safe list of active redemptions in DB
        List<Redemption> dbRedemptions = new CopyOnWriteArrayList<>();

        when(companyRepo.findByIdForUpdate(testCompanyId)).thenAnswer(invocation -> {
            dbRowLock.lock();
            return Optional.of(company);
        });

        // Company has total 100 earned credits, 0 redeemed
        when(creditService.account(testCompanyId)).thenReturn(new CreditAccount(testCompanyId, 100.0, 0.0, 100.0, 0.0));

        when(redemptionRepo.findByCompanyId(testCompanyId)).thenReturn(dbRedemptions);

        when(redemptionRepo.save(any(Redemption.class))).thenAnswer(invocation -> {
            Redemption r = invocation.getArgument(0);
            r.setId((long) (dbRedemptions.size() + 1));
            dbRedemptions.add(r);
            return r;
        });

        when(ledgerRepo.save(any(CreditLedgerMovement.class))).thenAnswer(i -> i.getArgument(0));

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        // Request A: 100 GC cost option (CARBON_OFFSET_CERTIFICATE = 100 GC)
        // Request B: 150 GC cost option (ESG_REPORTING_CLAIM = 150 GC)
        // Or using two 100 GC requests against 100 total balance
        RedemptionRequest reqA = new RedemptionRequest(testCompanyId, RedemptionOption.CARBON_OFFSET_CERTIFICATE);
        RedemptionRequest reqB = new RedemptionRequest(testCompanyId, RedemptionOption.CARBON_OFFSET_CERTIFICATE);

        executor.submit(() -> {
            try {
                startLatch.await();
                redemptionService.request(reqA);
                successCount.incrementAndGet();
            } catch (BadRequestException e) {
                rejectedCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                if (dbRowLock.isHeldByCurrentThread()) {
                    dbRowLock.unlock();
                }
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                redemptionService.request(reqB);
                successCount.incrementAndGet();
            } catch (BadRequestException e) {
                rejectedCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                if (dbRowLock.isHeldByCurrentThread()) {
                    dbRowLock.unlock();
                }
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Concurrent redemption test timed out");
        executor.shutdown();

        assertEquals(0, errors.size(), "Unexpected errors during concurrent redemption: " + errors);
        assertEquals(1, successCount.get(), "Exactly one redemption request must succeed");
        assertEquals(1, rejectedCount.get(), "The competing redemption request must be rejected for insufficient credits");

        // Verify that total requested credits in DB do not exceed 100
        double totalRequested = dbRedemptions.stream()
                .filter(r -> r.getStatus() == RedemptionStatus.REQUESTED)
                .mapToDouble(Redemption::getCredits)
                .sum();
        assertEquals(100.0, totalRequested, "Total requested credits must not exceed available balance of 100.0");

        // Verify that exactly 1 hold movement was written to ledger
        verify(ledgerRepo, times(1)).save(argThat(m -> m.getType() == CreditMovementType.REDEMPTION_HOLD));
    }
}
