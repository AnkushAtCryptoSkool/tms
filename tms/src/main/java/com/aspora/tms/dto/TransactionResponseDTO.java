package com.aspora.tms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO after processing a transaction.
 * Now includes idempotency key and message for better response handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {
    // Idempotency key for tracking the original request
    private String idempotencyKey;
    
    private Long transactionId; // For ACCOUNT method
    private String externalReference; // For UPI, NetBanking confirmations
    private String status;
    private String message; // Additional response message
    private BigDecimal amount;
    private Instant createdAt;
    
    // Additional fields for better response tracking
    private String currency;
    private String customerId;
    private Long processingTimeMs; // Processing time in milliseconds
}