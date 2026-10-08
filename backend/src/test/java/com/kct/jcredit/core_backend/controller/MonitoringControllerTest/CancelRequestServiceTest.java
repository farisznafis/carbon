package com.kct.jcredit.core_backend.controller.MonitoringControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks private CancelRequestServiceImpl service;

    private ParticipatingCompany company;
    private Site site;
    private EnR002MonitoringRecord record;
    private RecordCancelRequest cancelRequest;
    private CreateCancelRequestRequest createRequest;

    @BeforeEach
    void setUp() {
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
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CancelRequestResponse create() {
        when(cancelRequestRepository.existsPendingRequestForRecord(RECORD_PK, RECORD_SK))
                .thenReturn(false);
        return service.createCancelRequest(USERNAME, COMPANY_ID, createRequest);
    }

    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = CancelRequestServiceImpl.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(service, args);
    }

    @Nested
    @DisplayName("POST /monitoring/cancel-request のテスト")
    class CreateCancelRequestTest {

        @Test
        @DisplayName("UT-CR-001: 指定siteIdが存在しない場合はNOT_FOUNDを返す")
        void createCancelRequest_siteNotFound_returnsNotFound() {
            // Input: request.siteId=DBに存在しない値
            // Output: statusCode=NOT_FOUND、message=MSG_SITE_NOT_FOUNDが返却される
            //         findByPublicIdWithFallbackの存在チェックが機能すること
        }

        @Test
        @DisplayName("UT-CR-002: 拠点のcompanyがnullまたは指定companyIdと不一致の場合はFORBIDDENを返す")
        void createCancelRequest_siteCompanyMismatchOrNull_returnsForbidden() {
            // Input: site.company=null、またはsite.company.companyId!=companyId
            // Output: statusCode=FORBIDDEN、message=MSG_FORBIDDENが返却される
            //         会社所属チェック（site.getCompany）が機能すること
        }

        @Test
        @DisplayName("UT-CR-003: 指定recordPK/SKのモニタリングレコードがNoSQLに存在しない場合はNOT_FOUNDを返す")
        void createCancelRequest_monitoringRecordNotFound_returnsNotFound() {
            // Input: recordPK/recordSKがDBに存在しない組合せ
            // Output: statusCode=NOT_FOUND、message=MSG_NOT_FOUNDが返却される
            //         findByPKandSKの存在チェックが機能すること
        }

        @Test
        @DisplayName("UT-CR-004: 対象レコードが既に取消済み(isActive=false)の場合はエラーを返す")
        void createCancelRequest_recordAlreadyCancelled_returnsBadRequest() {
            // Input: monitoringRecord.isActive=false
            // Output: statusCode=BAD_REQUEST、message=『このレコードは既に取り消されています。』が返却される（仕様書の400/ALREADY_CANCELLEDとの対応を確認）
            //         isActiveフラグによる重複取消防止チェックが機能すること
        }

        @Test
        @DisplayName("UT-CR-005【要レビュー】: ALREADY_CANCELLEDエラー時にerrorCodeが設定されない")
        void createCancelRequest_alreadyCancelledError_missingErrorCode() {
            // Input: monitoringRecord.isActive=false
            // Output: レスポンスにerrorCode=ALREADY_CANCELLEDが設定されていない（messageのみ）
            //         API仕様書のerrorCode契約とレスポンスDTOの実装差異を確認すること（要レビュー）
        }

        @Test
        @DisplayName("UT-CR-006: 同一レコードに対する未処理(PENDING)の取消申請が既に存在する場合はエラーを返す")
        void createCancelRequest_pendingRequestAlreadyExists_returnsBadRequest() {
            // Input: existsPendingRequestForRecord=true
            // Output: statusCode=BAD_REQUEST、message=『このレコードの取消申請が既に存在します。』が返却される（仕様書の409/REQUEST_ALREADY_EXISTSとの対応を確認）
            //         existsPendingRequestForRecordによる重複申請防止チェックが機能すること
        }

        @Test
        @DisplayName("UT-CR-007【要レビュー】: ステータスコードが仕様書(400/409)と実装(全て400)で不整合")
        void createCancelRequest_statusCodeMapping_inconsistentWithSpec() {
            // Input: UT-CR-004・UT-CR-006と同条件
            // Output: 両ケースともstatusCode=400で返却され、仕様書の409との不整合が確認される
            //         ステータスコードマッピングの実装漏れを確認すること（要レビュー）
        }

        @Test
        @DisplayName("UT-CR-008: 全条件を満たす場合に取消申請が正常に作成される")
        void createCancelRequest_allValid_returnsOk() {
            // Input: 有効なsiteId・recordPK/SK・reasonTextを含むrequest
            // Output: statusCode=OK、cancelRequestId・status=PENDING・requestedBy・requestedAt等を含むレスポンスが返却される
            //         取消申請の正常登録処理全体が機能すること
        }

        @Test
        @DisplayName("UT-CR-009: 登録成功時に全運用ユーザーへの通知（アプリ内通知・メール）が送信される")
        void createCancelRequest_success_notifiesAllOperatingUsers() {
            // Input: 有効なrequestで登録成功
            // Output: notificationService.createNotificationForAllOperatingUsers/sendEmailToAllOperatingUsersが呼び出される
            //         cancel.createdテンプレートによる運用ユーザー通知ロジックが機能すること
        }

        @Test
        @DisplayName("UT-CR-010: 通知処理で例外が発生しても登録処理自体は成功として返す")
        void createCancelRequest_notificationException_stillReturnsOk() {
            // Input: notificationServiceがRuntimeExceptionを発生
            // Output: statusCode=OKのまま登録成功レスポンスが返却され、例外はログ警告のみで握りつぶされる
            //         通知失敗時のtry-catchによる本処理への影響分離が機能すること
        }

        @Test
        @DisplayName("UT-CR-011: 処理中に予期せぬ例外(DB接続エラー等)が発生した場合は内部エラーを返す")
        void createCancelRequest_unexpectedException_returnsInternalError() {
            // Input: DB接続エラー等をモックで発生させる
            // Output: statusCode=INTERNAL_SERVER_ERROR、message=MSG_INTERNAL_ERRORが返却される
            //         try-catchによる例外ハンドリングが機能すること
        }

        @Test
        @DisplayName("UT-CR-012【要レビュー】: usernameに対する会社所属・ユーザー存在チェックが行われていない")
        void createCancelRequest_noUserCompanyValidation_potentialGap() {
            // Input: 存在しないusername、または他社に所属するusernameを渡す
            // Output: ユーザーの実在性・会社所属に関する検証なしで処理が進む可能性がある
            //         呼び出し元(Controller等)での認可保証に依存している実装のため、Service単体での権限検証漏れを確認すること（要レビュー）
        }
    }

    @Nested
    @DisplayName("GET /monitoring/cancel-requests のテスト")
    class ListCancelRequestsTest {

        @Test
        @DisplayName("UT-CR-013【要レビュー・重大】: 会社・権限によるフィルタリングが一切行われない")
        void listCancelRequests_noTenantFiltering_returnsAllCompaniesData() {
            // Input: 引数なしでlistCancelRequestsを呼び出す
            // Output: findAll()により全社・全拠点の取消申請が無条件に返却される
            //         会社間データ分離（マルチテナント境界）が機能していないことを確認すること（要レビュー・重大）
        }

        @Test
        @DisplayName("UT-CR-014【要レビュー】: デフォルトPENDINGフィルタが実装されておらず全ステータスが返却される")
        void listCancelRequests_noStatusFilter_returnsAllStatuses() {
            // Input: PENDING・APPROVED・REJECTEDが混在するデータ
            // Output: 全ステータスのレコードがitemsに含まれて返却される
            //         仕様書とfindAll()実装の不整合（デフォルトフィルタ未実装）を確認すること（要レビュー）
        }

        @Test
        @DisplayName("UT-CR-015: 取得した取消申請エンティティがDTOへ正しく変換される")
        void toListItemDto_validEntity_mapsFieldsCorrectly() {
            // Input: status=PENDING、site.siteName=有効値、decidedAt=null等のentity
            // Output: statusが小文字(pending)に変換され、id/requestedAt/reason/recordPk/requestedBy/siteName等が正しく設定される
            //         toListItemDtoのフィールドマッピングが機能すること
        }

        @Test
        @DisplayName("UT-CR-016: site=nullの場合でも例外とならずsiteName=nullとして変換される")
        void toListItemDto_siteNull_returnsNullSiteNameWithoutException() {
            // Input: entity.site=null
            // Output: siteName=nullが設定され、NullPointerExceptionが発生しない
            //         site nullガード（三項演算子）が機能すること
        }

        @Test
        @DisplayName("UT-CR-017: decidedAt/decidedByが未設定(PENDING)の場合はnullとして変換される")
        void toListItemDto_pendingStatus_decidedFieldsNull() {
            // Input: entity.decidedAt=null、decidedBy=null
            // Output: decidedAt=null、decidedBy=nullが設定される
            //         未決定レコードのnullガードが機能すること
        }

        @Test
        @DisplayName("UT-CR-018: 取消申請が0件の場合は空リストが返却される（NOT_FOUND等のエラーにならない）")
        void listCancelRequests_noData_returnsEmptyListNotError() {
            // Input: データなし
            // Output: statusCode=OK、items=空リストが返却される
            //         0件時の正常終了（MonitoringServiceImplと異なりNOT_FOUNDにならない仕様差異）を確認すること
        }

        @Test
        @DisplayName("UT-CR-019【要レビュー】: 処理中に例外が発生した場合にtry-catchが存在せず伝播する")
        void listCancelRequests_unexpectedException_propagatesWithoutHandling() {
            // Input: DB接続エラー等をモックで発生させる
            // Output: try-catchが実装されていないため例外がそのまま呼び出し元に伝播する（INTERNAL_SERVER_ERRORへの変換なし）
            //         他API（create/processDecision内部）と異なり例外ハンドリングが未実装であることを確認すること（要レビュー）
        }
    }

    @Nested
    @DisplayName("POST /monitoring/{cancelRequestId}/cancel-decision のテスト")
    class ProcessDecisionTest {

        @Test
        @DisplayName("UT-CR-020【要レビュー・他APIと非対称】: 指定cancelRequestIdが存在しない場合の例外伝播を確認する")
        void processDecision_requestNotFound_illegalStateExceptionPropagates() {
            // Input: requestId=DBに存在しない値
            // Output: IllegalStateExceptionがorElseThrowで発生し、catchされず呼び出し元に伝播する
            //         findById().orElseThrowがService内でハンドリングされていないことを確認すること（要レビュー・他APIと非対称）
        }

        @Test
        @DisplayName("UT-CR-021: 取消申請のstatusがPENDING以外の場合はエラーを返す")
        void processDecision_statusNotPending_returnsBadRequest() {
            // Input: cancelRequest.status=APPROVED/REJECTED
            // Output: statusCode=BAD_REQUEST、message=『PENDING以外は処理できません。現在の状態: {status}』が返却される
            //         ステータス遷移の不正状態チェックが機能すること
        }

        @Test
        @DisplayName("UT-CR-022: 相殺レコードが既に作成済み(offsetRecordPk!=null)の場合はエラーを返す")
        void processDecision_offsetRecordAlreadyCreated_returnsBadRequest() {
            // Input: cancelRequest.offsetRecordPk=既存値あり
            // Output: statusCode=BAD_REQUEST、message=『相殺レコードが既に作成済みです』が返却される
            //         offsetRecordPkによる相殺レコード重複作成防止チェックが機能すること
        }

        @Test
        @DisplayName("UT-CR-023【要レビュー・重大】: 操作者の会社所属・権限（MASTER/管理者role等）チェックが存在しない")
        void processDecision_noOperatorAuthorizationCheck_potentialGap() {
            // Input: 他社に所属するoperatorUsername、または一般USER権限のユーザーを渡す
            // Output: 権限検証なしで承認・却下処理が実行される（会社・role問わず操作可能）
            //         管理者権限および会社間データ分離の検証が機能していないことを確認すること（要レビュー・重大）
        }

        @Test
        @DisplayName("UT-CR-024: decision=APPROVEの場合はapproveRequestに処理が委譲される")
        void processDecision_decisionApprove_delegatesToApproveRequest() {
            // Input: request.decision=APPROVE、status=PENDING
            // Output: approveRequestが呼び出され、承認処理が実行される
            //         decision分岐ロジックが機能すること
        }

        @Test
        @DisplayName("UT-CR-025【要レビュー】: decision=APPROVE以外（任意の値含む）はrejectRequestに処理が委譲される")
        void processDecision_decisionNotApprove_delegatesToRejectRequestWithoutValidation() {
            // Input: request.decision=REJECT、または未定義文字列
            // Output: rejectRequestが呼び出され、却下処理が実行される
            //         APPROVE以外を全て却下扱いとするelse分岐の仕様（文字列検証なし）を確認すること（要レビュー）
        }
    }

    @Nested
    @DisplayName("approveRequest（承認処理）のテスト")
    class ApproveRequestTest {

        @Test
        @DisplayName("UT-CR-026: 承認処理時に元のモニタリングレコードがNoSQLに存在しない場合はNOT_FOUNDを返す")
        void approveRequest_originalRecordNotFound_returnsNotFound() {
            // Input: originalRecord=NoSQLに存在しない recordPk/recordSk
            // Output: statusCode=NOT_FOUND、message=MSG_NOT_FOUNDが返却される
            //         findByPKandSKの存在チェックが機能すること
        }

        @Test
        @DisplayName("UT-CR-027: 承認時に元レコードの数値項目が反転(negate)された相殺レコードが作成される")
        void createOffsetRecord_validNumericFields_negatesAllValues() {
            // Input: original.ElPv=480.5等の有効な数値を含むレコード
            // Output: 各数値項目が符号反転(-480.5等)された相殺レコードが生成される
            //         相殺レコード生成の値反転ロジックが機能すること
        }

        @Test
        @DisplayName("UT-CR-028: 反転対象の数値項目がnullの場合はnullのまま維持される")
        void createOffsetRecord_nullNumericField_keepsNullWithoutException() {
            // Input: original.ElpjBatteryValue=null等
            // Output: nullガードにより反転処理がスキップされnullが維持される（NullPointerExceptionが発生しない）
            //         negate対象のnullチェックが機能すること
        }

        @Test
        @DisplayName("UT-CR-029: 相殺レコードは元レコードと同一PK・projectID・siteID・participatingUserID・dateRecordedを保持し新しいSKが設定される")
        void createOffsetRecord_sameKeyFields_newTimeUpdated() {
            // Input: 有効なoriginalレコード
            // Output: id/projectID/siteID/participatingUserID/dateRecordedが元レコードと一致し、timeUpdatedは新しい現在時刻文字列となる
            //         相殺レコードのPK/SK設計（同一PK・新SK）が機能すること
        }

        @Test
        @DisplayName("UT-CR-030: 相殺レコードの証憑関連項目は全てnullでクリアされる")
        void createOffsetRecord_evidenceFields_clearedToNull() {
            // Input: 有効な証憑URLを含むoriginalレコード
            // Output: ElPvEvidence/ElPvrEvidence/ElpjBatteryEvidence/ElpjControlEquipmentSpecFiles及び各EvidenceUrlが全てnullとなる
            //         相殺レコードにおける証憑クリアロジックが機能すること
        }

        @Test
        @DisplayName("UT-CR-031: 相殺レコードはisActive=trueとして作成される")
        void createOffsetRecord_isActiveTrue_setOnCreation() {
            // Input: 有効なoriginalレコード
            // Output: offsetRecord.isActive=trueが設定される
            //         相殺レコード自体は有効状態として登録される仕様の確認
        }

        @Test
        @DisplayName("UT-CR-032: 承認成功時にRecordCancelRequestのstatus・decidedBy・decidedAt・offsetRecordPk/SKが更新保存される")
        void approveRequest_success_updatesCancelRequestFields() {
            // Input: approveRequest正常実行
            // Output: cancelRequest.status=APPROVED、decidedBy=operatorUsername、decidedAt=現在時刻、offsetRecordPk/SKが設定される
            //         承認時のRDB更新ロジックが機能すること
        }

        @Test
        @DisplayName("UT-CR-033: 承認成功時に元レコードの参加ユーザーへ承認通知が送信される")
        void approveRequest_success_notifiesParticipatingUser() {
            // Input: originalRecord.participatingUserID=有効値
            // Output: notificationService.createNotificationがcancel.approvedテンプレートで呼び出される
            //         承認時の参加ユーザー通知ロジックが機能すること
        }

        @Test
        @DisplayName("UT-CR-034: participatingUserIDがnullの場合は通知がスキップされエラーにならない")
        void approveRequest_participatingUserIdNull_skipsNotification() {
            // Input: originalRecord.participatingUserID=null
            // Output: 通知送信がスキップされ、承認処理自体は正常完了する
            //         participantId nullガードが機能すること
        }

        @Test
        @DisplayName("UT-CR-035: 通知処理で例外が発生しても承認処理自体は成功として返す")
        void approveRequest_notificationException_stillReturnsOk() {
            // Input: notificationServiceがRuntimeExceptionを発生
            // Output: statusCode=OKのまま承認成功レスポンスが返却され、例外はログ警告のみで握りつぶされる
            //         通知失敗時のtry-catchによる本処理への影響分離が機能すること
        }

        @Test
        @DisplayName("UT-CR-036: 承認処理中に予期せぬ例外(DB保存エラー等)が発生した場合は内部エラーを返す")
        void approveRequest_unexpectedException_returnsInternalError() {
            // Input: monitoringRecordSolarRepository.save等でRuntimeExceptionを発生
            // Output: statusCode=INTERNAL_SERVER_ERROR、message=MSG_INTERNAL_ERRORが返却される
            //         approveRequestのtry-catchによる例外ハンドリングが機能すること
        }
    }

    @Nested
    @DisplayName("rejectRequest（却下処理）のテスト")
    class RejectRequestTest {

        @Test
        @DisplayName("UT-CR-037: 却下成功時にRecordCancelRequestのstatus・decidedBy・decidedAtが更新保存される")
        void rejectRequest_success_updatesCancelRequestFields() {
            // Input: rejectRequest正常実行
            // Output: cancelRequest.status=REJECTED、decidedBy=operatorUsername、decidedAt=現在時刻が設定され保存される
            //         却下時のRDB更新ロジックが機能すること
        }

        @Test
        @DisplayName("UT-CR-038: 却下時はoffsetRecordPk/SKが設定されずnullのまま、managerRemarksがレスポンスに含まれる")
        void rejectRequest_success_responseContainsManagerRemarksNoOffsetRecord() {
            // Input: rejectRequest正常実行、request.managerRemarks=『証跡不足のため却下』
            // Output: レスポンスにoffsetRecordPk等が含まれずmanagerRemarksが設定される
            //         却下レスポンスの構築ロジックが機能すること
        }

        @Test
        @DisplayName("UT-CR-039: 却下成功時に元レコードの参加ユーザーへ却下通知が送信される")
        void rejectRequest_success_notifiesParticipatingUser() {
            // Input: originalRecord.participatingUserID=有効値
            // Output: notificationService.createNotificationがcancel.rejectedテンプレート(managerRemarks含む)で呼び出される
            //         却下時の参加ユーザー通知ロジックが機能すること
        }

        @Test
        @DisplayName("UT-CR-040【要レビュー・approveと非対称】: 元レコードが存在しない場合でも却下処理自体は続行される")
        void rejectRequest_originalRecordNotFound_stillProceedsUnlikeApprove() {
            // Input: originalRecord=NoSQLに存在しない
            // Output: originalRecord.isPresent()=falseのため通知はスキップされるが、却下処理（status=REJECTED保存）自体は正常完了する
            //         approveRequestとの非対称仕様（元レコード必須性の違い）を確認すること（要レビュー）
        }

        @Test
        @DisplayName("UT-CR-041【要レビュー・approveと非対称】: 例外ハンドリングが実装されておらず伝播する")
        void rejectRequest_unexpectedException_propagatesWithoutHandling() {
            // Input: cancelRequestRepository.saveでRuntimeExceptionを発生させる
            // Output: 例外がハンドリングされずそのまま呼び出し元（processDecision/Controller）に伝播する
            //         approveRequestとの実装非対称（例外ハンドリング欠如）を確認すること（要レビュー）
        }
    }
}