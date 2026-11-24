package com.stylehub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket:stylehub-media-bucket}")
    private String bucketName;

    @Value("${aws.cloudfront.domain:}")
    private String cloudFrontDomain;

    @Value("${stylehub.upload.dir:uploads}")
    private String localUploadDir;

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = "products/" + UUID.randomUUID() + extension;

        // Try AWS S3 first if S3Client is available
        if (s3Client != null && bucketName != null && !bucketName.isBlank()) {
            try {
                PutObjectRequest putReq = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .build();

                s3Client.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
                log.info("Successfully uploaded image to AWS S3: {}", key);

                if (cloudFrontDomain != null && !cloudFrontDomain.isBlank()) {
                    return "https://" + cloudFrontDomain + "/" + key;
                }
                return "https://" + bucketName + ".s3.amazonaws.com/" + key;
            } catch (Exception e) {
                log.warn("S3 upload failed, falling back to local file storage: {}", e.getMessage());
            }
        }

        // Fallback: local file system storage
        try {
            Path uploadPath = Paths.get(localUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String localFilename = UUID.randomUUID() + extension;
            Path destination = uploadPath.resolve(localFilename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved image locally to: {}", destination.toAbsolutePath());
            return "/uploads/" + localFilename;
        } catch (IOException e) {
            log.error("Failed to store file locally: {}", e.getMessage());
            throw new RuntimeException("Could not store image file: " + e.getMessage(), e);
        }
    }
}
