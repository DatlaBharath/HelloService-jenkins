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
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.RedisBucketBuilder;
import io.github.bucket4j.redis.lettuce.RedisProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class HelloServiceController {

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${encryption.key}")
    private String encryptionKey;

    private final Bucket bucket;
    private static final Logger logger = Logger.getLogger(HelloServiceController.class.getName());

    public HelloServiceController(@Value("${rate.limit.capacity:10}") int capacity,
                                  @Value("${rate.limit.refill.duration:1}") int refillDuration,
                                  @Value("${redis.url}") String redisUrl) {
        RedisClient redisClient = RedisClient.create(redisUrl);
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        ProxyManager<String> proxyManager = Bucket4j.extension(RedisBucketBuilder.class)
                .proxyManagerForRedis(connection.sync());

        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofSeconds(refillDuration)));
        this.bucket = proxyManager.builder().addLimit(limit).build("rate-limit-bucket");
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
        String secret = getSecretValueResult.getSecretString();

        logger.info("Accessed secret: " + secretName);

        return encryptValue(secret);
    }

    private void validateAccess(String secretName) {
        if (!isAuthorized(secretName)) {
            throw new SecurityException("Unauthorized access to secret: " + secretName);
        }
    }

    private boolean isAuthorized(String secretName) {
        return true; // Replace with actual logic
    }

    private String encryptValue(String value) {
        try {
            validateEncryptionKey(encryptionKey);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encryptedValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedValue);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Invalid encryption key provided", e);
            throw new EncryptionException("Encryption failed due to invalid key", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during encryption", e);
            throw new EncryptionException("Encryption failed due to an unexpected error", e);
        }
    }

    private String decryptValue(String encryptedValue) {
        try {
            validateEncryptionKey(encryptionKey);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decodedValue = Base64.getDecoder().decode(encryptedValue);
            return new String(cipher.doFinal(decodedValue), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            logger.log(Level.SEVERE, "Invalid encryption key provided", e);
            throw new DecryptionException("Decryption failed due to invalid key", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during decryption", e);
            throw new DecryptionException("Decryption failed due to an unexpected error", e);
        }
    }

    private void validateEncryptionKey(String key) {
        if (key == null || key.length() != 16) {
            throw new IllegalArgumentException("Invalid encryption key. Key must be 16 characters long.");
        }
    }
}

class EncryptionException extends RuntimeException {
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}

class DecryptionException extends RuntimeException {
    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
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