package com.kct.jcredit.core_backend.dto.response;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Builder;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ParticipationReviewDetailResponse extends BaseResponse {
    private Integer participationId;
    private String  projectName;
    private String  siteName;
    private String companyName;
    private String submittedAt;
    private String role;
    private EnR002EquipmentResponse equipment;
    private EnR002ConfirmationItemResponse confirmationItems;
    private EnR002SustainabilityCommitmentResponse sustainabilityCommitment;
    private ProjectParticipationConfirmationResponse review; 
}
