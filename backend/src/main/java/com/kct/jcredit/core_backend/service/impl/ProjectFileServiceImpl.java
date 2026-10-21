package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.FileVersionSummaryResponse;
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
import com.kct.jcredit.core_backend.exception.FileVersionLimitExceededException;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private static final int MAX_FILE_SIZE_MB = 10;
    private static final int PRESIGN_MINUTES = 15;

    private final ProjectRepository projectRepository;
    private final ProjectFilesRepository projectFilesRepository;
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${app.s3.bucket:gx-project-files-dev}")
    private String bucketName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectFileResponse uploadFile(String projectPublicId, ProjectFileType fileType,
                                          MultipartFile file, String username) {
        Integer projectId = resolveProjectId(projectPublicId);
        validateFile(file, fileType);

        // Lock the project row so concurrent uploads can't both pass the limit check
        projectRepository.findByIdForUpdate(projectId);

        // Check the limit before touching S3, so a rejected upload leaves nothing behind
        long activeCount = projectFilesRepository.countByProjectIdAndFileTypeAndIsActiveTrue(projectId, fileType);
        if (activeCount >= fileType.getVersionLimit()) {
            throw new FileVersionLimitExceededException(fileType, fileType.getVersionLimit());
        }

        int nextVersion = projectFilesRepository.findMaxVersionNo(projectId, fileType) + 1;

        String s3Key = uploadToS3(fileType, file, projectId);
        // S3 is not transactional: if the DB part fails, remove the object we just uploaded
        registerS3CleanupOnRollback(s3Key);

        ProjectFiles entity = ProjectFiles.builder()
                .projectId(projectId)
                .fileType(fileType)
                .versionNo(nextVersion)
                .ownedByKanrisha(fileType.isOwnedByKanrisha())
                .s3Key(s3Key)
                .originalFileName(displayFileName(file.getOriginalFilename()))
                .build();
        entity.setIsActive(true);
        entity.setCreatedBy(username);

        ProjectFiles saved = projectFilesRepository.save(entity);
        log.info("Uploaded {} v{} for project {} -> {}", fileType, nextVersion, projectPublicId, s3Key);

        ProjectFileResponse response = toItemAsResponse(saved);
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(null);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectFileListResponse listFiles(String projectPublicId, ProjectFileType fileTypeFilter) {
        Integer projectId = resolveProjectId(projectPublicId);

        List<ProjectFiles> entities = (fileTypeFilter != null)
                ? projectFilesRepository.findByProjectIdAndFileTypeAndIsActiveTrueOrderByCreatedAtDesc(projectId, fileTypeFilter)
                : projectFilesRepository.findByProjectIdAndIsActiveTrueOrderByCreatedAtDesc(projectId);

        ProjectFileListResponse response = new ProjectFileListResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(null);
        response.setFiles(entities.stream().map(this::toItem).toList());
        response.setVersionSummary(buildVersionSummary(projectId));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectFileDownloadResponse getDownloadUrl(String projectPublicId, Integer fileId) {
        Integer projectId = resolveProjectId(projectPublicId);
        ProjectFiles file = getOwnedFile(projectId, fileId);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(file.getS3Key())
                // Without this the browser saves the file under the S3 key name, losing Japanese names
                .responseContentDisposition(contentDisposition(file.getOriginalFileName()))
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

        // Soft delete: frees an upload slot but keeps the record (and S3 object) for audit
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
                .orElseThrow(() -> new ResourceNotFoundException(ResponseConstants.MSG_PROJECT_NOT_FOUND));
    }

    private ProjectFiles getOwnedFile(Integer projectId, Integer fileId) {
        ProjectFiles file = projectFilesRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException(ResponseConstants.MSG_FILE_NOT_FOUND));
        if (!file.getProjectId().equals(projectId) || Boolean.FALSE.equals(file.getIsActive())) {
            throw new ResourceNotFoundException(ResponseConstants.MSG_FILE_NOT_FOUND);
        }
        return file;
    }

    /** One entry per file type (including types with zero files), so the UI can disable upload up front. */
    private List<FileVersionSummaryResponse> buildVersionSummary(Integer projectId) {
        Map<ProjectFileType, Long> counts = projectFilesRepository.countActiveByFileType(projectId).stream()
        .collect(Collectors.toMap(
                ProjectFilesRepository.FileTypeCount::getFileType,
                ProjectFilesRepository.FileTypeCount::getActiveCount));

        return Arrays.stream(ProjectFileType.values())
                .map(type -> {
                    long count = counts.getOrDefault(type, 0L);
                    return FileVersionSummaryResponse.builder()
                            .fileType(type)
                            .activeCount(count)
                            .limit(type.getVersionLimit())
                            .canUpload(count < type.getVersionLimit())
                            .build();
                })
                .toList();
    }

    private void validateFile(MultipartFile file, ProjectFileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("アップロードするファイルが空です。");
        }
        if (file.getSize() > (long) MAX_FILE_SIZE_MB * 1024 * 1024) {
            double actualSizeMB = file.getSize() / (1024.0 * 1024.0);
            throw new FileTooLargeException(MAX_FILE_SIZE_MB, actualSizeMB);
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (!fileType.accepts(extension, file.getContentType())) {
            throw new UnsupportedFileTypeException(fileType.getAllowedExtensions(), extension);
        }
    }

    /** Lower-cased extension without the dot, or "" when the name has none. */
    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String uploadToS3(ProjectFileType fileType, MultipartFile file, Integer projectId) {
        String fileId = "file_" + UUID.randomUUID().toString().substring(0, 8);
        String cleanName = sanitizeForS3Key(file.getOriginalFilename());
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

    private void registerS3CleanupOnRollback(String s3Key) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteS3Quietly(s3Key);
                }
            }
        });
    }

    /** Cleanup failures must not mask the original error; log and move on. */
    private void deleteS3Quietly(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
            log.info("Removed orphaned S3 object {}", key);
        } catch (Exception e) {
            log.warn("Failed to remove orphaned S3 object {}", key, e);
        }
    }

    /** RFC 5987 encoding so non-ASCII (Japanese) file names survive the download. */
    private String contentDisposition(String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }

    /** Keeps the name readable: only what is unsafe to store or display is stripped. */
    private String displayFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "document";
        }
        String name = originalFilename.replaceAll(".*[/\\\\]", "")
                                      .replaceAll("\\p{Cntrl}", "");
        return name.isBlank() ? "document" : name;
    }

    /** ASCII-safe segment for the S3 key. Never shown to the user. */
    private String sanitizeForS3Key(String originalFilename) {
        return originalFilename != null
                ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_")
                : "document";
    }

    private ProjectFileItem toItem(ProjectFiles entity) {
        return ProjectFileItem.builder()
                .id(entity.getId())
                .fileType(entity.getFileType())
                .versionNo(entity.getVersionNo())
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
        response.setVersionNo(entity.getVersionNo());
        response.setOwnedByKanrisha(entity.getOwnedByKanrisha());
        response.setOriginalFileName(entity.getOriginalFileName());
        response.setUploadedBy(entity.getCreatedBy());
        response.setUploadedAt(entity.getCreatedAt());
        return response;
    }
}