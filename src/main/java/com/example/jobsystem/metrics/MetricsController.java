package com.example.jobsystem.metrics;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jobsystem.enums.JobStatus;
import com.example.jobsystem.repository.JobRepository;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    @Autowired
    private JobRepository repository;

    @GetMapping("/jobs")
    public Map<String, Long> metrics() {

        Map<String, Long> response =
                new HashMap<>();

        response.put(
                "queued",
                repository.countByStatus(
                        JobStatus.QUEUED
                )
        );

        response.put(
                "processing",
                repository.countByStatus(
                        JobStatus.PROCESSING
                )
        );

        response.put(
                "success",
                repository.countByStatus(
                        JobStatus.SUCCESS
                )
        );

        response.put(
                "deadLetter",
                repository.countByStatus(
                        JobStatus.DEAD_LETTER
                )
        );

        return response;
    }
}