package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CancelRequestDecisionRequest;
import com.kct.jcredit.core_backend.dto.request.CreateCancelRequestRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestResponse;
import com.kct.jcredit.core_backend.entity.RecordCancelRequest;
import com.kct.jcredit.core_backend.dto.response.CancelRequestDecisionResponse;
import com.kct.jcredit.core_backend.dto.response.CancelRequestItemDto;
import com.kct.jcredit.core_backend.dto.response.CancelRequestListResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.EnR002MonitoringRecord;
import com.kct.jcredit.core_backend.repository.postgres.RecordCancelRequestRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringTableRecordSolarRepository;
import com.kct.jcredit.core_backend.service.CancelRequestService;
import com.kct.jcredit.core_backend.service.impl.CancelRequestServiceImpl;
import com.kct.jcredit.core_backend.service.NotificationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelRequestServiceImpl のテスト")
class CancelRequestServiceImplTest {

    @Mock
    private RecordCancelRequestRepository cancelRequestRepository;
    @Mock
    private SiteRepository siteRepository;
    @Mock
    private MonitoringTableRecordSolarRepository monitoringRecordSolarRepository;
    @Mock
    private NotificationService notificationService;

    // 実バンドルを使う。文面抽出が忠実かどうかを既存アサーションがそのまま検証する。
    @Spy
    private NotificationMessages messages = new NotificationMessages();

    @InjectMocks
    private CancelRequestServiceImpl service;

    private static final String USERNAME = "user001";
    private static final String COMPANY_ID = "COMP001";
    private static final String RECORD_PK = "1#1";
    private static final String RECORD_SK = "2026-07-10T12:00:00Z";

    // ---------- helper ----------

    private CreateCancelRequestRequest createRequest() {
        CreateCancelRequestRequest r = new CreateCancelRequestRequest();
        r.setSiteId("site-1");
        r.setRecordPK(RECORD_PK);
        r.setRecordSK(RECORD_SK);
        r.setReasonText("計測ミスのため");
        return r;
    }

