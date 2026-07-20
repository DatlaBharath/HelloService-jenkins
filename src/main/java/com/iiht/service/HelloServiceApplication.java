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

    private String getUserIdentifier(HttpServletRequest request) throws ServletException {
        // Check for a secure token in the Authorization header
        String authToken = request.getHeader("Authorization");
        if (authToken != null && !authToken.isEmpty()) {
            return authToken; // Use the token as the user identifier
        }

        // Check for a session attribute
        Object sessionUser = request.getSession().getAttribute("userId");
        if (sessionUser != null) {
            return sessionUser.toString(); // Use the session attribute as the user identifier
        }

        // Reject the request if no valid identifier is found
        throw new ServletException("Unable to identify user for rate limiting. Missing secure token or session attribute.");
    }

    private String generateBucketKey(String userId, String endpoint) {
        // Combine user identifier and endpoint path to create a unique key
        return userId + ":" + endpoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
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
        } catch (ServletException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(e.getMessage());
        }
    }
}