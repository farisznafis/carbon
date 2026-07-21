package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// TncValidateRequest.java
@Getter
@Setter
public class TncValidateRequest {

    @NotNull
    private Boolean isAgreeDataUse;

    @NotNull
    private Boolean isAgreeExtraData;

    @NotNull
    private Boolean isAgreeTransfer;

    @NotNull
    private Boolean isNotRegistered;

    @NotNull
    private Boolean isKeidanrenParticipation;

    @NotNull
    private Boolean isEmitter;

    @NotNull
    private Boolean isAdjustedEmissionsAdded;


    private String emitterCode;

    @NotNull
    private Boolean isSubjectToPeriodicReport;

    @NotNull
    private Boolean isPeriodicReportTypeOne;

    private String factoryCode;

    private String energyReport;
    private String energyBusinessCode;

}
