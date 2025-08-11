package com.aspora.tms.dto;

import com.aspora.tms.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;


/**
 * Request DTO for initiating a payment transaction.
 * Supports multiple payment methods (ACCOUNT, UPI, NETBANKING, etc.).
 */
@Data
public class TransactionRequestDTO {
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
}