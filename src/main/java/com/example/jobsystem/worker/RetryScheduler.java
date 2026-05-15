package com.example.jobsystem.worker;

import com.example.jobsystem.entity.JobEntity;
import com.example.jobsystem.enums.JobStatus;
import com.example.jobsystem.repository.JobRepository;
import com.example.jobsystem.service.RedisQueueService;
// STEP 2 — Added Logger Imports
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RetryScheduler {

    // STEP 2 — Initialize the Logger instance
    private static final Logger log = LoggerFactory.getLogger(RetryScheduler.class);

    @Autowired
    private JobRepository repository;

    @Autowired
    private RedisQueueService queueService;

    @Scheduled(fixedDelay = 5000)
    public void retryFailedJobs() {

        List<JobEntity> jobs =
                repository.findByStatusAndNextRetryAtBefore(
                        JobStatus.RETRY_SCHEDULED,
                        LocalDateTime.now()
                );

        for (JobEntity job : jobs) {

            job.setStatus(JobStatus.QUEUED);

            repository.save(job);

            queueService.enqueue(job.getId(), job.getPriority());
            
            // STEP 3 — Replaced System.out.println with log.info
            log.info("Requeued Job: {} for retry execution", job.getId());
        }
    }
}