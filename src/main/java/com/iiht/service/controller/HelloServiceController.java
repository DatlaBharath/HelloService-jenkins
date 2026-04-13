package com.iiht.service.controller;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import java.time.Duration;
import java.util.regex.Pattern;

@RestController
public class HelloServiceController {

    private final Bucket bucket;

    public HelloServiceController(RateLimitConfig rateLimitConfig) {
        CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();
        Cache<String, GridBucketState> cache = cacheManager.createCache("buckets",
                new MutableConfiguration<String, GridBucketState>().setStoreByValue(false));
        ProxyManager<String> proxyManager = new JCacheProxyManager<>(cache);
        Bandwidth limit = Bandwidth.classic(rateLimitConfig.getCapacity(),
                                            Refill.greedy(rateLimitConfig.getCapacity(), Duration.ofSeconds(rateLimitConfig.getRefillDuration())));
        this.bucket = proxyManager.builder().addLimit(limit).build("global-rate-limit");
    }

    private boolean isRateLimitExceeded() {
        return !bucket.tryConsume(1);
    }

    @GetMapping
    public ResponseEntity<String> hello() {
        if (isRateLimitExceeded()) {
            return ResponseEntity.status(429).body("Too Many Requests - Rate limit exceeded");
        }
        String htmlContent = "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Application Deployed</title>" +
                "<style>" +
                "body {" +
                "  font-family: Arial, sans-serif;" +
                "  background-color: #f4f4f4;" +
                "  text-align: center;" +
                "  margin: 0;" +
                "  padding: 0;" +
                "}" +
                "h1 {" +
                "  color: #4CAF50;" +
                "  font-size: 50px;" +
                "  margin-top: 20%;" +
                "}" +
                ".container {" +
                "  padding: 20px;" +
                "  background-color: white;" +
                "  border-radius: 10px;" +
                "  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);" +
                "  display: inline-block;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<h1> Congratulations! The app is Deployed for the first time!!  😁 </h1>" +
                "<p>Your application is up and running successfully!</p>" +
                "</div>" +
                "</body>" +
                "</html>";
        PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.LINKS).and(Sanitizers.STYLES);
        String sanitizedHtmlContent = policy.sanitize(htmlContent);
        return ResponseEntity.ok(sanitizedHtmlContent);
    }

    @GetMapping("/greet")
    public ResponseEntity<String> greet() {
        if (isRateLimitExceeded()) {
            return ResponseEntity.status(429).body("Too Many Requests - Rate limit exceeded");
        }
        return ResponseEntity.ok("Good Morning, Welcome To Demo Project");
    }

    @GetMapping("/add/{a}/{b}")
    public ResponseEntity<String> add(@PathVariable String a, @PathVariable String b) {
        if (isRateLimitExceeded()) {
            return ResponseEntity.status(429).body("Too Many Requests - Rate limit exceeded");
        }
        try {
            int numA = Integer.parseInt(a);
            int numB = Integer.parseInt(b);
            if (numA < 0 || numB < 0) {
                return ResponseEntity.badRequest().body("Inputs must be non-negative integers.");
            }
            return ResponseEntity.ok(String.valueOf(numA + numB));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid input. Inputs must be integers.");
        }
    }

    @GetMapping("/fact/{a}")
    public ResponseEntity<String> factorial(@RequestHeader HttpHeaders headers, @PathVariable String a) {
        if (isRateLimitExceeded()) {
            return ResponseEntity.status(429).body("Too Many Requests - Rate limit exceeded");
        }
        if (headers != null) {
            for (String headerName : headers.keySet()) {
                String headerValue = headers.getFirst(headerName);
                if (!isValidHeaderValue(headerValue)) {
                    return ResponseEntity.badRequest().body("Invalid header value detected.");
                }
            }
        }
        try {
            int numA = Integer.parseInt(a);
            if (numA < 0) {
                return ResponseEntity.badRequest().body("Input must be a non-negative integer.");
            }
            if (numA > 20) {
                return ResponseEntity.badRequest().body("Input is too large to compute factorial.");
            }
            long fact = 1;
            for (int i = 1; i <= numA; i++) {
                fact *= i;
            }
            return ResponseEntity.ok(String.valueOf(fact));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid input. Input must be an integer.");
        }
    }

    private boolean isValidHeaderValue(String value) {
        String safePattern = "^[a-zA-Z0-9-_:;,.]+$";
        return value != null && Pattern.matches(safePattern, value);
    }
}

@Component
class RateLimitConfig {

    @Value("${rate.limit.capacity:10}")
    private int capacity;

    @Value("${rate.limit.refill.duration:1}")
    private int refillDuration;

    public int getCapacity() {
        return capacity;
    }

    public int getRefillDuration() {
        return refillDuration;
    }
}