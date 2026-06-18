package com.kct.jcredit.core_backend.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.FileUploadResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.details.FileErrorDetailsResponse;
import com.kct.jcredit.core_backend.dto.response.details.FileInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDataSolarResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.exception.FileTooLargeException;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.MonitoringService;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class MonitoringServiceImpl implements MonitoringService {

    private final ProjectRepository projectRepository;
    private final MonitoringTableRecordSolarRepository monitoringRecordSolarRepository;
    private final ParticipatingUserRepository userRepository;
    private final SiteRepository siteRepository;
    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final S3Client s3Client;
    
    @Value("${app.s3.bucket}")
    private String bucketName;

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_FILE_SIZE_MB = 10;
    
    //太陽光発電の毎日のデータをs3に保存
    @Override
    public MonitoringRecordSolarResponse registerMonitoringRecordSolar(
            String username,
            String companyId,
            RegisterMonitoringRecordSolarRequest request
    ) {
        MonitoringRecordSolarResponse response = new MonitoringRecordSolarResponse();

        try {
            // 1) Fetch user, site, and project
            //ユーザの取得
            ParticipatingUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException(ResponseConstants.MSG_UNEXPEXTED_ERROR));

            // Validate user belongs to the company
            // ユーザが企業に所属していない場合
            if (!user.getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
                return response;
            }

            // 2) Fetch site and project - now company-scoped
            // 入力情報のsiteIdから企業情報を1件取得
            Site site = siteRepository.findById(request.getSiteId()).orElse(null);
            // 企業情報のnullチェック
            if (site == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }

            // Validate site belongs to the company
            //サイトに企業が紐付いていない（データ不整合）or サイトは存在するが、別の企業のもの
            if (site.getCompany() == null || !site.getCompany().getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_IN_COMPANY);
                return response;
            }

            Project project = projectRepository.findById(request.getProjectId()).orElse(null);
            //プロジェクトのnullチェック
            if (project == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
                return response;
            }

            // 3) Checks: User→Site access (role-based), Project company ownership, Site-Project consistency
            //validateResourceOwnershipがfalseだった場合
            if (!validateResourceOwnership(user, site, project)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response;
            }

            // 4) Validate site-project relation exists
            //拠点とプロジェクトが一致しない場合
            if (!siteProjectRelationRepository.existsByProjectAndSite(project, site)) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_NOT_FOUND);
                return response;
            }

            // 5) Validate all 4 mandatory evidence URLs are provided
            //4つの証憑ファイルURLがnulか空文字の場合
            Map<String, String> evidenceErrors = collectMandatoryEvidenceErrors(request);
            if (!evidenceErrors.isEmpty()) {
                response.setStatusCode(ResponseConstants.VALIDATION_ERROR);
                response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
                response.setErrors(evidenceErrors);
                return response;
            }

            // 6) Validate numerical data to prevent NullPointerException
            // elpv,elpvr,elpjcontrolが0未満の場合
            Map<String, String> monitoringDataErrors = collectMonitoringDataErrors(request);
            if (!monitoringDataErrors.isEmpty()) {
                response.setStatusCode(ResponseConstants.VALIDATION_ERROR);
                response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
                response.setErrors(monitoringDataErrors);
                return response;
            }

            // 7) Create and populate DynamoDB entity
            // DynamoDBエンティティを作成してデータを入力する
            EnR002MonitoringRecord entity = new EnR002MonitoringRecord();
            // Changed PK format: Now SITE#PROJECT (company validation still done via JWT + entity validation)
            entity.setId(site.getId() + "#" + project.getId());
            entity.setProjectID(project.getId());
            entity.setSiteID(site.getId());
            entity.setParticipatingUserID(user.getId());
            entity.setTimeUpdated(Instant.now().toString()); // Auto-set with server time in ISO format as SK
            entity.setDateRecorded(request.getDateRecorded());

            // 8) Set evidence URLs (ELpv_evidence, ELpvr_evidence, ELPJ_battery_evidence, ELPJ_control_equipment_spec_files)
            //証拠URLを設定する（ELpv_evidence、ELpvr_evidence、ELPJ_battery_evidence、ELPJ_control_equipment_spec_files）
            entity.setElPvEvidenceUrl(request.getElPvEvidenceUrl());
            entity.setElPvrEvidenceUrl(request.getElPvrEvidenceUrl());
            entity.setElpjBatterySpecificationUrl(request.getElpjBatterySpecificationUrl());
            entity.setElpjControlEvidenceUrl(request.getElpjControlEvidenceUrl());

            // 9) Set monitoring measurements (ELpv, ELpvr, ELPJ_control_value)
            // 監視測定値を設定する（ELpv、ELpvr、ELPJ_control_value）
            entity.setElPv(request.getElPv());
            entity.setElPvr(request.getElPvr());
            entity.setElpjControlValue(request.getElpjControl());

            // 10) Handle optional accuracy field (ELPJ_accuracy)
            // ELPJ計測精度がnullでない場合にentity.setElpjAccuracy上書き
            if (request.getElpjAccuracy() != null) {
                entity.setElpjAccuracy(request.getElpjAccuracy());
            }

            // 11) Handle battery-related data (ELPJ_battery_value)
            // バッテリーが交換されたかを(true)安全に判定
            if (Boolean.TRUE.equals(request.getChangedBattery())) {
                if (request.getElpjBatteryValue() != null) {
                    entity.setElpjBatteryValue(request.getElpjBatteryValue());
                }
            }

            // 12) Set status fields for DynamoDB (with defaults)
            // DynamoDBのステータスフィールドを設定する（デフォルト値あり）
            entity.setIsActive(true);
            entity.setProcessedAt(null);

            // 13) Save to DynamoDB (with PK=projectID#siteID, SK=timeUpdated)
            // DynamoDBに保存（PK=projectID#siteID、SK=timeUpdated）
            monitoringRecordSolarRepository.save(entity);

            // 14) Build response
            // レスポンスの作成
            response.setData(mapEntityToResponse(entity, request.getProjectType()));
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage(ResponseConstants.MSG_MONITORING_RECORD_REGISTERED);
            return response;

        } catch (Exception e) {
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
            return response;
        }
    }

    @Override
    public MonitoringRecordSolarResponse readLastMonitoringRecordSolar(
            String username,
            String companyId,
            Integer projectId,
            Integer siteId
    ) {
        MonitoringRecordSolarResponse response = new MonitoringRecordSolarResponse();

        try {
            // 1) Fetch user, site, and project
            // ユーザ取得
            ParticipatingUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException(ResponseConstants.MSG_UNEXPEXTED_ERROR));

            // Validate user belongs to the company
            // ユーザが指定した会社に所属していない場合
            if (!user.getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
                return response;
            }

            //入力情報のsiteIdから企業情報を1件取得
            Site site = siteRepository.findById(siteId).orElse(null);
            //拠点情報がnullの場合
            if (site == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }

            // Validate site belongs to the company
            //サイトに企業が紐付いていない（データ不整合）or サイトは存在するが、別の企業のもの
            if (site.getCompany() == null || !site.getCompany().getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_IN_COMPANY);
                return response;
            }

            //プロジェクト取得
            Project project = projectRepository.findById(projectId).orElse(null);
            //プロジェクトのnullチェック
            if (project == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
                return response;
            }

            //2) Checks: User→Site access, Project company ownership, Site-Project consistency
            // validateResourceOwnershipがfalseだった場合
            if (!validateResourceOwnership(user, site, project)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response;
            }

            // 3) Validate site-project relation
            //拠点とプロジェクトが一致しない場合
            if (!siteProjectRelationRepository.existsByProjectAndSite(project, site)) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_NOT_FOUND);
                return response;
            }

            // 4) Fetch latest monitoring record from DynamoDB
            // DynamoDBから最新の監視記録を取得する
            String recordId = siteId + "#" + projectId;
            EnR002MonitoringRecord entity = monitoringRecordSolarRepository.findLatest(recordId)
                    .orElse(null);

            //entityがnullの場合
            if (entity == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
                return response;
            }

            // 5) Build response
            // レスポンスの作成
            response.setData(mapEntityToResponse(entity, null));
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage(ResponseConstants.OK);
            return response;

        } catch (Exception e) {
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
            return response;
        }
    }



    //太陽光の日ごとのモニタリングデータをページ分取得する
    @Override
    public MonitoringRecordSolarListResponse readAllMonitoringRecordsSolar(
            String username,
            String companyId,
            Integer projectId,
            Integer siteId,
            Integer page,
            Integer limit,
            String nextPageToken
    ) {
        MonitoringRecordSolarListResponse response = new MonitoringRecordSolarListResponse();

        
        try {
            // Set defaults
            // page がnull or pageが0以下の場合
            if (page == null || page < 1) {
                page = 1;
            }
            // limit がnull or limitが0以下の場合
            if (limit == null || limit <= 0) {
                limit = 50;
            }
            // limit が 101以上の場合
            if (limit > 100) {
                limit = 100; // Max limit
            }

            // 1) Fetch user, site, and project
            //ユーザ取得
            ParticipatingUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException(ResponseConstants.MSG_UNEXPEXTED_ERROR));

            // Validate user belongs to the company
            //ユーザが指定した会社に所属していない場合
            if (!user.getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
                return response;
            }

            //入力情報のsiteIdから企業情報を1件取得
            Site site = siteRepository.findById(siteId).orElse(null);
            //拠点情報がnullの場合
            if (site == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }

            // Validate site belongs to the company
            //サイトに企業が紐付いていない（データ不整合）or サイトは存在するが、別の企業のもの
            if (site.getCompany() == null || !site.getCompany().getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_IN_COMPANY);
                return response;
            }

            //プロジェクト取得
            Project project = projectRepository.findById(projectId).orElse(null);
            //プロジェクトのnullチェック
            if (project == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
                return response;
            }

            //2) Checks: User→Site access, Project company ownership, Site-Project consistency
            // validateResourceOwnershipがfalseだった場合
            if (!validateResourceOwnership(user, site, project)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response;
            }

            // 3) Validate site-project relation
            // 拠点とプロジェクトが一致しない場合
            if (!siteProjectRelationRepository.existsByProjectAndSite(project, site)) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_NOT_FOUND);
                return response;
            }

            // Create pagination object
            //ページネーション作成
            PaginationDetailResponse pagination = new PaginationDetailResponse();
            pagination.setPage(page);
            pagination.setLimit(limit);
            pagination.setNextPageToken(nextPageToken);

            // Fetch data - pagination object is updated by repository
            //データ取得 - ページネーションオブジェクトはリポジトリによって更新されます
            String recordId = siteId + "#" + projectId;
            List<EnR002MonitoringRecord> entities = 
                monitoringRecordSolarRepository.findAllByRecordIdPaginated(recordId, pagination);

            //entitiesが空文字の場合
            if (entities.isEmpty()) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_MONITORING_RECORD_NOT_FOUND);
                return response;
            }

            // // Map to DTOs
            // List<EnR002MonitoringRecord> items = entities.stream()
            //         .map(entity -> mapEntityToResponse(entity, null))
            //         .collect(Collectors.toList());

            //レスポンスの作成
            response.setItems(entities);
            response.setPagination(pagination);  // Already updated by repository
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage(ResponseConstants.OK);
            return response;

        } catch (Exception e) {
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
            return response;
        }
    }



    private FileUploadResponse uploadMonitoringFileSolar(
            String username,
            String companyId,
            Integer projectId,
            Integer siteId,
            String fileType,
            String description,
            MultipartFile file
    ) {
        FileUploadResponse response = new FileUploadResponse();

        try {
            // 1) Validate file
            validateFileOrThrow(file, fileType);

            // 2) Fetch and validate user, site, project
            ParticipatingUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException(ResponseConstants.MSG_UNEXPEXTED_ERROR));

            // Validate user belongs to the company
            if (!user.getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
                return response;
            }

            Site site = siteRepository.findById(siteId).orElse(null);
            if (site == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }

            // Validate site belongs to the company
            if (site.getCompany() == null) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_SITE_HAS_NO_COMPANY);
                return response;
            }
            if (!site.getCompany().getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_IN_COMPANY);
                return response;
            }

            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
                return response;
            }

            // 3) Checks: User→Site access (role-based)
            if (!validateResourceOwnership(user, site, project)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response;
            }

            // 4) Validate site-project relation
            if (!siteProjectRelationRepository.existsByProjectAndSite(project, site)) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_PROJECT_RELATION_NOT_FOUND);
                return response;
            }

            // 5) Generate unique file ID and S3 key
            String fileId = "file_" + UUID.randomUUID().toString().substring(0, 6);
            String safeFilename = sanitizeFileName(file.getOriginalFilename());
            String s3Key = String.format("companies/%s/projects/%d/sites/%d/%s/%s_%s",
                    companyId, projectId, siteId, fileType, fileId, safeFilename);

            // SECURITY FIX: Use server-side determined Content-Type (prevent MIME-Type Spoofing)
            String safeExtension = getFileExtension(file.getOriginalFilename()).toLowerCase();
            String safeContentType = getSafeContentType(safeExtension);

            // 6) Upload to S3 (with XSS mitigation via Content-Disposition: attachment)
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .contentType(safeContentType)
                            .contentDisposition("attachment; filename=\"" + safeFilename + "\"")
                            .metadata(Map.of(
                                    "description", description != null ? description : "",
                                    "fileId", fileId,
                                    "uploadedBy", username,
                                    "fileType", fileType,
                                    "companyId", companyId,
                                    "projectId", projectId.toString(),
                                    "siteId", siteId.toString()
                            ))
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            // 7) Build success response with S3 key as URL (actual access via pre-signed URL)
            FileInfoResponse fileInfo = FileInfoResponse.builder()
                    .id(fileId)
                    .fileType(fileType)
                    .fileName(file.getOriginalFilename())
                    .contentType(safeContentType)
                    .fileSize(file.getSize())
                    .url(s3Key)
                    .description(description)
                    .uploadedAt(LocalDate.now().toString())
                    .build();

            response.setStatusCode(ResponseConstants.OK);
            response.setFile(fileInfo);
            return response;

        } catch (FileTooLargeException e) {
            response.setStatusCode(ResponseConstants.FILE_TOO_LARGE);
            response.setMessage(ResponseConstants.MSG_FILE_TOO_LARGE);
            response.setDetails(FileErrorDetailsResponse.builder()
                    .maxSizeMB(MAX_FILE_SIZE_MB)
                    .actualSizeMB(e.getActualSizeMB())
                    .build());
            return response;

        } catch (UnsupportedFileTypeException e) {
            response.setStatusCode(ResponseConstants.UNSUPPORTED_FILE_TYPE);
            response.setMessage(ResponseConstants.MSG_UNSUPPORTED_FILE_TYPE);
            response.setDetails(FileErrorDetailsResponse.builder()
                    .allowedTypes(e.getAllowedTypes())
                    .receivedType(e.getReceivedType())
                    .build());
            return response;

        } catch (Exception e) {
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
            return response;
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Requirement 6.2 & Update 1: Validate that all 4 mandatory evidence URLs are provided
     */
    private Map<String, String> collectMandatoryEvidenceErrors(RegisterMonitoringRecordSolarRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (isBlank(request.getElPvEvidenceUrl())) {
            errors.put("ELpv_evidence", ResponseConstants.MSG_ELPV_EVIDENCE_REQUIRED);
        }
        if (isBlank(request.getElPvrEvidenceUrl())) {
            errors.put("ELpvr_evidence", ResponseConstants.MSG_ELPVR_EVIDENCE_REQUIRED);
        }
        if (isBlank(request.getElpjBatterySpecificationUrl())) {
            errors.put("ELPJ_battery_evidence", ResponseConstants.MSG_ELPJ_BATTERY_EVIDENCE_REQUIRED);
        }
        if (isBlank(request.getElpjControlEvidenceUrl())) {
            errors.put("ELPJ_control_equipment_spec_files", ResponseConstants.MSG_ELPJ_CONTROL_EVIDENCE_REQUIRED);
        }

        return errors;
    }

    /**
     * Requirement 6.1: Validate monitoring data to prevent NullPointerException
     */
    private Map<String, String> collectMonitoringDataErrors(RegisterMonitoringRecordSolarRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (request.getElPv() == null || request.getElPv().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("ELpv_value", ResponseConstants.MSG_ELPV_VALUE_MIN);
        }
        if (request.getElPvr() == null || request.getElPvr().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("ELpvr_value", ResponseConstants.MSG_ELPVR_VALUE_MIN);
        }
        if (request.getElpjControl() == null || request.getElpjControl().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("ELPJ_control_value", ResponseConstants.MSG_ELPJ_CONTROL_VALUE_MIN);
        }
        if (Boolean.TRUE.equals(request.getChangedBattery()) && request.getElpjBatteryValue() == null) {
            errors.put("ELPJ_battery_value", ResponseConstants.MSG_ELPJ_BATTERY_VALUE_REQUIRED_WHEN_CHANGED);
        }

        return errors;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Validate file size and type, throw exceptions if invalid
     */
    private void validateFileOrThrow(MultipartFile file, String fileType) {
        // Check size (10MB max)
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            double actualSizeMB = file.getSize() / (1024.0 * 1024.0);
            throw new FileTooLargeException(MAX_FILE_SIZE_MB, actualSizeMB);
        }

        // Validate file type and extract file extension
        Map<String, List<String>> fileTypeToAllowedExtensions = Map.of(
                "ELPV_EVIDENCE", Arrays.asList("pdf", "csv"),
                "ELPVR_EVIDENCE", Arrays.asList("pdf", "png", "jpeg", "jpg"),
                "ELPJ_BATTERY_SPECIFICATION", Arrays.asList("pdf", "png", "jpeg", "jpg"),
                "ELPJ_CONTROL_EVIDENCE", Arrays.asList("pdf", "png", "jpeg", "jpg")
        );

        List<String> allowedExtensions = fileTypeToAllowedExtensions.get(fileType);
        if (allowedExtensions == null) {
            throw new UnsupportedFileTypeException(
                    Arrays.asList("ELPV_EVIDENCE", "ELPVR_EVIDENCE", "ELPJ_BATTERY_SPECIFICATION", "ELPJ_CONTROL_EVIDENCE"),
                    fileType
            );
        }

        // Get file extension
        String fileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(fileName).toLowerCase();

        if (!allowedExtensions.contains(fileExtension)) {
            throw new UnsupportedFileTypeException(allowedExtensions, fileExtension);
        }
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    /**
     * Sanitize filename for S3 storage
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return UUID.randomUUID().toString();
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * SECURITY FIX: Server-side Content-Type mapping untuk mencegah MIME-Type Spoofing
     * Tidak mempercayai Content-Type dari client. Menentukan MIME type berdasarkan ekstensi file
     * yang sudah divalidasi sebelumnya.
     * 
     * @param extension File extension yang sudah divalidasi (lowercase)
     * @return Safe MIME type untuk S3 upload
     */
    private String getSafeContentType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "application/octet-stream";
        }
        
        switch (extension.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "csv":
                return "text/csv";
            default:
                return "application/octet-stream"; // Safe fallback
        }
    }

    /**
     * Strict IDOR validation - 2 layer check
     * Layer 1: User has access to Site (company + role-based)
     * Layer 2: Site-Project relation exists (checked separately in controller)
     * Note: Operator company is global and can access all projects.
     * Participating company users only need Site access + Site-Project relation.
     */
    private boolean validateResourceOwnership(ParticipatingUser user, Site site, Project project) {
        try {
            // Null safety
            if (user == null || site == null || project == null) {
                return false;
            }

            // Layer 1: Validate user has access to this specific site
            // This checks: user's company == site's company, and role-based access
            if (!hasAccessToSite(user, site)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Layer 1: Validate user has permission to access the site
     * MASTER role: can access ALL sites in company (even if not personally assigned)
     * USER role: can ONLY access their assigned site
     */
    private boolean hasAccessToSite(ParticipatingUser user, Site site) {
        try {
            // Null safety checks
            if (user == null || site == null) {
                return false;
            }

            // Check if user has companyId set
            if (user.getCompanyId() == null) {
                return false;
            }

            // Check if site has company information
            if (site.getCompany() == null || site.getCompany().getCompanyId() == null) {
                return false;
            }

            // CRITICAL: Check if user's company ID matches site's company ID
            if (!site.getCompany().getCompanyId().equals(user.getCompanyId())) {
                return false;
            }

            // Role-based access control
            if ("master".equals(user.getRole())) {
                // MASTER role: can access ALL sites in this company (no site assignment needed)
                return true;
            } else if ("user".equals(user.getRole())) {
                // USER role (restricted): ONLY access their assigned site
                if (user.getSite() == null || user.getSite().getId() == null) {
                    return false;
                }
                // CRITICAL: Site IDs must match exactly
                return user.getSite().getId().equals(site.getId());
            }
            
            // Unknown role - deny access
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Map DynamoDB entity to response DTO 
     */
    private MonitoringDataSolarResponse mapEntityToResponse(EnR002MonitoringRecord entity, String projectType) {
        return MonitoringDataSolarResponse.builder()
                .projectId(entity.getProjectID())
                .siteId(entity.getSiteID())
                .userId(entity.getParticipatingUserID())
                .projectType(projectType != null ? projectType : "ENR002")
                .timeUpdated(entity.getTimeUpdated())
                .dateRecorded(entity.getDateRecorded())
                .elPv(entity.getElPv())
                .elPvr(entity.getElPvr())
                // CRITICAL: Check if battery value exists to set changedBattery boolean
                .changedBattery(entity.getElpjBatteryValue() != null)
                .elpjBatteryValue(entity.getElpjBatteryValue())
                .elpjControl(entity.getElpjControlValue())
                .elpjAccuracy(entity.getElpjAccuracy())
                .build();
    }

    
    // ===== 4 SPECIFIC EVIDENCE UPLOAD APIs =====

    @Override
    public FileUploadResponse uploadElpvEvidence(
            String username,
            String companyId,
            Integer projectId,
            Integer siteId,
            String description,
            MultipartFile file
    ) {
        return uploadMonitoringFileSolar(username, companyId, projectId, siteId, "ELPV_EVIDENCE", description, file);
    }

    @Override
    public FileUploadResponse uploadElpvrEvidence(
            String username,
            String companyId,
            Integer projectId,
            Integer siteId,
            String description,
            MultipartFile file
    ) {
        return uploadMonitoringFileSolar(username, companyId, projectId, siteId, "ELPVR_EVIDENCE", description, file);
    }

    @Override
    public FileUploadResponse uploadElpjBatterySpecificationEvidence(
            String username,
            String companyId,
            Integer projectId,
            Integer siteId,
            String description,
            MultipartFile file
    ) {
        return uploadMonitoringFileSolar(username, companyId, projectId, siteId, "ELPJ_BATTERY_SPECIFICATION", description, file);
    }

    @Override
    public FileUploadResponse uploadElpjControlEvidence(
            String username,
            String companyId,
            Integer projectId,
            Integer siteId,
            String description,
            MultipartFile file
    ) {
        return uploadMonitoringFileSolar(username, companyId, projectId, siteId, "ELPJ_CONTROL_EVIDENCE", description, file);
    }

}
