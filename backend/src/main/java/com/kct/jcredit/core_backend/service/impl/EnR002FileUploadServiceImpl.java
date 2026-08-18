package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.service.storage.S3FileStorage;
import com.kct.jcredit.core_backend.dto.response.EnR002FileResponse;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.Map;

/**
 * Equipment evidence files.
 *
 * <p>The S3 mechanics live in {@link S3FileStorage}; what stays here is the key
 * layout, which is deliberately unchanged — existing objects and
 * {@code extractFileNameFroms3Key} depend on these exact shapes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnR002FileUploadServiceImpl implements EnR002FileUploadService {

    private static final String TEMP_KEY_FORMAT = S3FileStorage.TEMP_ROOT + "equipments/%s/%s_%s";
    private static final String TEMP_KEY_PREFIX_PATTERN =
            "^" + S3FileStorage.TEMP_ROOT + "equipments/[^/]+/";
    private static final String PERMANENT_KEY_FORMAT = "sites/%s/equipments/%d/%s";

    private final S3FileStorage storage;

    // UPLOAD

    @Override
    public EnR002FileResponse uploadFile(String siteId,
                                                EquipmentFileType fileType,
                                                String uploadedBy,
                                                MultipartFile file) {
        storage.validate(file);

        String fileId    = UUID.randomUUID().toString();
        String sanitized = S3FileStorage.sanitizeFileName(file.getOriginalFilename());

        String s3Key = String.format(TEMP_KEY_FORMAT, fileId, fileType.getValue(), sanitized);

        storage.put(s3Key, file, Map.of(
                "fileId",     fileId,
                "uploadedBy", uploadedBy,
                "fileType",   fileType.getValue(),
                "siteId",     siteId));

        log.info("File uploaded to S3 tmp. siteId={}, fileType={}, s3Key={}",
                 siteId, fileType.getValue(), s3Key);

        return EnR002FileResponse.builder()
                .statusCode("OK")
                .s3Key(s3Key)
                .fileId(fileId)
                .originalName(sanitized)
                .build();
    }

    // MOVE

    @Override
    public String buildPermanentKey(String tempS3Key,
                                     String siteId,
                                     Integer equipmentId) {

        String fileNamePart = tempS3Key.replaceAll(TEMP_KEY_PREFIX_PATTERN, "");

        return String.format(PERMANENT_KEY_FORMAT, siteId, equipmentId, fileNamePart);
    }

    @Override
    public void moveToPermanent(String tempS3Key, String permanentKey) {
        storage.promote(tempS3Key, permanentKey);
    }
}
