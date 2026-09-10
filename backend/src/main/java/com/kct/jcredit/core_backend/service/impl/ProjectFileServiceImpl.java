package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.ProjectFileDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectFileItem;
import com.kct.jcredit.core_backend.dto.response.ProjectFileListResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectFileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.ProjectFiles;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.exception.BusinessRuleViolationException;
import com.kct.jcredit.core_backend.exception.FileTooLargeException;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;
import com.kct.jcredit.core_backend.repository.postgres.ProjectFilesRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.ProjectFileService;
import com.kct.jcredit.core_backend.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    // Widened from the old PDF-only allow list - the actual J-Credit
    // application packages (project plan, monitoring report) are .xlsx.
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );
    private static final int MAX_FILE_SIZE_MB = 10;
    private static final int PRESIGN_MINUTES = 15;

    private final ProjectRepository projectRepository;
    private final ProjectFilesRepository projectFilesRepository;
    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket:}")
    private String bucketName;

    private final S3Client s3Client;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectFileResponse uploadFile(String projectPublicId, ProjectFileType fileType, MultipartFile file, String username) {
        Integer projectId = resolveProjectId(projectPublicId);
        validateFile(file);

        String s3Key = uploadToS3(username, fileType, file, projectId);

        ProjectFiles entity = ProjectFiles.builder()
                .projectId(projectId)
                .fileType(fileType)
                .ownedByKanrisha(fileType.isOwnedByKanrisha())
                .s3Key(s3Key)
                .originalFileName(sanitizeFileName(file.getOriginalFilename()))
                .build();
        entity.setIsActive(true);
        entity.setCreatedBy(username);

        ProjectFiles saved = projectFilesRepository.save(entity);
        log.info("Uploaded {} for project {} -> {}", fileType, projectPublicId, s3Key);

        ProjectFileResponse response = toItemAsResponse(saved);
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(null);
        return response;
    }

    @Override
    public ProjectFileListResponse listFiles(String projectPublicId, ProjectFileType fileTypeFilter) {
        Integer projectId = resolveProjectId(projectPublicId);

        List<ProjectFiles> entities = (fileTypeFilter != null)
                ? projectFilesRepository.findByProjectIdAndFileTypeAndIsActiveTrueOrderByCreatedAtDesc(projectId, fileTypeFilter)
                : projectFilesRepository.findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(projectId);

        ProjectFileListResponse response = new ProjectFileListResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(null);
        response.setFiles(entities.stream().map(this::toItem).toList());
        return response;
    }

    @Override
    public ProjectFileDownloadResponse getDownloadUrl(String projectPublicId, Integer fileId) {
        Integer projectId = resolveProjectId(projectPublicId);
        ProjectFiles file = getOwnedFile(projectId, fileId);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(file.getS3Key())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(PRESIGN_MINUTES))
                .getObjectRequest(getObjectRequest)
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();

        ProjectFileDownloadResponse response = new ProjectFileDownloadResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(null);
        response.setDownloadUrl(url);
        response.setOriginalFileName(file.getOriginalFileName());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse deleteFile(String projectPublicId, Integer fileId, String username) {
        Integer projectId = resolveProjectId(projectPublicId);
        ProjectFiles file = getOwnedFile(projectId, fileId);

        file.setIsActive(false);
        file.setDeletedAt(LocalDateTime.now());
        file.setDeletedBy(username);
        projectFilesRepository.save(file);

        BaseResponse response = new BaseResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(ResponseConstants.MSG_FILE_DELETED);
        return response;
    }


    // --- helpers ---

    private Integer resolveProjectId(String projectPublicId) {
        return projectRepository.findByPublicIdWithFallback(projectPublicId)
                .map(Project::getId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseConstants.NOT_FOUND));
    }

    private ProjectFiles getOwnedFile(Integer projectId, Integer fileId) {
        ProjectFiles file = projectFilesRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseConstants.MSG_FILE_NOT_FOUND));
        if (!file.getProjectId().equals(projectId) || Boolean.FALSE.equals(file.getIsActive())) {
            throw new ResourceNotFoundException(ResponseConstants.MSG_FILE_NOT_FOUND);
        }
        return file;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("アップロードするファイルが空です。");
        }
        if (file.getSize() > (long) MAX_FILE_SIZE_MB * 1024 * 1024) {
            double actualSizeMB = file.getSize() / (1024.0 * 1024.0);
            throw new FileTooLargeException(MAX_FILE_SIZE_MB, actualSizeMB);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new UnsupportedFileTypeException(ALLOWED_CONTENT_TYPES, contentType);
        }
    }

    private String uploadToS3(String username, ProjectFileType fileType, MultipartFile file, Integer projectId) {
        String fileId = "file_" + UUID.randomUUID().toString().substring(0, 8);
        String cleanName = sanitizeFileName(file.getOriginalFilename());
        // matches the existing "participation/{id}/{id}/{id}/name.pdf" style —
        // flat numeric path segments, no extra "review-documents" wrapper
        String s3Key = String.format("project/%d/files/%s/%s_%s",
                projectId, fileType.name().toLowerCase(), fileId, cleanName);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            log.error("S3アップロードに失敗しました (Failed to upload to S3)", e);
            throw new RuntimeException("S3アップロードに失敗しました", e);
        }
        return s3Key;
    }

    private String sanitizeFileName(String originalFilename) {
        return originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_")
                : "document";
    }

    // private ProjectFileResponse toResponse(ProjectFiles entity) {
    // return ProjectFileResponse.builder()
    //         .id(entity.getId())
    //         .fileType(entity.getFileType())
    //         .ownedByKanrisha(entity.getOwnedByKanrisha())
    //         .originalFileName(entity.getOriginalFileName())
    //         .uploadedBy(entity.getCreatedBy())
    //         .uploadedAt(entity.getCreatedAt())
    //         .build();
    // }

    private ProjectFileItem toItem(ProjectFiles entity) {
    return ProjectFileItem.builder()
            .id(entity.getId())
            .fileType(entity.getFileType())
            .ownedByKanrisha(entity.getOwnedByKanrisha())
            .originalFileName(entity.getOriginalFileName())
            .uploadedBy(entity.getCreatedBy())
            .uploadedAt(entity.getCreatedAt())
            .build();
}

    private ProjectFileResponse toItemAsResponse(ProjectFiles entity) {
        ProjectFileResponse response = new ProjectFileResponse();
        response.setId(entity.getId());
        response.setFileType(entity.getFileType());
        response.setOwnedByKanrisha(entity.getOwnedByKanrisha());
        response.setOriginalFileName(entity.getOriginalFileName());
        response.setUploadedBy(entity.getCreatedBy());
        response.setUploadedAt(entity.getCreatedAt());
        return response;
    }
}