package com.example.jobsystem.controller;

import com.example.jobsystem.dto.CreateJobRequest;
import com.example.jobsystem.entity.JobEntity;
import com.example.jobsystem.service.JobService;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @PostMapping
    public ResponseEntity<JobEntity> create(
            @RequestBody @Valid CreateJobRequest request
    ) {

        return ResponseEntity.ok(
                jobService.createJob(request)
        );
    }
    
    @GetMapping("/dead-letter")
    public ResponseEntity<List<JobEntity>>
    getDeadLetterJobs() {

        return ResponseEntity.ok(
                jobService.getDeadLetterJobs()
        );
    }
    
    @PostMapping("/{id}/retry")
    public ResponseEntity<String>
    retryDeadLetterJob(
            @PathVariable Long id
    ) {

        jobService.retryDeadLetterJob(id);

        return ResponseEntity.ok(
                "Job replayed successfully"
        );
    }
}