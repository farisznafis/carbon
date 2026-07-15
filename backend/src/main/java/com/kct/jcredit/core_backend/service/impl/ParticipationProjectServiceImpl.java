package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
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
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.EnR002SustainabilityCommitment;
import com.kct.jcredit.core_backend.entity.ConfirmationItems;
import com.kct.jcredit.core_backend.entity.EnR002ConfirmationItems;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.exception.BulkValidationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.SiteService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002SustainabilityCommitmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Collections;


@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationProjectServiceImpl implements ParticipationProjectService {

    private final ProjectRepository                          projectRepository;
    private final SiteRepository                             siteRepository;
    private final ParticipatingCompanyRepository             companyRepository;
    private final ParticipatingUserRepository                userRepository;
    private final SiteProjectRelationRepository              siteProjectRelationRepository;
    private final EnR002EquipmentRepository                  equipmentRepository;
    private final EnR002EquipmentFileRepository              equipmentFileRepository;
    private final EnR002ApplicationConfirmationRepository    confirmationRepository;
    private final EnR002SustainabilityCommitmentRepository   sustainabilityRepository;
    private final ParticipatingProjectRepository             participatingProjectRepository; 
    private final EnR002FileUploadService                    fileUploadSolarService;
    private final ParticipationProjectValidationService      validationService;
    private final ProjectParticipationPdfService  pdfService;

    // ─────────────────────────────────────────────────────────────
    // BROWSE
    // ─────────────────────────────────────────────────────────────

        @Override
        public ProjectListInfoResponse getActiveProjects(String companyId) {

        List<Project> projects = projectRepository.findByProjectStatus("ACTIVE");

        List<ProjectDetailResponse> projectDetails = projects.stream()
        .map((Project project) -> {

                boolean alreadyJoined =
                        participatingProjectRepository
                                .existsByCompany_CompanyIdAndSiteProjectRelation_Project_Id(
                                        companyId,
                                        project.getId());

                return ProjectDetailResponse.builder()
                        .id(String.valueOf(project.getId()))
                        .projectName(project.getProjectName())
                        .projectNumber(project.getProjectNumber())
                        // .projectStatus(project.getStatus())
                        .isJoined(alreadyJoined)
                        .build();
        })
        .collect(Collectors.toList());

        return ProjectListInfoResponse.builder()
                .projects(projectDetails)
                .pagination(null)
                .build();
        }

       @Override
        public SiteListResponse getSites(String companyId, Integer projectId) {

        List<Site> sites =
                siteRepository.findByCompany_CompanyId(companyId);

        List<SiteResponse> siteResponses = sites.stream()
                .map((Site site) -> {  

                        boolean alreadyInProject =
                                siteProjectRelationRepository
                                        .existsBySite_IdAndProject_Id(
                                                site.getId(),
                                                projectId);

                        return SiteResponse.builder()
                                .id(String.valueOf(site.getId()))
                                .siteName(site.getSiteName())
                                .postalCode(site.getPostalCode())
                                .prefecture(site.getPrefecture())
                                .municipality(site.getMunicipality())
                                .blocknum(site.getBlocknum())
                                .buildingName(site.getBuildingName())
                                .picFirstName(site.getPicFirstName())
                                .picLastName(site.getPicLastName())
                                .projects(Collections.emptyList())
                                .data(null)
                                // .alreadyInProject(alreadyInProject)
                                .build();
                })
                .collect(Collectors.toList());

        return SiteListResponse.builder()
                .siteList(siteResponses)
                .build();
        }

    @Override
    public EnR002SustainabilityCommitmentResponse getSustainabilityCommitmentDefault(
            Integer siteId) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));

        ParticipatingCompany company = site.getCompany();
        String repName = company.getRepFirstName() + company.getRepLastName();

        return EnR002SustainabilityCommitmentResponse.builder()
                .companyName(company.getNameJa())
                .repName(repName)
                .repRole(company.getRepPosition())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // SUBMIT
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProjectJoinResponse submitParticipation(Integer projectId,
                                                    String companyId,
                                                    Integer userId,
                                                    EnR002JoinProjectRequest request) {
        // 1. Guard checks
        Site site = siteRepository
                .findById(request.getEquipment().getSiteId()) 
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
            throw new CustomException(ResponseConstants.FORBIDDEN,
                    ResponseConstants.MSG_UNAUTHORIZED);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_PROJECT_NOT_FOUND));

        ParticipatingCompany company = companyRepository
                .findByCompanyId(companyId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "会社情報が見つかりません。"));

        ParticipatingUser user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "ユーザー情報が見つかりません。"));

        // 2. Double submission check
        boolean alreadyJoined = participatingProjectRepository
        .existsByCompany_CompanyIdAndSiteProjectRelation_Project_Id(
                companyId, projectId);
        if (alreadyJoined)
            throw new CustomException(
                    ResponseConstants.ALREADY_SUBMITTED,
                    ResponseConstants.MSG_ALREADY_SUBMITTED);

        // 3. Validation — safety net
        validationService.validateEquipment(request.getEquipment());
        validationService.validateConfirmationItems(request.getConfirmationItems());
        validationService.validateSustainabilityCommitment(request.getSustainabilityCommitment());
        validationService.validateFinalAgreement(request.getFinalAgreement());

        // 4. Save SiteProjectRelation
        SiteProjectRelation relation = saveSiteProjectRelation(site, project, companyId);

        // 5. Save Equipment
        EnR002Equipment savedEquipment = saveEquipment(request.getEquipment(), site);

        // 6. Save EquipmentFile
        List<EnR002EquipmentFile> savedFiles = saveEquipmentFiles(
                request.getEquipment(), savedEquipment);

        // 7. Save ParticipatingProject
        ParticipatingProject participation = saveParticipatingProject(
                relation, savedEquipment, user, company);

        final Integer participationId = participation.getId();

        // 8. afterCommit
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {

                        savedFiles.forEach(fileEntity -> {
                            try {
                                String permanentKey = fileUploadSolarService
                                        .moveTempToPermanent(
                                                fileEntity.getS3Key(),
                                                savedEquipment.getId());
                                equipmentFileRepository.updateS3Key(
                                        fileEntity.getId(), permanentKey);
                            } catch (Exception e) {
                                log.error("S3 move failed. fileId={}, error={}",
                                        fileEntity.getFileId(), e.getMessage());
                            }
                        });

                        try {
                            saveConfirmationItems(request.getConfirmationItems(),
                                    participationId, companyId);
                        } catch (Exception e) {
                            log.error("ConfirmationItems save failed. participationId={}, error={}",
                                    participationId, e.getMessage());
                        }

                        try {
                            saveSustainabilityCommitment(
                                    request.getSustainabilityCommitment(),
                                    request.getFinalAgreement().getIsAgreeTerms(),
                                    participationId, companyId);
                        } catch (Exception e) {
                            log.error("SustainabilityCommitment save failed. participationId={}, error={}",
                                    participationId, e.getMessage());
                        }

                        try {
                            String pdfS3Key = pdfService.createAndUpload(
                                    request.getEquipment(),
                                    request.getConfirmationItems(),
                                    request.getSustainabilityCommitment(),
                                    participation);
                            participatingProjectRepository.updatePdfS3Key(
                                    participationId, pdfS3Key);
                        } catch (Exception e) {
                            log.error("PDF pipeline failed. participationId={}, error={}",
                                    participationId, e.getMessage());
                        }
                    }
                }
        );

        return ProjectJoinResponse.builder()
                .statusCode(ResponseConstants.OK)
                .message("プロジェクトへの参加が完了しました。")
                .participationId(participationId)
                .equipmentId(savedEquipment.getId())
                .siteProjectRelationId(relation.getId())
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — DB saves
    // ─────────────────────────────────────────────────────────────

    private SiteProjectRelation saveSiteProjectRelation(Site site, Project project,
                                                          String companyId) {
        SiteProjectRelation relation = SiteProjectRelation.builder()
                .site(site)
                .project(project)
                .activityStartDate(LocalDate.now())
                .status("OPEN")
                .build();
        relation.setCreatedBy(companyId);
        relation.setUpdatedBy(companyId);
        return siteProjectRelationRepository.save(relation);
    }

    private EnR002Equipment saveEquipment(EnR002EquipmentRequest req, Site site) {
        return equipmentRepository.save(EnR002Equipment.builder()
                .site(site)
                .equipmentOpDate(req.getEquipmentOpDate())
                .nominalMaxOutput(req.getNominalMaxOutput())
                .pcsManufacturer(req.getPcsManufacturer())
                .pcsModel(req.getPcsModel())
                .pcsUnitCount(req.getPcsUnitCount())
                .deviceNo(req.getDeviceNo())
                .pcsStandby(req.getPcsStandby())
                .hasBattery(req.getHasBattery())
                .batteryMaker(req.getBatteryMaker())
                .batteryModel(req.getBatteryModel())
                .batteryCapacity(req.getBatteryCapacity())
                .totalInvestment(req.getTotalInvestment())
                .hasSubsidy(req.getHasSubsidy())
                .subsidyName(req.getSubsidyName())
                .subsidyAmount(req.getSubsidyAmount())
                .preProjectUnitPrice(req.getPreProjectUnitPrice())
                .build());
    }

    private List<EnR002EquipmentFile> saveEquipmentFiles(EnR002EquipmentRequest req,
                                                          EnR002Equipment saved) {
        return equipmentFileRepository.saveAll(
                req.getFiles().stream()
                        .map(f -> EnR002EquipmentFile.builder()
                                .equipment(saved)
                                .fileType(f.getFileType())
                                .fileId(f.getFileId())
                                .s3Key(f.getS3Key())
                                .build())
                        .collect(Collectors.toList()));
    }

    private ParticipatingProject saveParticipatingProject(SiteProjectRelation relation,
                                                            EnR002Equipment equipment,
                                                            ParticipatingUser user,
                                                            ParticipatingCompany company) {
        ParticipatingProject participation = ParticipatingProject.builder()
                .siteProjectRelation(relation)
                .equipment(equipment)
                .user(user)
                .company(company)
                .isEligible(false)
                .applicantInkan(null) // TODO: digital inkan
                .build();
        return participatingProjectRepository.save(participation); 
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — DynamoDB saves
    // ─────────────────────────────────────────────────────────────

    private void saveConfirmationItems(EnR002ConfirmationItemRequest req,
                                        Integer participationId, String companyId) {
        EnR002ConfirmationItems items = EnR002ConfirmationItems.builder() // ← nama entity DynamoDB
                .pk("PART#" + participationId)
                .isAgreeDataUse(req.getIsAgreeDataUse())
                .isAgreeExtraData(req.getIsAgreeExtraData())
                .isAgreeTransfer(req.getIsAgreeTransfer())
                .isNotRegistered(req.getIsNotRegistered())
                .isKeidanrenParticipation(req.getIsKeidanrenParticipation())
                .isEmitter(req.getIsEmitter())
                .emitterCode(req.getEmitterCode())
                .isAdjustedEmissionsAdded(req.getIsAdjustedEmissionsAdded())
                .isSubjectToPeriodicReport(req.getIsSubjectToPeriodicReport())
                .isPeriodicReportTypeOne(req.getIsPeriodicReportTypeOne())
                .energyBusinessCode(req.getEnergyBusinessCode())
                .factoryCode(req.getFactoryCode())
                .agreedAt(Instant.now().toString())
                .createdBy(companyId)
                .build();
        confirmationRepository.save(items);
    }

    private void saveSustainabilityCommitment(EnR002SustainabilityCommitmentRequest req,
                                               Boolean isAgreeTerms,
                                               Integer participationId, String companyId) {
        EnR002SustainabilityCommitment commitment = EnR002SustainabilityCommitment.builder() // ← nama entity DynamoDB
                .pk("PART#" + participationId)
                .companyName(req.getCompanyName())
                .repName(req.getRepName())
                .repRole(req.getRepRole())
                .hasConsent(req.getHasConsent())
                .isAgreeTerms(isAgreeTerms)
                .committedAt(Instant.now().toString())
                .createdBy(companyId)
                .build();
        sustainabilityRepository.save(commitment);
    }
}