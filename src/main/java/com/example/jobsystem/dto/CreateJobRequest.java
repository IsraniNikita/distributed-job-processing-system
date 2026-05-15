package com.example.jobsystem.dto;

import com.example.jobsystem.enums.JobPriority;

import jakarta.validation.constraints.NotBlank;

public class CreateJobRequest {

    @NotBlank
    private String type;

    @NotBlank
    private String payload;
    
    private String idempotencyKey;
    
    private JobPriority priority;

    public JobPriority getPriority() {
		return priority;
	}

	public void setPriority(JobPriority priority) {
		this.priority = priority;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

	public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}