package com.kct.jcredit.core_backend.controller.MonitoringControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CancelRequestDecisionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateCancelRequestRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestDecisionResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestItemDto;
import com.kct.jcredit.core_backend.dto.response.CancelRequestListResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestResponse;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.RecordCancelRequest;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.repository.postgres.RecordCancelRequestRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.impl.CancelRequestServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CancelRequestServiceTest {

    private static final String USERNAME = "requester";
    private static final String COMPANY_ID = "company-1";
    private static final String SITE_ID = "site-1";
    private static final String RECORD_PK = "record-pk";
    private static final String RECORD_SK = "record-sk";

    @Mock private RecordCancelRequestRepository cancelRequestRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private MonitoringTableRecordSolarRepository monitoringRecordSolarRepository;
    @Mock private NotificationService notificationService;
    private final NotificationMessages messages = new NotificationMessages();

    private CancelRequestServiceImpl service;

    private ParticipatingCompany company;
    private Site site;
    private EnR002MonitoringRecord record;
    private RecordCancelRequest cancelRequest;
    private CreateCancelRequestRequest createRequest;

    @BeforeEach
    void setUp() {
        service = new CancelRequestServiceImpl(cancelRequestRepository, siteRepository,
                monitoringRecordSolarRepository, notificationService, messages);
        company = new ParticipatingCompany();
        company.setCompanyId(COMPANY_ID);
        site = new Site();
        site.setId(1);
        site.setPublicId(SITE_ID);
        site.setSiteName("Test site");
        site.setCompany(company);
        record = new EnR002MonitoringRecord();
        record.setId(RECORD_PK);
        record.setTimeUpdated(RECORD_SK);
        record.setIsActive(true);
        record.setParticipatingUserID(7);
        record.setElPv(new BigDecimal("12.5"));
        record.setElPvr(new BigDecimal("3.5"));
        record.setElpjControlValue(new BigDecimal("1.5"));
        record.setElpjBatteryValue(new BigDecimal("2.5"));
        record.setElpjAccuracy(new BigDecimal("90"));
        cancelRequest = RecordCancelRequest.builder()
                .site(site).recordPk(RECORD_PK).recordSk(RECORD_SK)
                .status("PENDING").reason("Reason").requestedBy(USERNAME)
                .requestedAt(Instant.parse("2026-01-01T00:00:00Z")).build();
        cancelRequest.setId(42);
        cancelRequest.setIsActive(true);
        createRequest = new CreateCancelRequestRequest();
        createRequest.setSiteId(SITE_ID);
        createRequest.setRecordPK(RECORD_PK);
        createRequest.setRecordSK(RECORD_SK);
        createRequest.setReasonText("Reason");
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                .thenReturn(Optional.of(record));
        when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                .thenAnswer(invocation -> {
                    RecordCancelRequest saved = invocation.getArgument(0);
                    if (saved.getId() == null) {
                        saved.setId(42);
                    }
                    return saved;
                });
    }

    private CancelRequestResponse create() {
        when(cancelRequestRepository.existsPendingRequestForRecord(RECORD_PK, RECORD_SK))
                .thenReturn(false);
        return service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = CancelRequestServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
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
    @DisplayName("POST /monitoring/cancel-request のテスト")
    class CreateCancelRequestTest {

        @Test
        @DisplayName("UT-CR-001: 指定siteIdが存在しない場合はNOT_FOUNDを返す")
        void createCancelRequest_siteNotFound_returnsNotFound() {
            when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.empty());
            CancelRequestResponse response = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
            verify(monitoringRecordSolarRepository, never()).findByPKandSK(anyString(), anyString());
        }

        @Test
        @DisplayName("UT-CR-002: 拠点のcompanyがnullまたは指定companyIdと不一致の場合はFORBIDDENを返す")
        void createCancelRequest_siteCompanyMismatchOrNull_returnsForbidden() {
            site.setCompany(null);
            CancelRequestResponse noCompany = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            assertThat(noCompany.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            assertThat(noCompany.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
            ParticipatingCompany differentCompany = new ParticipatingCompany();
            differentCompany.setCompanyId("another-company");
            site.setCompany(differentCompany);
            CancelRequestResponse wrongCompany = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            assertThat(wrongCompany.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            assertThat(wrongCompany.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
        }

        @Test
        @DisplayName("UT-CR-003: 指定recordPK/SKのモニタリングレコードがNoSQLに存在しない場合はNOT_FOUNDを返す")
        void createCancelRequest_monitoringRecordNotFound_returnsNotFound() {
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.empty());
            CancelRequestResponse response = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-CR-004: 対象レコードが既に取消済み(isActive=false)の場合はエラーを返す")
        void createCancelRequest_recordAlreadyCancelled_returnsBadRequest() {
            record.setIsActive(false);
            CancelRequestResponse response = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            assertThat(response.getMessage()).isEqualTo("このレコードは既に取り消されています。");
            verify(cancelRequestRepository, never()).save(any(RecordCancelRequest.class));
        }

        @Test
        @DisplayName("UT-CR-005【要レビュー】: ALREADY_CANCELLEDエラー時にerrorCodeが設定されない")
        void createCancelRequest_alreadyCancelledError_missingErrorCode() {
            record.setIsActive(false);
            CancelRequestResponse response = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            assertThat(response.getMessage()).isEqualTo("このレコードは既に取り消されています。");
            JsonNode json = new ObjectMapper().valueToTree(response);
            assertThat(json.path("errorCode").asText()).isEqualTo("ALREADY_CANCELLED");
        }

        @Test
        @DisplayName("UT-CR-006: 同一レコードに対する未処理(PENDING)の取消申請が既に存在する場合はエラーを返す")
        void createCancelRequest_pendingRequestAlreadyExists_returnsBadRequest() {
            when(cancelRequestRepository.existsPendingRequestForRecord(RECORD_PK, RECORD_SK)).thenReturn(true);
            CancelRequestResponse response = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.CONFLICT);
            assertThat(response.getMessage()).isEqualTo("このレコードの取消申請が既に存在します。");
        }

        @Test
        @DisplayName("UT-CR-007【要レビュー】: ステータスコードが仕様書(400/409)と実装(全て400)で不整合")
        void createCancelRequest_statusCodeMapping_inconsistentWithSpec() {
            record.setIsActive(false);
            CancelRequestResponse cancelled = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            record.setIsActive(true);
            when(cancelRequestRepository.existsPendingRequestForRecord(RECORD_PK, RECORD_SK)).thenReturn(true);
            CancelRequestResponse pending = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            assertThat(cancelled.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            assertThat(pending.getStatusCode()).isEqualTo(ResponseConstants.CONFLICT);
        }

        @Test
        @DisplayName("UT-CR-008: 全条件を満たす場合に取消申請が正常に作成される")
        void createCancelRequest_allValid_returnsOk() {
            CancelRequestResponse response = create();
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getCancelRequestId()).isEqualTo(42);
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getRequestedBy()).isEqualTo(USERNAME);
            assertThat(response.getRecordPk()).isEqualTo(RECORD_PK);
            assertThat(response.getRecordSk()).isEqualTo(RECORD_SK);
            assertThat(response.getSiteId()).isEqualTo(SITE_ID);
            assertThat(response.getRequestedAt()).isNotBlank();
            ArgumentCaptor<RecordCancelRequest> captor = ArgumentCaptor.forClass(RecordCancelRequest.class);
            verify(cancelRequestRepository).save(captor.capture());
            assertThat(captor.getValue().getReason()).isEqualTo("Reason");
        }

        @Test
        @DisplayName("UT-CR-009: 登録成功時に全運用ユーザーへの通知（アプリ内通知・メール）が送信される")
        void createCancelRequest_success_notifiesAllOperatingUsers() {
            assertThat(create().getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(notificationService).createNotificationForAllOperatingUsers(anyString(), anyString());
            verify(notificationService).sendEmailToAllOperatingUsers(anyString(), anyString());
        }

        @Test
        @DisplayName("UT-CR-010: 通知処理で例外が発生しても登録処理自体は成功として返す")
        void createCancelRequest_notificationException_stillReturnsOk() {
            doThrow(new RuntimeException("notification failure"))
                    .when(notificationService).createNotificationForAllOperatingUsers(anyString(), anyString());
            assertThat(create().getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(cancelRequestRepository).save(any(RecordCancelRequest.class));
        }

        @Test
        @DisplayName("UT-CR-011: 処理中に予期せぬ例外(DB接続エラー等)が発生した場合は内部エラーを返す")
        void createCancelRequest_unexpectedException_returnsInternalError() {
            when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenThrow(new RuntimeException("database unavailable"));
            CancelRequestResponse response = service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        }

        @Test
        @DisplayName("UT-CR-012【要レビュー】: usernameに対する会社所属・ユーザー存在チェックが行われていない")
        void createCancelRequest_noUserCompanyValidation_potentialGap() {
            assertThat(service.createCancelRequest("unknown-user", COMPANY_ID, createRequest).getStatusCode())
                    .isEqualTo(ResponseConstants.OK);
            verify(cancelRequestRepository).save(any(RecordCancelRequest.class));
        }
    }

    @Nested
    @DisplayName("GET /monitoring/cancel-requests のテスト")
    class ListCancelRequestsTest {

        @Test
        @DisplayName("UT-CR-013【要レビュー・重大】: 会社・権限によるフィルタリングが一切行われない")
        void listCancelRequests_noTenantFiltering_returnsAllCompaniesData() {
            when(cancelRequestRepository.findAll()).thenReturn(List.of(cancelRequest));
            CancelRequestListResponse response = service.listCancelRequests();
            assertThat(response.getItems()).hasSize(1);
            verify(cancelRequestRepository).findAll();
        }

        @Test
        @DisplayName("UT-CR-014【要レビュー】: デフォルトPENDINGフィルタが実装されておらず全ステータスが返却される")
        void listCancelRequests_noStatusFilter_returnsAllStatuses() {
            RecordCancelRequest approved = RecordCancelRequest.builder().site(site).status("APPROVED")
                    .requestedAt(Instant.now()).requestedBy(USERNAME).reason("reason").recordPk("pk2").recordSk("sk2").build();
            RecordCancelRequest rejected = RecordCancelRequest.builder().site(site).status("REJECTED")
                    .requestedAt(Instant.now()).requestedBy(USERNAME).reason("reason").recordPk("pk3").recordSk("sk3").build();
            when(cancelRequestRepository.findAll()).thenReturn(List.of(cancelRequest, approved, rejected));
            assertThat(service.listCancelRequests().getItems()).extracting(CancelRequestItemDto::getStatus)
                    .containsExactly("pending");
        }

        @Test
        @DisplayName("UT-CR-015: 取得した取消申請エンティティがDTOへ正しく変換される")
        void toListItemDto_validEntity_mapsFieldsCorrectly() {
            CancelRequestItemDto item = service.toListItemDto(cancelRequest);
            assertThat(item.getId()).isEqualTo(42);
            assertThat(item.getRequestedAt()).isEqualTo(cancelRequest.getRequestedAt().toString());
            assertThat(item.getReason()).isEqualTo("Reason");
            assertThat(item.getRecordPk()).isEqualTo(RECORD_PK);
            assertThat(item.getStatus()).isEqualTo("pending");
            assertThat(item.getRequestedBy()).isEqualTo(USERNAME);
            assertThat(item.getSiteName()).isEqualTo("Test site");
        }

        @Test
        @DisplayName("UT-CR-016: site=nullの場合でも例外とならずsiteName=nullとして変換される")
        void toListItemDto_siteNull_returnsNullSiteNameWithoutException() {
            cancelRequest.setSite(null);
            assertThat(service.toListItemDto(cancelRequest).getSiteName()).isNull();
        }

        @Test
        @DisplayName("UT-CR-017: decidedAt/decidedByが未設定(PENDING)の場合はnullとして変換される")
        void toListItemDto_pendingStatus_decidedFieldsNull() {
            cancelRequest.setDecidedAt(null);
            cancelRequest.setDecidedBy(null);
            CancelRequestItemDto item = service.toListItemDto(cancelRequest);
            assertThat(item.getDecidedAt()).isNull();
            assertThat(item.getDecidedBy()).isNull();
        }

        @Test
        @DisplayName("UT-CR-018: 取消申請が0件の場合は空リストが返却される（NOT_FOUND等のエラーにならない）")
        void listCancelRequests_noData_returnsEmptyListNotError() {
            when(cancelRequestRepository.findAll()).thenReturn(List.of());
            CancelRequestListResponse response = service.listCancelRequests();
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getItems()).isEmpty();
        }

        @Test
        @DisplayName("UT-CR-019【要レビュー】: 処理中に例外が発生した場合にtry-catchが存在せず伝播する")
        void listCancelRequests_unexpectedException_propagatesWithoutHandling() {
            when(cancelRequestRepository.findAll()).thenThrow(new RuntimeException("database unavailable"));
            AtomicReference<CancelRequestListResponse> response = new AtomicReference<>();
            assertThatCode(() -> response.set(service.listCancelRequests())).doesNotThrowAnyException();
            assertThat(response.get().getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.get().getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        }
    }

    @Nested
    @DisplayName("POST /monitoring/{cancelRequestId}/cancel-decision のテスト")
    class ProcessDecisionTest {

        @Test
        @DisplayName("UT-CR-020【要レビュー・他APIと非対称】: 指定cancelRequestIdが存在しない場合の例外伝播を確認する")
        void processDecision_requestNotFound_illegalStateExceptionPropagates() {
            when(cancelRequestRepository.findById(123)).thenReturn(Optional.empty());
            AtomicReference<CancelRequestDecisionResponse> response = new AtomicReference<>();
            assertThatCode(() -> response.set(service.processDecision(123,
                    CancelRequestDecisionRequest.builder().decision("REJECT").build(), "operator")))
                    .doesNotThrowAnyException();
            assertThat(response.get().getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.get().getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-CR-021: 取消申請のstatusがPENDING以外の場合はエラーを返す")
        void processDecision_statusNotPending_returnsBadRequest() {
            cancelRequest.setStatus("APPROVED");
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            CancelRequestDecisionResponse response = service.processDecision(42,
                    CancelRequestDecisionRequest.builder().decision("REJECT").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            assertThat(response.getMessage()).contains("APPROVED");
        }

        @Test
        @DisplayName("UT-CR-022: 相殺レコードが既に作成済み(offsetRecordPk!=null)の場合はエラーを返す")
        void processDecision_offsetRecordAlreadyCreated_returnsBadRequest() {
            cancelRequest.setOffsetRecordPk("offset-pk");
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            CancelRequestDecisionResponse response = service.processDecision(42,
                    CancelRequestDecisionRequest.builder().decision("APPROVE").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            assertThat(response.getMessage()).isEqualTo("相殺レコードが既に作成済みです");
        }

        @Test
        @DisplayName("UT-CR-023【要レビュー・重大】: 操作者の会社所属・権限（MASTER/管理者role等）チェックが存在しない")
        void processDecision_noOperatorAuthorizationCheck_potentialGap() {
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.empty());
            CancelRequestDecisionResponse response = service.processDecision(42,
                    CancelRequestDecisionRequest.builder().decision("REJECT").build(), "foreign-operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(cancelRequest.getDecidedBy()).isEqualTo("foreign-operator");
        }

        @Test
        @DisplayName("UT-CR-024: decision=APPROVEの場合はapproveRequestに処理が委譲される")
        void processDecision_decisionApprove_delegatesToApproveRequest() {
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.empty());
            CancelRequestDecisionResponse response = service.processDecision(42,
                    CancelRequestDecisionRequest.builder().decision("APPROVE").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verify(monitoringRecordSolarRepository).findByPKandSK(RECORD_PK, RECORD_SK);
        }

        @Test
        @DisplayName("UT-CR-025【要レビュー】: decision=APPROVE以外（任意の値含む）はrejectRequestに処理が委譲される")
        void processDecision_decisionNotApprove_delegatesToRejectRequestWithoutValidation() {
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.empty());
            CancelRequestDecisionResponse response = service.processDecision(42,
                    CancelRequestDecisionRequest.builder().decision("UNKNOWN").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(cancelRequest.getStatus()).isEqualTo("REJECTED");
        }
    }

    @Nested
    @DisplayName("approveRequest（承認処理）のテスト")
    class ApproveRequestTest {

        @Test
        @DisplayName("UT-CR-026: 承認処理時に元のモニタリングレコードがNoSQLに存在しない場合はNOT_FOUNDを返す")
        void approveRequest_originalRecordNotFound_returnsNotFound() {
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.empty());
            CancelRequestDecisionResponse response = (CancelRequestDecisionResponse) invokePrivate("approveRequest",
                    new Class<?>[] { RecordCancelRequest.class, CancelRequestDecisionRequest.class, String.class },
                    cancelRequest, CancelRequestDecisionRequest.builder().decision("APPROVE").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-CR-027: 承認時に元レコードの数値項目が反転(negate)された相殺レコードが作成される")
        void createOffsetRecord_validNumericFields_negatesAllValues() {
            EnR002MonitoringRecord offset = (EnR002MonitoringRecord) invokePrivate("createOffsetRecord",
                    new Class<?>[] { EnR002MonitoringRecord.class }, record);
            assertThat(offset.getElPv()).isEqualByComparingTo("-12.5");
            assertThat(offset.getElPvr()).isEqualByComparingTo("-3.5");
            assertThat(offset.getElpjControlValue()).isEqualByComparingTo("-1.5");
            assertThat(offset.getElpjBatteryValue()).isEqualByComparingTo("-2.5");
            assertThat(offset.getElpjAccuracy()).isEqualByComparingTo("-90");
        }

        @Test
        @DisplayName("UT-CR-028: 反転対象の数値項目がnullの場合はnullのまま維持される")
        void createOffsetRecord_nullNumericField_keepsNullWithoutException() {
            record.setElpjBatteryValue(null);
            EnR002MonitoringRecord offset = (EnR002MonitoringRecord) invokePrivate("createOffsetRecord",
                    new Class<?>[] { EnR002MonitoringRecord.class }, record);
            assertThat(offset.getElpjBatteryValue()).isNull();
            assertThat(offset.getElPv()).isEqualByComparingTo("-12.5");
        }

        @Test
        @DisplayName("UT-CR-029: 相殺レコードは元レコードと同一PK・projectID・siteID・participatingUserID・dateRecordedを保持し新しいSKが設定される")
        void createOffsetRecord_sameKeyFields_newTimeUpdated() {
            record.setProjectID(10);
            record.setSiteID(20);
            record.setParticipatingUserID(30);
            record.setDateRecorded("2026-01-01");
            EnR002MonitoringRecord offset = (EnR002MonitoringRecord) invokePrivate("createOffsetRecord",
                    new Class<?>[] { EnR002MonitoringRecord.class }, record);
            assertThat(offset.getId()).isEqualTo(record.getId());
            assertThat(offset.getProjectID()).isEqualTo(10);
            assertThat(offset.getSiteID()).isEqualTo(20);
            assertThat(offset.getParticipatingUserID()).isEqualTo(30);
            assertThat(offset.getDateRecorded()).isEqualTo("2026-01-01");
            assertThat(Instant.parse(offset.getTimeUpdated())).isAfterOrEqualTo(Instant.now().minusSeconds(2));
        }

        @Test
        @DisplayName("UT-CR-030: 相殺レコードの証憑関連項目は全てnullでクリアされる")
        void createOffsetRecord_evidenceFields_clearedToNull() {
            EnR002MonitoringRecord offset = (EnR002MonitoringRecord) invokePrivate("createOffsetRecord",
                    new Class<?>[] { EnR002MonitoringRecord.class }, record);
            assertThat(offset.getElpjControlEquipmentSpecFiles()).isNull();
            assertThat(offset.getElpjBatteryEvidence()).isNull();
            assertThat(offset.getElPvEvidence()).isNull();
            assertThat(offset.getElPvrEvidence()).isNull();
            assertThat(offset.getElPvEvidenceUrl()).isNull();
            assertThat(offset.getElPvrEvidenceUrl()).isNull();
            assertThat(offset.getElpjBatterySpecificationUrl()).isNull();
            assertThat(offset.getElpjControlEvidenceUrl()).isNull();
        }

        @Test
        @DisplayName("UT-CR-031: 相殺レコードはisActive=trueとして作成される")
        void createOffsetRecord_isActiveTrue_setOnCreation() {
            EnR002MonitoringRecord offset = (EnR002MonitoringRecord) invokePrivate("createOffsetRecord",
                    new Class<?>[] { EnR002MonitoringRecord.class }, record);
            assertThat(offset.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("UT-CR-032: 承認成功時にRecordCancelRequestのstatus・decidedBy・decidedAt・offsetRecordPk/SKが更新保存される")
        void approveRequest_success_updatesCancelRequestFields() {
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.of(record));
            when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            CancelRequestDecisionResponse response = (CancelRequestDecisionResponse) invokePrivate("approveRequest",
                    new Class<?>[] { RecordCancelRequest.class, CancelRequestDecisionRequest.class, String.class },
                    cancelRequest, CancelRequestDecisionRequest.builder().decision("APPROVE").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(cancelRequest.getStatus()).isEqualTo("APPROVED");
            assertThat(cancelRequest.getDecidedBy()).isEqualTo("operator");
            assertThat(cancelRequest.getDecidedAt()).isNotNull();
            assertThat(cancelRequest.getOffsetRecordPk()).isEqualTo(RECORD_PK);
            assertThat(cancelRequest.getOffsetRecordSk()).isNotBlank();
        }

        @Test
        @DisplayName("UT-CR-033: 承認成功時に元レコードの参加ユーザーへ承認通知が送信される")
        void approveRequest_success_notifiesParticipatingUser() {
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.of(record));
            when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            invokePrivate("approveRequest", new Class<?>[] { RecordCancelRequest.class,
                    CancelRequestDecisionRequest.class, String.class }, cancelRequest,
                    CancelRequestDecisionRequest.builder().decision("APPROVE").build(), "operator");
            verify(notificationService).createNotification(anyString(), anyString(), anyInt(), org.mockito.ArgumentMatchers.isNull());
        }

        @Test
        @DisplayName("UT-CR-034: participatingUserIDがnullの場合は通知がスキップされエラーにならない")
        void approveRequest_participatingUserIdNull_skipsNotification() {
            record.setParticipatingUserID(null);
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.of(record));
            when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            CancelRequestDecisionResponse response = (CancelRequestDecisionResponse) invokePrivate("approveRequest",
                    new Class<?>[] { RecordCancelRequest.class, CancelRequestDecisionRequest.class, String.class },
                    cancelRequest, CancelRequestDecisionRequest.builder().decision("APPROVE").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(notificationService, never()).createNotification(anyString(), anyString(), anyInt(), any());
        }

        @Test
        @DisplayName("UT-CR-035: 通知処理で例外が発生しても承認処理自体は成功として返す")
        void approveRequest_notificationException_stillReturnsOk() {
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.of(record));
            when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            doThrow(new RuntimeException("notification failure"))
                    .when(notificationService).createNotification(anyString(), anyString(), anyInt(), org.mockito.ArgumentMatchers.isNull());
            CancelRequestDecisionResponse response = (CancelRequestDecisionResponse) invokePrivate("approveRequest",
                    new Class<?>[] { RecordCancelRequest.class, CancelRequestDecisionRequest.class, String.class },
                    cancelRequest, CancelRequestDecisionRequest.builder().decision("APPROVE").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        }

        @Test
        @DisplayName("UT-CR-036: 承認処理中に予期せぬ例外(DB保存エラー等)が発生した場合は内部エラーを返す")
        void approveRequest_unexpectedException_returnsInternalError() {
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.of(record));
            when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                    .thenThrow(new RuntimeException("database unavailable"));
            CancelRequestDecisionResponse response = (CancelRequestDecisionResponse) invokePrivate("approveRequest",
                    new Class<?>[] { RecordCancelRequest.class, CancelRequestDecisionRequest.class, String.class },
                    cancelRequest, CancelRequestDecisionRequest.builder().decision("APPROVE").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        }
    }

    @Nested
    @DisplayName("rejectRequest（却下処理）のテスト")
    class RejectRequestTest {

        @Test
        @DisplayName("UT-CR-037: 却下成功時にRecordCancelRequestのstatus・decidedBy・decidedAtが更新保存される")
        void rejectRequest_success_updatesCancelRequestFields() {
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.empty());
            CancelRequestDecisionResponse response = service.processDecision(42,
                    CancelRequestDecisionRequest.builder().decision("REJECT").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(cancelRequest.getStatus()).isEqualTo("REJECTED");
            assertThat(cancelRequest.getDecidedBy()).isEqualTo("operator");
            assertThat(cancelRequest.getDecidedAt()).isNotNull();
            verify(cancelRequestRepository).save(cancelRequest);
        }

        @Test
        @DisplayName("UT-CR-038: 却下時はoffsetRecordPk/SKが設定されずnullのまま、managerRemarksがレスポンスに含まれる")
        void rejectRequest_success_responseContainsManagerRemarksNoOffsetRecord() {
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.empty());
            CancelRequestDecisionResponse response = service.processDecision(42,
                    CancelRequestDecisionRequest.builder().decision("REJECT")
                            .managerRemarks("証跡不足のため却下").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getManagerRemarks()).isEqualTo("証跡不足のため却下");
            assertThat(response.getOffsetRecordPk()).isNull();
            assertThat(response.getOffsetRecordSk()).isNull();
        }

        @Test
        @DisplayName("UT-CR-039: 却下成功時に元レコードの参加ユーザーへ却下通知が送信される")
        void rejectRequest_success_notifiesParticipatingUser() {
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.of(record));
            service.processDecision(42, CancelRequestDecisionRequest.builder().decision("REJECT")
                    .managerRemarks("Not accepted").build(), "operator");
            verify(notificationService).createNotification(anyString(), anyString(), anyInt(),
                    org.mockito.ArgumentMatchers.isNull());
        }

        @Test
        @DisplayName("UT-CR-040【要レビュー・approveと非対称】: 元レコードが存在しない場合でも却下処理自体は続行される")
        void rejectRequest_originalRecordNotFound_stillProceedsUnlikeApprove() {
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.empty());
            CancelRequestDecisionResponse response = service.processDecision(42,
                    CancelRequestDecisionRequest.builder().decision("REJECT").build(), "operator");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(cancelRequest.getStatus()).isEqualTo("REJECTED");
            verify(notificationService, never()).createNotification(anyString(), anyString(), anyInt(), any());
        }

        @Test
        @DisplayName("UT-CR-041【要レビュー・approveと非対称】: 例外ハンドリングが実装されておらず伝播する")
        void rejectRequest_unexpectedException_propagatesWithoutHandling() {
            when(cancelRequestRepository.findById(42)).thenReturn(Optional.of(cancelRequest));
            when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK)).thenReturn(Optional.empty());
            when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                    .thenThrow(new RuntimeException("save failed"));
            AtomicReference<CancelRequestDecisionResponse> response = new AtomicReference<>();
            assertThatCode(() -> response.set(service.processDecision(42,
                    CancelRequestDecisionRequest.builder().decision("REJECT").build(), "operator")))
                    .doesNotThrowAnyException();
            assertThat(response.get().getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.get().getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        }
    }
}