package com.kct.jcredit.core_backend.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
import com.kct.jcredit.core_backend.dto.response.MonitoringEvidenceItemResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringEvidenceItemsResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarListResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.FileErrorDetailsResponse;
import com.kct.jcredit.core_backend.dto.response.details.FileInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.MonitoringDataSolarResponse;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.enums.EvidenceRequirement;
import com.kct.jcredit.core_backend.enums.MonitoringEvidenceField;
import com.kct.jcredit.core_backend.enums.EvidenceType;
import com.kct.jcredit.core_backend.exception.FileTooLargeException;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.MonitoringService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringServiceImpl implements MonitoringService {

    private final ProjectRepository projectRepository;
    private final MonitoringTableRecordSolarRepository monitoringRecordSolarRepository;
    private final ParticipatingUserRepository userRepository;
    private final SiteRepository siteRepository;
    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final EnR002EquipmentRepository equipmentRepository;
    private final S3Client s3Client;
    
    @Value("${app.s3.bucket:}")
    private String bucketName;

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_FILE_SIZE_MB = 10;
    private static final BigDecimal MAX_PERCENT_VALUE = new BigDecimal("100.0");
    private static final ZoneId JAPAN_ZONE = ZoneId.of("Asia/Tokyo");
    private static final String REASON_NO_BATTERY = "蓄電池なし";
    private static final String FALLBACK_PCS_DEFAULT = "未提出の場合はデフォルト値（発電電力量×0.02kWh/kWh）で算定";
    private static final String METHODOLOGY_NAME = "EN-R";
    private static final String METHODOLOGY_NUMBER = "002";
    private static final String METHODOLOGY_VERSION = "3.7";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_NOT_SUBMITTED = "NOT_SUBMITTED";
    
    //太陽光発電の毎日のデータをs3に保存
    @Override
    public MonitoringRecordSolarResponse registerMonitoringRecordSolar(
            String username,
            String companyId,
            RegisterMonitoringRecordSolarRequest request
    ) {
        MonitoringRecordSolarResponse response = new MonitoringRecordSolarResponse();

        try {
            MonitoringValidationResult validation = validateMonitoringRecordRequest(username, companyId, request);
            if (!validation.isValid()) {
                copyBaseResponse(response, validation.getResponse());
                return response;
            }

            // 1) Fetch user, site, and project
            //ユーザの取得
            ParticipatingUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException(ResponseConstants.MSG_UNEXPEXTED_ERROR));

            // Validate user belongs to the company
            // ユーザが企業に所属していない場合
            if (user.getCompany() == null || !companyId.equals(user.getCompany().getCompanyId())) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
                return response;
            }

            // 2) Fetch site and project - now company-scoped
            // 入力情報のsiteIdから企業情報を1件取得
            Site site = siteRepository.findByPublicIdWithFallback(request.getSiteId()).orElse(null);
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

            Project project = projectRepository.findByPublicIdWithFallback(request.getProjectId()).orElse(null);
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

            // 4.5) Validate equipment exists and belongs to the resolved site
            // 設備が存在し、対象拠点に属していることを確認する
            EnR002Equipment equipment = equipmentRepository.findById(request.getEquipmentId()).orElse(null);
            if (equipment == null || equipment.getSite() == null || !equipment.getSite().getId().equals(site.getId())) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND);
                return response;
            }

            // 5) Validate all 4 mandatory evidence files are provided
            //4つの証憑ファイルがnullか空文字の場合
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

            // 6.5) Validate battery fields against equipment.hasBattery
            // 設備のhasBatteryに基づきバッテリー関連項目を検証する
            Map<String, String> batteryEligibilityErrors = collectBatteryEligibilityErrors(request, equipment);
            if (!batteryEligibilityErrors.isEmpty()) {
                response.setStatusCode(ResponseConstants.VALIDATION_ERROR);
                response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
                response.setErrors(batteryEligibilityErrors);
                return response;
            }

            // 7) Create and populate DynamoDB entity
            // DynamoDBエンティティを作成してデータを入力する
            EnR002MonitoringRecord entity = new EnR002MonitoringRecord();
            // PK format: sitePublicId#projectPublicId (company validation still done via JWT + entity validation)
            entity.setId(site.getPublicId() + "#" + project.getPublicId());
            entity.setProjectID(project.getId());
            entity.setSiteID(site.getId());
            entity.setEquipmentID(equipment.getId());
            entity.setParticipatingUserID(user.getId());
            entity.setTimeUpdated(Instant.now().toString()); // Auto-set with server time in ISO format as SK
            entity.setDateRecorded(request.getDateRecorded());

            // 8) Set evidence URLs (elpv_evidence, elpvr_evidence, elpj_battery_evidence, elpj_control_evidence)
            //証拠URLを設定する（elpv_evidence、elpvr_evidence、elpj_battery_evidence、elpj_control_evidence）
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
            if (Boolean.TRUE.equals(equipment.getHasBattery())) {
                entity.setElpjBatteryValue(request.getElpjBatteryValue());
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
    public BaseResponse validateMonitoringRecordSolar(
            String username,
            String companyId,
            RegisterMonitoringRecordSolarRequest request
    ) {
        try {
            MonitoringValidationResult validation = validateMonitoringRecordRequest(username, companyId, request);
            return validation.getResponse();
        } catch (Exception e) {
            return BaseResponse.error(ResponseConstants.INTERNAL_SERVER_ERROR, ResponseConstants.MSG_INTERNAL_ERROR);
        }
    }

    @Override
    public MonitoringRecordSolarResponse readLastMonitoringRecordSolar(
            String username,
            String companyId,
            String projectId,
            String siteId
    ) {
        MonitoringRecordSolarResponse response = new MonitoringRecordSolarResponse();

        try {
            // 1) Fetch user, site, and project
            // ユーザ取得
            ParticipatingUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException(ResponseConstants.MSG_UNEXPEXTED_ERROR));

            // Validate user belongs to the company
            // ユーザが指定した会社に所属していない場合
            if (user.getCompany() == null || !companyId.equals(user.getCompany().getCompanyId())) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
                return response;
            }

            //入力情報のsiteIdから企業情報を1件取得
            Site site = siteRepository.findByPublicIdWithFallback(siteId).orElse(null);
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
            Project project = projectRepository.findByPublicIdWithFallback(projectId).orElse(null);
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
            String recordId = site.getPublicId() + "#" + project.getPublicId();
            EnR002MonitoringRecord entity = monitoringRecordSolarRepository.findLatest(recordId)
                    .orElse(null);

            // 5) Build response
            // レスポンスの作成
            // 未登録（entityがnull）の場合はエラーではなく、dataなしのOKを返す
            if (entity != null) {
                response.setData(mapEntityToResponse(entity, null));
            }
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
            Integer page,
            Integer limit,
            String projectId,
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

            // Get list of site ids related to the project through site-project relation
            Project project = projectRepository.findByPublicIdWithFallback(projectId).orElse(null);
            if (project == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
                return response;
            }
            // Build target PKs for DynamoDB query (format: sitePublicId#projectPublicId)
            List<Integer> siteIds = siteProjectRelationRepository.findSiteIdsByProjectId(project.getId());
            List<String> targetPks = new java.util.ArrayList<>();
            if (siteIds != null) {
                for (Integer siteId : siteIds) {
                    siteRepository.findById(siteId)
                        .ifPresent(s -> targetPks.add(s.getPublicId() + "#" + project.getPublicId()));
                }
            }

            // Create pagination object
            //ページネーション作成
            PaginationDetailResponse pagination = new PaginationDetailResponse();
            pagination.setPage(page);
            pagination.setLimit(limit);
            pagination.setNextPageToken(nextPageToken);

            //Fetch data - pagination object is updated by repository
            //データ取得 - ページネーションオブジェクトはリポジトリによって更新されます
            List<EnR002MonitoringRecord> entities = 
                monitoringRecordSolarRepository.findAllByProjectIdPaginated(targetPks, pagination);

            // Map to DTOs
            // 0件の場合はエラーではなく、空のitemsでOKを返す
            List<MonitoringDataSolarResponse> items = entities.stream()
                    .map(entity -> mapEntityToResponse(entity, null))
                    .collect(java.util.stream.Collectors.toList());

            //レスポンスの作成
            response.setItems(items);
            response.setPagination(pagination);  // Already updated by repository
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage(ResponseConstants.OK);
            return response;
            

         


        } catch (Exception e) {
            log.error("readAllMonitoringRecordsSolar failed for projectId={}", projectId, e);
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
            return response;
        }
    }

    //太陽光の日ごとのモニタリングデータをページ分取得する
    @Override
    public MonitoringRecordSolarListResponse readSiteMonitoringRecordsSolar(
            String username,
            String companyId,
            String projectId,
            String siteId,
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
            if (user.getCompany() == null || !companyId.equals(user.getCompany().getCompanyId())) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
                return response;
            }

            //入力情報のsiteIdから企業情報を1件取得
            Site site = siteRepository.findByPublicIdWithFallback(siteId).orElse(null);
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
            Project project = projectRepository.findByPublicIdWithFallback(projectId).orElse(null);
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
            String recordId = site.getPublicId() + "#" + project.getPublicId();
            List<EnR002MonitoringRecord> entities = 
                monitoringRecordSolarRepository.findAllByRecordIdPaginated(recordId, pagination);

            // Map to DTOs
            // 0件の場合はエラーではなく、空のitemsでOKを返す
            List<MonitoringDataSolarResponse> items = entities.stream()
                    .map(entity -> mapEntityToResponse(entity, null))
                    .collect(java.util.stream.Collectors.toList());

            //レスポンスの作成
            response.setItems(items);
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
            String projectId,
            String siteId,
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
            if (user.getCompany() == null || !companyId.equals(user.getCompany().getCompanyId())) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
                return response;
            }

            Site site = siteRepository.findByPublicIdWithFallback(siteId).orElse(null);
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
            Project project = projectRepository.findByPublicIdWithFallback(projectId).orElse(null);
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
            String s3Key = String.format("companies/%s/projects/%d/sites/%s/%s/%s_%s",
                    companyId, project.getId(), site.getPublicId(), fileType, fileId, safeFilename);

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

    private MonitoringValidationResult validateMonitoringRecordRequest(
            String username,
            String companyId,
            RegisterMonitoringRecordSolarRequest request
    ) {
        Map<String, String> errors = collectMonitoringFieldErrors(request);
        if (request == null) {
            return MonitoringValidationResult.invalid(validationError(errors));
        }

        mergeErrors(errors, collectMonitoringPayloadDateErrors(request));
        if (request.getProjectId() == null || request.getSiteId() == null || request.getEquipmentId() == null) {
            return MonitoringValidationResult.invalid(validationError(errors));
        }

        ParticipatingUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(ResponseConstants.MSG_UNEXPEXTED_ERROR));

        if (user.getCompany() == null || !companyId.equals(user.getCompany().getCompanyId())) {
            return MonitoringValidationResult.invalid(
                    BaseResponse.error(ResponseConstants.FORBIDDEN, ResponseConstants.MSG_USER_NOT_IN_COMPANY)
            );
        }

        Site site = siteRepository.findByPublicIdWithFallback(request.getSiteId()).orElse(null);
        if (site == null) {
            return MonitoringValidationResult.invalid(
                    BaseResponse.error(ResponseConstants.NOT_FOUND, ResponseConstants.MSG_SITE_NOT_FOUND)
            );
        }

        if (site.getCompany() == null || !site.getCompany().getCompanyId().equals(companyId)) {
            return MonitoringValidationResult.invalid(
                    BaseResponse.error(ResponseConstants.FORBIDDEN, ResponseConstants.MSG_SITE_NOT_IN_COMPANY)
            );
        }

        Project project = projectRepository.findByPublicIdWithFallback(request.getProjectId()).orElse(null);
        if (project == null) {
            return MonitoringValidationResult.invalid(
                    BaseResponse.error(ResponseConstants.NOT_FOUND, ResponseConstants.MSG_PROJECT_NOT_FOUND)
            );
        }

        if (!validateResourceOwnership(user, site, project)) {
            return MonitoringValidationResult.invalid(
                    BaseResponse.error(ResponseConstants.FORBIDDEN, ResponseConstants.MSG_FORBIDDEN)
            );
        }

        if (!siteProjectRelationRepository.existsByProjectAndSite(project, site)) {
            return MonitoringValidationResult.invalid(
                    BaseResponse.error(ResponseConstants.NOT_FOUND, ResponseConstants.MSG_NOT_FOUND)
            );
        }

        EnR002Equipment equipment = equipmentRepository.findById(request.getEquipmentId()).orElse(null);
        if (equipment == null || equipment.getSite() == null || !equipment.getSite().getId().equals(site.getId())) {
            return MonitoringValidationResult.invalid(
                    BaseResponse.error(ResponseConstants.NOT_FOUND, ResponseConstants.MSG_EQUIPMENT_NOT_FOUND)
            );
        }

        SiteProjectRelation relation = siteProjectRelationRepository.findByProjectIdAndSiteId(
                project.getId(),
                site.getId()
        );
        Map<String, String> businessErrors = collectMonitoringBusinessErrors(request, relation, site, project);
        mergeErrors(errors, businessErrors);
        mergeErrors(errors, collectBatteryEligibilityErrors(request, equipment));

        if (!errors.isEmpty()) {
            return MonitoringValidationResult.invalid(validationError(errors));
        }

        return MonitoringValidationResult.valid(user, site, project);
    }

    private BaseResponse validationError(Map<String, String> errors) {
        BaseResponse response = BaseResponse.validationError(errors);
        response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
        return response;
    }

    private void copyBaseResponse(MonitoringRecordSolarResponse target, BaseResponse source) {
        target.setStatusCode(source.getStatusCode());
        target.setMessage(source.getMessage());
        target.setErrors(source.getErrors());
        target.setTimestamp(source.getTimestamp());
    }

    private Map<String, String> collectMonitoringFieldErrors(RegisterMonitoringRecordSolarRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (request == null) {
            errors.put("request", ResponseConstants.MSG_BAD_REQUEST);
            return errors;
        }

        if (request.getProjectId() == null) {
            errors.put("projectId", ResponseConstants.MSG_PROJECT_ID_REQUIRED);
        }
        if (request.getSiteId() == null) {
            errors.put("siteId", ResponseConstants.MSG_SITE_ID_REQUIRED);
        }
        if (request.getEquipmentId() == null) {
            errors.put("equipmentId", ResponseConstants.MSG_EQUIPMENT_ID_REQUIRED);
        }
        if (isBlank(request.getProjectType())) {
            errors.put("projectType", ResponseConstants.MSG_PROJECT_TYPE_REQUIRED);
        }
        if (isBlank(request.getDateRecorded())) {
            errors.put("dateRecorded", ResponseConstants.MSG_DATE_RECORDED_REQUIRED);
        }
        // changedBattery requiredness depends on equipment.hasBattery - see collectBatteryEligibilityErrors,
        // which runs once the equipment tied to this record has been resolved.

        errors.putAll(collectMandatoryEvidenceErrors(request));
        errors.putAll(collectMonitoringDataErrors(request));

        return errors;
    }

    /**
     * Requirement 6.2 & Update 1: Validate that all 4 mandatory evidence files are provided.
     */
    private Map<String, String> collectMandatoryEvidenceErrors(RegisterMonitoringRecordSolarRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (isBlank(request.getElPvEvidenceUrl())) {
            errors.put("elpv_evidence", ResponseConstants.MSG_ELPV_EVIDENCE_REQUIRED);
        }
        if (isBlank(request.getElPvrEvidenceUrl())) {
            errors.put("elpvr_evidence", ResponseConstants.MSG_ELPVR_EVIDENCE_REQUIRED);
        }
        // if (isBlank(request.getElpjControlEvidenceUrl())) {
        //     errors.put("elpj_control_evidence", ResponseConstants.MSG_ELPJ_CONTROL_EVIDENCE_REQUIRED);
        // } 
        // ELPJ,control の証憑は任意。
        // 未提出の場合、方法論 EN-R-002 Ver.3.7 注釈※2 に基づき
        // 「発電電力量 × 0.02kWh/kWh」のデフォルト値で算定する（セクターによる制限なし）。

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
        if (request.getElPv() != null
                && request.getElPvr() != null
                && request.getElPv().compareTo(BigDecimal.ZERO) >= 0
                && request.getElPvr().compareTo(BigDecimal.ZERO) >= 0
                && request.getElPvr().compareTo(request.getElPv()) > 0) {
            errors.put("ELpvr_value", ResponseConstants.MSG_ELPVR_EXCEEDS_ELPV);
        }
        if (request.getElpjControl() != null
        && request.getElpjControl().compareTo(BigDecimal.ZERO) < 0) {
        errors.put("ELPJ_control_value", ResponseConstants.MSG_ELPJ_CONTROL_VALUE_MIN);
}
        // if (Boolean.TRUE.equals(request.getChangedBattery()) && request.getElpjBatteryValue() == null) {
        //     errors.put("ELPJ_battery_value", ResponseConstants.MSG_ELPJ_BATTERY_VALUE_REQUIRED_WHEN_CHANGED);
        // }
        if (request.getElpjBatteryValue() != null && request.getElpjBatteryValue().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("ELPJ_battery_value", ResponseConstants.MSG_ELPJ_BATTERY_VALUE_MIN);
        }
        if (request.getElpjAccuracy() == null) {
            errors.put("ELPJ_accuracy", ResponseConstants.MSG_ELPJ_ACCURACY_REQUIRED);
        }
        if (request.getElpjAccuracy() != null && request.getElpjAccuracy().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("ELPJ_accuracy", ResponseConstants.MSG_ELPJ_ACCURACY_MIN);
        }
        if (request.getElpjAccuracy() != null && request.getElpjAccuracy().compareTo(MAX_PERCENT_VALUE) > 0) {
            errors.put("ELPJ_accuracy", ResponseConstants.MSG_ELPJ_ACCURACY_MAX);
        }

        return errors;
    }

        /**
     * 蓄電池関連項目は、設備情報（EnR002Equipment.hasBattery）を唯一の判定根拠とする。
     * モニタリング側の changedBattery には依存しない。
     *
     * 蓄電池ありの場合、充放電ロス量は毎回の実測が必要となる。
     * 方法論 EN-R-002 Ver.3.7 注釈※3 のデフォルト値（蓄電池経由率60%/70%）は
     * 家庭部門限定であり、本プロジェクトの会員は全て事業者のため適用できない。
     */
    private Map<String, String> collectBatteryEligibilityErrors(
            RegisterMonitoringRecordSolarRequest request,
            EnR002Equipment equipment
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (equipment == null) {
            return errors;
        }
        if (Boolean.TRUE.equals(equipment.getHasBattery())) {
            // 蓄電池あり: 実測値と証憑の双方が必須
            if (request.getElpjBatteryValue() == null) {
                errors.put("ELPJ_battery_value", ResponseConstants.MSG_ELPJ_BATTERY_VALUE_REQUIRED);
            }
            if (isBlank(request.getElpjBatterySpecificationUrl())) {
                errors.put("elpj_battery_evidence", ResponseConstants.MSG_ELPJ_BATTERY_EVIDENCE_REQUIRED);
            }
        } else {
            // 蓄電池なし: 対象外のため、値・証憑ともに送信を許容しない
            boolean hasBatteryData = request.getElpjBatteryValue() != null
                    || !isBlank(request.getElpjBatterySpecificationUrl());
            if (hasBatteryData) {
                errors.put("ELPJ_battery_value", ResponseConstants.MSG_BATTERY_NOT_APPLICABLE);
            }
        }
        return errors;
    }

    private Map<String, String> collectMonitoringBusinessErrors(
            RegisterMonitoringRecordSolarRequest request,
            SiteProjectRelation relation,
            Site site,
            Project project
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        LocalDate dateRecorded = parseDateRecorded(request.getDateRecorded());
        if (dateRecorded == null) {
            errors.put("dateRecorded", ResponseConstants.MSG_DATE_RECORDED_RANGE);
            return errors;
        }

        LocalDate todayInJapan = LocalDate.now(JAPAN_ZONE);
        LocalDate activityStartDate = relation != null ? relation.getActivityStartDate() : null;
        if (dateRecorded.isAfter(todayInJapan)
                || (activityStartDate != null && dateRecorded.isBefore(activityStartDate))) {
            errors.put("dateRecorded", ResponseConstants.MSG_DATE_RECORDED_RANGE);
            return errors;
        }

        String recordId = site.getPublicId() + "#" + project.getPublicId();
        if (monitoringRecordSolarRepository.existsActiveByRecordIdAndDateRecorded(
                recordId,
                request.getDateRecorded()
        )) {
            errors.put("dateRecorded", ResponseConstants.MSG_DATE_RECORDED_DUPLICATE);
        }

        return errors;
    }

    private Map<String, String> collectMonitoringPayloadDateErrors(RegisterMonitoringRecordSolarRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (isBlank(request.getDateRecorded())) {
            return errors;
        }

        LocalDate dateRecorded = parseDateRecorded(request.getDateRecorded());
        if (dateRecorded == null || dateRecorded.isAfter(LocalDate.now(JAPAN_ZONE))) {
            errors.put("dateRecorded", ResponseConstants.MSG_DATE_RECORDED_RANGE);
        }

        return errors;
    }

    private void mergeErrors(Map<String, String> target, Map<String, String> source) {
        source.forEach(target::putIfAbsent);
    }

    private LocalDate parseDateRecorded(String dateRecorded) {
        try {
            return LocalDate.parse(dateRecorded);
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Validate file size and type, throw exceptions if invalid
     */
    private void validateFileOrThrow(MultipartFile file, String fileType) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            double actualSizeMB = file.getSize() / (1024.0 * 1024.0);
            throw new FileTooLargeException(MAX_FILE_SIZE_MB, actualSizeMB);
        }

        MonitoringEvidenceField field = MonitoringEvidenceField.fromFileType(fileType);
        List<String> allowedExtensions = field.getAllowedExtensions();

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
            if (user.getCompany() == null) {
                return false;
            }

            // Check if site has company information
            if (site.getCompany() == null || site.getCompany().getCompanyId() == null) {
                return false;
            }

            // CRITICAL: Check if user's company ID matches site's company ID
            if (!site.getCompany().getCompanyId().equals(user.getCompany().getCompanyId())) {
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
                .id(entity.getId())
                .projectId(projectRepository.findById(entity.getProjectID()).map(Project::getPublicId).orElse(String.valueOf(entity.getProjectID())))
                .siteId(siteRepository.findById(entity.getSiteID()).map(Site::getPublicId).orElse(String.valueOf(entity.getSiteID())))
                .equipmentId(entity.getEquipmentID())
                .userId(entity.getParticipatingUserID())
                .projectType(projectType != null ? projectType : "ENR002")
                .timeUpdated(entity.getTimeUpdated())
                .dateRecorded(entity.getDateRecorded())
                .elPv(entity.getElPv())
                .elPvEvidence(entity.getElPvEvidence())
                .elPvr(entity.getElPvr())
                .elPvrEvidence(entity.getElPvrEvidence())
                .elPvEvidenceUrl(entity.getElPvEvidenceUrl())
                .elPvrEvidenceUrl(entity.getElPvrEvidenceUrl())
                .changedBattery(entity.getChangedBattery())
                .elpjBatteryValue(entity.getElpjBatteryValue())
                .elpjBatteryEvidence(entity.getElpjBatteryEvidence())
                .elpjBatterySpecificationUrl(entity.getElpjBatterySpecificationUrl())
                .elpjControl(entity.getElpjControlValue())
                .elpjControlEquipmentSpecFiles(entity.getElpjControlEquipmentSpecFiles())
                .elpjControlEvidenceUrl(entity.getElpjControlEvidenceUrl())
                .elpjAccuracy(entity.getElpjAccuracy())
                .isActive(entity.getIsActive())
                .processedAt(entity.getProcessedAt())
                .methodologyType(entity.getMethodologyType())
                .build();
    }

    private static final class MonitoringValidationResult {
        private final boolean valid;
        private final BaseResponse response;
        private final ParticipatingUser user;
        private final Site site;
        private final Project project;

        private MonitoringValidationResult(
                boolean valid,
                BaseResponse response,
                ParticipatingUser user,
                Site site,
                Project project
        ) {
            this.valid = valid;
            this.response = response;
            this.user = user;
            this.site = site;
            this.project = project;
        }

        private static MonitoringValidationResult valid(ParticipatingUser user, Site site, Project project) {
            return new MonitoringValidationResult(
                    true,
                    BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS),
                    user,
                    site,
                    project
            );
        }

        private static MonitoringValidationResult invalid(BaseResponse response) {
            return new MonitoringValidationResult(false, response, null, null, null);
        }

        private boolean isValid() {
            return valid;
        }

        private BaseResponse getResponse() {
            return response;
        }

        private ParticipatingUser getUser() {
            return user;
        }

        private Site getSite() {
            return site;
        }

        private Project getProject() {
            return project;
        }
    }

    
    // ===== 4 SPECIFIC EVIDENCE UPLOAD APIs =====

    @Override
    public FileUploadResponse uploadElpvEvidence(
            String username,
            String companyId,
            String projectId,
            String siteId,
            String description,
            MultipartFile file
    ) {
        return uploadMonitoringFileSolar(username, companyId, projectId, siteId, "ELPV_EVIDENCE", description, file);
    }

    @Override
    public FileUploadResponse uploadElpvrEvidence(
            String username,
            String companyId,
            String projectId,
            String siteId,
            String description,
            MultipartFile file
    ) {
        return uploadMonitoringFileSolar(username, companyId, projectId, siteId, "ELPVR_EVIDENCE", description, file);
    }

    @Override
    public FileUploadResponse uploadElpjBatterySpecificationEvidence(
            String username,
            String companyId,
            String projectId,
            String siteId,
            String description,
            MultipartFile file
    ) {
        return uploadMonitoringFileSolar(username, companyId, projectId, siteId, "ELPJ_BATTERY_EVIDENCE", description, file);
    }

    @Override
    public FileUploadResponse uploadElpjControlEvidence(
            String username,
            String companyId,
            String projectId,
            String siteId,
            String description,
            MultipartFile file
    ) {
        return uploadMonitoringFileSolar(username, companyId, projectId, siteId, "ELPJ_CONTROL_EVIDENCE", description, file);
    }

    @Override
    public MonitoringEvidenceItemsResponse readMonitoringEvidenceItems(
            String username,
            String companyId,
            String projectId,
            String siteId,
            Integer equipmentId
    ) {
        MonitoringEvidenceItemsResponse response = new MonitoringEvidenceItemsResponse();
        try {
            // 1) ユーザ取得
            ParticipatingUser user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException(ResponseConstants.MSG_UNEXPEXTED_ERROR));
            // ユーザが指定した会社に所属していない場合
            if (user.getCompany() == null || !companyId.equals(user.getCompany().getCompanyId())) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
                return response;
            }
            // 入力情報のsiteIdから拠点情報を1件取得
            Site site = siteRepository.findByPublicIdWithFallback(siteId).orElse(null);
            // 拠点情報がnullの場合
            if (site == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }
            // サイトに企業が紐付いていない（データ不整合）or サイトは存在するが、別の企業のもの
            if (site.getCompany() == null || !site.getCompany().getCompanyId().equals(companyId)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_IN_COMPANY);
                return response;
            }
            // プロジェクト取得
            Project project = projectRepository.findByPublicIdWithFallback(projectId).orElse(null);
            // プロジェクトのnullチェック
            if (project == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
                return response;
            }
            // 2) Checks: User→Site access, Project company ownership, Site-Project consistency
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
            // 4) 設備が存在し、対象拠点に属していることを確認する
            EnR002Equipment equipment = equipmentRepository.findById(equipmentId).orElse(null);
            if (equipment == null || equipment.getSite() == null
                    || !equipment.getSite().getId().equals(site.getId())) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND);
                return response;
            }

            // 4.5) 当該設備の最新モニタリングレコードを取得する（存在しない場合は未提出として扱う）
            String recordId = site.getPublicId() + "#" + project.getPublicId();
            EnR002MonitoringRecord latestRecord = monitoringRecordSolarRepository
                    .findLatestByRecordIdAndEquipmentId(recordId, equipment.getId())
                    .orElse(null);

            //  設備情報をもとに全証憑項目の必須区分を判定する
            List<MonitoringEvidenceItemResponse> items = new ArrayList<>();
            for (MonitoringEvidenceField field : MonitoringEvidenceField.values()) {
                items.add(buildEvidenceItem(field, resolveEvidenceRequirement(equipment, field), latestRecord));
            }

            // 6) レスポンスの作成
            response.setEquipmentId(equipment.getId());
            response.setMethodologyName(METHODOLOGY_NAME);
            response.setMethodologyNumber(METHODOLOGY_NUMBER);
            response.setMethodologyVersion(METHODOLOGY_VERSION);
            response.setEvidenceItems(items);
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage(ResponseConstants.OK);
            return response;
        } catch (Exception e) {
            log.error("readMonitoringEvidenceItems failed for equipmentId={}", equipmentId, e);
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
            return response;
        }
    }
 
    /**
     * 証憑の必須区分を判定する。
     *
     * 基本設計 7.2.2（証憑ファイルアップロードの提出時バリデーション）からも本メソッドを使用すること。
     * 表示上は必須と示されているが提出できてしまう、といった不整合を防ぐため、判定はここに一本化する。
     *
     * 判定根拠（方法論 EN-R-002 Ver.3.7「6．モニタリング方法」）:
     *  - ELpv・ELpvr   : 主要排出量および ELPJ = ELpv - ELpvr の算定に必須
     *  - ELPJ,control  : 注釈※2 により「発電電力量 × 0.02kWh/kWh」で算定してもよいため任意。
     *                    セクターによる制限はない
     *  - ELPJ,battery  : 蓄電池がある場合のみ算定対象（EM PJ,S,battery は IF 蓄電池あり）。
     *                    注釈※3 のデフォルト値（蓄電池経由率60%/70%）は家庭部門限定であり、
     *                    本プロジェクトの会員は全て事業者のため適用できない。
     *                    よって蓄電池ありの場合は実測必須とする
     */
    private EvidenceRequirement resolveEvidenceRequirement(
            EnR002Equipment equipment,
            MonitoringEvidenceField field
    ) {
        switch (field) {
            case EL_PV:
            case EL_PVR:
                return EvidenceRequirement.REQUIRED;
            case EL_PJ_CONTROL:
                return EvidenceRequirement.OPTIONAL;
            case EL_PJ_BATTERY:
                return Boolean.TRUE.equals(equipment.getHasBattery())
                        ? EvidenceRequirement.REQUIRED
                        : EvidenceRequirement.NOT_APPLICABLE;
            default:
                return EvidenceRequirement.NOT_APPLICABLE;
        }
    }
 
    /**
     * 証憑の提出が必須かどうか。
     * OPTIONAL および NOT_APPLICABLE は false を返す。
     * 7.2.2 の提出時バリデーション（collectMandatoryEvidenceErrors）から使用する。
     */
    private boolean isEvidenceMandatory(EnR002Equipment equipment, MonitoringEvidenceField field) {
        return resolveEvidenceRequirement(equipment, field) == EvidenceRequirement.REQUIRED;
    }
 
    /**
     * 証憑項目1件分のレスポンスを作成する。
     * 既にモニタリングレコードが存在する場合は、提出状況と証憑のS3キーを併せて返却する。
     */
    private MonitoringEvidenceItemResponse buildEvidenceItem(
            MonitoringEvidenceField field,
            EvidenceRequirement requirement,
            EnR002MonitoringRecord record
    ) {
        boolean notApplicable = requirement == EvidenceRequirement.NOT_APPLICABLE;
        String evidenceUrl = notApplicable ? null : resolveEvidenceUrl(field, record);

        return MonitoringEvidenceItemResponse.builder()
                .fieldName(field.getFieldName())
                .labelJa(field.getLabelJa())
                .requirement(requirement)
                .notApplicableReason(
                        notApplicable && field == MonitoringEvidenceField.EL_PJ_BATTERY
                                ? REASON_NO_BATTERY : null)
                .fallbackNote(
                        requirement == EvidenceRequirement.OPTIONAL
                                && field == MonitoringEvidenceField.EL_PJ_CONTROL
                                ? FALLBACK_PCS_DEFAULT : null)
                .acceptedTypes(notApplicable ? List.of() : field.getAcceptedTypes())
                .status(notApplicable ? null
                        : (isBlank(evidenceUrl) ? STATUS_NOT_SUBMITTED : STATUS_SUBMITTED))
                .evidenceUrl(evidenceUrl)
                .build();
    }

    /**
     * 項目に対応する証憑のS3キーをモニタリングレコードから取得する。
     */
    private String resolveEvidenceUrl(MonitoringEvidenceField field, EnR002MonitoringRecord record) {
        if (record == null) {
            return null;
        }
        switch (field) {
            case EL_PV:
                return record.getElPvEvidenceUrl();
            case EL_PVR:
                return record.getElPvrEvidenceUrl();
            case EL_PJ_CONTROL:
                return record.getElpjControlEvidenceUrl();
            case EL_PJ_BATTERY:
                return record.getElpjBatterySpecificationUrl();
            default:
                return null;
        }
    }

}
