package com.example.jobsystem.repository;

import com.example.jobsystem.entity.JobEntity;
import com.example.jobsystem.enums.JobStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JobRepository
        extends JpaRepository<JobEntity, Long> {

    List<JobEntity> findByStatusAndNextRetryAtBefore(
            JobStatus status,
            LocalDateTime time
    );
    
    List<JobEntity> findByStatusAndProcessingStartedAtBefore(
            JobStatus status,
            LocalDateTime timeout
    );
    
    List<JobEntity> findByStatus(
            JobStatus status
    );
    
    Optional<JobEntity> findByIdempotencyKey(String idempotencyKey);
    
    @Modifying
    @Transactional
    @Query("""
    UPDATE JobEntity j
    SET j.status = :processing,
        j.processingStartedAt = :now,
        j.updatedAt = :now
    WHERE j.id = :id
    AND j.status = :queued
    """)
    int claimJob(
            @Param("id") Long id,
            @Param("queued") JobStatus queued,
            @Param("processing") JobStatus processing,
            @Param("now") LocalDateTime now
    );

    // =========================================
    // PHASE 10 STEP 9 — Spring Data Metrics Engine
    // =========================================
    long countByStatus(JobStatus status);
}