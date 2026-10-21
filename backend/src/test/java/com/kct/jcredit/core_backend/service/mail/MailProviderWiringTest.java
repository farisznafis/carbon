package com.kct.jcredit.core_backend.service.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.kct.jcredit.core_backend.config.AppMailProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * app.mail.provider の切り替えで、SMTP 自動構成と SES がどう入れ替わるかを固定する。
 *
 * spring.mail.* は provider=ses のとき「設定されていても無視される」。
 * MailSenderAutoConfiguration が @ConditionalOnMissingBean(MailSender.class) のため、
 * SesMailSender が登録された時点で SMTP 側が丸ごと降りるので、
 * application.properties の spring.mail.* をコメントアウトする必要はない。
 */
@DisplayName("メール送信経路の切り替え")
class MailProviderWiringTest {

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
                .withBean(SesV2Client.class, () -> mock(SesV2Client.class))
                .withBean(AppMailProperties.class)
                .withUserConfiguration(SesMailSender.class)
                // MailHog 向けの設定は入れっぱなしにしておく。これが無視されることを確かめたい。
                .withPropertyValues(
                        "spring.mail.host=172.30.41.122",
                        "spring.mail.port=1025",
                        "app.mail.from=no-reply@example.jp");
    }

    @Test
    @DisplayName("[ses] spring.mail.* が設定されていても SES が使われ、SMTP の Bean は作られない")
    void sesProviderWinsAndSmtpBacksOff() {
        runner().withPropertyValues("app.mail.provider=ses").run(context -> {
            assertThat(context).hasSingleBean(MailSender.class);
            assertThat(context.getBean(MailSender.class)).isInstanceOf(SesMailSender.class);
            assertThat(context).doesNotHaveBean(JavaMailSenderImpl.class);
        });
    }

    @Test
    @DisplayName("[smtp] 既定では SES は登録されず、従来どおり SMTP が使われる")
    void smtpProviderKeepsExistingBehaviour() {
        runner().withPropertyValues("app.mail.provider=smtp").run(context -> {
            assertThat(context).hasSingleBean(MailSender.class);
            assertThat(context.getBean(MailSender.class)).isInstanceOf(JavaMailSenderImpl.class);
            assertThat(context).doesNotHaveBean(SesMailSender.class);
            assertThat(context.getBean(JavaMailSenderImpl.class).getHost()).isEqualTo("172.30.41.122");
        });
    }

    @Test
    @DisplayName("[未設定] app.mail.provider が無い場合も SMTP のまま (既存環境を壊さない)")
    void missingProviderDefaultsToSmtp() {
        runner().run(context -> {
            assertThat(context.getBean(MailSender.class)).isInstanceOf(JavaMailSenderImpl.class);
            assertThat(context).doesNotHaveBean(SesMailSender.class);
        });
    }
}
