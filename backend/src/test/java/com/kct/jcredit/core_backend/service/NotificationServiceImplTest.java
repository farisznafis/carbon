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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSender;
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
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationServiceImpl 単体テスト")
class NotificationServiceImplTest {
 
    // =========================================================
    // Mocks — 外部依存をすべてモック化
    // =========================================================
    @Mock private MailSender mailSender;
    @Mock private NotificationRepository notificationRepository;
    @Mock private OperatingUserRepository operatingUserRepository;
    @Mock private OperatorCompanyRepository operatorCompanyRepository;
    @Mock private ParticipatingUserRepository participatingUserRepository;

    // 実バンドルを使う。文面抽出が忠実かを既存アサーションが検証する。
    @Spy private NotificationMessages messages = new NotificationMessages();
 
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
    @Test
    @DisplayName("UT-NS-01 [正常系] 有効メールアドレスへ件名・本文・差出人が正しいメールを送信する")
    void UT_NS_01_validEmail_sendsCorrectEmail() {
        notificationService.sendSuspensionNotification(participatingCompany);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("contact@sample-solar.co.jp");
        assertThat(sent.getFrom()).isEqualTo("no-reply@gxcarbon.local");
        assertThat(sent.getSubject()).contains("停止");
        assertThat(sent.getText())
            .contains("停止")
            .doesNotContain("支払い", "CREDIT_CARD");
    }

    @Test
    @DisplayName("UT-NS-02 [境界値] メールアドレスが null → send() は呼ばれない")
    void UT_NS_02_nullEmail_doesNotSend() {
        ParticipatingCompany noEmail = ParticipatingCompany.builder()
            .companyId("102")
            .email(null)
            .build();

        notificationService.sendSuspensionNotification(noEmail);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("UT-NS-03 [正常系] 停止通知本文に支払い情報を含めない")
    void UT_NS_03_suspensionBodyDoesNotContainPaymentInformation() {
        notificationService.sendSuspensionNotification(participatingCompany);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).doesNotContain("支払い", "payment", "CREDIT_CARD");
    }

