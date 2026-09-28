package com.kct.jcredit.core_backend.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * The S3 mechanics shared by every two-phase upload: validate, put under
 * {@code tmp/}, and promote to a permanent key once the owning record is
 * actually saved.
 *
 * <p>Extracted from the equipment uploader so the project plan can reuse it.
 * Deliberately knows nothing about key layout — each caller owns its own
 * prefixes, which is what keeps the equipment keys byte-identical.
 *
 * <p><b>Temp objects are not cleaned up here.</b> A page that never saves
 * leaves its upload under {@code tmp/}, so the bucket needs a lifecycle rule
 * expiring that prefix; without one, abandoned uploads accumulate forever.
 */
@Slf4j
@Component
public class S3FileStorage {

    /** Everything under this prefix is provisional and may be expired. */
    public static final String TEMP_ROOT = "tmp/";

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private static final Set<String> VALID_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png");

    @Value("${app.s3.bucket:}")
    private String bucketName;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3FileStorage(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Rejects anything outside the whitelist or over the size cap, before a
     * byte reaches S3.
     */
    public void validate(MultipartFile file) {
        String mime = file.getContentType();
        if (!VALID_MIME_TYPES.contains(mime)) {
            throw new UnsupportedFileTypeException(VALID_MIME_TYPES.stream().toList(), mime);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BulkValidationException(List.of(
                    ValidationErrorDetail.builder()
                            .tempId("request")
                            .field("file")
                            .message("ファイルサイズは10MB以下である必要があります。")
                            .build()));
        }
    }

    /** Uploads to the given key. The caller decides the key; this just stores. */
    public void put(String s3Key, MultipartFile file, Map<String, String> metadata) {
        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(file.getContentType())
                            .metadata(metadata)
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read uploaded file", e);
        }
    }

    /** Copies to the permanent key, then drops the temp object. */
    public void promote(String tempS3Key, String permanentKey) {
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(tempS3Key)
                .destinationBucket(bucketName)
                .destinationKey(permanentKey)
                .build());

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(tempS3Key)
                .build());

        log.info("S3 file moved. from={}, to={}", tempS3Key, permanentKey);
    }

    /**
     * A short-lived GET URL for one object. The caller decides whether the
     * requester may see that key; this only signs.
     */
    public String presignGet(String s3Key, Duration validFor) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(validFor)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public void delete(String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build());
    }

    /** True when the key still points at a provisional upload. */
    public static boolean isTemporary(String s3Key) {
        return s3Key != null && s3Key.startsWith(TEMP_ROOT);
    }

    /** Strips anything S3 would treat as a path separator or metacharacter. */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) return "document";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
