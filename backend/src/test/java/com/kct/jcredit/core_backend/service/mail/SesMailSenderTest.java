package com.kct.jcredit.core_backend.service.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kct.jcredit.core_backend.config.AppMailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SesMailSender 単体テスト")
class SesMailSenderTest {

    @Mock private SesV2Client sesV2Client;

    private AppMailProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AppMailProperties();
        properties.setProvider(AppMailProperties.Provider.SES);
        when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
            .thenReturn(SendEmailResponse.builder().messageId("msg-001").build());
    }

    private SesMailSender sender() {
        return new SesMailSender(sesV2Client, properties, "no-reply@example.jp");
    }

    private SimpleMailMessage message() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("no-reply@example.jp");
        message.setTo("user@example.jp");
        message.setSubject("アカウント停止のお知らせ");
        message.setText("お客様のアカウントは現在停止されています。");
        return message;
    }

    private SendEmailRequest capture() {
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesV2Client).sendEmail(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("[正常系] 差出人・宛先・件名・本文が SES リクエストに引き渡される")
    void mapsMessageToSesRequest() {
        sender().send(message());

        SendEmailRequest request = capture();
        assertThat(request.fromEmailAddress()).isEqualTo("no-reply@example.jp");
        assertThat(request.destination().toAddresses()).containsExactly("user@example.jp");
        assertThat(request.content().simple().subject().data()).isEqualTo("アカウント停止のお知らせ");
        assertThat(request.content().simple().body().text().data())
            .isEqualTo("お客様のアカウントは現在停止されています。");
    }

    @Test
    @DisplayName("[正常系] 日本語が化けないよう件名・本文に UTF-8 が指定される")
    void setsUtf8Charset() {
        sender().send(message());

        SendEmailRequest request = capture();
        assertThat(request.content().simple().subject().charset()).isEqualTo("UTF-8");
        assertThat(request.content().simple().body().text().charset()).isEqualTo("UTF-8");
    }

    @Test
    @DisplayName("[設定確認] 構成セット未設定なら configurationSetName は送らない")
    void omitsConfigurationSetWhenNotConfigured() {
        sender().send(message());

        assertThat(capture().configurationSetName()).isNull();
    }

    @Test
    @DisplayName("[設定確認] 構成セット設定時は configurationSetName が付与される")
    void includesConfigurationSetWhenConfigured() {
        properties.getSes().setConfigurationSet("gxcarbon-notifications");
        sender().send(message());

        assertThat(capture().configurationSetName()).isEqualTo("gxcarbon-notifications");
    }

    @Test
    @DisplayName("[正常系] from 未設定時は app.mail.from をフォールバックに使う")
    void fallsBackToDefaultFrom() {
        SimpleMailMessage message = message();
        message.setFrom(null);

        sender().send(message);

        assertThat(capture().fromEmailAddress()).isEqualTo("no-reply@example.jp");
    }

    @Test
    @DisplayName("[異常系] 宛先がない場合は送信せず MailSendException を投げる")
    void rejectsMessageWithoutRecipient() {
        SimpleMailMessage message = message();
        message.setTo(new String[0]);

        assertThatThrownBy(() -> sender().send(message))
            .isInstanceOf(MailSendException.class);
        verify(sesV2Client, never()).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    @DisplayName("[異常系] SES 側のエラーは MailSendException に変換される")
    void wrapsSdkFailureInMailSendException() {
        when(sesV2Client.sendEmail(any(SendEmailRequest.class)))
            .thenThrow(SesV2Exception.builder().message("Email address is not verified").build());

        assertThatThrownBy(() -> sender().send(message()))
            .isInstanceOf(MailSendException.class);
    }
}
