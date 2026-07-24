package com.kct.jcredit.core_backend.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

@Getter
@Builder
@Setter
public class ProjectParticipationConfirmationResponse {
    private Boolean isSolarContractDoc;
    private Boolean isPcsSpecDoc;
    private Boolean isBatterySpecDoc;
    private Boolean isOperationStartDoc;
    private Boolean isGenerationDataDoc;
    private Boolean isReversePowerDoc;
    private Boolean isInvestmentProofDoc;
    private Boolean isApplicationCompleteCheck;
    private Boolean isFormsAttachCheck;
    private Boolean isEmissionsReductionActivityListCheck;
    private Boolean isOriginalSpecCheck;
    private Boolean isSubsidyCheck;
    private String  primaryApplReviewerName;
    private String  secondaryApplReviewerName;
}
