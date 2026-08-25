package com.kct.jcredit.core_backend.service.impl;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;

import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewHistoryResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectParticipationConfirmationResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002ConfirmationItemResponse;
import com.kct.jcredit.core_backend.dto.response.PendingReviewParticipationResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentFileResponse;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;
import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.kct.jcredit.core_backend.dto.request.ApproveReviewRequest;
import com.kct.jcredit.core_backend.dto.request.RejectReviewRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.ParticipationReviewHistory;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
import com.kct.jcredit.core_backend.entity.EnR002SustainabilityCommitment;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
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
    private final ParticipationReviewHistoryRepository reviewHistoryRepository; 
    private final ProjectParticipationPdfService pdfService;
    private final NotificationService notificationService;

    // ─────────────────────────────────────────────────────────────
    // LIST
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationReviewDetailResponse> getPendingReviews() {
        return findPendingParticipations().stream()
                .map(participation -> getReviewDetail(participation.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationReviewDetailResponse> getPendingReviewsByUserId(Integer userId) {
        return findPendingParticipations().stream()
                .filter(participation -> {
                    ProjectParticipationConfirmation review = reviewRepository
                            .findByParticipationId(participation.getId())
                            .orElse(null);
                    return review == null || !userId.equals(review.getPrimaryReviewerId());
                })
                .map(participation -> getReviewDetail(participation.getId()))
                .collect(Collectors.toList());
    }

    private List<ParticipatingProject> findPendingParticipations() {
        return siteProjectRelationRepository.findByStatus("OPEN").stream()
                .map(this::toParticipationOrNull)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // DETAIL
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ParticipationReviewDetailResponse getReviewDetail(Integer participationId) {

        ParticipatingProject participation = participatingProjectRepository
                .findById(participationId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        EnR002Equipment equipment = participation.getEquipment();
        Site site = equipment.getSite();
        Project project = participation.getProject();

        List<EnR002EquipmentFile> files = equipmentFileRepository.findByEquipment_Id(equipment.getId());

        EnR002ApplicationConfirmation confirmationItems = confirmationItemsRepository
                .findByParticipationId(participationId).orElse(null);
        EnR002SustainabilityCommitment sustainability = sustainabilityRepository
                .findByParticipationId(participationId).orElse(null);
        ProjectParticipationConfirmation review = reviewRepository
                .findByParticipationId(participationId).orElse(null);

        return ParticipationReviewDetailResponse.builder()
                .participationId(participationId)
                .projectId(project.getId())
                .siteId(site.getId())
                .projectName(project.getProjectName())
                .siteName(site.getSiteName())
                .companyName(site.getCompany().getNameJa())
                .submittedAt(participation.getCreatedAt())
                .equipment(toEquipmentResponse(equipment, files))
                .confirmationItems(toConfirmationResponse(confirmationItems))
                .sustainabilityCommitment(toSustainabilityResponse(sustainability))
                .review(toReviewResponse(review))
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // APPROVE
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void approveReview(Integer participationId, Integer operatorUserId, ApproveReviewRequest request) {

        OperatingUser operator = operatingUserRepository.findById(operatorUserId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "オペレーター情報が見つかりません。"));
        String operatorName = operator.getFirstName() + operator.getLastName();

        ParticipatingProject participation = participatingProjectRepository
                .findById(participationId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        SiteProjectRelation relation = siteProjectRelationRepository
                .findBySite_IdAndProject_Id(
                        participation.getEquipment().getSite().getId(),
                        participation.getProject().getId())
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

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
            pdfService.generateAndProcess(participationId);
        }
        // else: submission PRIMARY, status tetap OPEN menunggu secondary
    }

    // ─────────────────────────────────────────────────────────────
    // REJECT
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void rejectReview(Integer participationId, Integer operatorUserId, RejectReviewRequest request) {

        OperatingUser operator = operatingUserRepository.findById(operatorUserId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "オペレーター情報が見つかりません。"));
        String operatorName = operator.getFirstName() + operator.getLastName();

        ParticipatingProject participation = participatingProjectRepository
                .findById(participationId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        SiteProjectRelation relation = siteProjectRelationRepository
                .findBySite_IdAndProject_Id(
                        participation.getEquipment().getSite().getId(),
                        participation.getProject().getId())
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

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
        int attemptNumber = reviewHistoryRepository.countByParticipationId(participationId) + 1;

        ParticipationReviewHistory history = ParticipationReviewHistory.builder()
                .pk("PART#" + participationId)
                .sk("ATTEMPT#" + attemptNumber)
                .attemptNumber(attemptNumber)
                .decision("REJECTED")
                .rejectReason(request.getRejectReason())
                .primaryReviewerName(confirmation.getPrimaryApplReviewerName())
                .secondaryReviewerName(confirmation.getSecondaryApplReviewerName())
                .reviewedByOperatorId(operatorUserId)
                .reviewedAt(Instant.now().toString())
                .build();
        reviewHistoryRepository.save(history);

        notificationService.sendParticipationRejectionNotification(
                participation.getUser(), request.getRejectReason());
        }

        @Override
        public List<ParticipationReviewHistoryResponse> getReviewHistory(Integer participationId) {
        return reviewHistoryRepository.findByParticipationId(participationId).stream()
                .map(h -> ParticipationReviewHistoryResponse.builder()
                        .attemptNumber(h.getAttemptNumber())
                        .decision(h.getDecision())
                        .rejectReason(h.getRejectReason())
                        .primaryReviewerName(h.getPrimaryReviewerName())
                        .secondaryReviewerName(h.getSecondaryReviewerName())
                        .reviewedAt(h.getReviewedAt())
                        .build())
                .collect(Collectors.toList());
        }
    // ─────────────────────────────────────────────────────────────
    // PRIVATE — mapping
    // ─────────────────────────────────────────────────────────────

    private ParticipatingProject toParticipationOrNull(SiteProjectRelation relation) {
        return participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(relation.getSite().getId(), relation.getProject().getId())
                .orElse(null);
    }

    private EnR002EquipmentResponse toEquipmentResponse(EnR002Equipment equipment, List<EnR002EquipmentFile> files) {
        List<EnR002EquipmentFileResponse> fileResponses = files.stream()
                .map(f -> EnR002EquipmentFileResponse.builder()
                        .id(f.getId())
                        .fileType(f.getFileType() != null ? f.getFileType().getValue() : null)
                        .fileId(f.getFileId())
                        .s3Key(f.getS3Key())
                        .build())
                .collect(Collectors.toList());

        return EnR002EquipmentResponse.builder()
                .id(equipment.getId())
                .siteId(equipment.getSite().getPublicId()) 
                .equipmentOpDate(equipment.getEquipmentOpDate())
                .nominalMaxOutput(equipment.getNominalMaxOutput())
                .pcsManufacturer(equipment.getPcsManufacturer())
                .pcsModel(equipment.getPcsModel())
                .pcsUnitCount(equipment.getPcsUnitCount())
                .deviceNo(equipment.getDeviceNo())
                .pcsStandby(equipment.getPcsStandby())
                .hasBattery(equipment.getHasBattery())
                .batteryMaker(equipment.getBatteryMaker())
                .batteryModel(equipment.getBatteryModel())
                .batteryCapacity(equipment.getBatteryCapacity())
                .totalInvestment(equipment.getTotalInvestment())
                .hasSubsidy(equipment.getHasSubsidy())
                .subsidyName(equipment.getSubsidyName())
                .subsidyAmount(equipment.getSubsidyAmount())
                .preProjectUnitPrice(equipment.getPreProjectUnitPrice())
                .files(fileResponses)
                .build();
    }

    private EnR002ConfirmationItemResponse toConfirmationResponse(EnR002ApplicationConfirmation c) {
        if (c == null) return null;
        return EnR002ConfirmationItemResponse.builder()
                .isAgreeDataUse(c.getIsAgreeDataUse())
                .isAgreeExtraData(c.getIsAgreeExtraData())
                .isAgreeTransfer(c.getIsAgreeTransfer())
                .isNotRegistered(c.getIsNotRegistered())
                .isKeidanrenParticipation(c.getIsKeidanrenParticipation())
                .isEmitter(c.getIsEmitter())
                .emitterCode(c.getEmitterCode())
                .isAdjustedEmissionsAdded(c.getIsAdjustedEmissionsAdded())
                .isSubjectToPeriodicReport(c.getIsSubjectToPeriodicReport())
                .isPeriodicReportTypeOne(c.getIsPeriodicReportTypeOne())
                .energyBusinessCode(c.getEnergyBusinessCode())
                .factoryCode(c.getFactoryCode())
                .build();
    }

    private EnR002SustainabilityCommitmentResponse toSustainabilityResponse(EnR002SustainabilityCommitment s) {
        if (s == null) return null;
        return EnR002SustainabilityCommitmentResponse.builder()
                .companyName(s.getCompanyName())
                .repName(s.getRepName())
                .repRole(s.getRepRole())
                .hasConsent(s.getHasConsent())
                .isAgreeTerms(s.getIsAgreeTerms())
                .build();
    }

    private ProjectParticipationConfirmationResponse toReviewResponse(ProjectParticipationConfirmation r) {
        if (r == null) return null;
        return ProjectParticipationConfirmationResponse.builder()
                .isSolarContractDoc(r.getIsSolarContractDoc())
                .isPcsSpecDoc(r.getIsPcsSpecDoc())
                .isBatterySpecDoc(r.getIsBatterySpecDoc())
                .isOperationStartDoc(r.getIsOperationStartDoc())
                .isGenerationDataDoc(r.getIsGenerationDataDoc())
                .isReversePowerDoc(r.getIsReversePowerDoc())
                .isInvestmentProofDoc(r.getIsInvestmentProofDoc())
                .isApplicationCompleteCheck(r.getIsApplicationCompleteCheck())
                .isFormsAttachCheck(r.getIsFormsAttachCheck())
                .isEmissionsReductionActivityListCheck(r.getIsEmissionsReductionActivityListCheck())
                .isOriginalSpecCheck(r.getIsOriginalSpecCheck())
                .isSubsidyCheck(r.getIsSubsidyCheck())
                .primaryApplReviewerName(r.getPrimaryApplReviewerName())
                .secondaryApplReviewerName(r.getSecondaryApplReviewerName())
                .build();
    }
}