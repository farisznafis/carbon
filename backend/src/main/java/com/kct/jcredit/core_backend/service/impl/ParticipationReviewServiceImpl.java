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
import com.kct.jcredit.core_backend.dto.response.EnR002ConfirmationItemResponse;
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
import com.kct.jcredit.core_backend.dto.response.EnR002ConfirmationItemResponse;
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
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002SustainabilityCommitmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository; 
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;  
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;
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
    private final ProjectParticipationConfirmationRepository projectParticipationConfirmationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PendingReviewParticipationResponse> getPendingReviews() {
        return siteProjectRelationRepository.findByStatus("PENDING_REVIEW").stream()
                .map(this::toParticipationOrNull)
                .filter(Objects::nonNull)
                .filter(participation -> {
                    ProjectParticipationConfirmation review = projectParticipationConfirmationRepository
                            .findByParticipationId(participation.getId())
                            .orElse(null);
                    return review == null || review.getPrimaryReview() == null;
                })
                .map(participation -> toPendingResponse(participation, "PRIMARY_AVAILABLE"))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingReviewParticipationResponse> getPendingReviewsByUserId(Integer userId) {
        return siteProjectRelationRepository.findByStatus("PENDING_REVIEW").stream()
                .map(this::toParticipationOrNull)
                .filter(Objects::nonNull)
                .filter(participation -> {
                    ProjectParticipationConfirmation review = projectParticipationConfirmationRepository
                            .findByParticipationId(participation.getId())
                            .orElse(null);
                    return review != null
                            && review.getSecondaryReview() == null
                            && userId.equals(review.getNominatedSecondaryReviewerId());
                })
                .map(participation -> toPendingResponse(participation, "SECONDARY_ASSIGNED"))
                .collect(Collectors.toList());
    }

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
        ProjectParticipationConfirmation review = projectParticipationConfirmationRepository
                .findByParticipationId(participationId).orElse(null);

        return ParticipationReviewDetailResponse.builder()
                .participationId(participationId)
                .projectName(project.getProjectName())
                .siteName(site.getSiteName())
                .equipment(toEquipmentResponse(equipment, files))
                .confirmationItems(toConfirmationResponse(confirmationItems))
                .sustainabilityCommitment(toSustainabilityResponse(sustainability))
                .primaryReview(review != null ? review.getPrimaryReview() : null)
                .nominatedSecondaryReviewerId(review != null ? review.getNominatedSecondaryReviewerId() : null)
                .secondaryReview(review != null ? review.getSecondaryReview() : null)
                .build();
    }

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

        if (!"PENDING_REVIEW".equals(relation.getStatus())) {
            throw new CustomException("REVIEW_CLOSED", "このレビューは既に完了しています。");
        }

        ProjectParticipationConfirmation confirmation = projectParticipationConfirmationRepository
                .findByParticipationId(participationId)
                .orElseGet(() -> ProjectParticipationConfirmation.builder()
                        .pk("PART#" + participationId)
                        .build());

        ReviewRecord newReview = ReviewRecord.builder()
                .reviewerId(operatorUserId)
                .reviewerName(operatorName)
                .reviewerInkan(request.getReviewerInkan())
                .decision(request.getDecision())
                .rejectReason(request.getRejectReason())
                .docChecks(request.getDocChecks())
                .dataMgmtChecks(request.getDataMgmtChecks())
                .reviewedAt(Instant.now().toString())
                .build();

        if (confirmation.getPrimaryReview() == null) {
            // ── Submit sebagai PRIMARY reviewer ──────────────────────
            confirmation.setPrimaryReview(newReview);

            if ("APPROVED".equals(request.getDecision())) {
                if (request.getNominateSecondaryReviewerId() == null) {
                    throw new CustomException("VALIDATION_ERROR",
                            "承認する場合、二次確認者の指名が必要です。");
                }
                if (request.getNominateSecondaryReviewerId().equals(operatorUserId)) {
                    throw new CustomException("SAME_REVIEWER",
                            "一次確認者と同一の担当者は二次確認者になれません。");
                }
                confirmation.setNominatedSecondaryReviewerId(request.getNominateSecondaryReviewerId());
                projectParticipationConfirmationRepository.save(confirmation);
                // status tetap PENDING_REVIEW — menunggu secondary
                // TODO: kirim notifikasi ke secondary reviewer yang dinominasikan

            } else {
                projectParticipationConfirmationRepository.save(confirmation);
                relation.setStatus("REJECTED");
                siteProjectRelationRepository.save(relation);
                notifyParticipantRejected(participation, request.getRejectReason());
            }

        } else if (confirmation.getSecondaryReview() == null) {
            // ── Submit sebagai SECONDARY reviewer ────────────────────
            if (!operatorUserId.equals(confirmation.getNominatedSecondaryReviewerId())) {
                throw new CustomException("NOT_NOMINATED",
                        "あなたは二次確認者として指名されていません。");
            }
            if (confirmation.getPrimaryReview().getReviewerId().equals(operatorUserId)) {
                throw new CustomException("SAME_REVIEWER",
                        "一次確認者と同一の担当者は二次確認者になれません。");
            }

            confirmation.setSecondaryReview(newReview);
            projectParticipationConfirmationRepository.save(confirmation);

            if ("REJECTED".equals(request.getDecision())) {
                relation.setStatus("REJECTED");
                siteProjectRelationRepository.save(relation);
                notifyParticipantRejected(participation, request.getRejectReason());
            } else {
                relation.setStatus("REVIEWED");
                siteProjectRelationRepository.save(relation);
                // TODO: trigger PDF draft generation di sini
            }

        } else {
            throw new CustomException("REVIEW_CLOSED", "レビューは既に2名によって完了しています。");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — mapping helpers
    // ─────────────────────────────────────────────────────────────

    private ParticipatingProject toParticipationOrNull(SiteProjectRelation relation) {
        return participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(
                        relation.getSite().getId(), relation.getProject().getId())
                .orElse(null);
    }

    private PendingReviewParticipationResponse toPendingResponse(
            ParticipatingProject participation, String role) {
        return PendingReviewParticipationResponse.builder()
                .participationId(participation.getId())
                .projectName(participation.getProject().getProjectName())
                .siteName(participation.getEquipment().getSite().getSiteName())
                .companyName(participation.getEquipment().getSite().getCompany().getNameJa())
                .submittedAt(participation.getCreatedAt().toString())
                .role(role)
                .build();
    }

    private EnR002EquipmentResponse toEquipmentResponse(
            EnR002Equipment equipment, List<EnR002EquipmentFile> files) {

        List<EnR002EquipmentFileResponse> fileResponses = files.stream()
                .map(f -> EnR002EquipmentFileResponse.builder()
                        .id(f.getId())
                        .fileType(f.getFileType() != null ? f.getFileType().toString() : null)
                        .fileId(f.getFileId())
                        .s3Key(f.getS3Key())
                        .build())
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
                .files(fileResponses)
                .build();
    }

    private EnR002ConfirmationItemResponse toConfirmationResponse(
            EnR002ApplicationConfirmation c) {
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

    private EnR002SustainabilityCommitmentResponse toSustainabilityResponse(
            EnR002SustainabilityCommitment s) {
        if (s == null) return null;
        return EnR002SustainabilityCommitmentResponse.builder()
                .companyName(s.getCompanyName())
                .repName(s.getRepName())
                .repRole(s.getRepRole())
                .hasConsent(s.getHasConsent())
                .isAgreeTerms(s.getIsAgreeTerms())
                .build();
    }

    private void notifyParticipantRejected(ParticipatingProject participation, String reason) {
        // TODO: email service belum dikonfirmasi keberadaannya
        log.warn("[TODO] Rejection email belum terkirim. participationId={}, reason={}",
                participation.getId(), reason);
    }
}