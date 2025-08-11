package com.aspora.tms.dto;

import com.aspora.tms.enums.PaymentMethod;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for initiating a payment transaction.
 * Supports multiple payment methods (ACCOUNT, UPI, NETBANKING, etc.).
 * Now includes idempotency key for concurrent request handling.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequestDTO {
    // Idempotency key for preventing duplicate transactions
    private String idempotencyKey;
    
    // For ACCOUNT → ACCOUNT transfer
    private Long fromAccountId;
    private Long toAccountId;

    // Common fields
    private BigDecimal amount;

    // New: identifies which strategy to use
    private PaymentMethod paymentMethod; // instead of String

    // Optional: fields for UPI
    private String upiId;

    // Optional: fields for NetBanking
    private String bankCode;
    private String referenceNumber;
    
    // Additional fields for better transaction tracking
    private String description;
    private String currency;
    private String customerId;
}