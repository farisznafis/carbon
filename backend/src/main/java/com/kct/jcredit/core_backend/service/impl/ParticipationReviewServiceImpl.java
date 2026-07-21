package com.kct.jcredit.core_backend.service.impl;
import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectApplicationResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectParticipationConfirmationResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002ConfirmationItemResponse;
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
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;

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

    // ─────────────────────────────────────────────────────────────
    // LIST
    // ─────────────────────────────────────────────────────────────

    @Override
        @Transactional(readOnly = true)
        public List<ParticipationReviewDetailResponse> getPendingReviews() {
        return siteProjectRelationRepository.findByStatus("PENDING").stream()
                .map(this::toParticipationOrNull)
                .filter(Objects::nonNull)
                .map(p -> toPendingResponse(p, "AVAILABLE"))
                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<ParticipationReviewDetailResponse> getPendingReviewsByUserId(Integer userId) {
        return siteProjectRelationRepository.findByStatus("PENDING").stream()
                .map(this::toParticipationOrNull)
                .filter(Objects::nonNull)
                .filter(participation -> {
                        ProjectParticipationConfirmation review = reviewRepository
                                .findByParticipationId(participation.getId())
                                .orElse(null);
                        return review == null || !userId.equals(review.getPrimaryReviewerId());
                })
                .map(p -> toPendingResponse(p, "AVAILABLE"))
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
                .projectName(project.getProjectName())
                .siteName(site.getSiteName())
                .equipment(toEquipmentResponse(equipment, files))
                .confirmationItems(toConfirmationResponse(confirmationItems))
                .sustainabilityCommitment(toSustainabilityResponse(sustainability))
                .review(toReviewResponse(review))
                .build();
        }

        // ─────────────────────────────────────────────────────────────
        // SUBMIT
        // ─────────────────────────────────────────────────────────────

        @Override
        @Transactional
        public void submitReview(Integer participationId, Integer operatorUserId,
                                String operatorName, ReviewParticipationRequest request) {

        ParticipatingProject participation = participatingProjectRepository
                .findById(participationId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        Integer siteId = participation.getEquipment().getSite().getId();
        Integer projectId = participation.getProject().getId();

        SiteProjectRelation relation = siteProjectRelationRepository
                .findBySite_IdAndProject_Id(siteId, projectId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        if (!"PENDING".equals(relation.getStatus())) {
                throw new CustomException("REVIEW_CLOSED", "このレビューは既に完了しています。");
        }

        ProjectParticipationConfirmation confirmation = reviewRepository
                .findByParticipationId(participationId)
                .orElseGet(() -> ProjectParticipationConfirmation.builder()
                        .pk("PART#" + participationId)
                        .build());

        boolean isPrimarySlotEmpty   = confirmation.getPrimaryReviewerId() == null;
        boolean isSecondarySlotEmpty = confirmation.getSecondaryReviewerId() == null;

        if (!isPrimarySlotEmpty && !isSecondarySlotEmpty) {
                throw new CustomException("REVIEW_CLOSED", "レビューは既に2名によって完了しています。");
        }

        if (!isPrimarySlotEmpty && confirmation.getPrimaryReviewerId().equals(operatorUserId)) {
                throw new CustomException("SAME_REVIEWER", "一次確認者と同一の担当者は二次確認者になれません。");
        }

        // Checklist ditulis oleh reviewer yang sedang submit (baik primary maupun secondary)
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

        if ("REJECTED".equals(request.getDecision())) {
                relation.setStatus("OPEN");
                siteProjectRelationRepository.save(relation);
                notifyParticipantRejected(participation, request.getRejectReason());

        } else if (!isPrimarySlotEmpty) {
                // Ini submission SECONDARY dengan APPROVED → kedua reviewer selesai
                relation.setStatus("APPROVED");
                siteProjectRelationRepository.save(relation);
                // TODO: trigger PDF draft generation di sini
        }
        // else: submission PRIMARY dengan APPROVED, status tetap PENDING
        }

        // ─────────────────────────────────────────────────────────────
        // PRIVATE — mapping
        // ─────────────────────────────────────────────────────────────

        private ParticipatingProject toParticipationOrNull(SiteProjectRelation relation) {
        return participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(relation.getSite().getId(), relation.getProject().getId())
                .orElse(null);
        }

        private ParticipationReviewDetailResponse toPendingResponse(ParticipatingProject p, String role) {
        return ParticipationReviewDetailResponse.builder()
                .participationId(p.getId())
                .projectName(p.getProject().getProjectName())
                .siteName(p.getEquipment().getSite().getSiteName())
                .companyName(p.getEquipment().getSite().getCompany().getNameJa())
                .submittedAt(p.getCreatedAt().toString())
                .role(role)
                .build();
        }

        private EnR002EquipmentResponse toEquipmentResponse(EnR002Equipment equipment, List<EnR002EquipmentFile> files) {
        List<EnR002EquipmentFile> equipmentFiles = equipmentFileRepository
                        .findByEquipment_Id(equipment.getId()); 

                if (equipmentFiles.isEmpty() && equipment.getId() != null) {
                        equipmentFiles = equipmentFileRepository.findByEquipment_Id(equipment.getId());
                }

                List<String> evidenceDocuments = equipmentFiles.stream()
                        .map(EnR002EquipmentFile::getS3Key)
                        .map(this::extractFileNameFromS3Key) 
                        .filter(Objects::nonNull)
                        .filter(name -> !name.isBlank())
                        .collect(Collectors.toList());

        return EnR002EquipmentResponse.builder()
                .id(equipment.getId())
                .siteId(equipment.getSite().getId())
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
                .evidenceDocuments(evidenceDocuments)  
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

        private void notifyParticipantRejected(ParticipatingProject participation, String reason) {
        log.warn("[TODO] Rejection email belum terkirim. participationId={}, reason={}",
                participation.getId(), reason);
        }

        private String extractFileNameFromS3Key(String s3Key) {
        if (s3Key == null) {
                return null;
        }
        int lastSlashIndex = s3Key.lastIndexOf('/');
        return lastSlashIndex >= 0 ? s3Key.substring(lastSlashIndex + 1) : s3Key;
        }   
        }