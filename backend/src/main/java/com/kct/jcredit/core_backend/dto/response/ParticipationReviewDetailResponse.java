package com.kct.jcredit.core_backend.dto.response;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Builder;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ParticipationReviewDetailResponse extends BaseResponse {
    private Integer participationId;
    private String  projectName;
    private String  siteName;
    private EnR002EquipmentResponse equipment;
    private EnR002ConfirmationItemRequest confirmationItems;
    private EnR002SustainabilityCommitmentRequest sustainabilityCommitment;
    // private ReviewRecord primaryReview;   
    // private ReviewRecord secondaryReview; 
}
