package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.MemberTermsInfoResponse;
import com.kct.jcredit.core_backend.dto.response.MyParticipationResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;



public interface ParticipationProjectService {

    // Browse
    ProjectListInfoResponse getActiveProjects(String companyId);

    SiteListResponse getSites(String companyId, String projectId);

    // Auto-fill
    EnR002SustainabilityCommitmentResponse getSustainabilityCommitmentDefault(
            String siteId);

    // Submit
    ProjectJoinResponse submitParticipation(String projectId,
                                             String companyId,
                                             Integer userId,
                                             EnR002JoinProjectRequest request); 

   //Join Site
       ProjectJoinResponse joinSite(String projectId,
                                             String companyId,
                                             Integer userId,
                                             String siteId);

        
        public MemberTermsInfoResponse getMemberTermsInfo(String projectId);

        public ProjectPdfDownloadResponse getMembershipPdfByCompany(String projectId, String companyId);



//     // PDF
//     ProjectPdfDownloadResponse getPdfDownloadUrl(
//             Integer participationId,
//             String companyId);

//     EnR002EquipmentResponse registerEquipments(
//             Integer siteId,
//             String projectId,
//             String companyId,
//             EnR002ConfirmationItemRequest request);

//     EnR002SustainabilityCommitmentResponse getJigyoushaDefault(
//             Integer siteId,
//             String projectId);

//     void validateEquipmentsNotEmpty(List<EnR002EquipmentRequest> equipments);

//     void validateAllEquipments(List<EnR002EquipmentRequest> equipments);

//     void validateTncConsent(EnR002ConfirmationItemRequest request);

//     void validateTncFields(EnR002ConfirmationItemRequest request);

//     void validateJigyousha(EnR002ConfirmationItemRequest request);

//     void validateFinalAgreement(EnR002ConfirmationItemRequest request);boolean existsByCompany_CompanyIdAndSiteProjectRelation_Project_Id(String companyId, String projectId);
}