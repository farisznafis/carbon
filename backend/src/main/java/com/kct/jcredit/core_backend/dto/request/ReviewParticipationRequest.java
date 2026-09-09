package com.kct.jcredit.core_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.kct.jcredit.core_backend.dto.request.base.BaseRequest;

import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class ReviewParticipationRequest {

    @NotNull
    private String decision; // "APPROVED" / "REJECTED"
    private String rejectReason;

    @NotBlank
    private String reviewerInkan;
    
    @NotBlank
    private String reviewerName;

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