package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.service.FileUploadSolarService;

import java.io.IOException;

import com.kct.jcredit.core_backend.dto.request.FilePreRegisSolarRequest;
import com.kct.jcredit.core_backend.dto.response.FileSolarUploadResponse;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;
import com.kct.jcredit.core_backend.exception.BulkValidationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;  
import java.time.Duration;
import java.util.Map;
import java.util.List;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.EncryptionTypeMismatchException;
import software.amazon.awssdk.services.s3.model.InvalidRequestException;
import software.amazon.awssdk.services.s3.model.InvalidWriteOffsetException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.TooManyPartsException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadSolarServiceImpl implements FileUploadSolarService {

    @Value("${app.s3.bucket:gx-monitoring-files}")
    private String bucketName;

    private final S3Client s3Client;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private static final Set<String> VALID_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    // ─────────────────────────────────────────────────────────────
    // UPLOAD
    // ─────────────────────────────────────────────────────────────

    @Override
public FileSolarUploadResponse uploadFile(Integer siteId,
                                          Integer projectId,
                                          EquipmentFileType fileType,
                                          String uploadedBy,
                                          MultipartFile file) throws IOException {

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

    // 4. S3 key dengan prefix temp/
    String s3Key = String.format(
            "projects/%d/sites/%d/equipments/temp/%s/%s_%s",
            projectId,
            siteId,
            fileId,
            fileType.getValue(),
            sanitized
    );

    // 5. Upload ke S3
    s3Client.putObject(
            PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(mime)
                    .metadata(Map.of(
                            "fileId",     fileId,
                            "uploadedBy", uploadedBy,
                            "fileType",   fileType.getValue(),
                            "siteId",     String.valueOf(siteId),
                            "projectId",  String.valueOf(projectId)
                    ))
                    .build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
    );

    log.info("File uploaded to S3 temp. projectId={}, siteId={}, fileType={}, s3Key={}",
            projectId, siteId, fileType.getValue(), s3Key);

    return FileSolarUploadResponse.builder()
            .statusCode("OK")
            .s3Key(s3Key)
            .fileId(fileId)
            .originalName(sanitized)
            .build();
}

    // MOVE 

    @Override
    public String moveTempToPermanent(String tempS3Key, Integer equipmentId) {

        // dari: projects/1/sites/1/equipments/temp/{uuid}/solar_contract_file.pdf
        // ke:   projects/1/sites/1/equipments/{equipmentId}/solar_contract_file.pdf
        String permanentKey = tempS3Key.replaceAll(
                "equipments/temp/[^/]+/",
                "equipments/" + equipmentId + "/"
        );

        // Copy path permanent
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(tempS3Key)
                .destinationBucket(bucketName)
                .destinationKey(permanentKey)
                .build());

        // delate temp
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(tempS3Key)
                .build());

        log.info("S3 file moved. from={}, to={}", tempS3Key, permanentKey);
        return permanentKey;
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
    // ────────────────────────────────────────────────────────────

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "document";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    
}