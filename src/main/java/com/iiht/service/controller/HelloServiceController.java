package com.iiht.service.controller;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import io.github.bucket4j.Bandwidth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.util.regex.Pattern;

@RestController
public class HelloServiceController {

    private final Bucket bucket;

    public HelloServiceController(RateLimitConfig rateLimitConfig) {
        Bandwidth limit = Bandwidth.classic(rateLimitConfig.getCapacity(),
                                            Refill.greedy(rateLimitConfig.getCapacity(), Duration.ofSeconds(rateLimitConfig.getRefillDuration())));
        this.bucket = Bucket4j.builder().addLimit(limit).build();
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
        String sanitizedHtmlContent = HtmlUtils.htmlEscape(htmlContent);
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
    public ResponseEntity<String> add(@PathVariable int a, @PathVariable int b) {
        if (isRateLimitExceeded()) {
            return ResponseEntity.status(429).body("Too Many Requests - Rate limit exceeded");
        }
        if (a < 0 || b < 0) {
            return ResponseEntity.badRequest().body("Inputs must be non-negative integers.");
        }
        return ResponseEntity.ok(String.valueOf(a + b));
    }

    @GetMapping("/fact/{a}")
    public ResponseEntity<String> factorial(@RequestHeader HttpHeaders headers, @PathVariable int a) {
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
        if (a < 0) {
            return ResponseEntity.badRequest().body("Input must be a non-negative integer.");
        }

        int fact = 1;
        for (int i = 1; i <= a; i++) {
            fact *= i;
        }
        return ResponseEntity.ok(String.valueOf(fact));
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