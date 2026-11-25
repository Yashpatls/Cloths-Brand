package com.stylehub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class CloudConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudConfig.class);

    @Value("${aws.region:ap-south-1}")
    private String region;

    @Value("${aws.accessKeyId:}")
    private String accessKeyId;

    @Value("${aws.secretKey:}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        if (accessKeyId == null || accessKeyId.isBlank() || secretKey == null || secretKey.isBlank()) {
            log.info("AWS S3 credentials not configured. Operating in local storage mode (uploads/ directory).");
            return null;
        }
        try {
            log.info("Initializing S3Client with explicit credentials for region: {}", region);
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretKey)))
                    .build();
        } catch (Exception e) {
            log.warn("AWS S3Client could not be initialized (falling back to local storage handler): {}", e.getMessage());
            return null;
        }
    }
}
