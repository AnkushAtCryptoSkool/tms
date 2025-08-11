package com.aspora.tms.service.impl;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import com.aspora.tms.service.PaymentStrategy;
import com.aspora.tms.service.ConcurrentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Strategy for processing ACCOUNT to ACCOUNT transfers.
 * Now uses ConcurrentTransactionService instead of synchronized methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountTransferStrategy implements PaymentStrategy {

    private final ConcurrentTransactionService concurrentTransactionService;

    @Override
    public boolean supports(String paymentMethod) {
        return "ACCOUNT".equalsIgnoreCase(paymentMethod);
    }

    @Override
    public TransactionResponseDTO processPayment(TransactionRequestDTO request) {
        log.info("Processing ACCOUNT transfer for request: {}", request.getIdempotencyKey());
        
        // Use the concurrent transaction service instead of synchronized methods
        return concurrentTransactionService.processAccountTransfer(request);
    }
}