package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.FileSolarUploadResponse;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import jakarta.validation.constraints.NotNull;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import com.kct.jcredit.core_backend.dto.request.FilePreRegisSolarRequest;


public interface FileUploadSolarService {

    FileSolarUploadResponse uploadFile(Integer siteId,
                                       Integer projectId,
                                       EquipmentFileType fileType,
                                       String uploadedBy,
                                       MultipartFile file) throws IOException;

    String moveTempToPermanent(String tempS3Key, Integer equipmentId);
}