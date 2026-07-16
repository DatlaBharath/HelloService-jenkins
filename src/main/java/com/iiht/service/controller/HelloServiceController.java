package com.iiht.service.controller;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
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

import com.hazelcast.config.Config;
import com.hazelcast.config.EncryptionConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Pattern;

@RestController
public class HelloServiceController {

    private final ProxyManager<String> proxyManager;
    private final Bandwidth limit;

    public HelloServiceController(RateLimitConfig rateLimitConfig) {
        Config hazelcastConfig = new Config();
        NetworkConfig networkConfig = hazelcastConfig.getNetworkConfig();

        // Securely retrieve and decrypt sensitive values from AWS Secrets Manager
        String encryptionAlgorithm = decryptValue(getSecret("ENCRYPTION_ALGORITHM"));
        String encryptionSalt = decryptValue(getSecret("ENCRYPTION_SALT"));
        String encryptionPassword = decryptValue(getSecret("ENCRYPTION_PASSWORD"));

        // Validate sensitive values
        validateEncryptionConfig(encryptionAlgorithm, encryptionSalt, encryptionPassword);

        EncryptionConfig encryptionConfig = new EncryptionConfig()
                .setEnabled(true)
                .setAlgorithm(encryptionAlgorithm)
                .setSalt(encryptionSalt)
                .setPassword(encryptionPassword);
        networkConfig.setEncryptionConfig(encryptionConfig);

        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
        CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();
        Cache<String, GridBucketState> cache = cacheManager.createCache("buckets",
                new MutableConfiguration<String, GridBucketState>().setStoreByValue(false));
        this.proxyManager = new JCacheProxyManager<>(cache);
        this.limit = Bandwidth.classic(rateLimitConfig.getCapacity(),
                                       Refill.greedy(rateLimitConfig.getCapacity(), Duration.ofSeconds(rateLimitConfig.getRefillDuration())));
    }

    private void validateEncryptionConfig(String algorithm, String salt, String password) {
        if (algorithm == null || !algorithm.equals("AES")) {
            throw new IllegalArgumentException("Invalid encryption algorithm. Only AES is supported.");
        }
        if (salt == null || salt.length() < 16) {
            throw new IllegalArgumentException("Encryption salt must be at least 16 characters long.");
        }
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("Encryption password must be at least 12 characters long.");
        }
    }

    private String decryptValue(String encryptedValue) {
        try {
            String secretKey = getSecret("DECRYPTION_KEY");
            validateEncryptionKey(secretKey);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decodedValue = Base64.getDecoder().decode(encryptedValue);
            return new String(cipher.doFinal(decodedValue), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt sensitive value", e);
        }
    }

    private void validateEncryptionKey(String key) {
        if (key == null || key.length() != 16) {
            throw new IllegalArgumentException("Invalid encryption key. Key must be 16 characters long.");
        }
    }

    private String getSecret(String secretName) {
        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
                .withRegion("us-east-1")
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();

        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName);
        GetSecretValueResult getSecretValueResult = client.getSecretValue(getSecretValueRequest);
        return getSecretValueResult.getSecretString();
    }

    private boolean isRateLimitExceeded(String clientIdentifier) {
        Bucket bucket = proxyManager.builder().addLimit(limit).build(clientIdentifier);
        return !bucket.tryConsume(1);
    }

    @GetMapping
    public ResponseEntity<String> hello(HttpServletRequest request) {
        String clientIdentifier = getClientIdentifier(request);
        if (isRateLimitExceeded(clientIdentifier)) {
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

    private String getClientIdentifier(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        return clientIp != null ? clientIp : "unknown-client";
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