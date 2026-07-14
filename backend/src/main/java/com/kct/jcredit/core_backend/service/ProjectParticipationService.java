package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityComitmentRequest;
import com.kct.jcredit.core_backend.dto.request.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002ConfirmationItems;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ProjectParticipationService;
import com.kct.jcredit.core_backend.service.FileUploadSolarService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import java.util.List;


public interface ProjectParticipationService {

     // Browse
    List<Project>  getActiveProjects(String companyId);
    List<Site>       getSites(String companyId, Integer projectId);

    // Auto-fill form halaman sustainability commitment
    EnR002SustainabilityCommitmentResponse getSustainabilityCommitmentDefault(
            Integer siteId);

    // Submit join project
    ProjectJoinResponse submitParticipation(Integer projectId,
                                             String companyId,
                                             Integer userId,
                                             ProjectApplicationRequest request);

    // PDF download URL
    ProjectPdfDownloadResponse getPdfDownloadUrl(Integer participationId,
                                           String companyId);

    EnR002EquipmentResponse registerEquipments(
            Integer siteId, Integer projectId, String companyId,
            EnR002ConfirmationItemRequest request);

    EnR002SustainabilityCommitmentResponse getJigyoushaDefault(Integer siteId, Integer projectId);

    void validateEquipmentsNotEmpty(List<EnR002EquipmentRequest> equipments);
    void validateAllEquipments(List<EnR002EquipmentRequest> equipments);
    void validateTncConsent(EnR002ConfirmationItemRequest request);
    void validateTncFields(EnR002ConfirmationItemRequest request);
    void validateJigyousha(EnR002ConfirmationItemRequest request);
    void validateFinalAgreement(EnR002ConfirmationItemRequest request);
}