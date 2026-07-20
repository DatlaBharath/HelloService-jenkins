package com.iiht.service.controller;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@RestController
public class HelloServiceController {

    @Value("${aws.region}")
    private String awsRegion;

    private final Bucket bucket;

    public HelloServiceController(@Value("${rate.limit.capacity:10}") int capacity,
                                  @Value("${rate.limit.refill.duration:1}") int refillDuration) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofSeconds(refillDuration)));
        this.bucket = Bucket4j.builder().addLimit(limit).build();
    }

    private String getSecret(String secretName) {
        if (!bucket.tryConsume(1)) {
            throw new RuntimeException("Rate limit exceeded. Please try again later.");
        }

        AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
                .withRegion(awsRegion)
                .build();

        validateAccess(secretName);

        GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName);
        GetSecretValueResult getSecretValueResult = client.getSecretValue(getSecretValueRequest);
        return getSecretValueResult.getSecretString();
    }

    private void validateAccess(String secretName) {
        if (!isAuthorized(secretName)) {
            throw new SecurityException("Unauthorized access to secret: " + secretName);
        }
    }

    private boolean isAuthorized(String secretName) {
        return true; // Replace with actual logic
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