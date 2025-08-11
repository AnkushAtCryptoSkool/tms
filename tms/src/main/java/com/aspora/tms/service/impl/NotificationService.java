package com.aspora.tms.service.impl;

import com.aspora.tms.dto.TransactionRequestDTO;
import com.aspora.tms.dto.TransactionResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications asynchronously.
 * This service runs in the background to avoid blocking the main transaction flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    @Async("notificationTaskExecutor")
    public void sendNotification(TransactionRequestDTO request, TransactionResponseDTO response) {
        try {
            log.info("Sending notification for transaction: {} -> Status: {}", 
                request.getIdempotencyKey(), response.getStatus());
            
            // Here you would typically:
            // 1. Send email notifications
            // 2. Send SMS notifications
            // 3. Send push notifications
            // 4. Update webhook endpoints
            // 5. Send to message queues
            
            // Simulate some notification processing time
            Thread.sleep(200);
            
            log.info("Notification sent for transaction: {}", request.getIdempotencyKey());
            
        } catch (Exception e) {
            log.error("Error sending notification for transaction: {}", request.getIdempotencyKey(), e);
            // Don't throw exception as this is async and shouldn't affect main flow
        }
    }

    @Async("notificationTaskExecutor")
    public void sendBatchNotifications(java.util.List<TransactionRequestDTO> requests, 
                                     java.util.List<TransactionResponseDTO> responses) {
        log.info("Sending notifications for batch of {} transactions", requests.size());
        
        for (int i = 0; i < requests.size(); i++) {
            sendNotification(requests.get(i), responses.get(i));
        }
    }
}
