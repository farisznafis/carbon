package com.kct.jcredit.core_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.NotificationListResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.NotificationDetailResponse;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.entity.Notification;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.repository.postgres.NotificationRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.impl.NotificationServiceImpl;
 
 
/**
 * NotificationServiceImpl 単体テスト
 *
 * <p>テスト設計方針：JSTQB（日本ソフトウェアテスト資格審査委員会）標準に準拠
 * <ul>
 *   <li>正常系 ：期待通りの入力で正常動作することを確認</li>
 *   <li>異常系 ：不正・予期しない入力に対して適切に処理されることを確認</li>
 *   <li>境界値 ：仕様上の境界条件で正しく動作することを確認</li>
 *   <li>要件適合性：現実装と要件定義のギャップを明示的に記録</li>
 * </ul>
 *
 * <p>{@code @Disabled} のついたテストは実装ギャップの記録であり、CI を意図的に赤にしないための措置。
 * {@code @Tag("requirement-gap")} を付与しているため、CI レポート上で別途集計可能。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl 単体テスト")
class NotificationServiceImplTest {
 
    // =========================================================
    // Mocks — 外部依存をすべてモック化
    // =========================================================
    @Mock private JavaMailSender mailSender;
    @Mock private NotificationRepository notificationRepository;
    @Mock private OperatingUserRepository operatingUserRepository;
    @Mock private OperatorCompanyRepository operatorCompanyRepository;
    @Mock private ParticipatingUserRepository participatingUserRepository;
 
    @InjectMocks
    private NotificationServiceImpl notificationService;
 
    // =========================================================
    // Shared Fixtures
    // =========================================================
    private PaginationDetailResponse paginationDetails;
    private Notification notification;
    private OperatingUser operatingCompany;
    private ParticipatingCompany participatingCompany;
    private ParticipatingUser participatingUser;
    private OperatorCompany operatorCompany;
 
    @BeforeEach
    void setUp() {
        // @Value フィールドは Spring コンテキスト不使用の単体テストでは注入されないため手動セット
        ReflectionTestUtils.setField(notificationService, "fromAddress",    "no-reply@gxcarbon.local");
        ReflectionTestUtils.setField(notificationService, "overrideToAddress", "");
 
        participatingCompany = ParticipatingCompany.builder()
            .companyId("101")
            .nameJa("株式会社サンプル発電")
            .email("contact@sample-solar.co.jp")
            .paymentMethod("CREDIT_CARD")
            .build();
 
        // id/createdAt/updatedAt は base/audit entity 由来で builder に乗らないため setter で個別セット
        participatingUser = ParticipatingUser.builder()
            .company(participatingCompany)
            .build();
        participatingUser.setId(10);
 
        operatingCompany = OperatingUser.builder()
            .username("operator_yamada")
            .email("yamada@gxcarbon.jp")
            .build();
        operatingCompany.setId(1);
 
        operatorCompany = OperatorCompany.builder()
            .email("partner-operator@gxcarbon.jp")
            .build();
        operatorCompany.setCompanyId("201");
 
        notification = Notification.builder()
            .title("テスト通知")
            .message("これはテスト用の通知メッセージです。")
            .read(false)
            .participatingUser(participatingUser)
            .operatingUser(null)
            .build();
        notification.setId(1);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setUpdatedAt(LocalDateTime.now());
 
        // TODO: PaginationDetailResponse の 5番目引数の用途を実クラスで確認
        paginationDetails = new PaginationDetailResponse(0, 10, 1L, 1, null, true);
    }
 
    // =========================================================================
    // 【要件3】sendSuspensionNotification — アカウント停止通知
    // =========================================================================
    @Nested
    @DisplayName("【要件3】sendSuspensionNotification")
    class SendSuspensionNotificationTest {
 
        @Test
        @DisplayName("TC-3-01 [正常系] 有効メールアドレスへ件名・本文・差出人が正しいメールを送信する")
        void TC_3_01_validEmail_sendsCorrectEmail() {
            notificationService.sendSuspensionNotification(participatingCompany);
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
 
            SimpleMailMessage sent = captor.getValue();
            assertThat(sent.getTo()).containsExactly("contact@sample-solar.co.jp");
            assertThat(sent.getFrom()).isEqualTo("no-reply@gxcarbon.local");
            assertThat(sent.getSubject()).contains("停止");
            // 支払い方法が本文に含まれることを確認（要件3の必須要素）
            assertThat(sent.getText()).contains("CREDIT_CARD");
        }
 
        @Test
        @DisplayName("TC-3-02 [境界値] メールアドレスが null → send() は呼ばれない")
        void TC_3_02_nullEmail_doesNotSend() {
            ParticipatingCompany noEmail = ParticipatingCompany.builder()
                .companyId("102")
                .email(null)
                .paymentMethod("CREDIT_CARD")
                .build();
 
            notificationService.sendSuspensionNotification(noEmail);
 
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }
 
        @Test
        @DisplayName("TC-3-03 [異常系] paymentMethod が null でも例外は発生せず本文に 'null' が出力される")
        void TC_3_03_nullPaymentMethod_noExceptionBodyContainsNull() {
            ParticipatingCompany nullPayment = ParticipatingCompany.builder()
                .companyId("103")
                .email("contact@sample-solar.co.jp")
                .paymentMethod(null)
                .build();
 
            assertThatCode(() -> notificationService.sendSuspensionNotification(nullPayment))
                .doesNotThrowAnyException();
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            // 備考: null が文字列 "null" としてそのまま出力される。コンテンツ品質の改善余地あり
            assertThat(captor.getValue().getText()).contains("null");
        }
 
        @Test
        @Tag("requirement-gap")
        @Disabled("TC-3-04: 要件3 はメール＋ダッシュボード両チャンネルの通知を要求するが、" +
                  "現実装はメール送信のみ。アプリ内通知（DB保存）が未実装。対応チケット: JIRA-XXX")
        @DisplayName("TC-3-04 [要件適合性] アカウント停止時にダッシュボード/アプリ内通知も作成される")
        void TC_3_04_suspension_shouldAlsoCreateInAppNotification() {
            notificationService.sendSuspensionNotification(participatingCompany);
 
            // 要件3: メールに加え notificationRepository.save() も呼ばれる必要がある
            verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
        }
 
        @Test
        @DisplayName("TC-3-05 [設定確認] overrideToAddress 設定時は本来の宛先を無視して override 先に送信される")
        void TC_3_05_overrideToAddress_sendsToOverride() {
            ReflectionTestUtils.setField(notificationService, "overrideToAddress", "override@staging.local");
 
            notificationService.sendSuspensionNotification(participatingCompany);
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getTo()).containsExactly("override@staging.local");
        }
    }
 
    // =========================================================================
    // 【要件4】sendReactivationNotification — アカウント再開通知
    // =========================================================================
    @Nested
    @DisplayName("【要件4】sendReactivationNotification")
    class SendReactivationNotificationTest {
 
        @Test
        @DisplayName("TC-4-01 [正常系] 有効メールアドレスへ件名・本文（企業ID含む）が正しいメールを送信する")
        void TC_4_01_validEmail_sendsCorrectEmail() {
            notificationService.sendReactivationNotification(participatingCompany);
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
 
            SimpleMailMessage sent = captor.getValue();
            assertThat(sent.getTo()).containsExactly("contact@sample-solar.co.jp");
            assertThat(sent.getSubject()).contains("再開");
            // 企業ID が本文に含まれることを確認（再開後の本人確認要素）
            assertThat(sent.getText()).contains("101");
        }
 
        @Test
        @DisplayName("TC-4-02 [境界値] メールアドレスが null → send() は呼ばれない")
        void TC_4_02_nullEmail_doesNotSend() {
            ParticipatingCompany noEmail = ParticipatingCompany.builder()
                .companyId("102")
                .email(null)
                .paymentMethod("CREDIT_CARD")
                .build();
 
            notificationService.sendReactivationNotification(noEmail);
 
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }
    }
 
    // =========================================================================
    // 【要件2】sendFailureNotification — 支払い失敗通知
    // =========================================================================
    @Nested
    @DisplayName("【要件2】sendFailureNotification")
    class SendFailureNotificationTest {
 
        @Test
        @DisplayName("TC-2-01 [正常系/現状仕様] ハードコードの admin@ 宛に企業ID・失敗理由を含むメールが送信される")
        void TC_2_01_currentImpl_sendsToHardcodedAdminWithDetails() {
            notificationService.sendFailureNotification(participatingCompany, "カード残高不足");
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
 
            SimpleMailMessage sent = captor.getValue();
            // 備考: 現実装は admin@gxcarbon.jp へのハードコード。要件2 は動的通知を要求（TC-2-02参照）
            assertThat(sent.getTo()).containsExactly("admin@gxcarbon.jp");
            assertThat(sent.getText()).contains("101");
            assertThat(sent.getText()).contains("カード残高不足");
        }
 
        @Test
        @Tag("requirement-gap")
        @Disabled("TC-2-02: 要件2 は登録済み運営ユーザー全員への動的通知を要求するが、" +
                  "現実装は admin@gxcarbon.jp への単一ハードコードのみ。" +
                  "operatingUserRepository が inject 済みだが未使用。対応チケット: JIRA-XXX")
        @DisplayName("TC-2-02 [要件適合性] 支払い失敗時に登録済み運営ユーザー全員へ動的に通知される")
        void TC_2_02_shouldNotifyAllRegisteredOperatingUsers() {
            when(operatingUserRepository.findAll()).thenReturn(List.of(operatingCompany));
 
            notificationService.sendFailureNotification(participatingCompany, "カード残高不足");
 
            verify(operatingUserRepository).findAll();
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getTo()).containsExactly("yamada@gxcarbon.jp");
        }
 
        @Test
        @DisplayName("TC-2-03 [異常系] failureReason が null でも例外は発生しない")
        void TC_2_03_nullFailureReason_noException() {
            assertThatCode(() ->
                notificationService.sendFailureNotification(participatingCompany, null)
            ).doesNotThrowAnyException();
 
            verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
        }
    }
 
    // =========================================================================
    // 【要件1.3】sendMasterAccountCredentials — アカウント発行通知
    // =========================================================================
    @Nested
    @DisplayName("【要件1.3】sendMasterAccountCredentials")
    class SendMasterAccountCredentialsTest {
 
        @Test
        @DisplayName("TC-1.3-01 [正常系] 企業ID・username・password を含むメールが正しい宛先に送信される")
        void TC_1_3_01_validInput_sendsEmailWithAllCredentials() {
            notificationService.sendMasterAccountCredentials(
                participatingCompany, "solar_admin", "P@ssw0rd!");
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
 
            SimpleMailMessage sent = captor.getValue();
            assertThat(sent.getTo()).containsExactly("contact@sample-solar.co.jp");
            assertThat(sent.getText()).contains("101");
            assertThat(sent.getText()).contains("solar_admin");
            // 備考: パスワードが平文で本文に含まれる。セキュリティレビューを別途実施すること
            assertThat(sent.getText()).contains("P@ssw0rd!");
        }
 
        @Test
        @DisplayName("TC-1.3-02 [境界値] メールアドレスが null → send() は呼ばれない")
        void TC_1_3_02_nullEmail_doesNotSend() {
            ParticipatingCompany noEmail = ParticipatingCompany.builder()
                .companyId("102")
                .email(null)
                .build();
 
            notificationService.sendMasterAccountCredentials(noEmail, "user", "pass");
 
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }
 
        @Test
        @DisplayName("TC-1.3-03 [異常系] username・password が null でも例外は発生しない")
        void TC_1_3_03_nullCredentials_noException() {
            assertThatCode(() ->
                notificationService.sendMasterAccountCredentials(participatingCompany, null, null)
            ).doesNotThrowAnyException();
 
            verify(mailSender).send(any(SimpleMailMessage.class));
        }
    }
 
    // =========================================================================
    // 【要件1.2】sendCompanyRejectionNotification — 申請却下通知
    // =========================================================================
    @Nested
    @DisplayName("【要件1.2】sendCompanyRejectionNotification")
    class SendCompanyRejectionNotificationTest {
 
        @Test
        @DisplayName("TC-1.2-01 [正常系] nameJa 設定済み → 会社名・却下理由を含む本文が送信される")
        void TC_1_2_01_withNameJa_sendsEmailWithCompanyNameAndReason() {
            notificationService.sendCompanyRejectionNotification(participatingCompany, "書類不備");
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
 
            SimpleMailMessage sent = captor.getValue();
            assertThat(sent.getTo()).containsExactly("contact@sample-solar.co.jp");
            assertThat(sent.getSubject()).contains("却下");
            assertThat(sent.getText()).startsWith("株式会社サンプル発電");
            assertThat(sent.getText()).contains("書類不備");
        }
 
        @Test
        @DisplayName("TC-1.2-02 [境界値] nameJa が null → 例外なし、本文は空文字＋「ご担当者様」で始まる")
        void TC_1_2_02_nullNameJa_noExceptionBodyStartsWithHonorific() {
            ParticipatingCompany noName = ParticipatingCompany.builder()
                .companyId("102")
                .email("contact@sample-solar.co.jp")
                .nameJa(null)
                .build();
 
            assertThatCode(() ->
                notificationService.sendCompanyRejectionNotification(noName, "書類不備")
            ).doesNotThrowAnyException();
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            // nameJa=null のとき "" (空文字) として処理される実装を確認
            assertThat(captor.getValue().getText()).contains("ご担当者様");
        }
 
        @Test
        @DisplayName("TC-1.2-03 [境界値] メールアドレスが null → send() は呼ばれない")
        void TC_1_2_03_nullEmail_doesNotSend() {
            ParticipatingCompany noEmail = ParticipatingCompany.builder()
                .companyId("102")
                .nameJa("テスト会社")
                .email(null)
                .build();
 
            notificationService.sendCompanyRejectionNotification(noEmail, "書類不備");
 
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }
 
        @Test
        @DisplayName("TC-1.2-04 [異常系] rejectionReason が null でも例外は発生しない")
        void TC_1_2_04_nullRejectionReason_noException() {
            assertThatCode(() ->
                notificationService.sendCompanyRejectionNotification(participatingCompany, null)
            ).doesNotThrowAnyException();
 
            verify(mailSender).send(any(SimpleMailMessage.class));
        }
    }
 
    // =========================================================================
    // 【共通】sendMail 基盤挙動（private のため公開メソッド経由で間接検証）
    // =========================================================================
    @Nested
    @DisplayName("【共通】sendMail 基盤挙動")
    class SendMailBehaviorTest {
 
        @Test
        @DisplayName("TC-MAIL-01 [正常系] overrideToAddress 未設定 → 本来の宛先に送信される")
        void TC_MAIL_01_noOverride_sendsToOriginalRecipient() {
            // setUp で overrideToAddress = "" に設定済み
            notificationService.sendReactivationNotification(participatingCompany);
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getTo()).containsExactly("contact@sample-solar.co.jp");
        }
 
        @Test
        @DisplayName("TC-MAIL-02 [正常系] overrideToAddress 設定時 → 本来の宛先に関わらず override 先に送信される")
        void TC_MAIL_02_withOverride_sendsToOverrideRegardlessOfOriginal() {
            ReflectionTestUtils.setField(
                notificationService, "overrideToAddress", "dev-intercept@gxcarbon.local");
 
            notificationService.sendReactivationNotification(participatingCompany);
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getTo()).containsExactly("dev-intercept@gxcarbon.local");
        }
 
        @Test
        @DisplayName("TC-MAIL-03 [境界値] override なし・宛先が空文字 → send() は呼ばれず例外も発生しない")
        void TC_MAIL_03_blankToNoOverride_silentlySkips() {
            ParticipatingCompany blankEmail = ParticipatingCompany.builder()
                .companyId("999")
                .email("")
                .paymentMethod("CREDIT_CARD")
                .build();
 
            assertThatCode(() -> notificationService.sendSuspensionNotification(blankEmail))
                .doesNotThrowAnyException();
 
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }
    }
 
    // =========================================================================
    // 【要件10】getNotifications — 通知一覧取得
    // =========================================================================
    @Nested
    @DisplayName("【要件10】getNotifications")
    class GetNotificationsTest {
 
        @Test
        @DisplayName("TC-10-01 [正常系] 運営ユーザー＋未読のみ → OperatingUser 未読専用リポジトリメソッドが呼ばれる")
        void TC_10_01_operatingUser_unreadOnly_callsUnreadRepo() {
            Page<Notification> page = new PageImpl<>(
                List.of(notification), PageRequest.of(0, 10), 1L);
            when(notificationRepository.findByOperatingUserIdAndReadAndIsActive(
                eq(1), eq(false), any(Pageable.class))).thenReturn(page);
 
            notificationService.getNotifications(1, true, 0, 10, true);
 
            verify(notificationRepository)
                .findByOperatingUserIdAndReadAndIsActive(eq(1), eq(false), any(Pageable.class));
            verify(notificationRepository, never())
                .findByOperatingUserIdAndIsActive(any(), any(Pageable.class));
        }
 
        @Test
        @DisplayName("TC-10-02 [正常系] 運営ユーザー＋全件 → OperatingUser 全件リポジトリメソッドが呼ばれる")
        void TC_10_02_operatingUser_allNotifications_callsAllRepo() {
            Page<Notification> page = new PageImpl<>(List.of(notification));
            when(notificationRepository.findByOperatingUserIdAndIsActive(
                eq(1), any(Pageable.class))).thenReturn(page);
 
            notificationService.getNotifications(1, false, 0, 10, true);
 
            verify(notificationRepository)
                .findByOperatingUserIdAndIsActive(eq(1), any(Pageable.class));
            verify(notificationRepository, never())
                .findByOperatingUserIdAndReadAndIsActive(any(), any(), any(Pageable.class));
        }
 
        @Test
        @DisplayName("TC-10-03 [正常系] 参加企業ユーザー＋未読のみ → ParticipatingUser 未読専用リポジトリメソッドが呼ばれる")
        void TC_10_03_participatingUser_unreadOnly_callsUnreadRepo() {
            Page<Notification> page = new PageImpl<>(List.of(notification));
            when(notificationRepository.findByParticipatingUserIdAndReadAndIsActive(
                eq(1), eq(false), any(Pageable.class))).thenReturn(page);
 
            notificationService.getNotifications(1, true, 0, 10, false);
 
            verify(notificationRepository)
                .findByParticipatingUserIdAndReadAndIsActive(eq(1), eq(false), any(Pageable.class));
            verify(notificationRepository, never())
                .findByParticipatingUserIdAndIsActive(any(), any(Pageable.class));
        }
 
        @Test
        @DisplayName("TC-10-04 [正常系] 参加企業ユーザー＋全件 → ParticipatingUser 全件リポジトリメソッドが呼ばれる")
        void TC_10_04_participatingUser_allNotifications_callsAllRepo() {
            Page<Notification> page = new PageImpl<>(List.of(notification));
            when(notificationRepository.findByParticipatingUserIdAndIsActive(
                eq(1), any(Pageable.class))).thenReturn(page);
 
            notificationService.getNotifications(1, false, 0, 10, false);
 
            verify(notificationRepository)
                .findByParticipatingUserIdAndIsActive(eq(1), any(Pageable.class));
        }
 
        @Test
        @DisplayName("TC-10-05 [正常系] レスポンスのページネーション情報が Page 結果の値と一致する")
        void TC_10_05_paginationInfoMappedCorrectlyFromPage() {
            // totalElements=5 で Page を組み立てる
            Page<Notification> page = new PageImpl<>(
                List.of(notification), PageRequest.of(0, 10), 5L);
            when(notificationRepository.findByParticipatingUserIdAndIsActive(
                any(), any(Pageable.class))).thenReturn(page);
 
            NotificationListResponse response =
                notificationService.getNotifications(1, false, 0, 10, false);
 
            // TODO: PaginationDetailResponse の getter 名は実クラスで確認
            assertThat(response.getPagination().getTotalItems()).isEqualTo(5L);
            assertThat(response.getPagination().getTotalPages()).isEqualTo(1);
        }
 
        @Test
        @DisplayName("TC-10-06 [境界値] 検索結果が0件 → 空リストで返り例外なし")
        void TC_10_06_emptyResult_returnsEmptyListWithoutException() {
            Page<Notification> emptyPage = new PageImpl<>(List.of());
            when(notificationRepository.findByParticipatingUserIdAndIsActive(
                any(), any(Pageable.class))).thenReturn(emptyPage);
 
            NotificationListResponse response =
                notificationService.getNotifications(1, false, 0, 10, false);
 
            assertThat(response.getNotifications()).isEmpty();
        }
 
        @Test
        @DisplayName("TC-10-07 [境界値] unreadOnly が null → NPE なし、false 扱いで全件取得ブランチに入る")
        void TC_10_07_nullUnreadOnly_treatedAsFalseNoNPE() {
            // Boolean.TRUE.equals(null) は false のため、全件取得ブランチに入ることを期待
            Page<Notification> page = new PageImpl<>(List.of());
            when(notificationRepository.findByParticipatingUserIdAndIsActive(
                any(), any(Pageable.class))).thenReturn(page);
 
            assertThatCode(() ->
                notificationService.getNotifications(1, null, 0, 10, false)
            ).doesNotThrowAnyException();
 
            verify(notificationRepository)
                .findByParticipatingUserIdAndIsActive(any(), any(Pageable.class));
        }
 
        @Test
        @DisplayName("TC-10-08 [正常系] participatingUser のみ持つ通知の DTO 変換 → participatingUserId=設定値, operatingUserId=null")
        void TC_10_08_notificationWithParticipatingUser_dtoMappedCorrectly() {
            // fixture の notification は participatingUser(id=10), operatingUser=null
            Page<Notification> page = new PageImpl<>(
                List.of(notification), PageRequest.of(0, 10), 1L);
            when(notificationRepository.findByParticipatingUserIdAndIsActive(
                any(), any(Pageable.class))).thenReturn(page);
 
            NotificationListResponse response =
                notificationService.getNotifications(1, false, 0, 10, false);
 
            NotificationDetailResponse dto = response.getNotifications().get(0);
            assertThat(dto.getParticipatingUserId()).isEqualTo(10);
            assertThat(dto.getOperatingUserId()).isNull();
        }
 
        @Test
        @DisplayName("TC-10-09 [正常系] operatingUser のみ持つ通知の DTO 変換 → operatingUserId=設定値, participatingUserId=null")
        void TC_10_09_notificationWithOperatingUser_dtoMappedCorrectly() {
            Notification opNotification = Notification.builder()
                .title("運営向け通知")
                .message("運営ユーザーへのメッセージ")
                .read(false)
                .participatingUser(null)
                .operatingUser(operatingCompany) // id=1
                .build();
            opNotification.setId(2);
            opNotification.setCreatedAt(LocalDateTime.now());
            opNotification.setUpdatedAt(LocalDateTime.now());
 
            Page<Notification> page = new PageImpl<>(
                List.of(opNotification), PageRequest.of(0, 10), 1L);
            when(notificationRepository.findByOperatingUserIdAndIsActive(
                any(), any(Pageable.class))).thenReturn(page);
 
            NotificationListResponse response =
                notificationService.getNotifications(1, false, 0, 10, true);
 
            NotificationDetailResponse dto = response.getNotifications().get(0);
            assertThat(dto.getOperatingUserId()).isEqualTo(1);
            assertThat(dto.getParticipatingUserId()).isNull();
        }
 
        @Test
        @DisplayName("TC-10-10 [境界値] page が負の値 → Spring Data が IllegalArgumentException をスローする")
        void TC_10_10_negativePage_throwsIllegalArgumentException() {
            // PageRequest.of(-1, size, sort) は Spring Data 標準で例外をスロー
            assertThatThrownBy(() ->
                notificationService.getNotifications(1, false, -1, 10, false)
            ).isInstanceOf(IllegalArgumentException.class);
        }
    }
 
    // =========================================================================
    // 【要件10】createNotification — 単一通知 DB 保存
    // =========================================================================
    @Nested
    @DisplayName("【要件10】createNotification")
    class CreateNotificationTest {
 
        @Test
        @DisplayName("TC-CN-01 [異常系] ParticipatingUserId・OperatingUserId が両方 null → IllegalArgumentException")
        void TC_CN_01_bothNull_throwsIllegalArgumentException() {
            assertThatThrownBy(() ->
                notificationService.createNotification("タイトル", "本文", null, null)
            ).isInstanceOf(IllegalArgumentException.class);
        }
 
        @Test
        @DisplayName("TC-CN-02 [異常系] ParticipatingUserId・OperatingUserId が両方非 null → IllegalArgumentException (XOR 制約)")
        void TC_CN_02_bothNonNull_throwsIllegalArgumentException() {
            assertThatThrownBy(() ->
                notificationService.createNotification("タイトル", "本文", 10, 1)
            ).isInstanceOf(IllegalArgumentException.class);
        }
 
        @Test
        @DisplayName("TC-CN-03 [正常系] ParticipatingUserId のみ指定 → participatingUser 付き・read=false で保存される")
        void TC_CN_03_onlyParticipatingUserId_savesNotificationWithParticipatingUser() {
            when(participatingUserRepository.findById(10))
                .thenReturn(Optional.of(participatingUser));
            when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);
 
            notificationService.createNotification("タイトル", "本文", 10, null);
 
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification saved = captor.getValue();
            assertThat(saved.getParticipatingUser()).isEqualTo(participatingUser);
            assertThat(saved.getOperatingUser()).isNull();
            assertThat(saved.getRead()).isFalse();
            assertThat(saved.getTitle()).isEqualTo("タイトル");
        }
 
        @Test
        @DisplayName("TC-CN-04 [正常系] OperatingUserId のみ指定 → operatingUser 付き・participatingUser=null で保存される")
        void TC_CN_04_onlyOperatingUserId_savesNotificationWithOperatingUser() {
            when(operatingUserRepository.findById(1))
                .thenReturn(Optional.of(operatingCompany));
            when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);
 
            notificationService.createNotification("タイトル", "本文", null, 1);
 
            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            Notification saved = captor.getValue();
            assertThat(saved.getOperatingUser()).isEqualTo(operatingCompany);
            assertThat(saved.getParticipatingUser()).isNull();
        }
 
        @Test
        @DisplayName("TC-CN-05 [異常系] 存在しない ParticipatingUserId → RuntimeException（エラーメッセージに ID 含む）")
        void TC_CN_05_participatingUserNotFound_throwsRuntimeException() {
            when(participatingUserRepository.findById(99)).thenReturn(Optional.empty());
 
            assertThatThrownBy(() ->
                notificationService.createNotification("タイトル", "本文", 99, null)
            ).isInstanceOf(RuntimeException.class)
             .hasMessageContaining("99");
        }
 
        @Test
        @DisplayName("TC-CN-06 [異常系] 存在しない OperatingUserId → RuntimeException（エラーメッセージに ID 含む）")
        void TC_CN_06_operatingUserNotFound_throwsRuntimeException() {
            when(operatingUserRepository.findById(99)).thenReturn(Optional.empty());
 
            assertThatThrownBy(() ->
                notificationService.createNotification("タイトル", "本文", null, 99)
            ).isInstanceOf(RuntimeException.class)
             .hasMessageContaining("99");
        }
    }
 
    // =========================================================================
    // 【要件10】createNotificationForParticipatingCompany — 企業内全ユーザーへの通知
    // =========================================================================
    @Nested
    @DisplayName("【要件10】createNotificationForParticipatingCompany")
    class CreateNotificationForParticipatingCompanyTest {
 
        @Test
        @DisplayName("TC-CNFPC-01 [正常系] 会社に N 人ユーザー → N 回 save() が呼ばれる")
        void TC_CNFPC_01_multipleUsers_savesForEachUser() {
            ParticipatingUser user2 = ParticipatingUser.builder()
                .company(participatingCompany)
                .build();
            user2.setId(11);
 
            when(participatingUserRepository.findByCompany_Id(any()))
                .thenReturn(List.of(participatingUser, user2));
            // anyInt() で id=10, id=11 どちらの findById もカバー
            when(participatingUserRepository.findById(anyInt()))
                .thenReturn(Optional.of(participatingUser));
            when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);
 
            notificationService.createNotificationForParticipatingCompany(
                "タイトル", "本文", 101, null);
 
            verify(notificationRepository, times(2)).save(any(Notification.class));
        }
 
        @Test
        @DisplayName("TC-CNFPC-02 [境界値] 会社にユーザーが0人 → save() は呼ばれない、例外なし")
        void TC_CNFPC_02_noUsers_doesNotSaveAndNoException() {
            when(participatingUserRepository.findByCompany_Id(any()))
                .thenReturn(List.of());
 
            assertThatCode(() ->
                notificationService.createNotificationForParticipatingCompany(
                    "タイトル", "本文", 101, null)
            ).doesNotThrowAnyException();
 
            verify(notificationRepository, never()).save(any());
        }
 
        @Test
        @DisplayName("TC-CNFPC-03 [異常系] ループ中に例外発生 → 例外が伝播し後続ユーザーへの処理が中断される (fail-fast)")
        void TC_CNFPC_03_exceptionInLoop_propagatesAndAbortsRemainingUsers() {
            ParticipatingUser user2 = ParticipatingUser.builder()
                .company(participatingCompany)
                .build();
            user2.setId(11);
 
            when(participatingUserRepository.findByCompany_Id(any()))
                .thenReturn(List.of(participatingUser, user2));
            // 最初の findById で空を返すと createNotification が RuntimeException をスロー
            when(participatingUserRepository.findById(anyInt()))
                .thenReturn(Optional.empty());
 
            assertThatThrownBy(() ->
                notificationService.createNotificationForParticipatingCompany(
                    "タイトル", "本文", 101, null)
            ).isInstanceOf(RuntimeException.class);
 
            // fail-fast: 例外発生後は後続の save が呼ばれない
            verify(notificationRepository, never()).save(any());
        }
    }
 
    // =========================================================================
    // 【要件10】createNotificationForAllOperatingUsers — 全運営ユーザーへの DB 通知
    // =========================================================================
    @Nested
    @DisplayName("【要件10】createNotificationForAllOperatingUsers")
    class CreateNotificationForAllOperatingUsersTest {
 
        @Test
        @DisplayName("TC-CNFAOU-01 [正常系] 運営ユーザーが存在する → ユーザー数分 save() が呼ばれる")
        void TC_CNFAOU_01_withUsers_savesForEach() {
            when(operatingUserRepository.findAll())
                .thenReturn(List.of(operatingCompany));
            when(operatingUserRepository.findById(1))
                .thenReturn(Optional.of(operatingCompany));
            when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);
 
            notificationService.createNotificationForAllOperatingUsers("タイトル", "本文");
 
            verify(notificationRepository, times(1)).save(any(Notification.class));
        }
 
        @Test
        @DisplayName("TC-CNFAOU-02 [境界値] 運営ユーザーが0人 → save() は呼ばれない、例外なし")
        void TC_CNFAOU_02_noUsers_doesNotSaveAndNoException() {
            when(operatingUserRepository.findAll()).thenReturn(List.of());
 
            assertThatCode(() ->
                notificationService.createNotificationForAllOperatingUsers("タイトル", "本文")
            ).doesNotThrowAnyException();
 
            verify(notificationRepository, never()).save(any());
        }
    }
 
    // =========================================================================
    // 【要件1.1】sendEmailToAllOperatingUsers — 全運営ユーザーへのメール配信
    // =========================================================================
    @Nested
    @DisplayName("【要件1.1】sendEmailToAllOperatingUsers")
    class SendEmailToAllOperatingUsersTest {
 
        @Test
        @DisplayName("TC-SEAOU-01 [正常系] 運営ユーザーが存在する → 各ユーザーのメールアドレスに send() が呼ばれる")
        void TC_SEAOU_01_withUsers_sendsToEachUserEmail() {
            when(operatingUserRepository.findAll())
                .thenReturn(List.of(operatingCompany));
 
            notificationService.sendEmailToAllOperatingUsers("件名", "本文");
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getTo()).containsExactly("yamada@gxcarbon.jp");
        }
 
        @Test
        @DisplayName("TC-SEAOU-02 [境界値] 運営ユーザーが0人 → send() は呼ばれない、例外なし")
        void TC_SEAOU_02_noUsers_doesNotSendAndNoException() {
            when(operatingUserRepository.findAll()).thenReturn(List.of());
 
            assertThatCode(() ->
                notificationService.sendEmailToAllOperatingUsers("件名", "本文")
            ).doesNotThrowAnyException();
 
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }
    }
 
    // =========================================================================
    // 【要件1.1】sendEmailToAllOperatorCompanies — 全オペレーター企業へのメール配信
    // =========================================================================
    @Nested
    @DisplayName("【要件1.1】sendEmailToAllOperatorCompanies")
    class SendEmailToAllOperatorCompaniesTest {
 
        @Test
        @DisplayName("TC-SEAOC-01 [正常系] オペレーター企業が存在する → 各社メールアドレスに send() が呼ばれる")
        void TC_SEAOC_01_withCompanies_sendsToEachCompanyEmail() {
            when(operatorCompanyRepository.findAll())
                .thenReturn(List.of(operatorCompany));
 
            notificationService.sendEmailToAllOperatorCompanies("件名", "本文");
 
            ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());
            assertThat(captor.getValue().getTo())
                .containsExactly("partner-operator@gxcarbon.jp");
        }
 
        @Test
        @DisplayName("TC-SEAOC-02 [境界値] オペレーター企業が0件 → send() は呼ばれない、例外なし")
        void TC_SEAOC_02_noCompanies_doesNotSendAndNoException() {
            when(operatorCompanyRepository.findAll()).thenReturn(List.of());
 
            assertThatCode(() ->
                notificationService.sendEmailToAllOperatorCompanies("件名", "本文")
            ).doesNotThrowAnyException();
 
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
        }
    }
}