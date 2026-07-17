package com.kct.jcredit.core_backend.service.impl;
import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.dto.response.PendingReviewParticipationResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectApplicationResponse;

import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002SustainabilityCommitmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository; 
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;  
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.ProjectParticipationReview;
import com.kct.jcredit.core_backend.entity.ReviewRecordParticipation;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;

import com.kct.jcredit.core_backend.entity.EnR002SustainabilityCommitment;
import com.kct.jcredit.core_backend.exception.CustomException;

import com.kct.jcredit.core_backend.repository.dynamodb.*;
import com.kct.jcredit.core_backend.repository.postgres.*;

import com.kct.jcredit.core_backend.service.ParticipationReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationReviewServiceImpl implements ParticipationReviewService {

    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final SiteR002MemberDataRepository siteR002MemberDataRepository;
    private final EnR002EquipmentFileRepository enR002EquipmentFileRepository;
    private final EnR002ApplicationConfirmationRepository enR002ApplicationConfirmationRepository;
    private final EnR002SustainabilityCommitmentRepository enR002SustainabilityCommitmentRepository;

    @Override
    public ParticipationReviewDetailResponse getReviewDetail(Long projectId, Long siteId) {

        SiteProjectRelation relation = siteProjectRelationRepository
                .findBySiteIdAndProjectId(siteId.intValue(), projectId.intValue())
                .orElseThrow(() -> new IllegalArgumentException(
                        "SiteProjectRelation not found for projectId=" + projectId + ", siteId=" + siteId));

        SiteR002MemberData memberData = siteR002MemberDataRepository
                .findBySiteProjectRelationId(relation.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Equipment data not found for siteProjectRelationId=" + relation.getId()));

        List<EnR002EquipmentFile> equipmentFiles =
                enR002EquipmentFileRepository.findByMemberDataId(memberData.getId());

        EnR002ApplicationConfirmation confirmation = enR002ApplicationConfirmationRepository
                .findBySiteProjectRelationId(relation.getId())
                .orElse(null);

        SustainabilityAgreement sustainabilityAgreement = sustainabilityAgreementRepository
                .findBySiteProjectRelationId(relation.getId())
                .orElse(null);

        EnR002SustainabilityCommitment sustainabilityCommitment = enR002SustainabilityCommitmentRepository
                .findBySiteProjectRelationId(relation.getId())
                .orElse(null);

        return ParticipationReviewDetailResponse.builder()
                .projectId(projectId)
                .siteId(siteId)
                .participationId(relation.getId())
                .equipment(toEquipmentResponse(memberData, equipmentFiles))
                .confirmationItems(toConfirmationResponse(confirmation))
                .sustainabilityAgreement(toSustainabilityResponse(sustainabilityAgreement))
                .pdfS3Key(relation.getPdfS3Key())
                .build();
    }

    // ---------------------------------------------------------------
    // Mapping helpers — naming convention: toXxxResponse(entity) -> DTO
    // ---------------------------------------------------------------

    private EnR002EquipmentResponse toEquipmentResponse(
            SiteR002MemberData memberData,
            List<EnR002EquipmentFile> files) {

        if (memberData == null) {
            return null;
        }

        List<EnR002EquipmentFileResponse> fileResponses = files.stream()
                .map(this::toEquipmentFileResponse)
                .toList();

        return EnR002EquipmentResponse.builder()
                .id(memberData.getId())
                .operationStartDate(memberData.getOperationStartDate())
                .panelMaxOutputKw(memberData.getPanelMaxOutputKw())
                .inverterManufacturer(memberData.getInverterManufacturer())
                .inverterModel(memberData.getInverterModel())
                .inverterQuantity(memberData.getInverterQuantity())
                .inverterSerialNumber(memberData.getInverterSerialNumber())
                .inverterStandbyPowerWatt(memberData.getInverterStandbyPowerWatt())
                .hasBattery(memberData.getHasBattery())
                .batteryManufacturer(memberData.getBatteryManufacturer())
                .batteryModel(memberData.getBatteryModel())
                .batteryCapacityKw(memberData.getBatteryCapacityKw())
                .totalInvestmentAmount(memberData.getTotalInvestmentAmount())
                .subsidyName(memberData.getSubsidyName())
                .subsidyAmount(memberData.getSubsidyAmount())
                .electricityUnitPriceBeforeProject(memberData.getElectricityUnitPriceBeforeProject())
                .files(fileResponses)
                .build();
    }

    private EnR002EquipmentFileResponse toEquipmentFileResponse(EnR002EquipmentFile file) {
        return EnR002EquipmentFileResponse.builder()
                .id(file.getId())
                .fileType(file.getFileType())
                .s3Key(file.getS3Key())
                .originalFileName(file.getOriginalFileName())
                .build();
    }

    private EnR002ConfirmationItemResponse toConfirmationResponse(
            EnR002ApplicationConfirmation confirmation) {

        if (confirmation == null) {
            return null;
        }

        return EnR002ConfirmationItemResponse.builder()
                .id(confirmation.getId())
                .agreeMembershipTerms(confirmation.getAgreeMembershipTerms())
                .agreeInfoUsage(confirmation.getAgreeInfoUsage())
                .agreeAdditionalInfoProvision(confirmation.getAgreeAdditionalInfoProvision())
                .agreeCreditTransfer(confirmation.getAgreeCreditTransfer())
                .noDuplicateRegistration(confirmation.getNoDuplicateRegistration())
                .keidanrenParticipation(confirmation.getKeidanrenParticipation())
                .isSpecifiedEmitter(confirmation.getIsSpecifiedEmitter())
                .specifiedEmitterCode(confirmation.getSpecifiedEmitterCode())
                .isEnergyReportingTarget(confirmation.getIsEnergyReportingTarget())
                .energyBusinessOperatorNumber(confirmation.getEnergyBusinessOperatorNumber())
                .energyDesignatedFactoryNumber(confirmation.getEnergyDesignatedFactoryNumber())
                .isAgreeTerms(confirmation.getIsAgreeTerms())
                .build();
    }

    private SustainabilityAgreementResponse toSustainabilityResponse(
            SustainabilityAgreement agreement) {

        if (agreement == null) {
            return null;
        }

        return SustainabilityAgreementResponse.builder()
                .id(agreement.getId())
                .companyName(agreement.getCompanyName())
                .representativeRole(agreement.getRepresentativeRole())
                .representativeName(agreement.getRepresentativeName())
                .hasConsent(agreement.getHasConsent())
                .build();
    }
}