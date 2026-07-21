package com.kct.jcredit.core_backend.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.dto.request.ProjectUploadDocumentRequest;
import com.kct.jcredit.core_backend.dto.response.ProjectUploadDocumentResponse;
import com.kct.jcredit.core_backend.entity.ProjectDocumentStatus;
import com.kct.jcredit.core_backend.entity.ProjectFiles;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectDocumentStatusRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectFilesRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.ProjectUploadDocumentService;

import com.kct.jcredit.core_backend.exception.BusinessRuleViolationException;
import com.kct.jcredit.core_backend.exception.FileTooLargeException;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectUploadDocumentServiceImpl implements ProjectUploadDocumentService {

    private final ProjectRepository projectRepository; 
    private final ProjectFilesRepository projectFilesRepository;
    private final ProjectDocumentStatusRepository projectDocumentStatusRepository; 
    private final S3Client s3Client;

    @Value("${app.S3_BUCKET:}") 
    private String BUCKET_NAME;

    @Override
    @Transactional(rollbackFor = Exception.class) 
    public ProjectUploadDocumentResponse processUpload(String projectId, 
                                                       MultipartFile fileRegistrationCert, 
                                                       MultipartFile fileValidationReport, 
                                                       MultipartFile fileVerificationReport, 
                                                       String username) { 
    
        Integer projIdInt = Integer.valueOf(projectId);
        
        projectRepository.findById(projIdInt)
                .orElseThrow(() -> new RuntimeException("エラー: プロジェクトが見つかりません (Project Not Found)"));

        validateUploadFiles(fileRegistrationCert, fileValidationReport, fileVerificationReport);

        ProjectFiles projectFiles = projectFilesRepository.findById(projIdInt)
                .orElseGet(() -> {
                    ProjectFiles newFiles = new ProjectFiles();
                    newFiles.setId(projIdInt); 
                    return newFiles;
                });

        try {
            // 1. Upload Registration Cert
            if (fileRegistrationCert != null && !fileRegistrationCert.isEmpty()) {
                deleteFromS3IfExists(projectFiles.getCertificateRegistration()); 
                String s3Key = uploadToS3(projIdInt, "cert_registration", fileRegistrationCert, username);
                projectFiles.setCertificateRegistration(s3Key);
            }
            
            // 2. Upload Validation Report
            if (fileValidationReport != null && !fileValidationReport.isEmpty()) {
                deleteFromS3IfExists(projectFiles.getReportValidation()); 
                String s3Key = uploadToS3(projIdInt, "report_validation", fileValidationReport, username);
                projectFiles.setReportValidation(s3Key);
            }
            
            // 3. Upload Verification Report
            if (fileVerificationReport != null && !fileVerificationReport.isEmpty()) {
                deleteFromS3IfExists(projectFiles.getReportVerification()); 
                String s3Key = uploadToS3(projIdInt, "report_verification", fileVerificationReport, username);
                projectFiles.setReportVerification(s3Key);
            }
        } catch (IOException e) {
            log.error("S3アップロードに失敗しました (Gagal upload ke S3)", e);
            throw new RuntimeException("S3アップロードに失敗しました", e);
        }

        projectFilesRepository.save(projectFiles);

        ProjectUploadDocumentResponse response = new ProjectUploadDocumentResponse();
        response.setStatusCode("200");
        response.setMessage("ファイルのアップロードが完了しました (Upload File Selesai)");
        return response;
    }

    private void validateUploadFiles(MultipartFile... files) {
        List<String> allowedTypes = List.of("application/pdf");
        int maxSizeMB = 10;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new BusinessRuleViolationException("アップロードするファイルが空です。");
            }
            if (file.getSize() > (long) maxSizeMB * 1024 * 1024) {
                double actualSizeMB = file.getSize() / (1024.0 * 1024.0);
                throw new FileTooLargeException(maxSizeMB, actualSizeMB);
            }
            String contentType = file.getContentType();
            if (contentType == null || !allowedTypes.contains(contentType)) {
                throw new UnsupportedFileTypeException(allowedTypes, contentType);
            }
        }
    }

    private String uploadToS3(Integer projectId, String fileType, MultipartFile file, String username) throws IOException {
        String fileId = "file_" + UUID.randomUUID().toString().substring(0, 6);
        
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.-]", "_") : "document.pdf";
        String s3Key = String.format("projects/%d/solar/%s/%s_%s", projectId, fileType, fileId, originalFilename);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .metadata(Map.of(
                                "fileId", fileId,
                                "uploadedBy", username,
                                "fileType", fileType
                        ))
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        log.info("Successfully uploaded file to S3: {}", s3Key);
        return s3Key; 
    }

    private void deleteFromS3IfExists(String existingS3Key) {
        if (existingS3Key != null && !existingS3Key.isBlank()) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(BUCKET_NAME)
                        .key(existingS3Key)
                        .build());
                log.info("Deleted old file from S3: {}", existingS3Key);
            } catch (Exception e) {
                log.warn("Failed to delete old file from S3 (Orphan file alert): {}", existingS3Key, e);
            }
        }
    }

    @Override
    public ProjectUploadDocumentResponse getDocumentsByProjectId(String projectId) {
        ProjectUploadDocumentResponse response = new ProjectUploadDocumentResponse();
        Integer projIdInt = Integer.valueOf(projectId);

        projectFilesRepository.findById(projIdInt).ifPresent(files -> {
            response.setRegistrationCertUrl(files.getCertificateRegistration());
            response.setValidationReportUrl(files.getReportValidation());
            response.setVerificationReportUrl(files.getReportVerification());
        });

        projectDocumentStatusRepository.findById(projectId).ifPresent(status -> {
            response.setIsRegistrationCertReceived(status.getIsRegistrationCertReceived());
            response.setIsValidationReportReceived(status.getIsValidationReportReceived());
            response.setIsVerificationReportReceived(status.getIsVerificationReportReceived());
        });

        response.setStatusCode("200");
        response.setMessage("データ取得完了 (Get Data Berhasil)");
        
        return response;
    }

    @Override
    public ProjectUploadDocumentResponse updateStatus(String projectId, ProjectUploadDocumentRequest request) {
        ProjectDocumentStatus status = projectDocumentStatusRepository.findById(projectId)
                .orElseGet(() -> {
                    ProjectDocumentStatus newStatus = new ProjectDocumentStatus();
                    newStatus.setProjectId(projectId);
                    return newStatus;
                });

        if (request.getIsRegistrationCertReceived() != null) {
            status.setIsRegistrationCertReceived(request.getIsRegistrationCertReceived());
        }
        if (request.getIsValidationReportReceived() != null) {
            status.setIsValidationReportReceived(request.getIsValidationReportReceived());
        }
        if (request.getIsVerificationReportReceived() != null) {
            status.setIsVerificationReportReceived(request.getIsVerificationReportReceived());
        }

        projectDocumentStatusRepository.save(status);

        ProjectUploadDocumentResponse response = new ProjectUploadDocumentResponse();
        response.setStatusCode("200");  
        return response;
    }
}