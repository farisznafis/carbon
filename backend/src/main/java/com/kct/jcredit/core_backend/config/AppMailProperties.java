package com.kct.jcredit.core_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * メール送信の切り替え設定。
 *
 * app.mail.provider=smtp … ローカル開発 (MailHog) 用。spring.mail.* の SMTP 設定を使う。
 * app.mail.provider=ses  … Amazon SES v2 SDK 経由で送信する。
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class AppMailProperties {

    /** smtp / ses */
    private Provider provider = Provider.SMTP;

    @NestedConfigurationProperty
    private Ses ses = new Ses();

    public enum Provider {
        SMTP,
        SES
    }

    @Getter
    @Setter
    public static class Ses {
        /** 未指定なら spring.cloud.aws.region.static の解決結果を使う。 */
        private String region;

        /** バウンス / 苦情イベントを追跡する SES 構成セット名 (任意)。 */
        private String configurationSet;
    }
}
