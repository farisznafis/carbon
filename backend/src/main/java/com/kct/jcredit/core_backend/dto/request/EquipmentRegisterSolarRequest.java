package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.kct.jcredit.core_backend.dto.request.base.BaseRequest;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentRegisterSolarRequest extends BaseRequest {
 
    @NotNull
    private Integer siteId;
 
    // @NotNull
    // private Integer projectId;
 
    @NotEmpty
    @Valid
    private List<EquipmentItemSolarRequest> equipments;
 
    // Page 2 — TnC
    @NotNull private Boolean isAgreeDataUse;
    @NotNull private Boolean isAgreeExtraData;
    @NotNull private Boolean isAgreeTransfer;
    @NotNull private Boolean isNotRegistered;
    @NotNull private Boolean isKeidanrenParticipation;
    @NotNull private Boolean isEmitter;
    @NotNull private Boolean isAdjustedEmissionsAdded;
    @NotNull private Boolean isPeriodicReportTypeOne;
 
    // isEmitter = true
    private String emitterCode;
 
    @NotNull private Boolean isSubjectToPeriodicReport;
 
    // isSubjectToPeriodicReport = true
    private String energyReport;
    private String energyBusinessCode;
    private String factoryCode;
 
    // Page 3
    @NotBlank private String companyName;
    @NotBlank private String repName;
    @NotBlank private String repRole;
    @NotNull  private Boolean hasConsent;
 
    // Page 4
    @NotNull  private Boolean isAgreeTerms;
}