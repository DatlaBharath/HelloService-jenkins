package com.iiht.service;

import org.springframework.web.filter.OncePerRequestFilter;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.Bandwidth;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> userEndpointBuckets = new ConcurrentHashMap<>();

    private Bucket createBucket(int capacity, int refillTokensPerSecond) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(refillTokensPerSecond, Duration.ofSeconds(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }

    private String getUserIdentifier(HttpServletRequest request) {
        // Extract user-specific information, such as IP address or user ID
        String userId = request.getRemoteAddr(); // Fallback to IP address if user ID is unavailable
        if (request.getUserPrincipal() != null) {
            userId = request.getUserPrincipal().getName();
        }
        return userId;
    }

    private String generateBucketKey(String userId, String endpoint) {
        // Combine user identifier and endpoint path to create a unique key
        return userId + ":" + endpoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userId = getUserIdentifier(request);
        String endpoint = request.getRequestURI();
        String bucketKey = generateBucketKey(userId, endpoint);

        // Dynamically create or retrieve the bucket for the user-endpoint combination
        Bucket bucket = userEndpointBuckets.computeIfAbsent(bucketKey, key -> createBucket(50, 5)); // Default rate limit

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.getWriter().write("Rate limit exceeded. Try again later.");
        }
    }
}