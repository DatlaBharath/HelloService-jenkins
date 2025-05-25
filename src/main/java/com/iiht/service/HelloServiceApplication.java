package com.iiht.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.validation.annotation.Validated;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@SpringBootApplication
@EnableDiscoveryClient
public class HelloServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloServiceApplication.class, args);
    }
    
    // Placeholder for future controller implementation utilizing input validation
    // Example demonstration for a basic User entity class that would appear in controllers.
    @Validated
    public static class User {
        
        @NotNull(message = "Name cannot be null")
        @Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters")
        private String name;

        // Note: Actual validation implementation should be done in controller methods
        // Validate using @Valid in method params for input objects and @RequestParam for simple types
        // Method Example: public String createUser(@Valid @RequestBody User user) { ... }

        public User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}