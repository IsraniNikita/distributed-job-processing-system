package com.example.jobsystem.service;

import com.example.jobsystem.dto.CreateJobRequest;
import com.example.jobsystem.entity.JobEntity;
import com.example.jobsystem.enums.JobPriority;
import com.example.jobsystem.enums.JobStatus;
import com.example.jobsystem.repository.JobRepository;
import com.example.jobsystem.outbox.OutboxEntity; // Added for Phase 11
import com.example.jobsystem.outbox.OutboxRepository; // Added for Phase 11
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    @Autowired
    private JobRepository repository;

    // PHASE 11 — Injecting OutboxRepository to track our transactional events
    @Autowired
    private OutboxRepository outboxRepository;

    @Transactional
    public JobEntity createJob(CreateJobRequest request) {

        // STEP 5 — Check if a job with this exact idempotency key already exists
        Optional<JobEntity> existingJob = repository.findByIdempotencyKey(
                request.getIdempotencyKey()
        );

        if (existingJob.isPresent()) {
            System.out.println("Duplicate request detected for key: " + request.getIdempotencyKey());
            return existingJob.get();
        }

        // If no duplicate exists, create the brand-new job execution track
        JobEntity job = new JobEntity();

        job.setType(request.getType());
        job.setPayload(request.getPayload());
        
        // STEP 6 — Map the request's uniqueness token to our database entity layer
        job.setIdempotencyKey(request.getIdempotencyKey());

        // PHASE 10 STEP 12 — Generate and store the Correlation ID string
        job.setCorrelationId(UUID.randomUUID().toString());

        // PHASE 8 STEP 4 & 6 — Set priority from client request, fallback to MEDIUM safely
        if (request.getPriority() != null) {
            job.setPriority(request.getPriority());
        } else {
            job.setPriority(JobPriority.MEDIUM);
        }

        job.setStatus(JobStatus.QUEUED);

        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        JobEntity saved = repository.save(job);

        // =======================================================================
        // PHASE 11 STEP 4 & 5 — REMOVED Direct Redis Push & Saved Outbox Event
        // =======================================================================
        OutboxEntity event = new OutboxEntity();
        event.setEventType("JOB_CREATED");
        
        // Storing both ID and Priority separated by a colon (e.g. "41:HIGH") 
        // so the background scheduler knows exactly which queue to send it to.
        event.setPayload(saved.getId() + ":" + saved.getPriority().name());
        event.setProcessed(false);
        event.setCreatedAt(LocalDateTime.now());

        outboxRepository.save(event);

        return saved;
    }
    
    public List<JobEntity> getDeadLetterJobs() {
        return repository.findByStatus(
                JobStatus.DEAD_LETTER
        );
    }
    
    @Transactional
    public void retryDeadLetterJob(Long id) {

        JobEntity job = repository.findById(id)
                .orElseThrow();

        if (job.getStatus() != JobStatus.DEAD_LETTER) {
            throw new RuntimeException(
                    "Job is not in DEAD_LETTER state"
            );
        }

        job.setRetryCount(0);
        job.setNextRetryAt(null);
        job.setProcessingStartedAt(null);
        job.setStatus(JobStatus.QUEUED);
        job.setUpdatedAt(LocalDateTime.now());

        JobEntity saved = repository.save(job);

        // =======================================================================
        // PHASE 11 STEP 4 & 5 — REMOVED Direct Redis Push from DLQ Replay too
        // =======================================================================
        OutboxEntity event = new OutboxEntity();
        event.setEventType("JOB_CREATED");
        event.setPayload(saved.getId() + ":" + saved.getPriority().name());
        event.setProcessed(false);
        event.setCreatedAt(LocalDateTime.now());

        outboxRepository.save(event);

        System.out.println(
                "Replayed DLQ job registered in Outbox: " + job.getId()
        );
    }
}