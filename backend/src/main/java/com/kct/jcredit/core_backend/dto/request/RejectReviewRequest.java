package com.kct.jcredit.core_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class RejectReviewRequest {

    @NotBlank
    private String reviewerInkan;
    @NotBlank
    private String reviewerName;

    @NotBlank
    private String rejectReason;

    @NotNull private Boolean isSolarContractDoc;
    @NotNull private Boolean isPcsSpecDoc;
    @NotNull private Boolean isBatterySpecDoc;
    @NotNull private Boolean isOperationStartDoc;
    @NotNull private Boolean isGenerationDataDoc;
    @NotNull private Boolean isReversePowerDoc;
    @NotNull private Boolean isInvestmentProofDoc;
    @NotNull private Boolean isApplicationCompleteCheck;
    @NotNull private Boolean isFormsAttachCheck;
    @NotNull private Boolean isEmissionsReductionActivityListCheck;
    @NotNull private Boolean isOriginalSpecCheck;
    @NotNull private Boolean isSubsidyCheck;
}
