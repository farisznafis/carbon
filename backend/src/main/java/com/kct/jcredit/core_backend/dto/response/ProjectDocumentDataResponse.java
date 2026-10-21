package com.kct.jcredit.core_backend.dto.response;

import java.util.List;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.enums.ProjectFileType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * What the system holds for one kind of project document, laid out to be read
 * and copied into the official Excel 様式.
 *
 * <p>The shape is the same for every document type: sections, each with
 * labelled values and optionally one table. A client renders it without
 * knowing the document, so a new document type or a new value needs no client
 * change. Every value is a string ready to paste; {@code null} means the
 * system has nothing for it.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ProjectDocumentDataResponse extends BaseResponse {

    private String projectId;

    private ProjectFileType fileType;

    /** Empty when the system keeps no data for this document type. */
    private List<Section> sections;

    /**
     * @param key   stable identifier, unique within the response
     * @param items labelled single values; may be empty
     * @param table rows of values under this section; {@code null} when it has none
     */
    public record Section(String key, String title, List<Item> items, Table table) {
    }

    /**
     * @param key   stable identifier, unique within its section
     * @param value {@code null} when the system has no value
     * @param unit  e.g. {@code t-CO2}, {@code 件}; {@code null} when the value has none
     */
    public record Item(String key, String label, String value, String unit, ValueType type) {
    }

    /** @param rows one list per row, in column order; a cell is {@code null} when empty */
    public record Table(List<Column> columns, List<List<String>> rows) {
    }

    public record Column(String key, String label, String unit, ValueType type) {
    }

    /**
     * How a value string is written: {@code NUMBER} is a plain decimal without
     * separators, {@code DATE} is {@code yyyy-MM-dd}.
     */
    public enum ValueType {
        TEXT,
        NUMBER,
        DATE
    }
}
