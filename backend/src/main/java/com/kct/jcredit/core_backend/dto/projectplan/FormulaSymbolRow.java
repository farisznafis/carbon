package com.kct.jcredit.core_backend.dto.projectplan;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * One symbol of an evaluation formula in 10.排出削減量の算定方法.
 *
 * <p>Every 式 in section 10 is followed by the same four-column table —
 * 記号 / 定義 / 単位 / 想定値 — one row per symbol the formula uses. It appears
 * under 10.3(1), 10.3(2), 10.4(2), 10.5(1) and 10.5(2), so one row type serves
 * all five.
 *
 * <p>Each of those pages used to hold a single {@code symbol} /
 * {@code definition} / {@code unit} / {@code expectedValue}, which meant a
 * formula with more than one symbol could not be entered — and every formula
 * has at least two. The solar example's
 * {@code EMPJ,S,control = ELPJ,control × CEFelectricity,t} needs three.
 *
 * <p>単位 is the one column left optional. The sheet fills it on every row it
 * pre-fills, but requiring it would block a page over a dimensionless symbol,
 * and that failure mode — a required field the operator has nothing to put in —
 * has already cost this form several unsaveable pages.
 */
@Data
@NoArgsConstructor
@DynamoDbBean
public class FormulaSymbolRow implements ProjectPlanFormRow {

    /** 記号, e.g. {@code EMPJ,S,control}. */
    @NotBlank
    @Size(max = 50)
    @JsonProperty("symbol")
    private String symbol;

    /** 定義 */
    @NotBlank
    @Size(max = 500)
    @JsonProperty("definition")
    private String definition;

    /** 単位, e.g. {@code kg-CO2/kWh}. */
    @Size(max = 50)
    @JsonProperty("unit")
    private String unit;

    /** 想定値 */
    @NotNull
    @JsonProperty("expectedValue")
    private BigDecimal expectedValue;
}
