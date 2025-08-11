package com.aspora.tms.controller;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.service.IdempotencyService;
import com.aspora.tms.service.impl.PaymentService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.aspora.tms.service.PerformanceMonitoringService;

/**
 * REST Controller for payment transactions.
 * Now supports async processing, batch operations, and circuit breaker patterns.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;
    private final PerformanceMonitoringService performanceMonitoringService;

    @PostMapping
    @CircuitBreaker(name = "transactionService")
    @RateLimiter(name = "transactionService")
    public ResponseEntity<TransactionResponseDTO> createTransaction(@RequestBody TransactionRequestDTO request) {
        log.info("Received transaction request: {}", request.getIdempotencyKey());
        
        try {
            // Validate idempotency key
            if (!idempotencyService.isValidIdempotencyKey(request.getIdempotencyKey())) {
                TransactionResponseDTO errorResponse = TransactionResponseDTO.builder()
                        .status("FAILED")
                        .message("Invalid idempotency key. Key must be 16-128 characters long.")
                        .createdAt(Instant.now())
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Check if transaction already processed
            if (idempotencyService.isTransactionProcessed(request.getIdempotencyKey())) {
                TransactionResponseDTO duplicateResponse = TransactionResponseDTO.builder()
                        .idempotencyKey(request.getIdempotencyKey())
                        .status("DUPLICATE")
                        .message("Transaction with this idempotency key has already been processed")
                        .createdAt(Instant.now())
                        .build();
                return ResponseEntity.ok(duplicateResponse);
            }
            
            TransactionResponseDTO response = paymentService.process(request);
            
            // Mark transaction as processed
            idempotencyService.markTransactionProcessed(request.getIdempotencyKey());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing transaction: {}", request.getIdempotencyKey(), e);
            return ResponseEntity.internalServerError()
                    .body(TransactionResponseDTO.builder()
                            .idempotencyKey(request.getIdempotencyKey())
                            .status("FAILED")
                            .message("Transaction processing failed: " + e.getMessage())
                            .createdAt(Instant.now())
                            .build());
        }
    }

    @PostMapping("/async")
    @CircuitBreaker(name = "transactionService")
    @RateLimiter(name = "transactionService")
    public CompletableFuture<ResponseEntity<TransactionResponseDTO>> createTransactionAsync(
            @RequestBody TransactionRequestDTO request) {
        log.info("Received async transaction request: {}", request.getIdempotencyKey());
        
        // Validate idempotency key
        if (!idempotencyService.isValidIdempotencyKey(request.getIdempotencyKey())) {
            TransactionResponseDTO errorResponse = TransactionResponseDTO.builder()
                    .status("FAILED")
                    .message("Invalid idempotency key. Key must be 16-128 characters long.")
                    .createdAt(Instant.now())
                    .build();
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(errorResponse));
        }
        
        return paymentService.processAsync(request)
                .thenApply(response -> ResponseEntity.ok(response))
                .exceptionally(throwable -> {
                    log.error("Error processing async transaction: {}", request.getIdempotencyKey(), throwable);
                    return ResponseEntity.internalServerError()
                            .body(TransactionResponseDTO.builder()
                                    .idempotencyKey(request.getIdempotencyKey())
                                    .status("FAILED")
                                    .message("Transaction processing failed: " + throwable.getMessage())
                                    .createdAt(Instant.now())
                                    .build());
                });
    }

    @PostMapping("/batch")
    @CircuitBreaker(name = "transactionService")
    @RateLimiter(name = "transactionService")
    public CompletableFuture<ResponseEntity<List<TransactionResponseDTO>>> createBatchTransactions(
            @RequestBody List<TransactionRequestDTO> requests) {
        log.info("Received batch transaction request with {} transactions", requests.size());
        
        if (requests.size() > 100) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().build()
            );
        }
        
        // Validate idempotency keys for all requests
        for (TransactionRequestDTO request : requests) {
            if (!idempotencyService.isValidIdempotencyKey(request.getIdempotencyKey())) {
                return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().build()
                );
            }
        }
        
        List<CompletableFuture<TransactionResponseDTO>> futures = requests.stream()
                .map(paymentService::processAsync)
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList())
                .thenApply(ResponseEntity::ok)
                .exceptionally(throwable -> {
                    log.error("Error processing batch transactions", throwable);
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/idempotency-key")
    public ResponseEntity<IdempotencyKeyResponse> generateIdempotencyKey(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String fromAccount,
            @RequestParam(required = false) String toAccount,
            @RequestParam(required = false) String paymentMethod) {
        
        String idempotencyKey;
        if (customerId != null && amount != null && fromAccount != null && toAccount != null && paymentMethod != null) {
            idempotencyKey = idempotencyService.generateIdempotencyKey(customerId, amount, fromAccount, toAccount, paymentMethod);
        } else {
            idempotencyKey = idempotencyService.generateUniqueIdempotencyKey();
        }
        
        IdempotencyKeyResponse response = new IdempotencyKeyResponse(idempotencyKey, Instant.now());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Transaction Service is healthy");
    }

    @GetMapping("/performance")
    public ResponseEntity<PerformanceMonitoringService.PerformanceSummary> getPerformanceMetrics() {
        return ResponseEntity.ok(performanceMonitoringService.getPerformanceSummary());
    }

    @GetMapping("/concurrency")
    public ResponseEntity<ConcurrencyStatus> getConcurrencyStatus() {
        int currentConcurrency = performanceMonitoringService.getCurrentConcurrency("processAccountTransfer");
        int maxConcurrency = performanceMonitoringService.getMaxConcurrentTransactions();
        
        ConcurrencyStatus status = new ConcurrencyStatus(currentConcurrency, maxConcurrency);
        return ResponseEntity.ok(status);
    }

    // Inner class for idempotency key response
    public static class IdempotencyKeyResponse {
        private String idempotencyKey;
        private Instant generatedAt;

        public IdempotencyKeyResponse(String idempotencyKey, Instant generatedAt) {
            this.idempotencyKey = idempotencyKey;
            this.generatedAt = generatedAt;
        }

        // Getters
        public String getIdempotencyKey() { return idempotencyKey; }
        public Instant getGeneratedAt() { return generatedAt; }
    }

    // Inner class for concurrency status
    public static class ConcurrencyStatus {
        private int currentConcurrency;
        private int maxConcurrency;
        private Instant timestamp;

        public ConcurrencyStatus(int currentConcurrency, int maxConcurrency) {
            this.currentConcurrency = currentConcurrency;
            this.maxConcurrency = maxConcurrency;
            this.timestamp = Instant.now();
        }

        // Getters
        public int getCurrentConcurrency() { return currentConcurrency; }
        public int getMaxConcurrency() { return maxConcurrency; }
        public Instant getTimestamp() { return timestamp; }
    }
}