package com.aspora.tms.service.impl;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for auditing transaction operations asynchronously.
 * This service runs in the background to avoid blocking the main transaction flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionAuditService {

    @Async("auditTaskExecutor")
    public void auditTransaction(TransactionRequestDTO request, TransactionResponseDTO response) {
        try {
            log.info("Auditing transaction: {} -> Status: {}", 
                request.getIdempotencyKey(), response.getStatus());
            
            // Here I would typically:
            // 1. Log to audit database
            // 2. Send to audit log system
            // 3. Store in audit trail
            // 4. Generate audit reports
            
            // Simulate some audit processing time
            Thread.sleep(100);
            
            log.info("Audit completed for transaction: {}", request.getIdempotencyKey());
            
        } catch (Exception e) {
            log.error("Error during transaction audit for: {}", request.getIdempotencyKey(), e);
            // Don't throw exception as this is async and shouldn't affect main flow
        }
    }

    @Async("auditTaskExecutor")
    public void auditBatchTransactions(java.util.List<TransactionRequestDTO> requests, 
                                     java.util.List<TransactionResponseDTO> responses) {
        log.info("Auditing batch of {} transactions", requests.size());
        
        for (int i = 0; i < requests.size(); i++) {
            auditTransaction(requests.get(i), responses.get(i));
        }
    }
}
