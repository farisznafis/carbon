package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/** One row of 12.1（１）①特定計量器の場合. */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class SpecifiedInstrumentRow extends MeasureInstrumentRow {

    /**
     * 検定の有効期限.
     *
     * <p>A duration, not a date — the solar example writes 「10年間」 — so no
     * date format is imposed on it.
     */
    @NotBlank
    @Size(max = 50)
    @JsonProperty("validityPeriodOfCert")
    private String validityPeriodOfCert;
}
