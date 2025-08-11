package com.aspora.tms.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO after processing a transaction.
 */
@Data
@NoArgsConstructor
public class TransactionResponseDTO {
    private Long transactionId; // For ACCOUNT method
    private String externalReference; // For UPI, NetBanking confirmations
    private String status;
    private BigDecimal amount;
    private Instant createdAt;
}