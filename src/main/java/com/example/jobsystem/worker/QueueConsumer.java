package com.example.jobsystem.worker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class QueueConsumer {

    // PHASE 10 STEP 2 — Setup Logger Instance
    private static final Logger log = LoggerFactory.getLogger(QueueConsumer.class);

    private static final String HIGH_QUEUE = "high_priority_queue";
    private static final String MEDIUM_QUEUE = "medium_priority_queue";
    private static final String LOW_QUEUE = "low_priority_queue";

    private volatile boolean running = true;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ExecutorService workerExecutor;

    @Autowired
    private JobWorker jobWorker;

    @PostConstruct
    public void startWorkers() {

        for (int i = 0; i < 5; i++) {

            workerExecutor.submit(() -> {

                while (running) {
                    try {
                        // =========================
                        // PRIORITY POLLING
                        // =========================
                        String jobId = redisTemplate.opsForList().leftPop(HIGH_QUEUE);

                        if (jobId == null) {
                            jobId = redisTemplate.opsForList().leftPop(MEDIUM_QUEUE);
                        }

                        if (jobId == null) {
                            jobId = redisTemplate.opsForList().leftPop(LOW_QUEUE);
                        }

                        // =========================
                        // NO JOB FOUND
                        // =========================
                        if (jobId == null) {
                            Thread.sleep(1000);
                            continue;
                        }

                        // PHASE 10 STEP 3 — Replaced System.out
                        log.info("Thread: {} picked up Job ID: {}", Thread.currentThread().getName(), jobId);

                        jobWorker.process(Long.parseLong(jobId));

                    } catch (InterruptedException e) {
                        // PHASE 10 STEP 3 — Replaced System.out
                        log.info("Worker thread interrupted during sleep, exiting loop cleanly.");
                    } catch (Exception e) {
                        log.error("Unhandled exception caught in queue worker polling loop", e);
                    }
                }
            });
        }
    }

    @PreDestroy
    public void shutdown() {
        // PHASE 10 STEP 3 — Replaced System.out
        log.info("Shutting down workers gracefully...");
        
        running = false;
        workerExecutor.shutdown();

        try {
            if (!workerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Workers did not finish in time. Force shutdown triggered.");
                workerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Shutdown sequence interrupted. Forcing abrupt thread halt.", e);
            workerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("Worker pool shutdown completed successfully.");
    }
}