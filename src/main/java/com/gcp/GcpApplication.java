package com.gcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GcpApplication {
    public static void main(String[] args) {
        SpringApplication.run(GcpApplication.class, args);
    }
}
