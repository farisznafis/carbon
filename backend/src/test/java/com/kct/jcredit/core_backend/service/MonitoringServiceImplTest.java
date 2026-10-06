package com.kct.jcredit.core_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.FileUploadResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringRecordSolarResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
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
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private EnR002EquipmentRepository equipmentRepository;

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
        project.setPublicId("PROJ0001");
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
        when(tokyoSite.getPublicId()).thenReturn("21");
        when(tokyoSite.getSiteName()).thenReturn("Tokyo HQ");
        when(tokyoSite.getCompany()).thenReturn(participatingCompany);
        when(tokyoSite.getIsActive()).thenReturn(true);

        // Setup Fukuoka Site mock
        when(fukuokaSite.getId()).thenReturn(22);
        when(fukuokaSite.getPublicId()).thenReturn("22");
        when(fukuokaSite.getSiteName()).thenReturn("Fukuoka Office");
        when(fukuokaSite.getCompany()).thenReturn(participatingCompany);
        when(fukuokaSite.getIsActive()).thenReturn(true);

        // Setup default equipment (Tokyo, hasBattery=true) returned for any equipmentId lookup
        EnR002Equipment defaultEquipment = new EnR002Equipment();
        defaultEquipment.setId(100);
        defaultEquipment.setSite(tokyoSite);
        defaultEquipment.setHasBattery(true);
        when(equipmentRepository.findById(anyInt())).thenReturn(Optional.of(defaultEquipment));
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
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
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
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(regularUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
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
        String projectId = "PROJ0001";
        String siteId = "22"; // Fukuoka - NOT assigned to regularUserv(福岡 - regularUserには割り当てられていません)

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(regularUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(fukuokaSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));

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
        String projectId = "PROJ0001";
        String siteId = "21";

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
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request =
                buildValidMonitoringRequest(projectId, siteId);

        request.setElPvEvidenceUrl(null);
        request.setElPvrEvidenceUrl(null);
        request.setElpjControlEvidenceUrl(null);

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(masterUser));

        when(siteRepository.findByPublicIdWithFallback(siteId))
                .thenReturn(Optional.of(tokyoSite));

        when(projectRepository.findByPublicIdWithFallback(projectId))
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
        assertEquals(ResponseConstants.MSG_ELPV_EVIDENCE_REQUIRED,
                response.getErrors().get("elpv_evidence"));
        assertEquals(ResponseConstants.MSG_ELPVR_EVIDENCE_REQUIRED,
                response.getErrors().get("elpvr_evidence"));
        assertEquals(ResponseConstants.MSG_ELPJ_CONTROL_EVIDENCE_REQUIRED,
                response.getErrors().get("elpj_control_evidence"));

        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when equipmentId is missing")
    void testRegisterMonitoringRecord_MissingEquipmentId_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setEquipmentId(null);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);

        MonitoringRecordSolarResponse response =
                monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_EQUIPMENT_ID_REQUIRED, response.getErrors().get("equipmentId"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when equipmentId belongs to a different site")
    void testRegisterMonitoringRecord_EquipmentNotInSite_NotFound() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setEquipmentId(999);

        EnR002Equipment equipmentInAnotherSite = new EnR002Equipment();
        equipmentInAnotherSite.setId(999);
        equipmentInAnotherSite.setSite(fukuokaSite);
        equipmentInAnotherSite.setHasBattery(true);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(equipmentRepository.findById(999)).thenReturn(Optional.of(equipmentInAnotherSite));

        MonitoringRecordSolarResponse response =
                monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        assertNotNull(response);
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND, response.getMessage());
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should persist equipmentID on the saved monitoring record entity")
    void testRegisterMonitoringRecord_Success_PersistsEquipmentId() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(monitoringRecordSolarRepository.save(any())).thenReturn(new EnR002MonitoringRecord());

        MonitoringRecordSolarResponse response =
                monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        ArgumentCaptor<EnR002MonitoringRecord> captor = ArgumentCaptor.forClass(EnR002MonitoringRecord.class);
        verify(monitoringRecordSolarRepository).save(captor.capture());
        assertEquals(100, captor.getValue().getEquipmentID());
    }

    @Test
    @DisplayName("POST should require battery evidence when changedBattery is true")
    void testRegisterMonitoringRecord_ChangedBatteryWithoutEvidence_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";
        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjBatteryValue(new BigDecimal("10.00"));
        request.setElpjBatterySpecificationUrl(null);

        stubValidMonitoringContext(username, projectId, siteId);

        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
                username, companyId, request);

        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_ELPJ_BATTERY_EVIDENCE_REQUIRED,
                response.getErrors().get("elpj_battery_evidence"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST should allow missing battery evidence when changedBattery is false")
    void testRegisterMonitoringRecord_UnchangedBatteryWithoutEvidence_Success() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";
        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjBatterySpecificationUrl(null);

        stubValidMonitoringContext(username, projectId, siteId);
        when(monitoringRecordSolarRepository.save(any())).thenReturn(new EnR002MonitoringRecord());

        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
                username, companyId, request);

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        verify(monitoringRecordSolarRepository).save(argThat(
                entity -> entity.getElpjBatterySpecificationUrl() == null));
    }

    // @Test
    // @DisplayName("POST should still require changedBattery")
    // void testRegisterMonitoringRecord_MissingChangedBattery_ValidationError() {
    //     String username = "multi_site_master";
    //     String companyId = "COMP00000002";
    //     String projectId = "PROJ0001";
    //     String siteId = "21";
    //     RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
    //     request.setChangedBattery(null);
    //     request.setElpjBatterySpecificationUrl(null);

    //     stubValidMonitoringContext(username, projectId, siteId);

    //     MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
    //             username, companyId, request);

    //     assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
    //     assertEquals(ResponseConstants.MSG_CHANGED_BATTERY_REQUIRED,
    //             response.getErrors().get("changedBattery"));
    //     verify(monitoringRecordSolarRepository, never()).save(any());
    // }

    @Test
    @DisplayName("POST should succeed without changedBattery when equipment has no battery")
    void testRegisterMonitoringRecord_NoBatteryEquipment_ChangedBatteryOmitted_Success() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";
        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjBatteryValue(null);
        request.setElpjBatterySpecificationUrl(null);

        stubValidMonitoringContext(username, projectId, siteId);
        EnR002Equipment noBatteryEquipment = new EnR002Equipment();
        noBatteryEquipment.setId(100);
        noBatteryEquipment.setSite(tokyoSite);
        noBatteryEquipment.setHasBattery(false);
        when(equipmentRepository.findById(100)).thenReturn(Optional.of(noBatteryEquipment));
        when(monitoringRecordSolarRepository.save(any())).thenReturn(new EnR002MonitoringRecord());

        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
                username, companyId, request);

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        verify(monitoringRecordSolarRepository, times(1)).save(any(EnR002MonitoringRecord.class));
    }

    @Test
    @DisplayName("POST should reject changedBattery=true when equipment has no battery")
    void testRegisterMonitoringRecord_NoBatteryEquipment_ChangedBatteryTrue_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";
        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjBatteryValue(new BigDecimal("10.00"));
        request.setElpjBatterySpecificationUrl("https://example.com/battery.pdf");

        stubValidMonitoringContext(username, projectId, siteId);
        EnR002Equipment noBatteryEquipment = new EnR002Equipment();
        noBatteryEquipment.setId(100);
        noBatteryEquipment.setSite(tokyoSite);
        noBatteryEquipment.setHasBattery(false);
        when(equipmentRepository.findById(100)).thenReturn(Optional.of(noBatteryEquipment));

        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
                username, companyId, request);

        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_BATTERY_NOT_APPLICABLE,
                response.getErrors().get("changedBattery"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST should reject stray ELPJ_battery_value when equipment has no battery, even without changedBattery")
    void testRegisterMonitoringRecord_NoBatteryEquipment_StrayBatteryValue_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";
        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjBatteryValue(new BigDecimal("10.00"));
        request.setElpjBatterySpecificationUrl(null);

        stubValidMonitoringContext(username, projectId, siteId);
        EnR002Equipment noBatteryEquipment = new EnR002Equipment();
        noBatteryEquipment.setId(100);
        noBatteryEquipment.setSite(tokyoSite);
        noBatteryEquipment.setHasBattery(false);
        when(equipmentRepository.findById(100)).thenReturn(Optional.of(noBatteryEquipment));

        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
                username, companyId, request);

        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_BATTERY_NOT_APPLICABLE,
                response.getErrors().get("changedBattery"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    //数値モニタリングデータに負の値が含まれている場合は拒否する必要があります〇
    @DisplayName("Should reject when numerical monitoring data has negative values")
    void testRegisterMonitoringRecord_NegativeValues_ValidationError() {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        // Set negative value
        //負の値を設定する
        request.setElPv(new BigDecimal("-100"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_VALIDATION_ERROR, response.getMessage());
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    //サイトとプロジェクトの関係が存在しない場合は拒否するべきです〇
    @DisplayName("Should reject when site-project relation doesn't exist")
    void testRegisterMonitoringRecord_MissingRelation_NotFound() {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
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
        String projectId = "PROJ0001";
        String siteId = "21";

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
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(wrongCompanySite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_SITE_NOT_IN_COMPANY, response.getMessage());
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
        String projectId = "PROJ0001";
        String siteId = "21";

        EnR002MonitoringRecord entity = new EnR002MonitoringRecord();
        // PK format: sitePublicId#projectPublicId (companyId still validated via user/site/project entities)
        // PK形式: サイト公開ID#プロジェクト公開ID（companyIdは引き続きユーザー/サイト/プロジェクトのエンティティで検証されます）
        entity.setId(siteId + "#" + projectId);
        entity.setProjectID(1);
        entity.setSiteID(Integer.parseInt(siteId));
        entity.setElPv(new BigDecimal("100"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
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
        String projectId = "PROJ0001";
        String siteId = "22"; // Fukuoka - not assigned

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(regularUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(fukuokaSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));

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
        String projectId = "PROJ0001";
        String siteId = "21";

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
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
        String projectId = "PROJ0001";
        String siteId = "21";

        MultipartFile file = createMockFile("test.pdf", "pdf", 1024);
        setupMockFileForUpload(file, "pdf", 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation spr1 = new SiteProjectRelation();
        spr1.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(spr1));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
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
        String projectId = "PROJ0001";
        String siteId = "21";

        // 15MB file
        MultipartFile file = createMockFile("large.pdf", "pdf", 15 * 1024 * 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation siteProjectRelation = new SiteProjectRelation();
        siteProjectRelation.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(siteProjectRelation));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));

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
        String projectId = "PROJ0001";
        String siteId = "21";

        // .txt is not allowed for ELPV
        MultipartFile file = createMockFile("test.txt", "txt", 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation siteProjectRelation = new SiteProjectRelation();
        siteProjectRelation.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(siteProjectRelation));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));

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
        String projectId = "PROJ0001";
        String siteId = "22"; // Fukuoka - not assigned

        MultipartFile file = createMockFile("test.pdf", "pdf", 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(regularUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(fukuokaSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
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
        String projectId = "PROJ0001";
        String siteId = "21";

        MultipartFile file = createMockFile("test.png", "png", 2048);        setupMockFileForUpload(file, "png", 2048);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation spr2 = new SiteProjectRelation();
        spr2.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(spr2));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
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
        String projectId = "PROJ0001";
        String siteId = "21";

        MultipartFile file = createMockFile("battery.jpg", "jpg", 1500);
        setupMockFileForUpload(file, "jpg", 1500);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation spr3 = new SiteProjectRelation();
        spr3.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(spr3));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        // Setup S3Client mock for this test
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(null);

        // Act
        FileUploadResponse response = monitoringService.uploadElpjBatterySpecificationEvidence(username, companyId, projectId, siteId, "Battery Spec", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertEquals("ELPJ_BATTERY_EVIDENCE", response.getFile().getFileType());
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    //ELPJコントロールの証拠をアップロードする必要があります
    @DisplayName("Should upload ELPJ Control evidence")
    void testUploadElpjControlEvidence_Success() throws IOException {
        // Arrange
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        MultipartFile file = createMockFile("control.pdf", "pdf", 3000);
        setupMockFileForUpload(file, "pdf", 3000);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation spr4 = new SiteProjectRelation();
        spr4.setProject(project);
        when(tokyoSite.getSiteProjectRelations()).thenReturn(Arrays.asList(spr4));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
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
        String projectId = "PROJ0001";
        String siteId = "21";

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
        String projectId = "PROJ0001";
        String siteId = "21";

        // Create a mock Site with null company
        Site brokenSite = mock(Site.class, withSettings().lenient());

        MultipartFile file = createMockFile("test.pdf", "pdf", 1024);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(brokenSite));
        when(brokenSite.getId()).thenReturn(Integer.parseInt(siteId));
        when(brokenSite.getCompany()).thenReturn(null); // NULL company FK!
        // Setup site with project relations for deriveProjectIdFromSite
        SiteProjectRelation siteProjectRelation = new SiteProjectRelation();
        siteProjectRelation.setProject(project);
        when(brokenSite.getSiteProjectRelations()).thenReturn(Arrays.asList(siteProjectRelation));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));

        // Act
        FileUploadResponse response = monitoringService.uploadElpvEvidence(username, companyId, projectId, siteId, "Test", file);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_SITE_HAS_NO_COMPANY, response.getMessage());
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
        String projectId = "PROJ0001";
        String siteId = "21";

        ParticipatingUser unknownRoleUser = new ParticipatingUser();
        unknownRoleUser.setId(99);
        unknownRoleUser.setUsername(username);
        ParticipatingCompany unknownRoleCompany = new ParticipatingCompany(); // ✅ Fix: was unknownRoleUser.setCompanyId(companyId)
        unknownRoleCompany.setId(Id);
        unknownRoleCompany.setCompanyId(companyId);
        unknownRoleUser.setCompany(unknownRoleCompany);
        unknownRoleUser.setRole("admin"); // Unknown role
        unknownRoleUser.setIsActive(true);

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(unknownRoleUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));

        // Act
        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(username, companyId, request);

        // Assert
        assertNotNull(response);
        assertEquals(ResponseConstants.FORBIDDEN, response.getStatusCode());
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when dateRecorded is after today in Japan")
    void testRegisterMonitoringRecord_FutureDateRecorded_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setDateRecorded(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).plusDays(1).toString());

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(10)));

        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_DATE_RECORDED_RANGE, response.getErrors().get("dateRecorded"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when dateRecorded is before activity start date")
    void testRegisterMonitoringRecord_DateRecordedBeforeActivityStart_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";
        LocalDate todayInJapan = LocalDate.now(java.time.ZoneId.of("Asia/Tokyo"));

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setDateRecorded(todayInJapan.minusDays(1).toString());

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(todayInJapan));

        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_DATE_RECORDED_RANGE, response.getErrors().get("dateRecorded"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when ELPVR is greater than ELPV")
    void testRegisterMonitoringRecord_ElpvrExceedsElpv_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";

        String projectId = "PROJ0001";
        String siteId = "21";
        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElPv(new BigDecimal("100.000"));
        request.setElPvr(new BigDecimal("100.001"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(10)));

        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_ELPVR_EXCEEDS_ELPV, response.getErrors().get("ELpvr_value"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject duplicate monitoring record for same project, site, and dateRecorded")
    void testRegisterMonitoringRecord_DuplicateDateRecorded_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(10)));
        when(monitoringRecordSolarRepository.existsActiveByRecordIdAndDateRecorded(
                siteId + "#" + projectId,
                request.getDateRecorded()
        )).thenReturn(true);

        MonitoringRecordSolarResponse response = monitoringService.registerMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_DATE_RECORDED_DUPLICATE, response.getErrors().get("dateRecorded"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validate endpoint should reject ELPVR greater than ELPV before confirmation page")
    void testValidateMonitoringRecord_ElpvrExceedsElpv_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElPv(new BigDecimal("10.000"));
        request.setElPvr(new BigDecimal("10.001"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(10)));

        BaseResponse response = monitoringService.validateMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_ELPVR_EXCEEDS_ELPV, response.getErrors().get("ELpvr_value"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validate endpoint should require battery evidence when changedBattery is true")
    void testValidateMonitoringRecord_ChangedBatteryWithoutEvidence_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";
        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjBatteryValue(new BigDecimal("10.00"));
        request.setElpjBatterySpecificationUrl("   ");

        stubValidMonitoringContext(username, projectId, siteId);

        BaseResponse response = monitoringService.validateMonitoringRecordSolar(
                username, companyId, request);

        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_ELPJ_BATTERY_EVIDENCE_REQUIRED,
                response.getErrors().get("elpj_battery_evidence"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validate endpoint should allow missing battery evidence when changedBattery is false")
    void testValidateMonitoringRecord_UnchangedBatteryWithoutEvidence_Success() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";
        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjBatterySpecificationUrl(null);

        stubValidMonitoringContext(username, projectId, siteId);

        BaseResponse response = monitoringService.validateMonitoringRecordSolar(
                username, companyId, request);

        assertEquals(ResponseConstants.OK, response.getStatusCode());
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validate endpoint should aggregate required field errors with future date")
    void testValidateMonitoringRecord_EmptyValuesAndFutureDate_AggregatesErrors() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setDateRecorded(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).plusDays(1).toString());
        request.setElPv(null);
        request.setElPvr(null);
        request.setElpjControl(null);
        request.setElpjAccuracy(null);
        request.setElPvEvidenceUrl(null);
        request.setElPvrEvidenceUrl(null);
        request.setElpjBatterySpecificationUrl(null);
        request.setElpjControlEvidenceUrl(null);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(10)));

        BaseResponse response = monitoringService.validateMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_DATE_RECORDED_RANGE, response.getErrors().get("dateRecorded"));
        assertEquals(ResponseConstants.MSG_ELPV_VALUE_MIN, response.getErrors().get("ELpv_value"));
        assertEquals(ResponseConstants.MSG_ELPVR_VALUE_MIN, response.getErrors().get("ELpvr_value"));
        assertEquals(ResponseConstants.MSG_ELPJ_ACCURACY_REQUIRED, response.getErrors().get("ELPJ_accuracy"));
        assertEquals(ResponseConstants.MSG_ELPV_EVIDENCE_REQUIRED, response.getErrors().get("elpv_evidence"));
        assertEquals(ResponseConstants.MSG_ELPVR_EVIDENCE_REQUIRED, response.getErrors().get("elpvr_evidence"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validate endpoint should reject missing ELPJ accuracy")
    void testValidateMonitoringRecord_MissingAccuracy_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjAccuracy(null);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(10)));

        BaseResponse response = monitoringService.validateMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_ELPJ_ACCURACY_REQUIRED, response.getErrors().get("ELPJ_accuracy"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validate endpoint should reject ELPJ accuracy below zero")
    void testValidateMonitoringRecord_AccuracyBelowZero_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjAccuracy(new BigDecimal("-0.01"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(10)));

        BaseResponse response = monitoringService.validateMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_ELPJ_ACCURACY_MIN, response.getErrors().get("ELPJ_accuracy"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validate endpoint should reject ELPJ accuracy above 100")
    void testValidateMonitoringRecord_AccuracyAboveMax_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);
        request.setElpjAccuracy(new BigDecimal("100.01"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(10)));

        BaseResponse response = monitoringService.validateMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_ELPJ_ACCURACY_MAX, response.getErrors().get("ELPJ_accuracy"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validate endpoint should reject duplicate date before confirmation page")
    void testValidateMonitoringRecord_DuplicateDateRecorded_ValidationError() {
        String username = "multi_site_master";
        String companyId = "COMP00000002";
        String projectId = "PROJ0001";
        String siteId = "21";

        RegisterMonitoringRecordSolarRequest request = buildValidMonitoringRequest(projectId, siteId);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(1, Integer.parseInt(siteId)))
                .thenReturn(buildRelation(LocalDate.now(java.time.ZoneId.of("Asia/Tokyo")).minusDays(10)));
        when(monitoringRecordSolarRepository.existsActiveByRecordIdAndDateRecorded(
                siteId + "#" + projectId,
                request.getDateRecorded()
        )).thenReturn(true);

        BaseResponse response = monitoringService.validateMonitoringRecordSolar(
                username,
                companyId,
                request
        );

        assertNotNull(response);
        assertEquals(ResponseConstants.VALIDATION_ERROR, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_DATE_RECORDED_DUPLICATE, response.getErrors().get("dateRecorded"));
        verify(monitoringRecordSolarRepository, never()).save(any());
    }

    // ============ TEST HELPER METHODS ============

    private RegisterMonitoringRecordSolarRequest buildValidMonitoringRequest(String projectId, String siteId) {
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId(projectId);
        request.setSiteId(siteId);
        request.setEquipmentId(100);
        request.setUserId(13); // Master user ID
        request.setElPvEvidenceUrl("https://example.com/elpv.pdf");
        request.setElPvrEvidenceUrl("https://example.com/elpvr.pdf");
        request.setElpjBatterySpecificationUrl("https://example.com/battery.pdf");
        request.setElpjControlEvidenceUrl("https://example.com/control.pdf");
        request.setElPv(new BigDecimal("100.50"));
        request.setElPvr(new BigDecimal("50.25"));
        request.setElpjControl(new BigDecimal("75.00"));
        request.setElpjAccuracy(new BigDecimal("95.5"));
        request.setDateRecorded(LocalDate.now().toString()); // Convert to String
        request.setProjectType("ENR002");
        return request;
    }

    private void stubValidMonitoringContext(String username, String projectId, String siteId) {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(masterUser));
        when(siteRepository.findByPublicIdWithFallback(siteId)).thenReturn(Optional.of(tokyoSite));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.findByPublicIdWithFallback(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, tokyoSite)).thenReturn(true);
    }

    private SiteProjectRelation buildRelation(LocalDate activityStartDate) {
        SiteProjectRelation relation = new SiteProjectRelation();
        relation.setProject(project);
        relation.setSite(tokyoSite);
        relation.setActivityStartDate(activityStartDate);
        return relation;
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
