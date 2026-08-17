package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.MyParticipationResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import java.util.List;


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

        List<MyParticipationResponse> getMyParticipations(String projectId, String companyId);



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