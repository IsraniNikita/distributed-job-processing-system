package com.example.jobsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class WorkerConfig {

    @Bean
    public ExecutorService workerExecutor() {

        return Executors.newFixedThreadPool(5);
    }
}