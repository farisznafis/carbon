package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.Hankaku;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 計量器の概要（特定計量器の場合）
 *
 * <p>Generated from db/projectplan.xlsx (table {@code measure_specified_instrument_overview}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MeasureSpecifiedInstrumentRequest extends ProjectPlanFormBase {

    /** モニタリング項目 */
    @NotBlank
    @Size(max = 200)
    @Hankaku
    @JsonProperty("monitoringItem")
    private String monitoringItem;

    /** 計量器の種類 */
    @NotBlank
    @Size(max = 200)
    @JsonProperty("instrumentType")
    private String instrumentType;

    /** モニタリングポイント */
    @NotBlank
    @Size(max = 50)
    @Hankaku
    @JsonProperty("monitoringPoint")
    private String monitoringPoint;

    /** 検定の有効期限 */
    @NotBlank
    @Size(max = 50)
    @JsonProperty("validityPeriodOfCert")
    private String validityPeriodOfCert;

    /** モニタリングポイントの図示 */
    @NotBlank
    @Size(max = 1000)
    @JsonProperty("methodCollDescriptionImg")
    private String methodCollDescriptionImg;
}
