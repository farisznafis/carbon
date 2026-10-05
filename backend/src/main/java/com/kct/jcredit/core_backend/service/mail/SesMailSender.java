package com.kct.jcredit.core_backend.service.mail;

import com.kct.jcredit.core_backend.config.AppMailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSender;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Amazon SES v2 SDK を使った {@link MailSender} 実装。
 *
 * app.mail.provider=ses のときだけ登録され、@Primary により Spring Boot が自動構成する
 * SMTP の JavaMailSenderImpl より優先される。呼び出し側 (NotificationServiceImpl) は
 * MailSender インターフェースに依存しているため、SMTP / SES の切り替えでコード変更は不要。
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "ses")
public class SesMailSender implements MailSender {

    /** SES はマルチバイト本文に charset 指定が必須。日本語の件名・本文が化けないようにする。 */
    private static final String CHARSET = "UTF-8";

    private final SesV2Client sesV2Client;
    private final String configurationSet;
    private final String defaultFrom;

    public SesMailSender(SesV2Client sesV2Client,
                         AppMailProperties mailProperties,
                         @Value("${app.mail.from:}") String defaultFrom) {
        this.sesV2Client = sesV2Client;
        this.configurationSet = blankToNull(mailProperties.getSes().getConfigurationSet());
        this.defaultFrom = defaultFrom;
    }

    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailSendException {
        send(new SimpleMailMessage[] { simpleMessage });
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailSendException {
        Map<Object, Exception> failed = new LinkedHashMap<>();
        for (SimpleMailMessage message : simpleMessages) {
            try {
                sendOne(message);
            } catch (SdkException | IllegalArgumentException ex) {
                log.error("[SES] Failed to send mail. subject={}, to={}",
                        message.getSubject(), Arrays.toString(message.getTo()), ex);
                failed.put(message, ex);
            }
        }
        if (!failed.isEmpty()) {
            throw new MailSendException("Amazon SES rejected " + failed.size() + " message(s)", null, failed);
        }
    }

    private void sendOne(SimpleMailMessage message) {
        String from = message.getFrom() != null && !message.getFrom().isBlank()
                ? message.getFrom()
                : defaultFrom;
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("No 'from' address; set app.mail.from to a SES-verified identity");
        }
        if (message.getTo() == null || message.getTo().length == 0) {
            throw new IllegalArgumentException("No recipient ('to') on the message");
        }

        Destination.Builder destination = Destination.builder().toAddresses(message.getTo());
        if (message.getCc() != null && message.getCc().length > 0) {
            destination.ccAddresses(message.getCc());
        }
        if (message.getBcc() != null && message.getBcc().length > 0) {
            destination.bccAddresses(message.getBcc());
        }

        Message.Builder mail = Message.builder()
                .subject(content(message.getSubject()))
                .body(Body.builder().text(content(message.getText())).build());

        SendEmailRequest.Builder request = SendEmailRequest.builder()
                .fromEmailAddress(from)
                .destination(destination.build())
                .content(EmailContent.builder().simple(mail.build()).build());

        if (message.getReplyTo() != null && !message.getReplyTo().isBlank()) {
            request.replyToAddresses(List.of(message.getReplyTo()));
        }
        if (configurationSet != null) {
            request.configurationSetName(configurationSet);
        }

        SendEmailResponse response = sesV2Client.sendEmail(request.build());
        log.info("[SES] Mail sent. messageId={}, subject={}", response.messageId(), message.getSubject());
    }

    private static Content content(String data) {
        return Content.builder().data(data == null ? "" : data).charset(CHARSET).build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
