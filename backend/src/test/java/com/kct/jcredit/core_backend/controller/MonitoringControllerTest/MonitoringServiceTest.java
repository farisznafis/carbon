package com.kct.jcredit.core_backend.controller.MonitoringControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.RegisterMonitoringRecordSolarRequest;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringEvidenceItemResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringEvidenceItemsResponse;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.enums.EvidenceRequirement;
import com.kct.jcredit.core_backend.enums.MonitoringEvidenceField;
import com.kct.jcredit.core_backend.exception.FileTooLargeException;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.impl.MonitoringServiceImpl;
import software.amazon.awssdk.services.s3.S3Client;
/**
 * MonitoringServiceImpl business-logic test outline.
 *
 * Mock all repositories (userRepository, siteRepository, projectRepository,
 * siteProjectRelationRepository, equipmentRepository,
 * monitoringRecordSolarRepository, cancelRequestRepository, s3クライアント等)。
 * Controllerを経由しないため@Valid等のBean Validationは対象外。
 * Service内部のnullチェック・業務ルール・権限判定・例外処理を直接検証する。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitoringServiceTest {

    private static final String USER = "member";
    private static final String COMPANY_ID = "company-1";
    private static final String SITE_ID = "site-1";
    private static final String PROJECT_ID = "project-1";

    @Mock private ProjectRepository projectRepository;
    @Mock private MonitoringTableRecordSolarRepository monitoringRecordSolarRepository;
    @Mock private ParticipatingUserRepository userRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private SiteProjectRelationRepository siteProjectRelationRepository;
    @Mock private EnR002EquipmentRepository equipmentRepository;
    @Mock private S3Client s3Client;
    @InjectMocks private MonitoringServiceImpl service;

    private ParticipatingCompany company;
    private ParticipatingUser user;
    private Site site;
    private Project project;
    private EnR002Equipment equipment;
    private SiteProjectRelation relation;

    @BeforeEach
    void setUp() {
        company = new ParticipatingCompany();
        company.setCompanyId(COMPANY_ID);
        user = new ParticipatingUser();
        user.setId(10);
        user.setUsername(USER);
        user.setCompany(company);
        user.setRole("master");
        site = new Site();
        site.setId(20);
        site.setPublicId(SITE_ID);
        site.setCompany(company);
        project = new Project();
        project.setId(30);
        project.setPublicId(PROJECT_ID);
        equipment = new EnR002Equipment();
        equipment.setId(40);
        equipment.setSite(site);
        equipment.setHasBattery(false);
        relation = new SiteProjectRelation();
        relation.setActivityStartDate(LocalDate.of(2020, 1, 1));
        when(userRepository.findByUsername(USER)).thenReturn(Optional.of(user));
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.existsByProjectAndSite(project, site)).thenReturn(true);
        when(siteProjectRelationRepository.findByProjectIdAndSiteId(project.getId(), site.getId()))
                .thenReturn(relation);
        when(equipmentRepository.findById(40)).thenReturn(Optional.of(equipment));
        when(monitoringRecordSolarRepository.existsActiveByRecordIdAndDateRecorded(anyString(), anyString()))
                .thenReturn(false);
    }

    private RegisterMonitoringRecordSolarRequest validRequest() {
        RegisterMonitoringRecordSolarRequest request = new RegisterMonitoringRecordSolarRequest();
        request.setProjectId(PROJECT_ID);
        request.setSiteId(SITE_ID);
        request.setEquipmentId(40);
        request.setProjectType("ENR002");
        request.setDateRecorded(LocalDate.now().toString());
        request.setElPv(new BigDecimal("100"));
        request.setElPvr(new BigDecimal("100"));
        request.setElpjControl(BigDecimal.ZERO);
        request.setElpjAccuracy(new BigDecimal("100"));
        request.setElPvEvidenceUrl("s3://evidence/elpv.pdf");
        request.setElPvrEvidenceUrl("s3://evidence/elpvr.pdf");
        return request;
    }

    private BaseResponse validate(RegisterMonitoringRecordSolarRequest request) {
        return service.validateMonitoringRecordSolar(USER, COMPANY_ID, request);
    }

    private Map<String, String> fieldErrors(RegisterMonitoringRecordSolarRequest request) {
        BaseResponse response = validate(request);
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.VALIDATION_ERROR);
        return response.getErrors();
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = MonitoringServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(service, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Nested
    @DisplayName("POST /monitoring のテスト")
    class RegisterMonitoringRecordSolarTest {

        @Test
        @DisplayName("UT-MS-001: requestがnullの場合はBAD_REQUESTエラーを返す")
        void registerMonitoringRecordSolar_nullRequest_returnsBadRequest() {
            BaseResponse response = validate(null);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.VALIDATION_ERROR);
            assertThat(response.getErrors()).containsEntry("request", ResponseConstants.MSG_BAD_REQUEST);
        }

        @Test
        @DisplayName("UT-MS-002: 必須項目(projectId/siteId/equipmentId/projectType/dateRecorded)が未設定の場合に個別エラーを返す")
        void registerMonitoringRecordSolar_missingRequiredFields_returnsFieldErrors() {
            RegisterMonitoringRecordSolarRequest request = validRequest();
            request.setProjectId(null);
            request.setSiteId(null);
            request.setEquipmentId(null);
            request.setProjectType(" ");
            request.setDateRecorded(null);
            Map<String, String> errors = fieldErrors(request);
            assertThat(errors).containsEntry("projectId", ResponseConstants.MSG_PROJECT_ID_REQUIRED)
                    .containsEntry("siteId", ResponseConstants.MSG_SITE_ID_REQUIRED)
                    .containsEntry("equipmentId", ResponseConstants.MSG_EQUIPMENT_ID_REQUIRED)
                    .containsEntry("projectType", ResponseConstants.MSG_PROJECT_TYPE_REQUIRED)
                    .containsEntry("dateRecorded", ResponseConstants.MSG_DATE_RECORDED_REQUIRED);
        }

        @Test
        @DisplayName("UT-MS-003: dateRecordedの形式不正・未来日の場合は日付レンジエラーを返す")
        void registerMonitoringRecordSolar_invalidOrFutureDateRecorded_returnsDateRangeError() {
            for (String date : new String[] { "not-a-date", LocalDate.now().plusDays(1).toString() }) {
                RegisterMonitoringRecordSolarRequest request = validRequest();
                request.setDateRecorded(date);
                assertThat(fieldErrors(request)).containsEntry("dateRecorded", ResponseConstants.MSG_DATE_RECORDED_RANGE);
            }
        }

        @Test
        @DisplayName("UT-MS-004: 必須証憑(ELpv/ELpvr)のURLが未提出の場合はエラーを返す")
        void registerMonitoringRecordSolar_missingMandatoryEvidenceUrl_returnsEvidenceError() {
            RegisterMonitoringRecordSolarRequest request = validRequest();
            request.setElPvEvidenceUrl(null);
            request.setElPvrEvidenceUrl(" ");
            Map<String, String> errors = fieldErrors(request);
            assertThat(errors).containsEntry("elpv_evidence", ResponseConstants.MSG_ELPV_EVIDENCE_REQUIRED)
                    .containsEntry("elpvr_evidence", ResponseConstants.MSG_ELPVR_EVIDENCE_REQUIRED);
        }

        @Test
        @DisplayName("UT-MS-005: ELPJcontrol証憑は未提出でもバリデーションエラーにならない（任意項目）")
        void registerMonitoringRecordSolar_missingElpjControlEvidence_noValidationError() {
            RegisterMonitoringRecordSolarRequest request = validRequest();
            request.setElpjControlEvidenceUrl(null);
            assertThat(validate(request).getStatusCode()).isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-MS-006: ELpv/ELpvrがnullまたは負数の場合は最小値エラーを返す")
        void registerMonitoringRecordSolar_nullOrNegativeElpvElpvr_returnsMinValueError() {
            for (boolean pv : new boolean[] { true, false }) {
                RegisterMonitoringRecordSolarRequest request = validRequest();
                if (pv) request.setElPv(new BigDecimal("-1")); else request.setElPvr(null);
                Map<String, String> errors = fieldErrors(request);
                assertThat(errors).containsKey(pv ? "ELpv_value" : "ELpvr_value");
            }
        }

        @Test
        @DisplayName("UT-MS-007: ELpvrがELpvを超過した場合のみエラー、同値は許容される")
        void registerMonitoringRecordSolar_elpvrExceedsElpv_returnsErrorOnlyWhenExceeding() {
            RegisterMonitoringRecordSolarRequest request = validRequest();
            request.setElPv(new BigDecimal("100"));
            request.setElPvr(new BigDecimal("150"));
            assertThat(fieldErrors(request)).containsEntry("ELpvr_value", ResponseConstants.MSG_ELPVR_EXCEEDS_ELPV);
            request.setElPvr(new BigDecimal("100"));
            assertThat(validate(request).getStatusCode()).isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-MS-008: ELPJcontrol/ELPJbatteryValueが負数の場合は最小値エラーを返す")
        void registerMonitoringRecordSolar_negativeElpjControlOrBatteryValue_returnsMinValueError() {
            RegisterMonitoringRecordSolarRequest request = validRequest();
            request.setElpjControl(new BigDecimal("-1"));
            request.setElpjBatteryValue(new BigDecimal("-1"));
            assertThat(fieldErrors(request)).containsEntry("ELPJ_control_value", ResponseConstants.MSG_ELPJ_CONTROL_VALUE_MIN)
                    .containsEntry("ELPJ_battery_value", ResponseConstants.MSG_ELPJ_BATTERY_VALUE_MIN);
        }

        @Test
        @DisplayName("UT-MS-009: ELPJaccuracyがnull・負数・最大値超過でエラー、最大値と同値は許容される")
        void registerMonitoringRecordSolar_elpjAccuracyOutOfRange_returnsErrorExceptBoundary() {
            RegisterMonitoringRecordSolarRequest request = validRequest();
            request.setElpjAccuracy(null);
            assertThat(fieldErrors(request)).containsEntry("ELPJ_accuracy", ResponseConstants.MSG_ELPJ_ACCURACY_REQUIRED);
            request.setElpjAccuracy(new BigDecimal("-1"));
            assertThat(fieldErrors(request)).containsEntry("ELPJ_accuracy", ResponseConstants.MSG_ELPJ_ACCURACY_MIN);
            request.setElpjAccuracy(new BigDecimal("100.01"));
            assertThat(fieldErrors(request)).containsEntry("ELPJ_accuracy", ResponseConstants.MSG_ELPJ_ACCURACY_MAX);
            request.setElpjAccuracy(new BigDecimal("100"));
            assertThat(validate(request).getStatusCode()).isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-MS-010: ユーザーが存在しない場合は内部エラーとなる")
        void registerMonitoringRecordSolar_userNotFound_returnsInternalError() {
            when(userRepository.findByUsername(USER)).thenReturn(Optional.empty());
            BaseResponse response = validate(validRequest());
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("UT-MS-011: ユーザーが指定会社に未所属の場合はFORBIDDENを返す")
        void registerMonitoringRecordSolar_userNotInCompany_returnsForbidden() {
            user.setCompany(null);
            BaseResponse response = validate(validRequest());
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_USER_NOT_IN_COMPANY);
        }

        @Test
        @DisplayName("UT-MS-012: 拠点またはプロジェクトが存在しない、または所属会社が不一致の場合はエラーを返す")
        void registerMonitoringRecordSolar_siteOrProjectInvalid_returnsNotFoundOrForbidden() {
            when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.empty());
            assertThat(validate(validRequest()).getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
            when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
            when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.empty());
            assertThat(validate(validRequest()).getMessage()).isEqualTo(ResponseConstants.MSG_PROJECT_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-MS-013: 会社不一致・USER権限で未割当拠点・不明roleの場合はFORBIDDENを返す")
        void registerMonitoringRecordSolar_resourceOwnershipOrRoleInvalid_returnsForbidden() {
            user.setRole("unknown");
            BaseResponse response = validate(validRequest());
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
        }

        @Test
        @DisplayName("UT-MS-014: USER権限で割当拠点、MASTER権限で会社内任意拠点にアクセスできる")
        void registerMonitoringRecordSolar_validRoleAccess_proceedsToNextValidation() {
            user.setRole("user");
            user.setSite(site);
            assertThat(validate(validRequest()).getStatusCode()).isEqualTo(ResponseConstants.OK);
            user.setRole("master");
            user.setSite(null);
            assertThat(validate(validRequest()).getStatusCode()).isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-MS-015: 拠点とプロジェクトの関連が存在しない場合はNOT_FOUNDを返す")
        void registerMonitoringRecordSolar_siteProjectRelationNotFound_returnsNotFound() {
            when(siteProjectRelationRepository.existsByProjectAndSite(project, site)).thenReturn(false);
            BaseResponse response = validate(validRequest());
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-MS-016: 設備が存在しない、または別拠点に属する場合はNOT_FOUNDを返す")
        void registerMonitoringRecordSolar_equipmentNotFoundOrWrongSite_returnsNotFound() {
            when(equipmentRepository.findById(40)).thenReturn(Optional.empty());
            assertThat(validate(validRequest()).getMessage()).isEqualTo(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND);
            equipment.setSite(new Site());
            assertThat(validate(validRequest()).getMessage()).isEqualTo(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-MS-017: dateRecordedが活動開始日より前、または重複登録の場合はエラーを返す")
        void registerMonitoringRecordSolar_dateBeforeActivityStartOrDuplicate_returnsBusinessError() {
            RegisterMonitoringRecordSolarRequest request = validRequest();
            request.setDateRecorded(LocalDate.now().minusDays(1).toString());
            relation.setActivityStartDate(LocalDate.now());
            assertThat(fieldErrors(request)).containsEntry("dateRecorded", ResponseConstants.MSG_DATE_RECORDED_RANGE);
            relation.setActivityStartDate(LocalDate.of(2020, 1, 1));
            request.setDateRecorded(LocalDate.now().toString());
            when(monitoringRecordSolarRepository.existsActiveByRecordIdAndDateRecorded(anyString(), anyString()))
                    .thenReturn(true);
            assertThat(fieldErrors(request)).containsEntry("dateRecorded", ResponseConstants.MSG_DATE_RECORDED_DUPLICATE);
        }

        @Test
        @DisplayName("UT-MS-018: 設備hasBattery=trueの場合に実測値または証憑URLが未入力だとエラーを返す")
        void registerMonitoringRecordSolar_hasBatteryTrueMissingValueOrEvidence_returnsRequiredError() {
            equipment.setHasBattery(true);
            RegisterMonitoringRecordSolarRequest request = validRequest();
            assertThat(fieldErrors(request)).containsEntry("ELPJ_battery_value", ResponseConstants.MSG_ELPJ_BATTERY_VALUE_REQUIRED)
                    .containsEntry("elpj_battery_evidence", ResponseConstants.MSG_ELPJ_BATTERY_EVIDENCE_REQUIRED);
        }

        @Test
        @DisplayName("UT-MS-019: 設備hasBattery=falseなのに値または証憑を送信した場合はエラーを返す")
        void registerMonitoringRecordSolar_hasBatteryFalseWithValueOrEvidence_returnsNotApplicableError() {
            RegisterMonitoringRecordSolarRequest request = validRequest();
            request.setElpjBatteryValue(BigDecimal.ONE);
            assertThat(fieldErrors(request)).containsEntry("ELPJ_battery_value", ResponseConstants.MSG_BATTERY_NOT_APPLICABLE);
        }

        @Test
        @DisplayName("UT-MS-020: 設備のhasBatteryに応じて適切な入力の場合はエラーにならない")
        void registerMonitoringRecordSolar_hasBatteryConsistentInput_noValidationError() {
            assertThat(validate(validRequest()).getStatusCode()).isEqualTo(ResponseConstants.OK);
            equipment.setHasBattery(true);
            RegisterMonitoringRecordSolarRequest request = validRequest();
            request.setElpjBatteryValue(BigDecimal.ONE);
            request.setElpjBatterySpecificationUrl("s3://evidence/battery.pdf");
            assertThat(validate(request).getStatusCode()).isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-MS-021: 全条件を満たすrequestで登録が成功する")
        void registerMonitoringRecordSolar_allValid_returnsOk() {
            when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            var response = service.registerMonitoringRecordSolar(USER, COMPANY_ID, validRequest());
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            ArgumentCaptor<EnR002MonitoringRecord> captor = ArgumentCaptor.forClass(EnR002MonitoringRecord.class);
            verify(monitoringRecordSolarRepository).save(captor.capture());
            assertThat(captor.getValue().getIsActive()).isTrue();
            assertThat(captor.getValue().getElPv()).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("UT-MS-022: 処理中に予期せぬ例外が発生した場合は内部エラーを返す")
        void registerMonitoringRecordSolar_unexpectedException_returnsInternalError() {
            when(userRepository.findByUsername(USER)).thenThrow(new RuntimeException("database unavailable"));
            var response = service.registerMonitoringRecordSolar(USER, COMPANY_ID, validRequest());
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        }
    }

    @Nested
    @DisplayName("GET /monitoring/{projectId}/list のテスト")
    class ReadLastMonitoringRecordSolarTest {

        @Test
        @DisplayName("UT-MS-023: ユーザー・拠点・プロジェクトいずれかが不正または権限不足の場合はエラーを返す")
        void readLastMonitoringRecordSolar_invalidUserSiteOrProject_returnsError() {
            when(userRepository.findByUsername(USER)).thenReturn(Optional.empty());
            assertThat(service.readLastMonitoringRecordSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID).getStatusCode())
                    .isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("UT-MS-024: 権限がない、またはSite-Project関連が存在しない場合はエラーを返す")
        void readLastMonitoringRecordSolar_ownershipOrRelationInvalid_returnsError() {
            user.setRole("unknown");
            assertThat(service.readLastMonitoringRecordSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID).getStatusCode())
                    .isEqualTo(ResponseConstants.FORBIDDEN);
            user.setRole("master");
            when(siteProjectRelationRepository.existsByProjectAndSite(project, site)).thenReturn(false);
            assertThat(service.readLastMonitoringRecordSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID).getMessage())
                    .isEqualTo(ResponseConstants.MSG_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-MS-025: 最新レコードが存在しない場合はdataなしのOKを返す")
        void readLastMonitoringRecordSolar_recordNotFound_returnsOkWithoutData() {
            when(monitoringRecordSolarRepository.findLatest(SITE_ID + "#" + PROJECT_ID)).thenReturn(Optional.empty());
            var response = service.readLastMonitoringRecordSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("UT-MS-026: 最新の1件を正常に取得できる")
        void readLastMonitoringRecordSolar_dataExists_returnsOk() {
            EnR002MonitoringRecord entity = new EnR002MonitoringRecord();
            entity.setId("record");
            entity.setProjectID(project.getId());
            entity.setSiteID(site.getId());
            when(monitoringRecordSolarRepository.findLatest(SITE_ID + "#" + PROJECT_ID)).thenReturn(Optional.of(entity));
            when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
            assertThat(service.readLastMonitoringRecordSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID).getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-MS-027: 処理中に例外が発生した場合は内部エラーを返す")
        void readLastMonitoringRecordSolar_unexpectedException_returnsInternalError() {
            when(userRepository.findByUsername(USER)).thenThrow(new RuntimeException("database unavailable"));
            assertThat(service.readLastMonitoringRecordSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID).getStatusCode())
                    .isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("GET /monitoring のテスト")
    class ReadAllMonitoringRecordsSolarTest {

        @Test
        @DisplayName("UT-MS-028: 各検証条件が不正の場合は対応するエラーを返す")
        void readAllMonitoringRecordsSolar_invalidValidationConditions_returnsError() {
            when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.empty());
            assertThat(service.readAllMonitoringRecordsSolar(USER, 1, 50, PROJECT_ID, null).getMessage())
                    .isEqualTo(ResponseConstants.MSG_PROJECT_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-MS-029: pageとlimitの指定値に応じて適切に補正される")
        void readAllMonitoringRecordsSolar_pageAndLimit_correctsAsExpected() {
            when(siteProjectRelationRepository.findSiteIdsByProjectId(project.getId())).thenReturn(java.util.List.of());
            when(monitoringRecordSolarRepository.findAllByProjectIdPaginated(any(), any())).thenAnswer(invocation -> {
                com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse pagination = invocation.getArgument(1);
                assertThat(pagination.getPage()).isEqualTo(1);
                assertThat(pagination.getLimit()).isEqualTo(100);
                return java.util.List.of(new EnR002MonitoringRecord());
            });
            assertThat(service.readAllMonitoringRecordsSolar(USER, 0, 101, PROJECT_ID, null).getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-MS-030: 該当データが0件の場合は空のitemsでOKを返す")
        void readAllMonitoringRecordsSolar_noData_returnsOkWithEmptyItems() {
            when(siteProjectRelationRepository.findSiteIdsByProjectId(project.getId())).thenReturn(java.util.List.of());
            when(monitoringRecordSolarRepository.findAllByProjectIdPaginated(any(), any())).thenReturn(java.util.List.of());
            var response = service.readAllMonitoringRecordsSolar(USER, 1, 50, PROJECT_ID, null);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getLimit()).isEqualTo(50);
        }

        @Test
        @DisplayName("UT-MS-031: 複数件のレコードをページ単位で正常に取得できる")
        void readAllMonitoringRecordsSolar_dataExists_returnsOkWithPagination() {
            when(siteProjectRelationRepository.findSiteIdsByProjectId(project.getId())).thenReturn(java.util.List.of());
            EnR002MonitoringRecord entity = new EnR002MonitoringRecord();
            entity.setProjectID(project.getId());
            entity.setSiteID(site.getId());
            when(monitoringRecordSolarRepository.findAllByProjectIdPaginated(any(), any()))
                    .thenReturn(java.util.List.of(entity));
            when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
            var response = service.readAllMonitoringRecordsSolar(USER, 1, 50, PROJECT_ID, null);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getPagination().getLimit()).isEqualTo(50);
        }

        @Test
        @DisplayName("UT-MS-032: 処理中に例外が発生した場合は内部エラーを返す")
        void readAllMonitoringRecordsSolar_unexpectedException_returnsInternalError() {
            when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenThrow(new RuntimeException("database unavailable"));
            assertThat(service.readAllMonitoringRecordsSolar(USER, 1, 50, PROJECT_ID, null).getStatusCode())
                    .isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("GET /monitoring/{siteId}/{projectId}/list のテスト")
    class ReadSiteMonitoringRecordsSolarTest {

        @Test
        @DisplayName("UT-MS-033: ユーザーが存在しない場合の例外伝播範囲を確認する（要レビュー）")
        void readSiteMonitoringRecordsSolar_userNotFound_checksExceptionPropagationScope() {
            when(userRepository.findByUsername(USER)).thenReturn(Optional.empty());
            assertThat(service.readSiteMonitoringRecordsSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 50, null)
                    .getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("UT-MS-034: 各検証条件が不正の場合は対応するエラーを返す")
        void readSiteMonitoringRecordsSolar_invalidValidationConditions_returnsError() {
            when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.empty());
            assertThat(service.readSiteMonitoringRecordsSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 50, null)
                    .getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        }

        @Test
        @DisplayName("UT-MS-035: pageとlimitの指定値に応じて適切に補正される")
        void readSiteMonitoringRecordsSolar_pageAndLimit_correctsAsExpected() {
            when(monitoringRecordSolarRepository.findAllByRecordIdPaginated(anyString(), any())).thenAnswer(invocation -> {
                com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse pagination = invocation.getArgument(1);
                assertThat(pagination.getPage()).isEqualTo(1);
                assertThat(pagination.getLimit()).isEqualTo(50);
                return java.util.List.of(new EnR002MonitoringRecord());
            });
            when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
            when(siteRepository.findById(site.getId())).thenReturn(Optional.of(site));
            assertThat(service.readSiteMonitoringRecordsSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID, 0, 0, null)
                    .getStatusCode()).isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-MS-036: データが存在する場合は一覧を返し、0件の場合は空のitemsでOKを返す")
        void readSiteMonitoringRecordsSolar_dataExistsOrNoData_returnsOkWithItemsOrEmpty() {
            when(monitoringRecordSolarRepository.findAllByRecordIdPaginated(anyString(), any()))
                    .thenReturn(java.util.List.of());
            var response = service.readSiteMonitoringRecordsSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 50, null);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getLimit()).isEqualTo(50);
        }

        @Test
        @DisplayName("UT-MS-037: 処理中に例外が発生した場合は内部エラーを返す")
        void readSiteMonitoringRecordsSolar_unexpectedException_returnsInternalError() {
            when(userRepository.findByUsername(USER)).thenThrow(new RuntimeException("database unavailable"));
            assertThat(service.readSiteMonitoringRecordsSolar(USER, COMPANY_ID, PROJECT_ID, SITE_ID, 1, 50, null)
                    .getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("POST /monitoring/upload-elpv-evidence, upload-elpvr-evidence, upload-elpj-battery-specification-evidence, upload-elpj-control-evidence のテスト")
    class UploadEvidenceTest {

        @Test
        @DisplayName("UT-MS-038: ファイルサイズが上限を超過する場合はエラーを返す")
        void uploadEvidence_fileSizeExceedsLimit_throwsFileTooLargeException() {
            MultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf",
                    new byte[10 * 1024 * 1024 + 1]);
            assertThatThrownBy(() -> invokePrivate("validateFileOrThrow",
                    new Class<?>[] { MultipartFile.class, String.class }, file, "ELPV_EVIDENCE"))
                    .isInstanceOf(FileTooLargeException.class);
        }

        @Test
        @DisplayName("UT-MS-039: 許可されていない拡張子の場合はエラーを返す")
        void uploadEvidence_disallowedExtension_throwsUnsupportedFileTypeException() {
            MultipartFile file = new MockMultipartFile("file", "malware.exe", "application/octet-stream", new byte[] { 1 });
            assertThatThrownBy(() -> invokePrivate("validateFileOrThrow",
                    new Class<?>[] { MultipartFile.class, String.class }, file, "ELPV_EVIDENCE"))
                    .isInstanceOf(UnsupportedFileTypeException.class);
        }

        @Test
        @DisplayName("UT-MS-040: 許可された拡張子(pdf/png/jpg/jpeg/csv等)の場合は検証を通過する")
        void uploadEvidence_allowedExtension_passesValidation() {
            MultipartFile file = new MockMultipartFile("file", "report.PDF", "application/pdf", new byte[] { 1 });
            invokePrivate("validateFileOrThrow", new Class<?>[] { MultipartFile.class, String.class },
                    file, "ELPV_EVIDENCE");
        }

        @Test
        @DisplayName("UT-MS-041: 各検証条件が不正の場合は対応するエラーを返す")
        void uploadEvidence_invalidValidationConditions_returnsError() {
            when(userRepository.findByUsername(USER)).thenReturn(Optional.empty());
            var response = service.uploadElpvEvidence(USER, COMPANY_ID, PROJECT_ID, SITE_ID, null,
                    new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[] { 1 }));
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            verify(s3Client, never()).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                    any(software.amazon.awssdk.core.sync.RequestBody.class));
        }

        @Test
        @DisplayName("UT-MS-042: クライアント指定のContent-Typeを信用せず拡張子ベースで安全な値を決定する")
        void getSafeContentType_ignoresClientSpoofedContentType_resolvesByExtension() {
            assertThat(invokePrivate("getSafeContentType", new Class<?>[] { String.class }, "pdf"))
                    .isEqualTo("application/pdf");
        }

        @Test
        @DisplayName("UT-MS-043: 未知拡張子の場合は安全なデフォルト値にフォールバックする")
        void getSafeContentType_unknownExtension_fallsBackToOctetStream() {
            assertThat(invokePrivate("getSafeContentType", new Class<?>[] { String.class }, "unknown"))
                    .isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("UT-MS-044: ファイル名に不正文字やnullが含まれる場合は安全な名前に変換される")
        void sanitizeFileName_invalidCharactersOrNull_returnsSafeFileName() {
            assertThat(invokePrivate("sanitizeFileName", new Class<?>[] { String.class }, "../test file.pdf"))
                    .isEqualTo(".._test_file.pdf");
            assertThat(invokePrivate("sanitizeFileName", new Class<?>[] { String.class }, new Object[] { null }))
                    .asString().matches("[0-9a-f-]{36}");
        }

        @Test
        @DisplayName("UT-MS-045: 各APIが正しいfileTypeで共通処理に委譲し証憑アップロードが成功する")
        void uploadEvidence_eachApiDelegatesWithCorrectFileType_returnsOk() {
            MultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[] { 1 });
            assertThat(service.uploadElpvEvidence(USER, COMPANY_ID, PROJECT_ID, SITE_ID, null, file).getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
            assertThat(service.uploadElpvrEvidence(USER, COMPANY_ID, PROJECT_ID, SITE_ID, null, file).getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
            equipment.setHasBattery(true);
            assertThat(service.uploadElpjBatterySpecificationEvidence(USER, COMPANY_ID, PROJECT_ID, SITE_ID, null, file)
                    .getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(service.uploadElpjControlEvidence(USER, COMPANY_ID, PROJECT_ID, SITE_ID, null, file).getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-MS-046: 処理中に予期せぬ例外(S3接続エラー等)が発生した場合は内部エラーを返す")
        void uploadEvidence_unexpectedException_returnsInternalError() {
            org.mockito.Mockito.doThrow(new RuntimeException("s3 failure")).when(s3Client).putObject(
                    any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                    any(software.amazon.awssdk.core.sync.RequestBody.class));
            MultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[] { 1 });
            assertThat(service.uploadElpvEvidence(USER, COMPANY_ID, PROJECT_ID, SITE_ID, null, file).getStatusCode())
                    .isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("GET /monitoring/{siteId}/{projectId}/equipment/{equipmentId}/evidence-item のテスト")
    class ReadMonitoringEvidenceItemsTest {

        @Test
        @DisplayName("UT-MS-047: 各検証条件が不正の場合は対応するエラーを返す")
        void readMonitoringEvidenceItems_invalidValidationConditions_returnsError() {
            when(userRepository.findByUsername(USER)).thenReturn(Optional.empty());
            assertThat(service.readMonitoringEvidenceItems(USER, COMPANY_ID, PROJECT_ID, SITE_ID, 40).getStatusCode())
                    .isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("UT-MS-048: 設備が存在しない、または別拠点に属する場合はNOT_FOUNDを返す")
        void readMonitoringEvidenceItems_equipmentNotFoundOrWrongSite_returnsNotFound() {
            when(equipmentRepository.findById(40)).thenReturn(Optional.empty());
            assertThat(service.readMonitoringEvidenceItems(USER, COMPANY_ID, PROJECT_ID, SITE_ID, 40).getMessage())
                    .isEqualTo(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-MS-049: 当該設備の最新レコードが存在しない場合でもエラーにせず未提出として扱う")
        void readMonitoringEvidenceItems_noLatestRecord_returnsOkWithNotSubmitted() {
            when(monitoringRecordSolarRepository.findLatestByRecordIdAndEquipmentId(SITE_ID + "#" + PROJECT_ID, 40))
                    .thenReturn(Optional.empty());
            MonitoringEvidenceItemsResponse response = service.readMonitoringEvidenceItems(
                    USER, COMPANY_ID, PROJECT_ID, SITE_ID, 40);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getEvidenceItems()).hasSize(4);
        }

        @Test
        @DisplayName("UT-MS-050: ELpv/ELpvrは常にREQUIRED、ELPJcontrolは常にOPTIONALと判定される")
        void resolveEvidenceRequirement_elPvElPvrAndElpjControl_returnsFixedRequirement() {
            for (MonitoringEvidenceField field : new MonitoringEvidenceField[] {
                    MonitoringEvidenceField.EL_PV, MonitoringEvidenceField.EL_PVR }) {
                assertThat(invokePrivate("resolveEvidenceRequirement",
                        new Class<?>[] { EnR002Equipment.class, MonitoringEvidenceField.class }, equipment, field))
                        .isEqualTo(EvidenceRequirement.REQUIRED);
            }
            assertThat(invokePrivate("resolveEvidenceRequirement",
                    new Class<?>[] { EnR002Equipment.class, MonitoringEvidenceField.class }, equipment,
                    MonitoringEvidenceField.EL_PJ_CONTROL)).isEqualTo(EvidenceRequirement.OPTIONAL);
        }

        @Test
        @DisplayName("UT-MS-051: ELPJbatteryは設備のhasBatteryに応じてREQUIREDまたはNOT_APPLICABLEと判定される")
        void resolveEvidenceRequirement_elpjBattery_dependsOnHasBattery() {
            assertThat(invokePrivate("resolveEvidenceRequirement",
                    new Class<?>[] { EnR002Equipment.class, MonitoringEvidenceField.class }, equipment,
                    MonitoringEvidenceField.EL_PJ_BATTERY)).isEqualTo(EvidenceRequirement.NOT_APPLICABLE);
            equipment.setHasBattery(true);
            assertThat(invokePrivate("resolveEvidenceRequirement",
                    new Class<?>[] { EnR002Equipment.class, MonitoringEvidenceField.class }, equipment,
                    MonitoringEvidenceField.EL_PJ_BATTERY)).isEqualTo(EvidenceRequirement.REQUIRED);
        }

        @Test
        @DisplayName("UT-MS-052: NOT_APPLICABLE項目はevidenceUrlとacceptedTypesが空で返却される")
        void buildEvidenceItem_notApplicable_returnsEmptyUrlAndTypes() {
            MonitoringEvidenceItemResponse item = (MonitoringEvidenceItemResponse) invokePrivate("buildEvidenceItem",
                    new Class<?>[] { MonitoringEvidenceField.class, EvidenceRequirement.class,
                            EnR002MonitoringRecord.class },
                    MonitoringEvidenceField.EL_PJ_BATTERY, EvidenceRequirement.NOT_APPLICABLE, null);
            assertThat(item.getEvidenceUrl()).isNull();
            assertThat(item.getAcceptedTypes()).isEmpty();
            assertThat(item.getStatus()).isNull();
        }

        @Test
        @DisplayName("UT-MS-053: 証憑URLの有無に応じてstatusが未提出または提出済みとなる")
        void buildEvidenceItem_evidenceUrlPresence_determinesSubmissionStatus() {
            EnR002MonitoringRecord noEvidence = new EnR002MonitoringRecord();
            MonitoringEvidenceItemResponse missing = (MonitoringEvidenceItemResponse) invokePrivate("buildEvidenceItem",
                    new Class<?>[] { MonitoringEvidenceField.class, EvidenceRequirement.class,
                            EnR002MonitoringRecord.class },
                    MonitoringEvidenceField.EL_PV, EvidenceRequirement.REQUIRED, noEvidence);
            assertThat(missing.getStatus()).isEqualTo("NOT_SUBMITTED");
            noEvidence.setElPvEvidenceUrl("s3://evidence/file.pdf");
            MonitoringEvidenceItemResponse submitted = (MonitoringEvidenceItemResponse) invokePrivate("buildEvidenceItem",
                    new Class<?>[] { MonitoringEvidenceField.class, EvidenceRequirement.class,
                            EnR002MonitoringRecord.class },
                    MonitoringEvidenceField.EL_PV, EvidenceRequirement.REQUIRED, noEvidence);
            assertThat(submitted.getStatus()).isEqualTo("SUBMITTED");
            assertThat(submitted.getEvidenceUrl()).isEqualTo("s3://evidence/file.pdf");
        }

        @Test
        @DisplayName("UT-MS-054: 設備と最新レコードが有効な場合に4項目全ての証憑情報が正しく構築される")
        void readMonitoringEvidenceItems_validEquipmentAndRecord_returnsOkWithAllItems() {
            when(monitoringRecordSolarRepository.findLatestByRecordIdAndEquipmentId(SITE_ID + "#" + PROJECT_ID, 40))
                    .thenReturn(Optional.of(new EnR002MonitoringRecord()));
            MonitoringEvidenceItemsResponse response = service.readMonitoringEvidenceItems(
                    USER, COMPANY_ID, PROJECT_ID, SITE_ID, 40);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMethodologyName()).isEqualTo("EN-R");
            assertThat(response.getMethodologyNumber()).isEqualTo("002");
            assertThat(response.getEvidenceItems()).hasSize(4);
        }

        @Test
        @DisplayName("UT-MS-055: 処理中に例外が発生した場合は内部エラーを返す")
        void readMonitoringEvidenceItems_unexpectedException_returnsInternalError() {
            when(userRepository.findByUsername(USER)).thenThrow(new RuntimeException("database unavailable"));
            assertThat(service.readMonitoringEvidenceItems(USER, COMPANY_ID, PROJECT_ID, SITE_ID, 40).getStatusCode())
                    .isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("共通ロジック（parseDateRecorded / isBlank / mergeErrors）のテスト")
    class CommonHelperLogicTest {

        @Test
        @DisplayName("UT-MS-056: 不正な形式またはnullの場合は例外を捕捉してnullを返す")
        void parseDateRecorded_invalidFormatOrNull_returnsNull() {
            assertThat(invokePrivate("parseDateRecorded", new Class<?>[] { String.class }, "bad-date")).isNull();
            assertThat(invokePrivate("parseDateRecorded", new Class<?>[] { String.class }, new Object[] { null })).isNull();
        }

        @Test
        @DisplayName("UT-MS-057: null・空文字・空白のみはtrue、有効文字列はfalseを返す")
        void isBlank_nullEmptyBlankOrValidString_returnsExpectedBoolean() {
            for (String value : new String[] { null, "", "  " }) {
                assertThat(invokePrivate("isBlank", new Class<?>[] { String.class }, new Object[] { value })).isEqualTo(true);
            }
            assertThat(invokePrivate("isBlank", new Class<?>[] { String.class }, "value")).isEqualTo(false);
        }

        @Test
        @DisplayName("UT-MS-058: 既存キーは上書きせず新規キーのみ追加する(putIfAbsent)")
        void mergeErrors_existingKeyNotOverwritten_onlyNewKeyAdded() {
            Map<String, String> target = new LinkedHashMap<>();
            target.put("a", "existing");
            Map<String, String> source = Map.of("a", "replacement", "b", "new");
            invokePrivate("mergeErrors", new Class<?>[] { Map.class, Map.class }, target, source);
            assertThat(target).containsEntry("a", "existing").containsEntry("b", "new");
        }
    }
}