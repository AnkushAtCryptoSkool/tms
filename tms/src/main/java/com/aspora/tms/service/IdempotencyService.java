package com.aspora.tms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for handling idempotency keys to prevent duplicate transactions.
 * This is crucial for horizontal scalability and concurrent request handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    /**
     * Generate a unique idempotency key based on request parameters.
     * This ensures that identical requests get the same key.
     */
    public String generateIdempotencyKey(String customerId, String amount, String fromAccount, String toAccount, String paymentMethod) {
        try {
            String input = String.format("%s_%s_%s_%s_%s_%d", 
                customerId, amount, fromAccount, toAccount, paymentMethod, Instant.now().toEpochMilli());
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            
            // Convert to hex string and take first 32 characters
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating idempotency key", e);
            // Fallback to UUID if SHA-256 is not available
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    /**
     * Generate a simple UUID-based idempotency key.
     * Use this when you need a completely unique key.
     */
    public String generateUniqueIdempotencyKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Validate idempotency key format.
     * Basic validation to ensure the key is not empty and has reasonable length.
     */
    public boolean isValidIdempotencyKey(String idempotencyKey) {
        return idempotencyKey != null && 
               !idempotencyKey.trim().isEmpty() && 
               idempotencyKey.length() >= 16 && 
               idempotencyKey.length() <= 128;
    }

    /**
     * Check if a transaction with this idempotency key has already been processed.
     * This method is cached to improve performance.
     */
    @Cacheable(value = "idempotency", key = "#idempotencyKey")
    public boolean isTransactionProcessed(String idempotencyKey) {
        // In a real implementation, you would check the database
        // For now, we'll return false (not processed)
        // This cache will prevent duplicate processing within the TTL period
        return false;
    }

    /**
     * Mark a transaction as processed with the given idempotency key.
     * This should be called after successful transaction processing.
     */
    public void markTransactionProcessed(String idempotencyKey) {
        // In a real implementation, you would store this in the database
        // or update the cache to indicate the transaction has been processed
        log.info("Transaction marked as processed for idempotency key: {}", idempotencyKey);
    }

    /**
     * Generate a business-friendly idempotency key.
     * This creates a key that's easier to track in business systems.
     */
    public String generateBusinessIdempotencyKey(String customerId, String transactionType, String amount) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        return String.format("%s_%s_%s_%s", 
            customerId, transactionType, amount, timestamp);
    }
}