    private Site siteFor(String companyId) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getCompanyId()).thenReturn(companyId);

        Site site = mock(Site.class);
        lenient().when(site.getCompany()).thenReturn(company);
        lenient().when(site.getPublicId()).thenReturn("1");
        lenient().when(site.getSiteName()).thenReturn("熊本第一発電所");
        return site;
    }

    private EnR002MonitoringRecord monitoringRecord(boolean isActive) {
        EnR002MonitoringRecord r = mock(EnR002MonitoringRecord.class);
        lenient().when(r.getIsActive()).thenReturn(isActive);
        lenient().when(r.getId()).thenReturn(RECORD_PK);
        lenient().when(r.getTimeUpdated()).thenReturn(RECORD_SK);
        lenient().when(r.getParticipatingUserID()).thenReturn(1);
        lenient().when(r.getProjectID()).thenReturn(1);
        lenient().when(r.getSiteID()).thenReturn(1);
        lenient().when(r.getElPv()).thenReturn(new BigDecimal("1200.5"));
        lenient().when(r.getElPvr()).thenReturn(new BigDecimal("300.0"));
        lenient().when(r.getElpjControlValue()).thenReturn(new BigDecimal("24.0"));
        lenient().when(r.getElpjBatteryValue()).thenReturn(new BigDecimal("15.5"));
        lenient().when(r.getElpjAccuracy()).thenReturn(new BigDecimal("98"));
        return r;
    }

    private RecordCancelRequest pendingCancelRequest(Site site) {
        RecordCancelRequest cr = RecordCancelRequest.builder()
                .site(site)
                .status("PENDING")
                .recordPk(RECORD_PK)
                .recordSk(RECORD_SK)
                .reason("計測ミスのため")
                .requestedBy(USERNAME)
                .requestedAt(Instant.parse("2026-07-10T09:00:00Z"))
                .build();
        cr.setId(10);
        cr.setIsActive(true);
        return cr;
    }

    // =========================================================================
    @Nested
    @DisplayName("createCancelRequest のテスト")
    class CreateCancelRequestTest {

        @Nested
        @DisplayName("【正常系】")
        class SuccessCase {

            @Test
            @DisplayName("SC-1: 全ての検証を通過した場合、取消申請が作成され OK レスポンスを返す")
            void allValidationsPass_createsRequestAndReturnsOk() {
                // Arrange
                Site site = siteFor(COMPANY_ID);
                EnR002MonitoringRecord record = monitoringRecord(true);
                RecordCancelRequest saved = pendingCancelRequest(site);

                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(record));
                when(cancelRequestRepository.existsPendingRequestForRecord(RECORD_PK, RECORD_SK))
                        .thenReturn(false);
                when(cancelRequestRepository.save(any(RecordCancelRequest.class))).thenReturn(saved);

                // Act
                CancelRequestResponse response = service.createCancelRequest(
                        USERNAME, COMPANY_ID, createRequest());

                // Assert
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
                assertThat(response.getCancelRequestId()).isEqualTo(10);
                assertThat(response.getRecordPk()).isEqualTo(RECORD_PK);
                assertThat(response.getStatus()).isEqualTo("PENDING");
                assertThat(response.getRequestedBy()).isEqualTo(USERNAME);
                assertThat(response.getSiteId()).isEqualTo(1);
                assertThat(response.getReason()).isEqualTo("計測ミスのため");
                // ISO_FORMATTER: yyyy-MM-dd'T'HH:mm:ss'Z'
                assertThat(response.getRequestedAt()).isEqualTo("2026-07-10T09:00:00Z");

                // Entity yang benar-benar disimpan harus punya field yang benar
                ArgumentCaptor<RecordCancelRequest> captor =
                        ArgumentCaptor.forClass(RecordCancelRequest.class);
                verify(cancelRequestRepository).save(captor.capture());
                RecordCancelRequest persisted = captor.getValue();
                assertThat(persisted.getStatus()).isEqualTo("PENDING");
                assertThat(persisted.getRequestedBy()).isEqualTo(USERNAME);
                assertThat(persisted.getCreatedBy()).isEqualTo(USERNAME);
                assertThat(persisted.getUpdatedBy()).isEqualTo(USERNAME);
                assertThat(persisted.getIsActive()).isTrue();

                verify(notificationService, times(1))
                        .createNotificationForAllOperatingUsers(anyString(), anyString());
                verify(notificationService, times(1))
                        .sendEmailToAllOperatingUsers(anyString(), anyString());
            }

            @Test
            @DisplayName("SC-1b: [回帰] サイトIDが文字列でもオペレーター通知が本文付きで送られる")
            void operatorNotificationCarriesSiteIdAndRequestId() {
                // 抽出前は String.format("...%d...", site.getPublicId(), ...) だったため、
                // publicId が String の実データでは毎回 IllegalFormatConversionException になり、
                // 外側の catch に飲み込まれて通知が一度も飛んでいなかった。
                Site site = siteFor(COMPANY_ID);
                EnR002MonitoringRecord record = monitoringRecord(true);
                RecordCancelRequest saved = pendingCancelRequest(site);

                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(record));
                when(cancelRequestRepository.existsPendingRequestForRecord(RECORD_PK, RECORD_SK))
                        .thenReturn(false);
                when(cancelRequestRepository.save(any(RecordCancelRequest.class))).thenReturn(saved);

                service.createCancelRequest(USERNAME, COMPANY_ID, createRequest());

                ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
                ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
                verify(notificationService).sendEmailToAllOperatingUsers(subject.capture(), body.capture());

                assertThat(subject.getValue()).isEqualTo("取消申請が作成されました");
                assertThat(body.getValue())
                        .contains(String.valueOf(site.getPublicId()))
                        .contains(USERNAME)
                        .contains("申請ID=10");
            }

            @Test
            @DisplayName("SC-2: オペレーター通知が失敗しても、成功レスポンスには影響しない")
            void operatorNotificationFails_stillReturnsOk() {
                // Arrange
                Site site = siteFor(COMPANY_ID);
                EnR002MonitoringRecord record = monitoringRecord(true);
                RecordCancelRequest saved = pendingCancelRequest(site);

                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(record));
                when(cancelRequestRepository.existsPendingRequestForRecord(RECORD_PK, RECORD_SK))
                        .thenReturn(false);
                when(cancelRequestRepository.save(any(RecordCancelRequest.class))).thenReturn(saved);
                doThrow(new RuntimeException("SES timeout"))
                        .when(notificationService).sendEmailToAllOperatingUsers(anyString(), anyString());

                // Act
                CancelRequestResponse response = service.createCancelRequest(
                        USERNAME, COMPANY_ID, createRequest());

                // Assert — notifikasi gagal dibungkus try-catch internal, TIDAK mempengaruhi hasil
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
                assertThat(response.getCancelRequestId()).isEqualTo(10);
            }
        }

        @Nested
        @DisplayName("【異常系】")
        class ErrorCase {

            @Test
            @DisplayName("EC-1: サイトが見つからない場合、NOT_FOUND を返し以降の処理は行われない")
            void siteNotFound_returnsNotFoundAndSkipsRest() {
                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.empty());

                CancelRequestResponse response = service.createCancelRequest(
                        USERNAME, COMPANY_ID, createRequest());

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
                assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);

                verifyNoInteractions(monitoringRecordSolarRepository, cancelRequestRepository, notificationService);
            }

            @Test
            @DisplayName("EC-2: サイトの会社IDがリクエスト元の companyId と一致しない場合、FORBIDDEN を返す")
            void companyIdMismatch_returnsForbidden() {
                Site site = siteFor("COMP999"); // company lain
                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));

                CancelRequestResponse response = service.createCancelRequest(
                        USERNAME, COMPANY_ID, createRequest());

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
                assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
                verifyNoInteractions(monitoringRecordSolarRepository, cancelRequestRepository);
            }

            @Test
            @DisplayName("EC-3: サイトに会社情報が null の場合、FORBIDDEN を返す（NPEなし）")
            void siteCompanyNull_returnsForbiddenWithoutNpe() {
                Site site = mock(Site.class);
                when(site.getCompany()).thenReturn(null);
                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));

                CancelRequestResponse response = service.createCancelRequest(
                        USERNAME, COMPANY_ID, createRequest());

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            }

            @Test
            @DisplayName("EC-4: NoSQL 上にモニタリングレコードが存在しない場合、NOT_FOUND を返す")
            void monitoringRecordNotFound_returnsNotFound() {
                Site site = siteFor(COMPANY_ID);
                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.empty());

                CancelRequestResponse response = service.createCancelRequest(
                        USERNAME, COMPANY_ID, createRequest());

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
                verifyNoInteractions(cancelRequestRepository);
            }

            @Test
            @DisplayName("EC-5: レコードが既に取り消し済み（isActive=false）の場合、BAD_REQUEST を返す")
            void recordAlreadyCancelled_returnsBadRequest() {
                Site site = siteFor(COMPANY_ID);
                EnR002MonitoringRecord inactiveRecord = monitoringRecord(false);

                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(inactiveRecord));

                CancelRequestResponse response = service.createCancelRequest(
                        USERNAME, COMPANY_ID, createRequest());

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
                assertThat(response.getMessage()).isEqualTo("このレコードは既に取り消されています。");
                verifyNoInteractions(cancelRequestRepository);
            }

            @Test
            @DisplayName("EC-6: 同レコードに対する未処理の取消申請が既に存在する場合、BAD_REQUEST を返す")
            void pendingRequestAlreadyExists_returnsBadRequest() {
                Site site = siteFor(COMPANY_ID);
                EnR002MonitoringRecord record = monitoringRecord(true);

                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(record));
                when(cancelRequestRepository.existsPendingRequestForRecord(RECORD_PK, RECORD_SK))
                        .thenReturn(true);

                CancelRequestResponse response = service.createCancelRequest(
                        USERNAME, COMPANY_ID, createRequest());

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
                assertThat(response.getMessage()).isEqualTo("このレコードの取消申請が既に存在します。");
                verify(cancelRequestRepository, never()).save(any());
            }

            @Test
            @DisplayName("EC-7: 保存中に予期しない例外が発生した場合、例外は伝播せず INTERNAL_SERVER_ERROR を返す")
            void unexpectedExceptionDuringSave_caughtAndReturnsInternalServerError() {
                // Method ini membungkus SELURUH isi dengan catch(Exception) generik —
                // berbeda dari service lain: exception TIDAK PERNAH lolos ke pemanggil.
                Site site = siteFor(COMPANY_ID);
                EnR002MonitoringRecord record = monitoringRecord(true);

                when(siteRepository.findByPublicIdWithFallback("site-1")).thenReturn(Optional.of(site));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(record));
                when(cancelRequestRepository.existsPendingRequestForRecord(RECORD_PK, RECORD_SK))
                        .thenReturn(false);
                when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                        .thenThrow(new RuntimeException("DB down"));

                CancelRequestResponse response = service.createCancelRequest(
                        USERNAME, COMPANY_ID, createRequest());

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
                assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
            }
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("listCancelRequests のテスト")
    class ListCancelRequestsTest {

        @Test
        @DisplayName("SC-1: 取消申請が存在する場合、DTO へマッピングされたリストを OK で返す")
        void hasData_returnsMappedList() {
            Site site = siteFor(COMPANY_ID);
            RecordCancelRequest cr = pendingCancelRequest(site);

            when(cancelRequestRepository.findAll()).thenReturn(List.of(cr));

            CancelRequestListResponse response = service.listCancelRequests();

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getId()).isEqualTo(10);
            assertThat(response.getItems().get(0).getStatus()).isEqualTo("pending"); // lowercase
        }

        @Test
        @DisplayName("SC-2: 取消申請が1件もない場合、空リストで OK を返す")
        void noData_returnsEmptyList() {
            when(cancelRequestRepository.findAll()).thenReturn(Collections.emptyList());

            CancelRequestListResponse response = service.listCancelRequests();

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getItems()).isEmpty();
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("toListItemDto のテスト（マッピング関数を直接検証）")
    class ToListItemDtoTest {

        @Test
        @DisplayName("UT-01: status が大文字から小文字に変換される（PENDING → pending）")
        void statusConvertedToLowercase() {
            RecordCancelRequest cr = pendingCancelRequest(siteFor(COMPANY_ID));

            CancelRequestItemDto dto = service.toListItemDto(cr);

            assertThat(dto.getStatus()).isEqualTo("pending");
        }

        @Test
        @DisplayName("UT-02: site が null の場合、siteName も null になる（NPEなし）")
        void siteNull_siteNameNull() {
            RecordCancelRequest cr = RecordCancelRequest.builder()
                    .site(null)
                    .status("PENDING")
                    .recordPk(RECORD_PK)
                    .requestedBy(USERNAME)
                    .requestedAt(Instant.parse("2026-07-10T09:00:00Z"))
                    .build();
            cr.setId(11);

            CancelRequestItemDto dto = service.toListItemDto(cr);

            assertThat(dto.getSiteName()).isNull();
        }

        @Test
        @DisplayName("UT-03: decidedAt が null の場合（未決定）、DTO の decidedAt も null になる")
        void decidedAtNull_dtoDecidedAtNull() {
            RecordCancelRequest cr = pendingCancelRequest(siteFor(COMPANY_ID));
            // decidedAt tidak di-set → null (belum diputuskan)

            CancelRequestItemDto dto = service.toListItemDto(cr);

            assertThat(dto.getDecidedAt()).isNull();
        }

        @Test
        @DisplayName("UT-04: decidedAt が設定済みの場合、文字列化されて DTO に反映される")
        void decidedAtPresent_mappedAsString() {
            RecordCancelRequest cr = pendingCancelRequest(siteFor(COMPANY_ID));
            cr.setDecidedAt(Instant.parse("2026-07-11T10:00:00Z"));
            cr.setDecidedBy("operator001");

            CancelRequestItemDto dto = service.toListItemDto(cr);

            assertThat(dto.getDecidedAt()).isEqualTo("2026-07-11T10:00:00Z");
            assertThat(dto.getDecidedBy()).isEqualTo("operator001");
        }

        /*
         * UT-05 — GAP: requestedAt TIDAK di-null-check, berbeda dari decidedAt yang dicek.
         * entity.getRequestedAt().toString() akan NPE kalau requestedAt null.
         * Dalam praktiknya requestedAt selalu di-set saat createCancelRequest(), tapi
         * asimetri null-safety ini tetap worth didokumentasikan.
         */
        @Test
        @DisplayName("UT-05: requestedAt が null の場合、NPE が発生する（欠陥候補・非対称なnullチェック）")
        void requestedAtNull_currentlyThrowsNpe() {
            RecordCancelRequest cr = RecordCancelRequest.builder()
                    .site(siteFor(COMPANY_ID))
                    .status("PENDING")
                    .recordPk(RECORD_PK)
                    .requestedBy(USERNAME)
                    .requestedAt(null) // ← sengaja null
                    .build();
            cr.setId(12);

            assertThrows(NullPointerException.class, () -> service.toListItemDto(cr));
        }
    }

    // =========================================================================
    @Nested
    @DisplayName("processDecision のテスト")
    class ProcessDecisionTest {

        private CancelRequestDecisionRequest decisionRequest(String decision) {
            CancelRequestDecisionRequest r = new CancelRequestDecisionRequest();
            r.setDecision(decision);
            r.setManagerRemarks("承認しました");
            return r;
        }

        @Nested
        @DisplayName("【承認（APPROVE）】")
        class ApproveDecision {

            @Test
            @DisplayName("SC-1: 承認が成功した場合、相殺レコードが作成され APPROVED として保存される")
            void approve_success_createsOffsetRecordAndSavesApproved() {
                // Arrange
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                EnR002MonitoringRecord original = monitoringRecord(true);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(original));
                when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                        .thenAnswer(inv -> inv.getArgument(0));
                when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                // Act
                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("APPROVE"), "operator001");

                // Assert — response
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
                assertThat(response.getStatus()).isEqualTo("APPROVED");
                assertThat(response.getDecidedBy()).isEqualTo("operator001");
                assertThat(response.getOffsetRecordPk()).isEqualTo(RECORD_PK); // = original.getId()

                // Assert — nilai relasi mentah dibalik (negate) pada record penyeimbang
                ArgumentCaptor<EnR002MonitoringRecord> offsetCaptor =
                        ArgumentCaptor.forClass(EnR002MonitoringRecord.class);
                verify(monitoringRecordSolarRepository).save(offsetCaptor.capture());
                EnR002MonitoringRecord offset = offsetCaptor.getValue();
                assertThat(offset.getElPv()).isEqualByComparingTo("-1200.5");
                assertThat(offset.getElPvr()).isEqualByComparingTo("-300.0");
                assertThat(offset.getElpjControlValue()).isEqualByComparingTo("-24.0");
                assertThat(offset.getElpjBatteryValue()).isEqualByComparingTo("-15.5");
                // Identitas (PK) & metadata proyek/site harus tetap sama dengan original
                assertThat(offset.getId()).isEqualTo(original.getId());
                assertThat(offset.getProjectID()).isEqualTo(original.getProjectID());
                assertThat(offset.getSiteID()).isEqualTo(original.getSiteID());
                // Bukti/evidence harus dikosongkan pada record penyeimbang
                assertThat(offset.getElPvEvidenceUrl()).isNull();
                assertThat(offset.getElpjControlEvidenceUrl()).isNull();

                // Assert — entity RDB yang disimpan
                ArgumentCaptor<RecordCancelRequest> crCaptor =
                        ArgumentCaptor.forClass(RecordCancelRequest.class);
                verify(cancelRequestRepository).save(crCaptor.capture());
                assertThat(crCaptor.getValue().getStatus()).isEqualTo("APPROVED");
                assertThat(crCaptor.getValue().getDecidedBy()).isEqualTo("operator001");

                verify(notificationService, times(1))
                        .createNotification(anyString(), anyString(), eq(1), eq(null));
            }

            @Test
            @DisplayName("SC-2: 対象者への通知が失敗しても、承認結果には影響しない")
            void approve_participantNotificationFails_stillReturnsOk() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                EnR002MonitoringRecord original = monitoringRecord(true);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(original));
                when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                        .thenAnswer(inv -> inv.getArgument(0));
                when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                        .thenAnswer(inv -> inv.getArgument(0));
                doThrow(new RuntimeException("notification service down"))
                        .when(notificationService).createNotification(anyString(), anyString(), any(), any());

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("APPROVE"), "operator001");

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
                assertThat(response.getStatus()).isEqualTo("APPROVED");
            }

            @Test
            @DisplayName("SC-3: participatingUserID が null の場合、通知はスキップされ承認は成功する")
            void approve_participantIdNull_skipsNotification() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                EnR002MonitoringRecord original = monitoringRecord(true);
                when(original.getParticipatingUserID()).thenReturn(null);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(original));
                when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                        .thenAnswer(inv -> inv.getArgument(0));
                when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("APPROVE"), "operator001");

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
                verify(notificationService, never())
                        .createNotification(anyString(), anyString(), any(), any());
            }

            @Test
            @DisplayName("EC-4: NoSQL 上に元のレコードが見つからない場合、NOT_FOUND を返し RDB は更新されない")
            void approve_originalRecordNotFound_returnsNotFoundAndSkipsRdbUpdate() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.empty());

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("APPROVE"), "operator001");

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
                verify(monitoringRecordSolarRepository, never()).save(any());
                verify(cancelRequestRepository, never()).save(any());
            }

            @Test
            @DisplayName("EC-5: 相殺レコードの保存に失敗した場合、INTERNAL_SERVER_ERROR を返し RDB は更新されない")
            void approve_offsetRecordSaveFails_returnsInternalServerErrorAndSkipsRdbUpdate() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                EnR002MonitoringRecord original = monitoringRecord(true);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(original));
                when(monitoringRecordSolarRepository.save(any(EnR002MonitoringRecord.class)))
                        .thenThrow(new RuntimeException("DynamoDB throttled"));

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("APPROVE"), "operator001");

                // approveRequest() punya try-catch sendiri → error graceful, TIDAK propagate
                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
                assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
                // Kegagalan terjadi sebelum update RDB, jadi cancelRequest tidak boleh ikut ter-update
                verify(cancelRequestRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("【却下（REJECT）】")
        class RejectDecision {

            @Test
            @DisplayName("SC-4: 却下が成功した場合、相殺レコードは作成されず REJECTED として保存される")
            void reject_success_savesRejectedWithoutOffsetRecord() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                EnR002MonitoringRecord original = monitoringRecord(true);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(original));
                when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("REJECT"), "operator001");

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
                assertThat(response.getStatus()).isEqualTo("REJECTED");
                assertThat(response.getDecidedBy()).isEqualTo("operator001");

                // Tidak ada relasi penyeimbang yang dibuat untuk penolakan
                verify(monitoringRecordSolarRepository, never()).save(any());

                ArgumentCaptor<RecordCancelRequest> captor =
                        ArgumentCaptor.forClass(RecordCancelRequest.class);
                verify(cancelRequestRepository).save(captor.capture());
                assertThat(captor.getValue().getStatus()).isEqualTo("REJECTED");

                verify(notificationService, times(1))
                        .createNotification(anyString(), anyString(), eq(1), eq(null));
            }

            @Test
            @DisplayName("SC-5: 対象者への却下通知が失敗しても、却下結果には影響しない")
            void reject_participantNotificationFails_stillReturnsOk() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                EnR002MonitoringRecord original = monitoringRecord(true);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(original));
                when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                        .thenAnswer(inv -> inv.getArgument(0));
                doThrow(new RuntimeException("notification service down"))
                        .when(notificationService).createNotification(anyString(), anyString(), any(), any());

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("REJECT"), "operator001");

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
                assertThat(response.getStatus()).isEqualTo("REJECTED");
            }

            @Test
            @DisplayName("EC-6: 保存中に例外が発生した場合、rejectRequest には try-catch がなく例外がそのまま伝播される（欠陥候補）")
            void reject_saveThrows_propagatesUncaught() {
                // BEDA dengan approveRequest (EC-5 di atas): rejectRequest TIDAK punya
                // try-catch sendiri. Kegagalan save() di sini akan lolos mentah ke pemanggil,
                // bukan dikonversi jadi response INTERNAL_SERVER_ERROR yang rapi.
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                EnR002MonitoringRecord original = monitoringRecord(true);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(original));
                when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                        .thenThrow(new RuntimeException("DB down"));

                assertThrows(RuntimeException.class,
                        () -> service.processDecision(10, decisionRequest("REJECT"), "operator001"));
            }
        }

        @Nested
        @DisplayName("【共通の検証・境界】")
        class CommonValidation {

            @Test
            @DisplayName("EC-1: cancelRequestId が存在しない場合、IllegalStateException がスローされる")
            void cancelRequestNotFound_throwsIllegalStateException() {
                when(cancelRequestRepository.findById(999)).thenReturn(Optional.empty());

                IllegalStateException thrown = assertThrows(IllegalStateException.class,
                        () -> service.processDecision(999, decisionRequest("APPROVE"), "operator001"));

                assertThat(thrown.getMessage()).contains("999");
                verifyNoInteractions(monitoringRecordSolarRepository);
                verify(cancelRequestRepository, never()).save(any());
            }

            @Test
            @DisplayName("EC-2: ステータスが PENDING 以外の場合、BAD_REQUEST を返す")
            void statusNotPending_returnsBadRequest() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest alreadyApproved = pendingCancelRequest(site);
                alreadyApproved.setStatus("APPROVED");

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(alreadyApproved));

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("APPROVE"), "operator001");

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
                assertThat(response.getMessage()).contains("APPROVED");
                verifyNoInteractions(monitoringRecordSolarRepository);
            }

            @Test
            @DisplayName("EC-3: 相殺レコードが既に作成済みの場合、BAD_REQUEST を返す")
            void offsetRecordAlreadyExists_returnsBadRequest() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                pending.setOffsetRecordPk(RECORD_PK); // sudah pernah diproses sebelumnya

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("APPROVE"), "operator001");

                assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
                assertThat(response.getMessage()).isEqualTo("相殺レコードが既に作成済みです");
                verifyNoInteractions(monitoringRecordSolarRepository);
            }

            /*
             * EC-7 — GAP PENTING: nilai decision selain "APPROVE" (termasuk null,
             * typo, atau huruf kecil) SEMUANYA diperlakukan sebagai PENOLAKAN tanpa
             * validasi maupun error. Klien yang salah kirim payload bisa diam-diam
             * menolak permintaan yang sah.
             */
            @Test
            @DisplayName("EC-7: decision が null の場合、暗黙的に却下として処理される（欠陥候補）")
            void nullDecision_silentlyTreatedAsReject() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                EnR002MonitoringRecord original = monitoringRecord(true);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(original));
                when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest(null), "operator001");

                // Perilaku SAAT INI: null → REJECTED, bukan error validasi
                assertThat(response.getStatus()).isEqualTo("REJECTED");

                // Idealnya: request dengan decision tidak valid seharusnya ditolak dengan
                // BAD_REQUEST, bukan diam-diam diproses sebagai penolakan sah.
            }

            @Test
            @DisplayName("EC-8: decision berupa typo huruf kecil ('approve') juga diperlakukan sebagai却下（欠陥候補）")
            void lowercaseApproveTypo_silentlyTreatedAsReject() {
                Site site = siteFor(COMPANY_ID);
                RecordCancelRequest pending = pendingCancelRequest(site);
                EnR002MonitoringRecord original = monitoringRecord(true);

                when(cancelRequestRepository.findById(10)).thenReturn(Optional.of(pending));
                when(monitoringRecordSolarRepository.findByPKandSK(RECORD_PK, RECORD_SK))
                        .thenReturn(Optional.of(original));
                when(cancelRequestRepository.save(any(RecordCancelRequest.class)))
                        .thenAnswer(inv -> inv.getArgument(0));

                CancelRequestDecisionResponse response =
                        service.processDecision(10, decisionRequest("approve"), "operator001");

                // "APPROVE".equals("approve") == false → jatuh ke rejectRequest()
                assertThat(response.getStatus()).isEqualTo("REJECTED");
                verify(monitoringRecordSolarRepository, never()).save(any());
            }
        }
    }
}
