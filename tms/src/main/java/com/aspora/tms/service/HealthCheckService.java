package com.aspora.tms.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring system health and performance metrics.
 * Provides insights into system performance for horizontal scaling decisions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final MeterRegistry meterRegistry;
    private final CacheManager cacheManager;
    private final DataSource dataSource;
    
    private final Counter transactionCounter;
    private final Timer transactionTimer;
    private final AtomicLong activeConnections = new AtomicLong(0);
    private final AtomicLong cacheHitRate = new AtomicLong(0);
    private final AtomicLong cacheMissRate = new AtomicLong(0);

    public HealthCheckService(MeterRegistry meterRegistry, CacheManager cacheManager, DataSource dataSource) {
        this.meterRegistry = meterRegistry;
        this.cacheManager = cacheManager;
        this.dataSource = dataSource;
        
        // Initialize metrics
        this.transactionCounter = Counter.builder("tms.transactions.total")
                .description("Total number of transactions processed")
                .register(meterRegistry);
        
        this.transactionTimer = Timer.builder("tms.transactions.duration")
                .description("Transaction processing duration")
                .register(meterRegistry);
    }

    public void incrementTransactionCount() {
        transactionCounter.increment();
    }

    public Timer.Sample startTransactionTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopTransactionTimer(Timer.Sample sample) {
        sample.stop(transactionTimer);
    }

    public void recordCacheHit() {
        cacheHitRate.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMissRate.incrementAndGet();
    }

    public double getCacheHitRate() {
        long hits = cacheHitRate.get();
        long misses = cacheMissRate.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total : 0.0;
    }

    public boolean isDatabaseHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            log.error("Database health check failed", e);
            return false;
        }
    }

    public boolean isCacheHealthy() {
        try {
            return cacheManager.getCache("transactions") != null;
        } catch (Exception e) {
            log.error("Cache health check failed", e);
            return false;
        }
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void logSystemMetrics() {
        log.info("System Metrics - Cache Hit Rate: {:.2f}%, Database Healthy: {}, Cache Healthy: {}", 
                getCacheHitRate() * 100, isDatabaseHealthy(), isCacheHealthy());
    }

    @Scheduled(fixedRate = 60000) // Every minute
    public void recordSystemMetrics() {
        // Record custom metrics for monitoring
        meterRegistry.gauge("tms.cache.hit.rate", getCacheHitRate());
        meterRegistry.gauge("tms.database.healthy", isDatabaseHealthy() ? 1 : 0);
        meterRegistry.gauge("tms.cache.healthy", isCacheHealthy() ? 1 : 0);
    }
}
