package com.aspora.tms.service.impl;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.service.PaymentStrategy;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main entry point for processing payments.
 * Uses PaymentStrategy to decide how to process a request.
 * Now supports async processing, caching, and circuit breaker patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final List<PaymentStrategy> strategies;
    private final TransactionAuditService auditService;
    private final NotificationService notificationService;

    @CircuitBreaker(name = "transactionService", fallbackMethod = "fallbackProcess")
    @RateLimiter(name = "transactionService")
    @Cacheable(value = "transactions", key = "#request.idempotencyKey", unless = "#result == null")
    public TransactionResponseDTO process(TransactionRequestDTO request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing transaction request: {}", request.getIdempotencyKey());
        
        // Validate idempotency key
        if (request.getIdempotencyKey() == null || request.getIdempotencyKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        
        TransactionResponseDTO response = strategies.stream()
                .filter(s -> s.supports(String.valueOf(request.getPaymentMethod())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported payment method: " + request.getPaymentMethod()
                ))
                .processPayment(request);
        
        // Update response with additional information
        TransactionResponseDTO enhancedResponse = TransactionResponseDTO.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .transactionId(response.getTransactionId())
                .externalReference(response.getExternalReference())
                .status(response.getStatus())
                .message(response.getMessage())
                .amount(response.getAmount())
                .createdAt(Instant.now())
                .currency(request.getCurrency())
                .customerId(request.getCustomerId())
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
        
        // Async processing for non-critical operations
        CompletableFuture.runAsync(() -> auditService.auditTransaction(request, enhancedResponse));
        CompletableFuture.runAsync(() -> notificationService.sendNotification(request, enhancedResponse));
        
        return enhancedResponse;
    }

    @Async("transactionTaskExecutor")
    public CompletableFuture<TransactionResponseDTO> processAsync(TransactionRequestDTO request) {
        log.info("Processing transaction asynchronously: {}", request.getIdempotencyKey());
        
        TransactionResponseDTO response = process(request);
        return CompletableFuture.completedFuture(response);
    }

    @Async("transactionTaskExecutor")
    public CompletableFuture<TransactionResponseDTO> processBatchAsync(List<TransactionRequestDTO> requests) {
        log.info("Processing batch of {} transactions asynchronously", requests.size());
        
        List<CompletableFuture<TransactionResponseDTO>> futures = requests.stream()
                .map(this::processAsync)
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.get(0).join()); // Return first result for now
    }

    // Fallback method for circuit breaker
    public TransactionResponseDTO fallbackProcess(TransactionRequestDTO request, Exception e) {
        log.error("Circuit breaker triggered for transaction: {}", request.getIdempotencyKey(), e);
        
        // Return a fallback response
        return TransactionResponseDTO.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .status("FAILED")
                .message("Service temporarily unavailable. Please try again later.")
                .amount(request.getAmount())
                .createdAt(Instant.now())
                .currency(request.getCurrency())
                .customerId(request.getCustomerId())
                .processingTimeMs(0L)
                .build();
    }
}