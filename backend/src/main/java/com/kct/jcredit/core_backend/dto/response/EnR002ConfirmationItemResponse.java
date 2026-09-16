package com.kct.jcredit.core_backend.dto.response;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter 
@Builder
public class EnR002ConfirmationItemResponse extends BaseResponse {
    private Boolean isAgreeDataUse;
    private Boolean isAgreeExtraData;
    private Boolean isAgreeTransfer;
    private Boolean isNotRegistered;
    private Boolean isKeidanrenParticipation;
    private Boolean isEmitter;
    private String  emitterCode;
    private Boolean isAdjustedEmissionsAdded;
    private Boolean isSubjectToPeriodicReport;
    private Boolean isPeriodicReportTypeOne;
    private String  energyBusinessCode;
    private String  factoryCode;
}
