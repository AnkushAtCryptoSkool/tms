package com.aspora.tms.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for monitoring performance improvements from removing synchronized methods.
 * Tracks concurrent processing, throughput, and response times.
 */
@Service
@Slf4j
public class PerformanceMonitoringService {

    private final MeterRegistry meterRegistry;
    
    private final Counter totalTransactionsCounter;
    private final Counter concurrentTransactionsCounter;
    private final Timer transactionProcessingTimer;
    private final Timer optimisticLockRetryTimer;
    
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicInteger maxConcurrentTransactions = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicInteger> methodConcurrencyMap = new ConcurrentHashMap<>();

    @Autowired
    public PerformanceMonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.totalTransactionsCounter = Counter.builder("tms.transactions.total")
                .description("Total number of transactions processed")
                .register(meterRegistry);
        
        this.concurrentTransactionsCounter = Counter.builder("tms.transactions.concurrent")
                .description("Number of concurrent transactions")
                .register(meterRegistry);
        
        this.transactionProcessingTimer = Timer.builder("tms.transactions.processing.time")
                .description("Transaction processing time")
                .register(meterRegistry);
        
        this.optimisticLockRetryTimer = Timer.builder("tms.transactions.optimistic.retry.time")
                .description("Time spent on optimistic lock retries")
                .register(meterRegistry);
    }

    /**
     * Record transaction start for concurrency tracking.
     */
    public void recordTransactionStart(String methodName) {
        int currentConcurrent = methodConcurrencyMap
            .computeIfAbsent(methodName, k -> new AtomicInteger(0))
            .incrementAndGet();
        
        maxConcurrentTransactions.updateAndGet(current -> Math.max(current, currentConcurrent));
        concurrentTransactionsCounter.increment();
        
        log.debug("Transaction started in method: {}. Current concurrent: {}", methodName, currentConcurrent);
    }

    /**
     * Record transaction completion.
     */
    public void recordTransactionComplete(String methodName, long processingTimeMs) {
        AtomicInteger counter = methodConcurrencyMap.get(methodName);
        if (counter != null) {
            counter.decrementAndGet();
        }
        totalTransactionsCounter.increment();
        totalProcessingTime.addAndGet(processingTimeMs);
        
        log.debug("Transaction completed in method: {}. Processing time: {}ms", methodName, processingTimeMs);
    }

    /**
     * Record optimistic lock retry.
     */
    public void recordOptimisticLockRetry(String methodName, long retryTimeMs) {
        optimisticLockRetryTimer.record(retryTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        log.warn("Optimistic lock retry in method: {}. Retry time: {}ms", methodName, retryTimeMs);
    }

    /**
     * Get current concurrency level for a specific method.
     */
    public int getCurrentConcurrency(String methodName) {
        return methodConcurrencyMap.getOrDefault(methodName, new AtomicInteger(0)).get();
    }

    /**
     * Get maximum concurrent transactions observed.
     */
    public int getMaxConcurrentTransactions() {
        return maxConcurrentTransactions.get();
    }

    /**
     * Get average processing time.
     */
    public double getAverageProcessingTime() {
        long total = (long) totalTransactionsCounter.count();
        return total > 0 ? (double) totalProcessingTime.get() / total : 0.0;
    }

    /**
     * Get performance summary.
     */
    public PerformanceSummary getPerformanceSummary() {
        return PerformanceSummary.builder()
                .totalTransactions(totalTransactionsCounter.count())
                .maxConcurrentTransactions(getMaxConcurrentTransactions())
                .averageProcessingTime(getAverageProcessingTime())
                .currentConcurrencyByMethod(new ConcurrentHashMap<>(methodConcurrencyMap))
                .build();
    }

    /**
     * Reset performance counters (useful for testing).
     */
    public void resetCounters() {
        totalProcessingTime.set(0);
        maxConcurrentTransactions.set(0);
        methodConcurrencyMap.clear();
        log.info("Performance counters reset");
    }

    /**
     * Performance summary data class.
     */
    public static class PerformanceSummary {
        private final double totalTransactions;
        private final int maxConcurrentTransactions;
        private final double averageProcessingTime;
        private final ConcurrentHashMap<String, AtomicInteger> currentConcurrencyByMethod;

        private PerformanceSummary(Builder builder) {
            this.totalTransactions = builder.totalTransactions;
            this.maxConcurrentTransactions = builder.maxConcurrentTransactions;
            this.averageProcessingTime = builder.averageProcessingTime;
            this.currentConcurrencyByMethod = builder.currentConcurrencyByMethod;
        }

        // Getters
        public double getTotalTransactions() { return totalTransactions; }
        public int getMaxConcurrentTransactions() { return maxConcurrentTransactions; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public ConcurrentHashMap<String, AtomicInteger> getCurrentConcurrencyByMethod() { return currentConcurrencyByMethod; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private double totalTransactions;
            private int maxConcurrentTransactions;
            private double averageProcessingTime;
            private ConcurrentHashMap<String, AtomicInteger> currentConcurrencyByMethod;

            public Builder totalTransactions(double totalTransactions) {
                this.totalTransactions = totalTransactions;
                return this;
            }

            public Builder maxConcurrentTransactions(int maxConcurrentTransactions) {
                this.maxConcurrentTransactions = maxConcurrentTransactions;
                return this;
            }

            public Builder averageProcessingTime(double averageProcessingTime) {
                this.averageProcessingTime = averageProcessingTime;
                return this;
            }

            public Builder currentConcurrencyByMethod(ConcurrentHashMap<String, AtomicInteger> currentConcurrencyByMethod) {
                this.currentConcurrencyByMethod = currentConcurrencyByMethod;
                return this;
            }

            public PerformanceSummary build() {
                return new PerformanceSummary(this);
            }
        }
    }
}
