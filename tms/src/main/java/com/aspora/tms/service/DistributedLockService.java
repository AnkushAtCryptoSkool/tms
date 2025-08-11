package com.aspora.tms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for distributed locking using Redis.
 * This provides better concurrency handling than synchronized methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String LOCK_PREFIX = "lock:";
    private static final String LOCK_VALUE = "LOCKED";
    private static final long DEFAULT_TIMEOUT = 30; // 30 seconds

    /**
     * Acquire a distributed lock with default timeout.
     * @param lockKey The key to lock on
     * @return true if lock acquired, false otherwise
     */
    public boolean acquireLock(String lockKey) {
        return acquireLock(lockKey, DEFAULT_TIMEOUT);
    }

    /**
     * Acquire a distributed lock with specified timeout.
     * @param lockKey The key to lock on
     * @param timeoutSeconds Timeout in seconds
     * @return true if lock acquired, false otherwise
     */
    public boolean acquireLock(String lockKey, long timeoutSeconds) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        try {
            Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(fullLockKey, LOCK_VALUE, Duration.ofSeconds(timeoutSeconds));
            
            boolean acquired = Boolean.TRUE.equals(result);
            if (acquired) {
                log.debug("Lock acquired for key: {}", lockKey);
            } else {
                log.debug("Failed to acquire lock for key: {}", lockKey);
            }
            return acquired;
        } catch (Exception e) {
            log.error("Error acquiring lock for key: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Release a distributed lock.
     * @param lockKey The key to unlock
     */
    public void releaseLock(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        try {
            Boolean result = redisTemplate.delete(fullLockKey);
            if (Boolean.TRUE.equals(result)) {
                log.debug("Lock released for key: {}", lockKey);
            } else {
                log.debug("Lock was already released for key: {}", lockKey);
            }
        } catch (Exception e) {
            log.error("Error releasing lock for key: {}", lockKey, e);
        }
    }

    /**
     * Try to acquire a lock with retry mechanism.
     * @param lockKey The key to lock on
     * @param maxRetries Maximum number of retries
     * @param retryDelayMs Delay between retries in milliseconds
     * @return true if lock acquired, false otherwise
     */
    public boolean acquireLockWithRetry(String lockKey, int maxRetries, long retryDelayMs) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (acquireLock(lockKey)) {
                return true;
            }
            
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Check if a lock exists.
     * @param lockKey The key to check
     * @return true if lock exists, false otherwise
     */
    public boolean isLocked(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        try {
            String value = redisTemplate.opsForValue().get(fullLockKey);
            return LOCK_VALUE.equals(value);
        } catch (Exception e) {
            log.error("Error checking lock for key: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Get remaining TTL for a lock.
     * @param lockKey The key to check
     * @return TTL in seconds, -1 if key doesn't exist, -2 if key has no TTL
     */
    public long getLockTTL(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        try {
            Long ttl = redisTemplate.getExpire(fullLockKey, TimeUnit.SECONDS);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            log.error("Error getting TTL for lock key: {}", lockKey, e);
            return -1;
        }
    }
}
