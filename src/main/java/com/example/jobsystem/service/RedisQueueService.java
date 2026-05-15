package com.example.jobsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.jobsystem.enums.JobPriority;

@Service
public class RedisQueueService {

    private static final String JOB_QUEUE = "job_queue";
    
    private static final String HIGH_QUEUE =
            "high_priority_queue";

    private static final String MEDIUM_QUEUE =
            "medium_priority_queue";

    private static final String LOW_QUEUE =
            "low_priority_queue";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void enqueue(
            Long jobId,
            JobPriority priority
    ) {

        String queueName = switch (priority) {

            case HIGH -> HIGH_QUEUE;

            case MEDIUM -> MEDIUM_QUEUE;

            case LOW -> LOW_QUEUE;
        };

        redisTemplate.opsForList()
                .rightPush(queueName, jobId.toString());

        System.out.println(
                "Enqueued Job: "
                        + jobId
                        + " into "
                        + queueName
        );
    }
}