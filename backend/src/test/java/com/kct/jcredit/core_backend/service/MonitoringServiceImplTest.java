package com.kct.jcredit.core_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.FileUploadResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.impl.MonitoringServiceImpl;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Comprehensive Unit Tests for MonitoringServiceImpl
 * 
 * Test Coverage:
 * 1. registerMonitoringRecordSolar - Happy path & error scenarios
 * 2. readLastMonitoringRecordSolar - Read operations
 * 3. uploadElpvEvidence, uploadElpvrEvidence, uploadElpjBatterySpecificationEvidence, uploadElpjControlEvidence
 * 4. Security validations: Company isolation, Role-based access
 * 5. File validation: Size, type, content
 * 6. Edge cases: NULL values, missing FK relations, invalid roles
 * 
 * * MonitoringServiceImpl の包括的なユニットテスト
*
* テストカバレッジ:
* 1. registerMonitoringRecordSolar - 正常系およびエラーシナリオ
* 2. readLastMonitoringRecordSolar - 読み取り操作
* 3. uploadElpvEvidence、uploadElpvrEvidence、uploadElpjBatterySpecificationEvidence、uploadElpjControlEvidence
* 4. セキュリティ検証: 会社の分離、役割に基づくアクセス
* 5. ファイル検証: サイズ、種類、内容
* 6. エッジケース: NULL値、欠落したFK関係、無効な役割
 * 
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonitoringServiceImpl Unit Tests")
class MonitoringServiceImplTest {

    @Mock(lenient = true)
    private ParticipatingUserRepository userRepository;

    @Mock(lenient = true)
    private SiteRepository siteRepository;

    @Mock(lenient = true)
    private ProjectRepository projectRepository;

    @Mock(lenient = true)
    private SiteProjectRelationRepository siteProjectRelationRepository;

    @Mock(lenient = true)
    private MonitoringTableRecordSolarRepository monitoringRecordSolarRepository;

    @Mock(lenient = true)
    private S3Client s3Client;

    @Mock(lenient = true)
    private Site tokyoSite;

    @Mock(lenient = true)
    private Site fukuokaSite;

    @InjectMocks
    private MonitoringServiceImpl monitoringService;

    // ============ TEST DATA FIXTURES ============

    private ParticipatingUser masterUser;
    private ParticipatingUser regularUser;
    private Project project;
    private ParticipatingCompany participatingCompany;

    @BeforeEach
    void setUp() {
        setupTestData();
        setupSiteMocks();
        // S3Client mock will be setup per-test method that needs it
    }

    private void setupTestData() {
        // ===== Company Setup (ParticipatingCompany) =====
        participatingCompany = new ParticipatingCompany();
        participatingCompany.setId(2);
        participatingCompany.setCompanyId("COMP00000002");
        participatingCompany.setNameJa("グリーンエネルギー株式会社");
        participatingCompany.setNameFurigana("グリーンエネルギーカブシキガイシャ");
        participatingCompany.setNameEn("Green Energy Corp");
        participatingCompany.setIsActive(true);
        participatingCompany.setCreatedBy("system");
        participatingCompany.setUpdatedBy("system");

        // ===== Project Setup =====
        project = new Project();
        project.setId(1);
        project.setProjectName("Solar Project 1");
        project.setIsActive(true);

        // ===== User Setup: MASTER role =====
        masterUser = new ParticipatingUser();
        masterUser.setId(13);
        masterUser.setUsername("multi_site_master");

        ParticipatingCompany masterCompany = new ParticipatingCompany();
        masterCompany.setId(2);
        masterCompany.setCompanyId("COMP00000002");
        masterCompany.setIsActive(true);

        masterUser.setCompany(masterCompany);

        masterUser.setRole("master");
        masterUser.setIsActive(true);
        masterUser.setPassword("$2a$10$Gz/GpXFrN/J6zWwGQxfhbuxtZfYL1vFIfa1FNsdpnRD5zkPfI0IuO");
        // MASTER doesn't need site assignment but can access all sites

        // ===== User Setup: USER role (restricted to Tokyo site) =====
        regularUser = new ParticipatingUser();
        regularUser.setId(14);
        regularUser.setUsername("tokyo_operator");

        ParticipatingCompany regularCompany = new ParticipatingCompany();
        regularCompany.setId(2);
        regularCompany.setCompanyId("COMP00000002");

        regularUser.setCompany(regularCompany);
        regularUser.setRole("user");
        regularUser.setIsActive(true);
        regularUser.setSite(tokyoSite);
    }

