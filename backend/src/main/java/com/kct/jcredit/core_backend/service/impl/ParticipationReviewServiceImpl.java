package com.kct.jcredit.core_backend.service.impl;
import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.kct.jcredit.core_backend.dto.request.RejectReviewRequest;
import com.kct.jcredit.core_backend.dto.request.ApproveReviewRequest;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectApplicationResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectParticipationConfirmationResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002ConfirmationItemResponse;
import com.kct.jcredit.core_backend.dto.response.PendingReviewParticipationResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentFileResponse;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;

import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;

import com.kct.jcredit.core_backend.entity.EnR002SustainabilityCommitment;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.exception.CustomException;

import com.kct.jcredit.core_backend.repository.dynamodb.*;
import com.kct.jcredit.core_backend.repository.postgres.*;

import com.kct.jcredit.core_backend.service.ParticipationReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationReviewServiceImpl implements ParticipationReviewService {

    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final ParticipatingProjectRepository participatingProjectRepository;
    private final EnR002EquipmentFileRepository equipmentFileRepository;
    private final EnR002ApplicationConfirmationRepository confirmationItemsRepository;
    private final EnR002SustainabilityCommitmentRepository sustainabilityRepository;
    private final ProjectParticipationConfirmationRepository reviewRepository;
    private final OperatingUserRepository operatingUserRepository;
    private final ProjectParticipationPdfService pdfService;       
    private final NotificationService notificationService;         

    // ... getPendingReviews / getPendingReviewsByUserId / findPendingParticipations /
    //     getReviewDetail / toEquipmentResponse / toConfirmationResponse /
    //     toSustainabilityResponse / toReviewResponse — TIDAK BERUBAH

    @Override
    @Transactional
    public void approveReview(Integer participationId, Integer operatorUserId, ApproveReviewRequest request) {

        OperatingUser operator = operatingUserRepository.findById(operatorUserId)
                .orElseThrow(() -> new CustomException(ResponseConstants.NOT_FOUND, "オペレーター情報が見つかりません。"));
        String operatorName = operator.getFirstName() + operator.getLastName();

        ParticipatingProject participation = participatingProjectRepository
                .findById(participationId)
                .orElseThrow(() -> new CustomException(ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        SiteProjectRelation relation = siteProjectRelationRepository
                .findBySite_IdAndProject_Id(
                        participation.getEquipment().getSite().getId(),
                        participation.getProject().getId())
                .orElseThrow(() -> new CustomException(ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        if (!"OPEN".equals(relation.getStatus())) {
            throw new CustomException("REVIEW_CLOSED", "このレビューは既に完了しています。");
        }

        ProjectParticipationConfirmation confirmation = reviewRepository
                .findByParticipationId(participationId)
                .orElseGet(() -> ProjectParticipationConfirmation.builder()
                        .pk("PART#" + participationId)
                        .build());

        boolean isPrimarySlotEmpty = confirmation.getPrimaryReviewerId() == null;

        if (!isPrimarySlotEmpty && confirmation.getSecondaryReviewerId() != null) {
            throw new CustomException("REVIEW_CLOSED", "レビューは既に2名によって完了しています。");
        }
        if (!isPrimarySlotEmpty && confirmation.getPrimaryReviewerId().equals(operatorUserId)) {
            throw new CustomException("SAME_REVIEWER", "一次確認者と同一の担当者は二次確認者になれません。");
        }

        confirmation.setIsSolarContractDoc(request.getIsSolarContractDoc());
        confirmation.setIsPcsSpecDoc(request.getIsPcsSpecDoc());
        confirmation.setIsBatterySpecDoc(request.getIsBatterySpecDoc());
        confirmation.setIsOperationStartDoc(request.getIsOperationStartDoc());
        confirmation.setIsGenerationDataDoc(request.getIsGenerationDataDoc());
        confirmation.setIsReversePowerDoc(request.getIsReversePowerDoc());
        confirmation.setIsInvestmentProofDoc(request.getIsInvestmentProofDoc());
        confirmation.setIsApplicationCompleteCheck(request.getIsApplicationCompleteCheck());
        confirmation.setIsFormsAttachCheck(request.getIsFormsAttachCheck());
        confirmation.setIsEmissionsReductionActivityListCheck(request.getIsEmissionsReductionActivityListCheck());
        confirmation.setIsOriginalSpecCheck(request.getIsOriginalSpecCheck());
        confirmation.setIsSubsidyCheck(request.getIsSubsidyCheck());

        if (isPrimarySlotEmpty) {
            confirmation.setPrimaryApplReviewerName(operatorName);
            confirmation.setPrimaryApplReviewerInkan(request.getReviewerInkan());
            confirmation.setPrimaryReviewerId(operatorUserId);
        } else {
            confirmation.setSecondaryApplReviewerName(operatorName);
            confirmation.setSecondaryApplReviewerInkan(request.getReviewerInkan());
            confirmation.setSecondaryReviewerId(operatorUserId);
        }

        reviewRepository.save(confirmation);

        if (!isPrimarySlotEmpty) {
            // submission SECONDARY → kedua reviewer approve
            relation.setStatus("APPROVED");
            siteProjectRelationRepository.save(relation);
            pdfService.generateAndProcess(participationId); // ← trigger PDF di sini
        }
        // else: submission PRIMARY, status tetap OPEN menunggu secondary
    }

    @Override
    @Transactional
    public void rejectReview(Integer participationId, Integer operatorUserId, RejectReviewRequest request) {

        OperatingUser operator = operatingUserRepository.findById(operatorUserId)
                .orElseThrow(() -> new CustomException(ResponseConstants.NOT_FOUND, "オペレーター情報が見つかりません。"));
        String operatorName = operator.getFirstName() + operator.getLastName();

        ParticipatingProject participation = participatingProjectRepository
                .findById(participationId)
                .orElseThrow(() -> new CustomException(ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        SiteProjectRelation relation = siteProjectRelationRepository
                .findBySite_IdAndProject_Id(
                        participation.getEquipment().getSite().getId(),
                        participation.getProject().getId())
                .orElseThrow(() -> new CustomException(ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        if (!"OPEN".equals(relation.getStatus())) {
            throw new CustomException("REVIEW_CLOSED", "このレビューは既に完了しています。");
        }

        ProjectParticipationConfirmation confirmation = reviewRepository
                .findByParticipationId(participationId)
                .orElseGet(() -> ProjectParticipationConfirmation.builder()
                        .pk("PART#" + participationId)
                        .build());

        boolean isPrimarySlotEmpty = confirmation.getPrimaryReviewerId() == null;
        if (!isPrimarySlotEmpty && confirmation.getPrimaryReviewerId().equals(operatorUserId)) {
            throw new CustomException("SAME_REVIEWER", "一次確認者と同一の担当者は二次確認者になれません。");
        }

        confirmation.setIsSolarContractDoc(request.getIsSolarContractDoc());
        confirmation.setIsPcsSpecDoc(request.getIsPcsSpecDoc());
        confirmation.setIsBatterySpecDoc(request.getIsBatterySpecDoc());
        confirmation.setIsOperationStartDoc(request.getIsOperationStartDoc());
        confirmation.setIsGenerationDataDoc(request.getIsGenerationDataDoc());
        confirmation.setIsReversePowerDoc(request.getIsReversePowerDoc());
        confirmation.setIsInvestmentProofDoc(request.getIsInvestmentProofDoc());
        confirmation.setIsApplicationCompleteCheck(request.getIsApplicationCompleteCheck());
        confirmation.setIsFormsAttachCheck(request.getIsFormsAttachCheck());
        confirmation.setIsEmissionsReductionActivityListCheck(request.getIsEmissionsReductionActivityListCheck());
        confirmation.setIsOriginalSpecCheck(request.getIsOriginalSpecCheck());
        confirmation.setIsSubsidyCheck(request.getIsSubsidyCheck());

        if (isPrimarySlotEmpty) {
            confirmation.setPrimaryApplReviewerName(operatorName);
            confirmation.setPrimaryApplReviewerInkan(request.getReviewerInkan());
            confirmation.setPrimaryReviewerId(operatorUserId);
        } else {
            confirmation.setSecondaryApplReviewerName(operatorName);
            confirmation.setSecondaryApplReviewerInkan(request.getReviewerInkan());
            confirmation.setSecondaryReviewerId(operatorUserId);
        }

        reviewRepository.save(confirmation);

        relation.setStatus("REJECTED");
        siteProjectRelationRepository.save(relation);

        notificationService.sendParticipationRejectionNotification(
                participation.getUser(), request.getRejectReason());
    }
}