    @Test
    @Tag("requirement-gap")
    @Disabled("UT-NS-04: 要件3 はメール＋ダッシュボード両チャンネルの通知を要求するが、" +
                "現実装はメール送信のみ。アプリ内通知（DB保存）が未実装。対応チケット: JIRA-XXX")
    @DisplayName("UT-NS-04 [要件適合性] アカウント停止時にダッシュボード/アプリ内通知も作成される")
    void UT_NS_04_suspension_shouldAlsoCreateInAppNotification() {
        notificationService.sendSuspensionNotification(participatingCompany);

        // 要件3: メールに加え notificationRepository.save() も呼ばれる必要がある
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    @DisplayName("UT-NS-05 [設定確認] overrideToAddress 設定時は本来の宛先を無視して override 先に送信される")
    void UT_NS_05_overrideToAddress_sendsToOverride() {
        ReflectionTestUtils.setField(notificationService, "overrideToAddress", "override@staging.local");

        notificationService.sendSuspensionNotification(participatingCompany);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("override@staging.local");
    }
// =========================================================================
// 【要件4】sendReactivationNotification — アカウント再開通知
// =========================================================================
    @Test
    @DisplayName("UT-NS-06 [正常系] 有効メールアドレスへ件名・本文（企業ID含む）が正しいメールを送信する")
    void UT_NS_06_validEmail_sendsCorrectEmail() {
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
    @DisplayName("UT-NS-07 [境界値] メールアドレスが null → send() は呼ばれない")
    void UT_NS_07_nullEmail_doesNotSend() {
        ParticipatingCompany noEmail = ParticipatingCompany.builder()
            .companyId("102")
            .email(null)
            .build();

        notificationService.sendReactivationNotification(noEmail);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

// =========================================================================
// 【要件1.3】sendMasterAccountCredentials — アカウント発行通知
// =========================================================================

    @Test
    @DisplayName("UT-NS-11 [正常系] 企業ID・username・password を含むメールが正しい宛先に送信される")
    void UT_NS_11_validInput_sendsEmailWithAllCredentials() {
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
    @DisplayName("UT-NS-12 [境界値] メールアドレスが null → send() は呼ばれない")
    void UT_NS_12_nullEmail_doesNotSend() {
        ParticipatingCompany noEmail = ParticipatingCompany.builder()
            .companyId("102")
            .email(null)
            .build();

        notificationService.sendMasterAccountCredentials(noEmail, "user", "pass");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("UT-NS-13 [異常系] username・password が null でも例外は発生しない")
    void UT_NS_13_nullCredentials_noException() {
        assertThatCode(() ->
            notificationService.sendMasterAccountCredentials(participatingCompany, null, null)
        ).doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
// =========================================================================
// 【要件1.2】sendCompanyRejectionNotification — 申請却下通知
// =========================================================================
    @Test
    @DisplayName("UT-NS-14 [正常系] nameJa 設定済み → 会社名・却下理由を含む本文が送信される")
    void UT_NS_14_withNameJa_sendsEmailWithCompanyNameAndReason() {
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
    @DisplayName("UT-NS-15 [境界値] nameJa が null → 例外なし、本文は空文字＋「ご担当者様」で始まる")
    void UT_NS_15_nullNameJa_noExceptionBodyStartsWithHonorific() {
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
    @DisplayName("UT-NS-16 [境界値] メールアドレスが null → send() は呼ばれない")
    void UT_NS_16_nullEmail_doesNotSend() {
        ParticipatingCompany noEmail = ParticipatingCompany.builder()
            .companyId("102")
            .nameJa("テスト会社")
            .email(null)
            .build();

        notificationService.sendCompanyRejectionNotification(noEmail, "書類不備");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("UT-NS-17 [異常系] rejectionReason が null でも例外は発生しない")
    void UT_NS_17_nullRejectionReason_noException() {
        assertThatCode(() ->
            notificationService.sendCompanyRejectionNotification(participatingCompany, null)
        ).doesNotThrowAnyException();

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
// =========================================================================
// 【共通】sendMail 基盤挙動（private のため公開メソッド経由で間接検証）
// =========================================================================
    @Test
    @DisplayName("UT-NS-18 [正常系] overrideToAddress 未設定 → 本来の宛先に送信される")
    void UT_NS_18_noOverride_sendsToOriginalRecipient() {
        // setUp で overrideToAddress = "" に設定済み
        notificationService.sendReactivationNotification(participatingCompany);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("contact@sample-solar.co.jp");
    }

    @Test
    @DisplayName("UT-NS-19 [正常系] overrideToAddress 設定時 → 本来の宛先に関わらず override 先に送信される")
    void UT_NS_19_withOverride_sendsToOverrideRegardlessOfOriginal() {
        ReflectionTestUtils.setField(
            notificationService, "overrideToAddress", "dev-intercept@gxcarbon.local");

        notificationService.sendReactivationNotification(participatingCompany);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("dev-intercept@gxcarbon.local");
    }

    @Test
    @DisplayName("UT-NS-20 [境界値] override なし・宛先が空文字 → send() は呼ばれず例外も発生しない")
    void UT_NS_20_blankToNoOverride_silentlySkips() {
        ParticipatingCompany blankEmail = ParticipatingCompany.builder()
            .companyId("999")
            .email("")
            .build();

        assertThatCode(() -> notificationService.sendSuspensionNotification(blankEmail))
            .doesNotThrowAnyException();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

// =========================================================================
// 【要件10】getNotifications — 通知一覧取得
// =========================================================================

    @Test
    @DisplayName("UT-NS-21 [正常系] 運営ユーザー＋未読のみ → OperatingUser 未読専用リポジトリメソッドが呼ばれる")
    void UT_NS_21_operatingUser_unreadOnly_callsUnreadRepo() {
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
    @DisplayName("UT-NS-22 [正常系] 運営ユーザー＋全件 → OperatingUser 全件リポジトリメソッドが呼ばれる")
    void UT_NS_22_operatingUser_allNotifications_callsAllRepo() {
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
    @DisplayName("UT-NS-23 [正常系] 参加企業ユーザー＋未読のみ → ParticipatingUser 未読専用リポジトリメソッドが呼ばれる")
    void UT_NS_23_participatingUser_unreadOnly_callsUnreadRepo() {
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
    @DisplayName("UT-NS-24 [正常系] 参加企業ユーザー＋全件 → ParticipatingUser 全件リポジトリメソッドが呼ばれる")
    void UT_NS_24_participatingUser_allNotifications_callsAllRepo() {
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByParticipatingUserIdAndIsActive(
            eq(1), any(Pageable.class))).thenReturn(page);

        notificationService.getNotifications(1, false, 0, 10, false);

        verify(notificationRepository)
            .findByParticipatingUserIdAndIsActive(eq(1), any(Pageable.class));
    }

    @Test
    @DisplayName("UT-NS-25 [正常系] レスポンスのページネーション情報が Page 結果の値と一致する")
    void UT_NS_25_paginationInfoMappedCorrectlyFromPage() {

        // Arrange
        List<Notification> notifications = List.of(
            notification,
            notification,
            notification,
            notification,
            notification
        );

        Page<Notification> page = new PageImpl<>(
            notifications,
            PageRequest.of(0, 10),
            5L
        );

        when(notificationRepository.findByParticipatingUserIdAndIsActive(
            any(),
            any(Pageable.class)))
            .thenReturn(page);

        // Act
        NotificationListResponse response =
            notificationService.getNotifications(
                1,
                false,
                0,
                10,
                false
            );

        // Assert
        assertThat(response.getPagination()).isNotNull();
        assertThat(response.getPagination().getTotalItems()).isEqualTo(5L);
        assertThat(response.getPagination().getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("UT-NS-26 [境界値] 検索結果が0件 → 空リストで返り例外なし")
    void UT_NS_26_emptyResult_returnsEmptyListWithoutException() {
        Page<Notification> emptyPage = new PageImpl<>(List.of());
        when(notificationRepository.findByParticipatingUserIdAndIsActive(
            any(), any(Pageable.class))).thenReturn(emptyPage);

        NotificationListResponse response =
            notificationService.getNotifications(1, false, 0, 10, false);

        assertThat(response.getNotifications()).isEmpty();
    }

    @Test
    @DisplayName("UT-NS-27 [境界値] unreadOnly が null → NPE なし、false 扱いで全件取得ブランチに入る")
    void UT_NS_27_nullUnreadOnly_treatedAsFalseNoNPE() {
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
    @DisplayName("UT-NS-28 [正常系] participatingUser のみ持つ通知の DTO 変換 → participatingUserId=設定値, operatingUserId=null")
    void UT_NS_28_notificationWithParticipatingUser_dtoMappedCorrectly() {
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
    @DisplayName("UT-NS-29 [正常系] operatingUser のみ持つ通知の DTO 変換 → operatingUserId=設定値, participatingUserId=null")
    void UT_NS_29_notificationWithOperatingUser_dtoMappedCorrectly() {
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
    @DisplayName("UT-NS-30 [境界値] page が負の値 → Spring Data が IllegalArgumentException をスローする")
    void UT_NS_30_negativePage_throwsIllegalArgumentException() {
        // PageRequest.of(-1, size, sort) は Spring Data 標準で例外をスロー
        assertThatThrownBy(() ->
            notificationService.getNotifications(1, false, -1, 10, false)
        ).isInstanceOf(IllegalArgumentException.class);
    }

// =========================================================================
// 【要件10】createNotification — 単一通知 DB 保存
// =========================================================================

    @Test
    @DisplayName("UT-NS-31 [異常系] ParticipatingUserId・OperatingUserId が両方 null → IllegalArgumentException")
    void UT_NS_31_bothNull_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
            notificationService.createNotification("タイトル", "本文", null, null)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT-NS-32 [異常系] ParticipatingUserId・OperatingUserId が両方非 null → IllegalArgumentException (XOR 制約)")
    void UT_NS_32_bothNonNull_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
            notificationService.createNotification("タイトル", "本文", 10, 1)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UT-NS-33 [正常系] ParticipatingUserId のみ指定 → participatingUser 付き・read=false で保存される")
    void UT_NS_33_onlyParticipatingUserId_savesNotificationWithParticipatingUser() {
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
    @DisplayName("UT-NS-34 [正常系] OperatingUserId のみ指定 → operatingUser 付き・participatingUser=null で保存される")
    void UT_NS_34_onlyOperatingUserId_savesNotificationWithOperatingUser() {
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
    @DisplayName("UT-NS-35 [異常系] 存在しない ParticipatingUserId → RuntimeException（エラーメッセージに ID 含む）")
    void UT_NS_35_participatingUserNotFound_throwsRuntimeException() {
        when(participatingUserRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            notificationService.createNotification("タイトル", "本文", 99, null)
        ).isInstanceOf(RuntimeException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("UT-NS-36 [異常系] 存在しない OperatingUserId → RuntimeException（エラーメッセージに ID 含む）")
    void UT_NS_36_operatingUserNotFound_throwsRuntimeException() {
        when(operatingUserRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            notificationService.createNotification("タイトル", "本文", null, 99)
        ).isInstanceOf(RuntimeException.class)
            .hasMessageContaining("99");
    }

// =========================================================================
// 【要件10】createNotificationForParticipatingCompany — 企業内全ユーザーへの通知
// =========================================================================

    @Test
    @DisplayName("UT-NS-37 [正常系] 会社に N 人ユーザー → N 回 save() が呼ばれる")
    void UT_NS_37_multipleUsers_savesForEachUser() {
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
    @DisplayName("UT-NS-38 [境界値] 会社にユーザーが0人 → save() は呼ばれない、例外なし")
    void UT_NS_38_noUsers_doesNotSaveAndNoException() {
        when(participatingUserRepository.findByCompany_Id(any()))
            .thenReturn(List.of());

        assertThatCode(() ->
            notificationService.createNotificationForParticipatingCompany(
                "タイトル", "本文", 101, null)
        ).doesNotThrowAnyException();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-NS-39 [異常系] ループ中に例外発生 → 例外が伝播し後続ユーザーへの処理が中断される (fail-fast)")
    void UT_NS_39_exceptionInLoop_propagatesAndAbortsRemainingUsers() {
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

// =========================================================================
// 【要件10】createNotificationForAllOperatingUsers — 全運営ユーザーへの DB 通知
// =========================================================================
    @Test
    @DisplayName("UT-NS-40 [正常系] 運営ユーザーが存在する → ユーザー数分 save() が呼ばれる")
    void UT_NS_40_withUsers_savesForEach() {
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
    @DisplayName("UT-NS-41 [境界値] 運営ユーザーが0人 → save() は呼ばれない、例外なし")
    void UT_NS_41_noUsers_doesNotSaveAndNoException() {
        when(operatingUserRepository.findAll()).thenReturn(List.of());

        assertThatCode(() ->
            notificationService.createNotificationForAllOperatingUsers("タイトル", "本文")
        ).doesNotThrowAnyException();

        verify(notificationRepository, never()).save(any());
    }

// =========================================================================
// 【要件1.1】sendEmailToAllOperatingUsers — 全運営ユーザーへのメール配信
// =========================================================================

    @Test
    @DisplayName("UT-NS-42 [正常系] 運営ユーザーが存在する → 各ユーザーのメールアドレスに send() が呼ばれる")
    void UT_NS_42_withUsers_sendsToEachUserEmail() {
        when(operatingUserRepository.findAll())
            .thenReturn(List.of(operatingCompany));

        notificationService.sendEmailToAllOperatingUsers("件名", "本文");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("yamada@gxcarbon.jp");
    }

    @Test
    @DisplayName("UT-NS-43 [境界値] 運営ユーザーが0人 → send() は呼ばれない、例外なし")
    void UT_NS_43_noUsers_doesNotSendAndNoException() {
        when(operatingUserRepository.findAll()).thenReturn(List.of());

        assertThatCode(() ->
            notificationService.sendEmailToAllOperatingUsers("件名", "本文")
        ).doesNotThrowAnyException();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
// =========================================================================
// 【要件1.1】sendEmailToAllOperatorCompanies — 全オペレーター企業へのメール配信
// =========================================================================
    @Test
    @DisplayName("UT-NS-44 [正常系] オペレーター企業が存在する → 各社メールアドレスに send() が呼ばれる")
    void UT_NS_44_withCompanies_sendsToEachCompanyEmail() {
        when(operatorCompanyRepository.findAll())
            .thenReturn(List.of(operatorCompany));

        notificationService.sendEmailToAllOperatorCompanies("件名", "本文");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getTo())
            .containsExactly("partner-operator@gxcarbon.jp");
    }

    @Test
    @DisplayName("UT-NS-45 [境界値] オペレーター企業が0件 → send() は呼ばれない、例外なし")
    void UT_NS_45_noCompanies_doesNotSendAndNoException() {
        when(operatorCompanyRepository.findAll()).thenReturn(List.of());

        assertThatCode(() ->
            notificationService.sendEmailToAllOperatorCompanies("件名", "本文")
        ).doesNotThrowAnyException();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
