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
 * 計量器の概要（特定計量器以外の計量器の場合） — 12.1（１）②.
 *
 * <p>Generated from db/projectplan.xlsx (table
 * {@code measure_other_instruments_overview}); the page held one 計量器 where
 * the official example lists two.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class MeasureOtherInstrumentsRequest extends ProjectPlanFormBase {

    /** 計量器の表。1 行 = 1 計量器。 */
    @Valid
    @NotEmpty
    @JsonProperty("instruments")
    private List<OtherInstrumentRow> instruments;

    /** 12.1（２）モニタリングポイントの図示. */
    @NotBlank
    @Size(max = 1000)
    @JsonProperty("monitoringPointDiagram")
    private String monitoringPointDiagram;
}
