package com.iiht.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.Bandwidth;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableDiscoveryClient
public class HelloServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloServiceApplication.class, args);
    }
}

@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

        http
            .csrf().disable()
            .authorizeRequests()
            .requestMatchers(getAuthenticatedEndpoints().toArray(new String[0])).authenticated()
            .and()
            .oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(jwtAuthenticationConverter);

        return http.build();
    }

    private List<String> getAuthenticatedEndpoints() {
        // Validate and sanitize endpoints loaded from configuration or environment variables
        String[] rawEndpoints = new String[]{"/eureka/**"};
        return Arrays.stream(rawEndpoints)
                     .map(this::sanitizeEndpoint)
                     .filter(this::isValidEndpoint)
                     .collect(Collectors.toList());
    }

    private boolean isValidEndpoint(String endpoint) {
        // Ensures endpoint matches specific patterns after canonicalization
        return endpoint != null && endpoint.matches("^(/[a-zA-Z0-9\\-_/]+)+$");
    }

    private String sanitizeEndpoint(String endpoint) {
        // Canonicalize and sanitize endpoint to remove potential malicious characters
        if (endpoint == null) {
            return "";
        }
        endpoint = endpoint.replaceAll("\\.{2,}", ""); // Remove path traversal patterns
        endpoint = endpoint.replaceAll("[^a-zA-Z0-9\\-_/]", ""); // Remove other malicious characters
        return endpoint.trim();
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter() {
        return new RateLimitingFilter();
    }
}

class RateLimitingFilter extends OncePerRequestFilter {

    private final Bucket bucket;

    public RateLimitingFilter() {
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        this.bucket = Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.getWriter().write("Rate limit exceeded. Try again later.");
        }
    }
}