package com.example.jobsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JobsystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobsystemApplication.class, args);
	}

}
