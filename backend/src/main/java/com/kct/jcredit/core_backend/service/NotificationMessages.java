package com.kct.jcredit.core_backend.service;

import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * 通知文面 (メールの件名・本文、アプリ内通知のタイトル・本文) の唯一の置き場所である
 * mail/messages.properties を解決する。
 *
 * 文面をコードから追い出すのが目的なので、呼び出し側は文字列を組み立てず
 * キーと引数だけを渡す。同じ出来事の文面がメールとアプリ内通知で二重管理され、
 * 少しずつ食い違っていた問題もこれで一箇所に集まる。
 *
 * 独自の MessageSource を持つのは、Bean Validation などが使う Boot 自動構成の
 * messageSource に相乗りして互いに影響し合うのを避けるため。
 */
@Component
public class NotificationMessages {

    /** 文面は日本語のみ。将来 messages_en.properties を足せば言語切り替えできる。 */
    private static final Locale LOCALE = Locale.JAPAN;

    private final ResourceBundleMessageSource messageSource;

    public NotificationMessages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("mail/messages");
        // properties の既定は ISO-8859-1。指定しないと日本語が文字化けする。
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        this.messageSource = source;
    }

    /** 件名と本文の組。アプリ内通知では subject をタイトルとして使う。 */
    public record Content(String subject, String body) {}

    /**
     * {@code <key>.subject} と {@code <key>.body} をまとめて解決する。
     * 引数は両方に同じ順序で渡されるため、件名側で使わない引数があっても問題ない。
     */
    public Content of(String key, Object... args) {
        return new Content(text(key + ".subject", args), text(key + ".body", args));
    }

    /** 単独のキーを解決する。キーが無い場合は NoSuchMessageException で早期に失敗する。 */
    public String text(String key, Object... args) {
        return messageSource.getMessage(key, args, LOCALE);
    }

    /**
     * 有効期限の表記を「3日間」「2時間」「30分」のように整える。
     * 単位は文面の一部なので properties 側に持たせている。
     */
    public String duration(Duration duration) {
        long days = duration.toDays();
        if (days >= 1) {
            return text("format.duration.days", String.valueOf(days));
        }
        long hours = duration.toHours();
        if (hours >= 1) {
            return text("format.duration.hours", String.valueOf(hours));
        }
        return text("format.duration.minutes", String.valueOf(duration.toMinutes()));
    }
}
