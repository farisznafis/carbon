package com.kct.jcredit.core_backend.dto.projectplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kct.jcredit.core_backend.validation.JapaneseText;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/** One row of 12.1（１）②特定計量器以外の計量器の場合. */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@DynamoDbBean
public class OtherInstrumentRow extends MeasureInstrumentRow {

    /**
     * 計量器の校正方法の説明.
     *
     * <p>Optional, and that is not a guess: the solar example fills it on its
     * first row and leaves it blank on the second. Required, as it was, the
     * page would refuse a table the official example itself contains.
     */
    @Size(max = 1000)
    @JapaneseText(allowAlphanumeric = true)
    @JsonProperty("instrumentCalibrationMethodDescription")
    private String instrumentCalibrationMethodDescription;
}
