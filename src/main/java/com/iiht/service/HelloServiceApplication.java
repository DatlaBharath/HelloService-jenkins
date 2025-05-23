package com.iiht.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class HelloServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloServiceApplication.class, args);
    }

    // Placeholder for future input validation integration
    // Utilize Hibernate Validator (JSR-380) annotations once controllers are implemented
    // e.g., @NotNull, @Size, @Pattern for validating inputs within controller methods
}