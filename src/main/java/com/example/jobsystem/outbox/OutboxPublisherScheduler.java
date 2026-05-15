package com.example.jobsystem.outbox;

import com.example.jobsystem.enums.JobPriority;
import com.example.jobsystem.service.RedisQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate; // STEP 4: Added for Redis Lock
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.List;

// STEP 6 — Create Publisher Scheduler Component
@Component
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private RedisQueueService queueService;

    // STEP 4: Inject StringRedisTemplate for Distributed Coordination Locking
    @Autowired
    private StringRedisTemplate redisTemplate;

    // STEP 4: Unique Redis key name across our instance cluster
    private static final String LOCK_KEY = "outbox_scheduler_lock";

    // STEP 10 — Process pending events automatically every 5000ms (5 seconds)
    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {
        
        // STEP 4 & 5 — Attempt to acquire the unique cluster-wide lock (Lease time: 30 seconds)
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", Duration.ofSeconds(30));

        // If lock is not acquired, another instance is running this loop. Exit quietly.
        if (Boolean.FALSE.equals(acquired) || acquired == null) {
            return;
        }

        try {
            // Only the instance that successfully acquired the lock executes the logic
            executeOutboxSync();
        } finally {
            // STEP 6 — Always release the lock so it doesn't leave the cluster deadlocked
            redisTemplate.delete(LOCK_KEY);
        }
    }

    // Extracted transactional logic to keep the try-finally lock block clean
    @Transactional
    public void executeOutboxSync() {
        // STEP 7 — Read Unprocessed Events from DB
        List<OutboxEntity> pendingEvents = outboxRepository.findByProcessedFalse();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Outbox Engine: This cluster instance won the lock. Detected {} pending events to publish.", pendingEvents.size());

        for (OutboxEntity event : pendingEvents) {
            try {
                if ("JOB_CREATED".equals(event.getEventType())) {
                    // Split our composite payload format (e.g., "41:HIGH" -> ID: 41, Priority: HIGH)
                    String[] payloadParts = event.getPayload().split(":");
                    Long jobId = Long.parseLong(payloadParts[0]);
                    JobPriority priority = JobPriority.valueOf(payloadParts[1]);

                    // STEP 8 — Safely execute dual-write fallback point by pushing directly into Redis
                    queueService.enqueue(jobId, priority);
                    log.info("Outbox Engine: Dispatched Job ID: {} [Priority: {}] to Redis Cluster Queue.", jobId, priority);
                }

                // STEP 9 — Mark Processed so it is never handled again
                event.setProcessed(true);
                outboxRepository.save(event);

            } catch (Exception e) {
                // If Redis is down, this block catches the error.
                log.error("Outbox Engine: Transmission breakdown for Event ID: {}. Retrying in 5 seconds.", event.getId(), e);
            }
        }
    }
}