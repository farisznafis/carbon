package com.kct.jcredit.core_backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;

import java.time.Duration;

/**
 * 文面抽出が忠実であることを担保するテスト。
 * 期待値は抽出前にコードへ直書きされていた文字列そのもの。
 */
@DisplayName("NotificationMessages 単体テスト")
class NotificationMessagesTest {

    private final NotificationMessages messages = new NotificationMessages();

    @Test
    @DisplayName("[抽出確認] アカウント停止メールが抽出前と同一")
    void accountSuspendedUnchanged() {
        NotificationMessages.Content content = messages.of("account.suspended");

        assertThat(content.subject()).isEqualTo("アカウント停止のお知らせ");
        assertThat(content.body())
            .isEqualTo("お客様のアカウントは現在停止されています。詳細は管理者へお問い合わせください。");
    }

    @Test
    @DisplayName("[抽出確認] 却下メールの複数行本文が抽出前と同一")
    void companyRejectedUnchanged() {
        NotificationMessages.Content content = messages.of("company.rejected", "テスト株式会社", "書類不備");

        assertThat(content.subject()).isEqualTo("申請アカウントが却下されました");
        assertThat(content.body()).isEqualTo("テスト株式会社 ご担当者様\n\n"
            + "このたびはご申請いただきありがとうございます。\n"
            + "申し訳ございませんが、貴社の申請は却下されました。\n\n"
            + "却下理由:\n"
            + "書類不備\n\n"
            + "ご不明点がございましたら、お問い合わせください。");
    }

    @Test
    @DisplayName("[抽出確認] マスターアカウント設定リンクの長文が抽出前と同一")
    void masterSetupLinkUnchanged() {
        NotificationMessages.Content content = messages.of(
            "link.master-setup", "C0001", "https://example.jp/setup/abc", "3日間", "30分");

        assertThat(content.subject()).isEqualTo("【マスターアカウント設定のお願い】");
        assertThat(content.body()).isEqualTo("いつもお世話になっております。\n\n"
            + "ご登録内容が承認されましたのでお知らせいたします。\n"
            + "企業ID: C0001\n\n"
            + "つきましては、下記のリンクよりマスターアカウントのユーザー名とパスワードを設定してください。\n\n"
            + "https://example.jp/setup/abc\n\n"
            + "【リンクに関するご注意】\n"
            + "・このリンクは一度クリックすると無効になります(一回限り有効です)。同じリンクを再度開くことはできませんのでご注意ください。\n"
            + "・このリンクの有効期限は発行から3日間です。"
            + "期限を過ぎた場合は再度リンクの発行が必要となりますので、サポートまでお問い合わせください。\n"
            + "・リンクを開いた後、設定を行うためのセッションの有効期限は30分です。時間内に設定を完了してください。\n\n"
            + "ご不明な点がございましたら、サポートまでお問い合わせください。");
    }

    @Test
    @DisplayName("[抽出確認] 招待メールで拠点行あり/なしの両方が抽出前と同一")
    void userInvitationWithAndWithoutSite() {
        String withSite = messages.of("link.user-invitation",
            "テスト株式会社", messages.text("link.user-invitation.site-line", "東京拠点"),
            "https://example.jp/invite/abc", "3日間", "30分").body();
        String withoutSite = messages.of("link.user-invitation",
            "テスト株式会社", "", "https://example.jp/invite/abc", "3日間", "30分").body();

        assertThat(withSite).contains("テスト株式会社 より、アカウント登録のご案内をお送りしております。\n拠点: 東京拠点\n\n下記のリンクより");
        assertThat(withoutSite).contains("テスト株式会社 より、アカウント登録のご案内をお送りしております。\n\n下記のリンクより");
    }

    @Test
    @DisplayName("[異常系] 数値を文字列で渡すため桁区切りが入らない")
    void numericArgumentsKeepNoGroupingSeparator() {
        String body = messages.of("cancel.created", "1234", "tanaka", "56789").body();

        assertThat(body).isEqualTo("サイトID 1234 の取消申請がユーザー tanaka により登録されました。申請ID=56789");
        assertThat(body).doesNotContain("1,234").doesNotContain("56,789");
    }

    @Test
    @DisplayName("[正常系] 有効期限の単位が日・時間・分で切り替わる")
    void durationUnits() {
        assertThat(messages.duration(Duration.ofDays(3))).isEqualTo("3日間");
        assertThat(messages.duration(Duration.ofHours(2))).isEqualTo("2時間");
        assertThat(messages.duration(Duration.ofMinutes(30))).isEqualTo("30分");
    }

    @Test
    @DisplayName("[異常系] 未定義キーは例外で早期に検出される")
    void missingKeyFailsFast() {
        assertThatThrownBy(() -> messages.text("does.not.exist"))
            .isInstanceOf(NoSuchMessageException.class);
    }
}
