package com.example.jobsystem.worker;

import com.example.jobsystem.entity.JobEntity;
import com.example.jobsystem.enums.JobStatus;
import com.example.jobsystem.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class JobWorker {

    // PHASE 10 STEP 2 — Setup Logger Instance
    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    @Autowired
    private JobRepository repository;

    public void process(Long jobId) {

        // =========================================
        // ATOMIC CLAIM
        // ONLY ONE WORKER CAN CLAIM JOB
        // =========================================
        int updated = repository.claimJob(
                jobId,
                JobStatus.QUEUED,
                JobStatus.PROCESSING,
                LocalDateTime.now()
        );

        // =========================================
        // ANOTHER WORKER ALREADY CLAIMED IT
        // =========================================
        if (updated == 0) {
            // PHASE 10 STEP 3 — Replaced System.out
            log.info("Job already claimed: {}", jobId);
            return;
        }

        try {
            // =========================================
            // FETCH FRESH ENTITY AFTER CLAIM
            // =========================================
            JobEntity job = repository.findById(jobId)
                    .orElseThrow();

            // PHASE 10 STEP 13 — Enhanced log line with dynamic traceability tracking codes
            log.info("Processing Job: {} | CorrelationId: {} on Thread: {}", 
                    job.getId(), 
                    job.getCorrelationId(), 
                    Thread.currentThread().getName());

            // =========================================
            // PHASE 10 STEP 6 — Start Duration Window
            // =========================================
            long start = System.currentTimeMillis();

            // =========================================
            // EXECUTE WORK
            // =========================================
            simulateWork();

            // =========================================
            // PHASE 10 STEP 6 — Calculate Metric Execution Time
            // =========================================
            long duration = System.currentTimeMillis() - start;
            job.setProcessingDurationMs(duration);

            // =========================================
            // SUCCESS FLOW
            // =========================================
            job.setStatus(JobStatus.SUCCESS);
            job.setProcessingStartedAt(null);
            job.setNextRetryAt(null);
            job.setUpdatedAt(LocalDateTime.now());

            repository.save(job);

            // PHASE 10 STEP 3 & STEP 13 — Enhanced with duration observability metrics
            log.info("Completed Job: {} | CorrelationId: {} | Duration: {}ms", job.getId(), job.getCorrelationId(), duration);

        } catch (Exception e) {
            // PHASE 10 STEP 3 — Structured error log with stack trace attachment
            log.error("Error while processing Job: {}", jobId, e);
            handleFailure(jobId);
        }
    }

    // =========================================
    // FAILURE HANDLER
    // =========================================
    private void handleFailure(Long jobId) {
        try {
            JobEntity failedJob = repository.findById(jobId)
                    .orElseThrow();

            // =========================================
            // INCREMENT RETRY COUNT
            // =========================================
            failedJob.setRetryCount(failedJob.getRetryCount() + 1);
            failedJob.setProcessingStartedAt(null);
            failedJob.setUpdatedAt(LocalDateTime.now());

            // =========================================
            // MAX RETRIES EXCEEDED
            // MOVE TO DEAD LETTER QUEUE
            // =========================================
            if (failedJob.getRetryCount() >= failedJob.getMaxRetries()) {
                failedJob.setStatus(JobStatus.DEAD_LETTER);
                failedJob.setNextRetryAt(null);

                // PHASE 10 STEP 3 — Replaced System.out
                log.info("Moved to DLQ: {} | CorrelationId: {}", failedJob.getId(), failedJob.getCorrelationId());
            } else {
                // =========================================
                // EXPONENTIAL BACKOFF
                // =========================================
                int retryDelay = (int) Math.pow(2, failedJob.getRetryCount());

                failedJob.setStatus(JobStatus.RETRY_SCHEDULED);
                failedJob.setNextRetryAt(LocalDateTime.now().plusSeconds(retryDelay));

                // PHASE 10 STEP 3 — Replaced System.out
                log.info("Retry scheduled for Job: {} | CorrelationId: {} after {} seconds", failedJob.getId(), failedJob.getCorrelationId(), retryDelay);
            }

            repository.save(failedJob);

        } catch (Exception e) {
            // PHASE 10 STEP 3 — Replaced System.out
            log.error("CRITICAL FAILURE updating job state for ID: {}", jobId, e);
        }
    }

    // =========================================
    // SIMULATED WORK
    // =========================================
    private void simulateWork() throws InterruptedException {
        log.info("...Worker is doing heavy processing...");
        Thread.sleep(3000);

        // Simulate random transient failures
        if (Math.random() < 0.3) {
            throw new RuntimeException("Random transient failure");
        }
    }
}