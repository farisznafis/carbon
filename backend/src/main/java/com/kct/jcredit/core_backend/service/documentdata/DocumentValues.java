package com.kct.jcredit.core_backend.service.documentdata;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Column;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Item;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Section;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Table;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.ValueType;

/**
 * Writes values the way the document data response carries them: as strings
 * ready to paste into Excel, {@code null} when there is nothing.
 */
final class DocumentValues {

    /** Dates on the official forms are Japanese calendar days. */
    private static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");

    private DocumentValues() {
    }

    static Section section(String key, String title, List<Item> items) {
        return new Section(key, title, items, null);
    }

    static Section section(String key, String title, List<Item> items, Table table) {
        return new Section(key, title, items, table);
    }

    static Item text(String key, String label, String value) {
        return new Item(key, label, blankToNull(value), null, ValueType.TEXT);
    }

    static Item number(String key, String label, Number value, String unit) {
        return new Item(key, label, plain(value), unit, ValueType.NUMBER);
    }

    static Item date(String key, String label, LocalDate value) {
        return new Item(key, label, value == null ? null : value.toString(), null, ValueType.DATE);
    }

    static Column textColumn(String key, String label) {
        return new Column(key, label, null, ValueType.TEXT);
    }

    /** The Japanese calendar day an instant falls on. */
    static LocalDate japanDate(Instant instant) {
        return instant == null ? null : instant.atZone(JAPAN).toLocalDate();
    }

    /** A number without separators or exponent, so Excel reads it as a number. */
    static String plain(Number value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        BigDecimal decimal = value instanceof BigDecimal big ? big : BigDecimal.valueOf(value.doubleValue());
        if (decimal.signum() == 0) {
            return "0";
        }
        return decimal.stripTrailingZeros().toPlainString();
    }

    /** The non-blank parts in order, joined; {@code null} when none is left. */
    static String join(String separator, String... parts) {
        String joined = Stream.of(parts)
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(separator));
        return joined.isEmpty() ? null : joined;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
