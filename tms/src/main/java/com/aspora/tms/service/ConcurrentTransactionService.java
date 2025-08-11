package com.aspora.tms.service;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.entity.Account;
import com.aspora.tms.entity.Transaction;
import com.aspora.tms.enums.TransactionStatus;
import com.aspora.tms.enums.TransactionType;
import com.aspora.tms.exception.ResourceNotFoundException;
import com.aspora.tms.exception.InsufficientBalanceException;
import com.aspora.tms.mappers.TransactionMapper;
import com.aspora.tms.repository.AccountRepository;
import com.aspora.tms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for handling concurrent transactions without synchronized methods.
 * Uses optimistic locking, distributed locks, and retry mechanisms.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConcurrentTransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final DistributedLockService distributedLockService;
    private final IdempotencyService idempotencyService;
    private final PerformanceMonitoringService performanceMonitoringService;

    private static final int MAX_RETRIES = 3;
    private static final long LOCK_TIMEOUT = 10; // 10 seconds
    private static final AtomicInteger concurrentTransactions = new AtomicInteger(0);

    /**
     * Process account transfer with concurrent safety.
     * No synchronized methods - uses optimistic locking and distributed locks.
     */
    @Transactional
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = MAX_RETRIES,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public TransactionResponseDTO processAccountTransfer(TransactionRequestDTO request) {
        long startTime = System.currentTimeMillis();
        String methodName = "processAccountTransfer";
        
        performanceMonitoringService.recordTransactionStart(methodName);
        int currentConcurrent = concurrentTransactions.incrementAndGet();
        
        log.info("Processing account transfer. Concurrent transactions: {}", currentConcurrent);
        
        try {
            // Use distributed lock for account-level concurrency control
            String lockKey = String.format("account_transfer:%d_%d", 
                request.getFromAccountId(), request.getToAccountId());
            
            if (!distributedLockService.acquireLock(lockKey, LOCK_TIMEOUT)) {
                throw new RuntimeException("Could not acquire lock for account transfer");
            }
            
            try {
                return executeTransfer(request);
            } finally {
                distributedLockService.releaseLock(lockKey);
            }
        } catch (OptimisticLockingFailureException e) {
            long retryTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordOptimisticLockRetry(methodName, retryTime);
            log.warn("Optimistic lock failure, will retry. Request: {}", request.getIdempotencyKey());
            throw e; // Re-throw to trigger retry
        } catch (Exception e) {
            log.error("Error processing account transfer for request: {}", request.getIdempotencyKey(), e);
            throw e;
        } finally {
            concurrentTransactions.decrementAndGet();
            long processingTime = System.currentTimeMillis() - startTime;
            performanceMonitoringService.recordTransactionComplete(methodName, processingTime);
        }
    }

    /**
     * Execute the actual transfer logic with optimistic locking.
     */
    @Transactional
    protected TransactionResponseDTO executeTransfer(TransactionRequestDTO request) {
        // Load accounts with optimistic locking
        Account fromAccount = loadAccountWithOptimisticLock(request.getFromAccountId());
        Account toAccount = loadAccountWithOptimisticLock(request.getToAccountId());

        // Validate balance
        if (!fromAccount.hasSufficientBalance(request.getAmount())) {
            throw new InsufficientBalanceException(
                "Insufficient balance. Required: " + request.getAmount() + 
                ", Available: " + fromAccount.getBalance());
        }

        // Execute atomic operations
        fromAccount.debit(request.getAmount());
        toAccount.credit(request.getAmount());

        // Save accounts (optimistic locking will handle concurrent updates)
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        // Create and save transaction
        Transaction transaction = createTransaction(request, fromAccount, toAccount);
        transactionRepository.save(transaction);

        log.info("Transfer completed successfully. Transaction ID: {}", transaction.getId());
        return transactionMapper.toDTO(transaction);
    }

    /**
     * Load account with optimistic locking support.
     */
    private Account loadAccountWithOptimisticLock(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
    }

    /**
     * Create transaction entity.
     */
    private Transaction createTransaction(TransactionRequestDTO request, Account fromAccount, Account toAccount) {
        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setIdempotencyKey(request.getIdempotencyKey());
        transaction.setCreatedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());
        return transaction;
    }

    /**
     * Process multiple transfers concurrently.
     */
    public CompletableFuture<TransactionResponseDTO> processTransferAsync(TransactionRequestDTO request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return processAccountTransfer(request);
            } catch (Exception e) {
                log.error("Error processing async transfer for request: {}", request.getIdempotencyKey(), e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Get current concurrent transaction count.
     */
    public int getCurrentConcurrentTransactions() {
        return concurrentTransactions.get();
    }

    /**
     * Process batch transfers with concurrency control.
     */
    public CompletableFuture<java.util.List<TransactionResponseDTO>> processBatchTransfers(
            java.util.List<TransactionRequestDTO> requests) {
        
        java.util.List<CompletableFuture<TransactionResponseDTO>> futures = requests.stream()
            .map(this::processTransferAsync)
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    /**
     * Get performance metrics for this service.
     */
    public PerformanceMonitoringService.PerformanceSummary getPerformanceMetrics() {
        return performanceMonitoringService.getPerformanceSummary();
    }
}
