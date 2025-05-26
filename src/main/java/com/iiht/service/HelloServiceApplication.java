package com.iiht.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@EnableDiscoveryClient
@EnableResourceServer
public class HelloServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloServiceApplication.class, args);
    }
    
    @Validated
    public static class User {
        
        @NotNull(message = "Name cannot be null")
        @Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters")
        private String name;

        public User(String name) {
            this.name = sanitize(name);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = sanitize(name);
        }
        
        private String sanitize(String input) {
            // Basic sanitation to prevent XSS and similar injection attacks
            return input.replaceAll("[<>]", "");
        }
    }
    
    @EnableWebSecurity
    public static class SecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .authorizeRequests()
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
                .and()
                .oauth2ResourceServer()
                .jwt()
                .and()
                .csrf().disable()
                .headers()
                .xssProtection()
                .and()
                .contentSecurityPolicy("script-src 'self'");
        }
        
        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
            resources.resourceId("resource-id").stateless(true);
        }
    }

    @Bean
    public SecurityConfiguration securityConfiguration() {
        return new SecurityConfiguration();
    }
}

@RestController
@RequestMapping("/api")
public class UserController {
    
    @PostMapping("/user")
    public String createUser(@Valid @RequestBody User user) {
        // More comprehensive validation can be added here if needed
        return "User " + user.getName() + " created.";
    }
}