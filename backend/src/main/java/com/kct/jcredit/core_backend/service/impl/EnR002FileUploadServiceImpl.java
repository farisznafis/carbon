package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.dto.response.EnR002FileUploadResponse;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;
import com.kct.jcredit.core_backend.exception.BulkValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;
import java.util.UUID;  
import java.util.Map;
import java.util.List;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


@Slf4j
@Service
@RequiredArgsConstructor
public class EnR002FileUploadServiceImpl implements EnR002FileUploadService {

    @Value("${app.s3.bucket:}")
    private String bucketName;

    private final S3Client s3Client;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private static final Set<String> VALID_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    // ─────────────────────────────────────────────────────────────
    // UPLOAD
    // ─────────────────────────────────────────────────────────────

    @Override
    public EnR002FileUploadResponse uploadFile(String siteId,
                                                EquipmentFileType fileType,
                                                String uploadedBy,
                                                MultipartFile file) {

        // 1. Validasi mime type
        String mime = file.getContentType();
        if (!VALID_MIME_TYPES.contains(mime)) {
            throw new UnsupportedFileTypeException(
                    VALID_MIME_TYPES.stream().toList(), mime);
        }

        // 2. Validasi ukuran file
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BulkValidationException(List.of(
                    ValidationErrorDetail.builder()
                            .tempId("request")
                            .field("file")
                            .message("ファイルサイズは10MB以下である必要があります。")
                            .build()
            ));
        }

        // 3. Generate fileId
        String fileId    = UUID.randomUUID().toString();
        String sanitized = sanitizeFileName(file.getOriginalFilename());

        // 4. S3 key dengan prefix tmp/ di ROOT (bukan di tengah path)
        //    Format: tmp/equipments/{uuid}/{fileType}_{sanitizedFileName}
        String s3Key = String.format(
                "tmp/equipments/%s/%s_%s",
                fileId,
                fileType.getValue(),
                sanitized
        );

        // 5. Upload ke S3
        try (java.io.InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(mime)
                            .metadata(Map.of(
                                    "fileId",     fileId,
                                    "uploadedBy", uploadedBy,
                                    "fileType",   fileType.getValue(),
                                    "siteId",     siteId
                            ))
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Unable to read uploaded file", e);
        }

        log.info("File uploaded to S3 tmp. siteId={}, fileType={}, s3Key={}",
                 siteId, fileType.getValue(), s3Key);

        return EnR002FileUploadResponse.builder()
                .statusCode("OK")
                .s3Key(s3Key)
                .fileId(fileId)
                .originalName(sanitized)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // MOVE
    // ─────────────────────────────────────────────────────────────

    @Override
    public String moveTempToPermanent(String tempS3Key,
                                       String siteId,
                                       Integer equipmentId) {

        // dari: tmp/equipments/{uuid}/solar_contract_file.pdf
        // ke:   projects/{projectId}/sites/{siteId}/equipments/{equipmentId}/solar_contract_file.pdf
        String fileNamePart = tempS3Key.replaceAll(
                "^tmp/equipments/[^/]+/", ""
        );

        String permanentKey = String.format(
                "sites/%s/equipments/%d/%s",
                siteId,
                equipmentId,
                fileNamePart
        );

        // Copy ke path permanent
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(tempS3Key)
                .destinationBucket(bucketName)
                .destinationKey(permanentKey)
                .build());

        // Delete file temp
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(tempS3Key)
                .build());

        log.info("S3 file moved. from={}, to={}", tempS3Key, permanentKey);
        return permanentKey;
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "document";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}