package gg.modl.replay.backend.replay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@Slf4j
public class StorageService {

    private static final Duration PRESIGN_UPLOAD_DURATION = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${replay.storage.bucket-name:}")
    private String bucketName;

    @Value("${replay.storage.cdn-domain:}")
    private String cdnDomain;

    public StorageService(
        @org.springframework.lang.Nullable S3Client s3Client,
        @org.springframework.lang.Nullable S3Presigner s3Presigner
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        if (s3Client == null) {
            log.warn("S3 storage is not configured. File storage features will be disabled.");
        }
    }

    public boolean isConfigured() {
        return s3Client != null && bucketName != null && !bucketName.isBlank();
    }

    /**
     * Generate a presigned PUT URL for uploading a replay file.
     */
    public String createPresignedUploadUrl(String key, String contentType, long fileSize) {
        if (s3Presigner == null) {
            throw new IllegalStateException("S3 storage is not configured");
        }

        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .contentLength(fileSize)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(PRESIGN_UPLOAD_DURATION)
            .putObjectRequest(putRequest)
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * Build the CDN URL for a given storage key.
     */
    public String getCdnUrl(String key) {
        if (cdnDomain == null || cdnDomain.isBlank()) {
            return null;
        }
        return String.format("https://%s/%s", cdnDomain, key);
    }

    /**
     * Verify that an uploaded file exists in storage.
     */
    public boolean verifyUploadExists(String key) {
        if (s3Client == null) return false;

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error verifying upload for key: {}", key, e);
            return false;
        }
    }
}
