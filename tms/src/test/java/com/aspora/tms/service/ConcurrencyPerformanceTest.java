package com.aspora.tms.service;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.entity.Account;
import com.aspora.tms.enums.PaymentMethod;
import com.aspora.tms.repository.AccountRepository;
import com.aspora.tms.repository.TransactionRepository;
import com.aspora.tms.service.ConcurrentTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to demonstrate performance improvements from removing synchronized methods.
 * This test shows how the system can handle concurrent transactions efficiently.
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyPerformanceTest {

    @Autowired
    private ConcurrentTransactionService concurrentTransactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PerformanceMonitoringService performanceMonitoringService;

    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        // Reset performance counters
        performanceMonitoringService.resetCounters();

        // Create test accounts
        sourceAccount = new Account("TestUser1", new BigDecimal("10000.00"));
        targetAccount = new Account("TestUser2", new BigDecimal("1000.00"));
        
        sourceAccount = accountRepository.save(sourceAccount);
        targetAccount = accountRepository.save(targetAccount);
    }

    @Test
    void testConcurrentTransactionsPerformance() throws InterruptedException {
        int numberOfTransactions = 100;
        int threadPoolSize = 20;
        
        System.out.println("=== Testing Concurrent Transactions Performance ===");
        System.out.println("Number of transactions: " + numberOfTransactions);
        System.out.println("Thread pool size: " + threadPoolSize);
        
        long startTime = System.currentTimeMillis();
        
        // Create concurrent transaction requests
        List<CompletableFuture<TransactionResponseDTO>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        
        for (int i = 0; i < numberOfTransactions; i++) {
            TransactionRequestDTO request = createTransactionRequest(i);
            
            CompletableFuture<TransactionResponseDTO> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return concurrentTransactionService.processAccountTransfer(request);
                } catch (Exception e) {
                    throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all transactions to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Verify all transactions completed successfully
        List<TransactionResponseDTO> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();
        
        assertEquals(numberOfTransactions, results.size());
        
        // Check that all transactions have unique IDs
        long uniqueIds = results.stream()
            .map(TransactionResponseDTO::getTransactionId)
            .distinct()
            .count();
        assertEquals(numberOfTransactions, uniqueIds);
        
        // Performance metrics
        double transactionsPerSecond = (double) numberOfTransactions / (totalTime / 1000.0);
        double averageProcessingTime = performanceMonitoringService.getAverageProcessingTime();
        int maxConcurrent = performanceMonitoringService.getMaxConcurrentTransactions();
        
        System.out.println("\n=== Performance Results ===");
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Transactions per second: " + String.format("%.2f", transactionsPerSecond));
        System.out.println("Average processing time: " + String.format("%.2f", averageProcessingTime) + "ms");
        System.out.println("Maximum concurrent transactions: " + maxConcurrent);
        System.out.println("Current concurrency: " + performanceMonitoringService.getCurrentConcurrency("processAccountTransfer"));
        
        // Performance assertions
        assertTrue(transactionsPerSecond > 10, "Should process at least 10 transactions per second");
        assertTrue(averageProcessingTime < 1000, "Average processing time should be less than 1 second");
        assertTrue(maxConcurrent > 1, "Should handle multiple concurrent transactions");
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    void testOptimisticLockingRetries() throws InterruptedException {
        System.out.println("\n=== Testing Optimistic Locking Retries ===");
        
        // Create multiple transactions that will likely cause optimistic lock conflicts
        List<CompletableFuture<TransactionResponseDTO>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Create transactions with the same accounts to trigger optimistic locking
        for (int i = 0; i < 20; i++) {
            TransactionRequestDTO request = createTransactionRequest(i);
            
            CompletableFuture<TransactionResponseDTO> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return concurrentTransactionService.processAccountTransfer(request);
                } catch (Exception e) {
                    throw new RuntimeException("Transaction failed: " + e.getMessage(), e);
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for completion
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Verify all completed successfully despite retries
        List<TransactionResponseDTO> results = futures.stream()
            .map(CompletableFuture::join)
            .toList();
        
        assertEquals(20, results.size());
        
        System.out.println("All transactions completed successfully with optimistic locking retries");
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    void testIdempotencyKeyHandling() {
        System.out.println("\n=== Testing Idempotency Key Handling ===");
        
        TransactionRequestDTO request = createTransactionRequest(1);
        String originalIdempotencyKey = request.getIdempotencyKey();
        
        // Process the same request multiple times
        TransactionResponseDTO firstResponse = concurrentTransactionService.processAccountTransfer(request);
        TransactionResponseDTO secondResponse = concurrentTransactionService.processAccountTransfer(request);
        
        // Both should return the same result due to idempotency
        assertEquals(firstResponse.getTransactionId(), secondResponse.getTransactionId());
        assertEquals(firstResponse.getIdempotencyKey(), secondResponse.getIdempotencyKey());
        assertEquals(originalIdempotencyKey, firstResponse.getIdempotencyKey());
        
        System.out.println("Idempotency key handling works correctly");
    }

    private TransactionRequestDTO createTransactionRequest(int index) {
        return TransactionRequestDTO.builder()
            .idempotencyKey("test_key_" + System.currentTimeMillis() + "_" + index)
            .fromAccountId(sourceAccount.getId())
            .toAccountId(targetAccount.getId())
            .amount(new BigDecimal("10.00"))
            .paymentMethod(PaymentMethod.ACCOUNT)
            .currency("USD")
            .customerId("customer_" + index)
            .description("Test transaction " + index)
            .build();
    }
}
