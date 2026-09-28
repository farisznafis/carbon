package com.kct.jcredit.core_backend.dto.projectplan;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/**
 * 計量器の概要（特定計量器の場合） — 12.1（１）①.
 *
 * <p>Filed when 11（１）uses 分類Ｂ（計量器）; the sheet's 注) says so.
 *
 * <p>Generated from db/projectplan.xlsx (table
 * {@code measure_specified_instrument_overview}), which had no way to express
 * a repeating table — so the page held one 計量器 where the official example
 * lists two.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MeasureSpecifiedInstrumentRequest extends ProjectPlanFormBase {

    /** 計量器の表。1 行 = 1 計量器。 */
    @Valid
    @NotEmpty
    @JsonProperty("instruments")
    private List<SpecifiedInstrumentRow> instruments;

    /**
     * 12.1（２）モニタリングポイントの図示.
     *
     * <p>One diagram for the whole of 12.1 — 「必ずしも個別項目ごとに図を作成
     * する必要はない」 — which is why it sits on the page rather than in a row.
     */
    @NotBlank
    @Size(max = 1000)
    @JsonProperty("monitoringPointDiagram")
    private String monitoringPointDiagram;
}
