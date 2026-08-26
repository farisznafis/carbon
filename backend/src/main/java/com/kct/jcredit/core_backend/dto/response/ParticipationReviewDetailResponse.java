package com.kct.jcredit.core_backend.dto.response;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Builder;

import java.time.LocalDateTime;

import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ParticipationReviewDetailResponse {
    private Integer participationId;
    private Integer siteId;
    private Integer projectId;
    private String  projectName;
    private String  siteName;
    private String  companyName;
    private LocalDateTime submittedAt;
    private EnR002EquipmentResponse equipment;
    private EnR002ConfirmationItemResponse confirmationItems;
    private EnR002SustainabilityCommitmentResponse sustainabilityCommitment;
    private ProjectParticipationConfirmationResponse review;
    private String pdfStatus;
}