    private void setupSiteMocks() {
        // Setup Tokyo Site mock
        when(tokyoSite.getId()).thenReturn(21);
        when(tokyoSite.getSiteName()).thenReturn("Tokyo HQ");
        when(tokyoSite.getCompany()).thenReturn(participatingCompany);
        when(tokyoSite.getIsActive()).thenReturn(true);

        // Setup Fukuoka Site mock
        when(fukuokaSite.getId()).thenReturn(22);
        when(fukuokaSite.getSiteName()).thenReturn("Fukuoka Office");
        when(fukuokaSite.getCompany()).thenReturn(participatingCompany);
        when(fukuokaSite.getIsActive()).thenReturn(true);
    }

    // ============ SUITE 1: registerMonitoringRecordSolar ============

    @Test
    //テスト項目名
    //全必須項目が揃った基本的なリクエストで登録成功

    //DisplayName翻訳
    //MASTERユーザーが有効なデータを提供した場合、監視記録を登録する必要があります〇
    @DisplayName("Should register monitoring record when MASTER user provides valid data")
    void testRegisterMonitoringRecord_MasterUser_Success() {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(monitoringRecordSolarRepository.save(any())).thenReturn(new EnR002MonitoringRecord());

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertTrue(response.getMessage().contains("正常に登録"));
        verify(monitoringRecordSolarRepository, times(1)).save(any(EnR002MonitoringRecord.class));
    }

