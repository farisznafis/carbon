package com.kct.jcredit.core_backend.dto.projectplan;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * プロジェクト排出量情報（付随的な排出活動）
 *
 * <p>10.3（２）付随的な排出活動.
 *
 * <p>Generated from db/projectplan.xlsx (table {@code incidental_activity_project_emission_information}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class IncidentalActivityProjectEmissionRequest extends ProjectPlanFormBase {

    /**
     * 方法論の評価式, e.g. {@code EMPJ,S,control=ELPJ,control×CEFelectricity,t}.
     *
     * <p>No character-set rule. A formula is arithmetic — the official sheets
     * write them with {@code =}, {@code ×}, {@code +} and {@code -} — and
     * {@code @JapaneseText} allows none of those, so every formula in either
     * workbook was rejected by the page meant to hold it.
     */
    @NotBlank
    @Size(max = 1000)
    @JsonProperty("evaluationFormula")
    private String evaluationFormula;

    /** 方法論の式番号 */
    @NotNull
    @JsonProperty("formulaNumber")
    private Integer formulaNumber;

    /**
     * 記号 / 定義 / 単位 / 想定値 — one row per symbol the 式 above uses.
     *
     * <p>Held a single symbol before, so a formula with more than one could
     * not be entered; every formula in the sheet has at least two.
     */
    @Valid
    @NotEmpty
    @JsonProperty("symbols")
    private List<FormulaSymbolRow> symbols;
}