    @Test
    //USERが割り当てられたサイトにアクセスしている場合、監視記録を登録する必要があります〇
    @DisplayName("Should register monitoring record when USER has access to their assigned site")
    void testRegisterMonitoringRecord_RegularUser_AssignedSite_Success() {
        // Arrange
        String username = "tokyo_operator";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(regularUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(monitoringRecordSolarRepository.save(any())).thenReturn(new EnR002MonitoringRecord());

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        verify(monitoringRecordSolarRepository, times(1)).save(any(EnR002MonitoringRecord.class));
    }

    @Test
    //USERが自分に割り当てられていないサイト（福岡）にアクセスしようとした場合は拒否する必要があります〇
    @DisplayName("Should reject when USER tries to access site not assigned to them (Fukuoka)")
    void testRegisterMonitoringRecord_RegularUser_UnauthorizedSite_Forbidden() {
        // Arrange: regularUser only has Tokyo, but tries to access Fukuoka
        String username = "tokyo_operator";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 22; // Fukuoka - NOT assigned to regularUserv(福岡 - regularUserには割り当てられていません)

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(regularUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(fukuokaSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_FORBIDDEN, response.getMessage());
        assertFalse(response.getMessage().contains("正常に登録"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    //異なる会社のユーザーが所属していない会社にアクセスしようとした場合は拒否する必要があります〇
    @DisplayName("Should reject when user from different company tries to access")
    void testRegisterMonitoringRecord_CrossCompanyAttempt_Forbidden() {
        // Arrange: User from COMP00000001 tries to access COMP00000002
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        // User from different company
        ParticipatingUser otherCompanyUser = new ParticipatingUser();
        otherCompanyUser.setId(1);
        otherCompanyUser.setUsername(username);

        ParticipatingCompany otherCompany = new ParticipatingCompany();
        otherCompany.setId(1);
        otherCompany.setCompanyId("COMP00000001");

        otherCompanyUser.setCompany(otherCompany);
        otherCompanyUser.setRole("master");
        otherCompanyUser.setIsActive(true);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(otherCompanyUser));

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when mandatory evidence URLs are missing")
    void testRegisterMonitoringRecord_MissingEvidenceURLs_BadRequest() {

        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        RegisterMonitoringRecordSolarRequest request =
                buildValidMonitoringRequest(projectId, siteId);

        request.setElPvEvidenceUrl(null);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(masterUser));

        when(siteRepository.findById(siteId))
                .thenReturn(Optional.of(tokyoSite));

        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(project));

        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite))
                .thenReturn(true);

        MonitoringRecordSolarResponse response =
                monitoringService.registerMonitoringRecordSolar(
                        username,
                        companyId,
                        request
                );

        assertNotNull(response);

        assertEquals(
                ResponseConstants.VALIDATION_ERROR,
                response.getStatusCode()
        );

        assertEquals(
                ResponseConstants.MSG_VALIDATION_ERROR,
                response.getMessage()
        );

        assertNotNull(response.getErrors());
        assertFalse(response.getErrors().isEmpty());

        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    //数値モニタリングデータに負の値が含まれている場合は拒否する必要があります〇
    @DisplayName("Should reject when numerical monitoring data has negative values")
    void testRegisterMonitoringRecord_NegativeValues_ValidationError() {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        // Set negative value
        //負の値を設定する
        request.setElPv(new BigDecimal("-100"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertTrue(response.getMessage().contains("無効"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    //サイトとプロジェクトの関係が存在しない場合は拒否するべきです〇
    @DisplayName("Should reject when site-project relation doesn't exist")
    void testRegisterMonitoringRecord_MissingRelation_NotFound() {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        // Site-Project relation doesn't exist!
        //サイトとプロジェクトの関係は存在しません
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(false);

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_NOT_FOUND, response.getMessage());
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    //サイトがリクエストで操作しようとしている会社とは別の会社に属している場合に
    // 属していない場合はモニタリングレコードの登録を拒否する必要があります〇
    @DisplayName("Should reject when site doesn't belong to the company")
    void testRegisterMonitoringRecord_SiteWrongCompany_Forbidden() {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        // Site belongs to different company
        //サイトは別の会社に属しています
        ParticipatingCompany differentCompany = new ParticipatingCompany();
        differentCompany.setId(1);
        differentCompany.setCompanyId("COMP00000001");
        differentCompany.setNameJa("別の企業");
        
        Site wrongCompanySite = new Site();
        wrongCompanySite.setId(21);
        wrongCompanySite.setSiteName("Tokyo HQ");
        wrongCompanySite.setCompany(differentCompany);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(wrongCompanySite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getMessage().contains("not in this company"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    // ============ SUITE 2: readLastMonitoringRecordSolar ============

    @Test
    //MASTERユーザーの最後の監視記録を読む必要があります〇
    @DisplayName("Should read last monitoring record for MASTER user")
    void testReadLastMonitoringRecord_MasterUser_Success() {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        EnR002MonitoringRecord entity = new EnR002MonitoringRecord();
        // PK format changed to SITE#PROJECT (companyId still validated via user/site/project entities)
        // PK形式はSITE#PROJECTに変更されました（companyIdは引き続きユーザー/サイト/プロジェクトのエンティティで検証されます）
        entity.setId(siteId + "#" + projectId);
        entity.setProjectID(projectId);
        entity.setSiteID(siteId);
        entity.setElPv(new BigDecimal("100"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(monitoringRecordSolarRepository.findLatest(entity.getId())).thenReturn(Optional.of(entity));

        // Act
        MonitoringRecordSolarResponse response = monitoringService.readLastMonitoringRecordSolar(username, companyId, projectId, siteId);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getData());
        assertEquals(new BigDecimal("100"), response.getData().getElPv());
    }

    @Test
    //ユーザーが許可されていないサイトから読み取ろうとした場合は拒否するべきです
    @DisplayName("Should reject when USER tries to read from unauthorized site")
    void testReadLastMonitoringRecord_RegularUser_UnauthorizedSite_Forbidden() {
        // Arrange
        String username = "tokyo_operator";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 22; // Fukuoka - not assigned

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(regularUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(fukuokaSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Act
        MonitoringRecordSolarResponse response = monitoringService.readLastMonitoringRecordSolar(username, companyId, projectId, siteId);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
    }

    @Test
    //監視記録が存在しない場合はNOT_FOUNDを返すべきです
    @DisplayName("Should return NOT_FOUND when no monitoring record exists")
    void testReadLastMonitoringRecord_NoData_NotFound() {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        // No record found
        when(monitoringRecordSolarRepository.findLatest(anyString())).thenReturn(Optional.empty());

        // Act
        MonitoringRecordSolarResponse response = monitoringService.readLastMonitoringRecordSolar(username, companyId, projectId, siteId);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
    }

    // ============ SUITE 3: File Upload Methods ============

    @Test
    //MASTERユーザーのためにELPVの証拠をアップロードする必要があります
    @DisplayName("Should upload ELPV evidence for MASTER user")
    void testUploadElpvEvidence_MasterUser_Success() throws IOException {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        MultipartFile file = createMockFile("test.pdf", "pdf", 1024);
        setupMockFileForUpload(file, "pdf", 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation spr1 = new SiteProjectRelation();
        spr1.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(spr1));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        // Setup S3Client mock for this test
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(null);

        // Act
        FileUploadResponse response = monitoringService.uploadElpvEvidence(username, companyId, projectId, siteId, "Test description", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getFile());
        assertEquals("ELPV_EVIDENCE", response.getFile().getFileType());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    //10MBのサイズ制限を超えるファイルのアップロードは拒否する必要があります
    @DisplayName("Should reject file upload exceeding 10MB size limit")
    void testUploadFile_ExceedsSizeLimit_FileTooLarge() throws IOException {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        // 15MB file
        MultipartFile file = createMockFile("large.pdf", "pdf", 15 * 1024 * 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation siteProjectRelation = new SiteProjectRelation();
        siteProjectRelation.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(siteProjectRelation));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));

        // Act
        FileUploadResponse response = monitoringService.uploadElpvEvidence(username, companyId, projectId, siteId, "Test", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FILE_TOO_LARGE, response.getStatusCode());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    //ELPVでは無効なファイルタイプを拒否する必要があります（pdf/csvのみ許可）
    @DisplayName("Should reject invalid file type for ELPV (only pdf/csv allowed)")
    void testUploadElpvEvidence_InvalidFileType_Unsupported() throws IOException {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        // .txt is not allowed for ELPV
        MultipartFile file = createMockFile("test.txt", "txt", 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation siteProjectRelation = new SiteProjectRelation();
        siteProjectRelation.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(siteProjectRelation));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));

        // Act
        FileUploadResponse response = monitoringService.uploadElpvEvidence(username, companyId, projectId, siteId, "Test", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.UNSUPPORTED_FILE_TYPE, response.getStatusCode());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    //ユーザーが許可されていないサイトにアップロードしようとした場合は拒否するべきです
    @DisplayName("Should reject when USER tries to upload to unauthorized site")
    void testUploadFile_RegularUser_UnauthorizedSite_Forbidden() throws IOException {
        // Arrange
        String username = "tokyo_operator";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 22; // Fukuoka - not assigned

        MultipartFile file = createMockFile("test.pdf", "pdf", 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(regularUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(fukuokaSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation siteProjectRelation = new SiteProjectRelation();
        siteProjectRelation.setProject(project);
        when(fukuokaSite.getSiteProjectRelations()).thenReturn(Arrays.asList(siteProjectRelation));

        // Act
        FileUploadResponse response = monitoringService.uploadElpvEvidence(username, companyId, projectId, siteId, "Test", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    //ELPVRの証拠をアップロードする必要があります（pdf、png、jpeg、jpgが使用可能）
    @DisplayName("Should upload ELPVR evidence (allows pdf, png, jpeg, jpg)")
    void testUploadElpvrEvidence_ValidFileType_Success() throws IOException {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        MultipartFile file = createMockFile("test.png", "png", 2048);        setupMockFileForUpload(file, "png", 2048);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation spr2 = new SiteProjectRelation();
        spr2.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(spr2));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        // Setup S3Client mock for this test
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(null);

        // Act
        FileUploadResponse response = monitoringService.uploadElpvrEvidence(username, companyId, projectId, siteId, "Evidence", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("ELPVR_EVIDENCE", response.getFile().getFileType());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    //ELPJバッテリー仕様の証拠をアップロードするべきです
    @DisplayName("Should upload ELPJ Battery Specification evidence")
    void testUploadElpjBatterySpecification_Success() throws IOException {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        MultipartFile file = createMockFile("battery.jpg", "jpg", 1500);
        setupMockFileForUpload(file, "jpg", 1500);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation spr3 = new SiteProjectRelation();
        spr3.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(spr3));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        // Setup S3Client mock for this test
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(null);

        // Act
        FileUploadResponse response = monitoringService.uploadElpjBatterySpecificationEvidence(username, companyId, projectId, siteId, "Battery Spec", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("ELPJ_BATTERY_SPECIFICATION", response.getFile().getFileType());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    //ELPJコントロールの証拠をアップロードする必要があります
    @DisplayName("Should upload ELPJ Control evidence")
    void testUploadElpjControlEvidence_Success() throws IOException {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        MultipartFile file = createMockFile("control.pdf", "pdf", 3000);
        setupMockFileForUpload(file, "pdf", 3000);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation spr4 = new SiteProjectRelation();
        spr4.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(spr4));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        // Setup S3Client mock for this test
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(null);

        // Act
        FileUploadResponse response = monitoringService.uploadElpjControlEvidence(username, companyId, projectId, siteId, "Control", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("ELPJ_CONTROL_EVIDENCE", response.getFile().getFileType());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    // ============ SUITE 4: Edge Cases & Security ============

    @Test
    //ユーザーが見つからない場合は適切に処理する必要があります
    @DisplayName("Should handle user not found gracefully")
    void testRegisterMonitoringRecord_UserNotFound_InternalError() {
        // Arrange
        String username = "nonexistent_user";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    //NULLサイト会社を処理する必要があります（データ整合性の問題）
    @DisplayName("Should handle NULL site company (data integrity issue)")
    void testUploadFile_SiteHasNullCompany_Forbidden() throws IOException {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        Integer projectId = 1;
        Integer siteId = 21;

        // Create a mock Site with null company
        Site brokenSite = mock(Site.class, withSettings().lenient());

        MultipartFile file = createMockFile("test.pdf", "pdf", 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(brokenSite));
        when(brokenSite.getId()).thenReturn(siteId);
        when(brokenSite.getCompany()).thenReturn(null); // NULL company FK!
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation siteProjectRelation = new SiteProjectRelation();
        siteProjectRelation.setProject(project);
        when(brokenSite.getSiteProjectRelations()).thenReturn(Arrays.asList(siteProjectRelation));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Act
        FileUploadResponse response = monitoringService.uploadElpvEvidence(username, companyId, projectId, siteId, "Test", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getMessage().contains("no company assigned"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    //ユーザーの役割が不明または無効な場合は拒否する必要があります
    @DisplayName("Should reject when user role is unknown/invalid")
    void testRegisterMonitoringRecord_UnknownRole_Forbidden() {
        // Arrange
        String username = "unknown_role_user";
        String companyId = "COMP00000002";
        Integer Id = 123;
        Integer projectId = 1;
        Integer siteId = 21;

        ParticipatingUser unknownRoleUser = new ParticipatingUser();
        unknownRoleUser.setId(99);
        unknownRoleUser.setUsername(username);
        ParticipatingCompany unknownRoleCompany = new ParticipatingCompany(); // ✅ Fix: was unknownRoleUser.setCompanyId(companyId)
        unknownRoleCompany.setId(Id);
        unknownRoleUser.setCompany(unknownRoleCompany);
        unknownRoleUser.setRole("admin"); // Unknown role
        unknownRoleUser.setIsActive(true);

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(unknownRoleUser));
        when(siteRepository.findById(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    // ============ TEST HELPER METHODS ============

    private RegisterMonitoringRecordSolarRequest buildValidMonitoringRequest(Integer projectId, Integer siteId) {
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId(projectId);
        request.setSiteId(siteId);
        request.setUserId(13); // Master user ID
        request.setElPvEvidenceUrl("https://example.com/elpv.pdf");
        request.setElPvrEvidenceUrl("https://example.com/elpvr.pdf");
        request.setElpjBatterySpecificationUrl("https://example.com/battery.pdf");
        request.setElpjControlEvidenceUrl("https://example.com/control.pdf");
        request.setElPv(new BigDecimal("100.50"));
        request.setElPvr(new BigDecimal("50.25"));
        request.setElpjControl(new BigDecimal("75.00"));
        request.setElpjAccuracy(new BigDecimal("95.5"));
        request.setChangedBattery(false);
        request.setDateRecorded(LocalDate.now().toString()); // Convert to String
        request.setProjectType("ENR002");
        return request;
    }

    private MultipartFile createMockFile(String filename, String extension, long sizeInBytes) throws IOException {
        // Create mock with minimal essential stubs (size & filename validation happens first)
        // Avoid stubbing getContentType() and getInputStream() here since tests may fail early
        MultipartFile mockFile = mock(MultipartFile.class, withSettings().lenient());
        when(mockFile.getOriginalFilename()).thenReturn(filename);
        when(mockFile.getSize()).thenReturn(sizeInBytes);
        // Note: getContentType() and getInputStream() must be stubbed by caller if needed
        return mockFile;
    }

    private void setupMockFileForUpload(MultipartFile mockFile, String extension, long sizeInBytes) throws IOException {
        // Setup additional stubs for tests that proceed past size/type validation
        when(mockFile.getContentType()).thenReturn("application/" + extension);
        when(mockFile.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(new byte[(int)Math.min(sizeInBytes, 1024)]));
    }
}
