package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002SustainabilityCommitmentResponse;
import com.kct.jcredit.core_backend.dto.response.MemberTermsInfoResponse;

import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
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
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.ConfirmationItems;
import com.kct.jcredit.core_backend.entity.EnR002ApplicationConfirmation;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002SustainabilityCommitmentRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectParticipationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;


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
    private final ProjectParticipationConfirmationRepository reviewRepository;
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
                        .map(project -> {
                                boolean alreadyJoined = siteProjectRelationRepository //TODO: change to fit youken
                                        .existsBySite_Company_CompanyIdAndProject_Id(
                                                companyId, project.getId());

                                return ProjectDetailResponse.builder()
                                        .id(project.getPublicId())
                                        .projectName(project.getProjectName())
                                        .projectNumber(project.getProjectNumber())
                                        .isCompanyJoined(alreadyJoined)
                                        .build();
                        })
                        .collect(Collectors.toList());

                return ProjectListInfoResponse.builder()
                        .projects(projectDetails)
                        .pagination(null)
                        .build();
                }
       @Override
        public SiteListResponse getSites(String companyId, String projectId) {
                List<Site> sites = siteRepository.findByCompany_CompanyId(companyId);
                Integer projectDbId = projectRepository.findByPublicIdWithFallback(projectId)
                        .map(com.kct.jcredit.core_backend.entity.Project::getId)
                        .orElse(0);

                List<SiteResponse> siteResponses = sites.stream()
                        .map(site -> {
                                boolean alreadyInProject = siteProjectRelationRepository
                                        .existsBySite_IdAndProject_Id(site.getId(), projectDbId);

                                return SiteResponse.builder()
                                        .siteId(site.getPublicId())
                                        .siteName(site.getSiteName())
                                        .postalCode(site.getPostalCode())
                                        .prefecture(site.getPrefecture())
                                        .municipality(site.getMunicipality())
                                        .blocknum(site.getBlocknum())
                                        .buildingName(site.getBuildingName())
                                        .picFirstName(site.getPicFirstName())
                                        .picLastName(site.getPicLastName())
                                        .alreadyInProject(alreadyInProject) 
                                        .build();
                        })
                        .collect(Collectors.toList());

                return SiteListResponse.builder()
                        .siteList(siteResponses)
                        .build();
                }

    @Override
    public EnR002SustainabilityCommitmentResponse getSustainabilityCommitmentDefault(
            String siteId) {
        Site site = siteRepository.findByPublicIdWithFallback(siteId)
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

       @Override
        public MemberTermsInfoResponse getMemberTermsInfo(String projectId) {

        Project project = projectRepository.findByPublicIdWithFallback(projectId)
                .orElse(null);

        if (project == null) {
                return MemberTermsInfoResponse.builder()
                        .statusCode(ResponseConstants.NOT_FOUND)
                        .message(ResponseConstants.MSG_PROJECT_NOT_FOUND)
                        .build();
        }

        OperatorCompany operatorCompany = project.getOperatorCompany();

        String adminRepName = safe(operatorCompany.getRepFirstName())
                + safe(operatorCompany.getRepLastName());

        String effectiveDate = project.getCreatedAt() != null
                ? project.getCreatedAt().toLocalDate().format(
                        DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
                : "";

        return MemberTermsInfoResponse.builder()
                .statusCode(ResponseConstants.OK)
                .projectName(safe(project.getProjectName()))
                .operatorNameJa(safe(operatorCompany.getNameJa()))
                .adminRepName(adminRepName)
                .projectOverview(safe(project.getMethodName()))
                .usePurpose(safe(project.getUsePurpose()))
                .startedAt(project.getStartedAt())
                .build();
        }

        private String safe(String v) { return v != null ? v : ""; }

    // ─────────────────────────────────────────────────────────────
    // SUBMIT
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProjectJoinResponse submitParticipation(String projectId,
                                                        String companyId,
                                                        Integer userId,
                                                        EnR002JoinProjectRequest request) {
        Site site = siteRepository
                .findByPublicId(request.getEquipment().getSiteId())
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_SITE_NOT_FOUND));

        if (!site.getCompany().getCompanyId().equals(companyId))
                throw new CustomException(ResponseConstants.FORBIDDEN,
                        ResponseConstants.MSG_UNAUTHORIZED);

        Project project = projectRepository.findByPublicIdWithFallback(projectId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND,
                        ResponseConstants.MSG_PROJECT_NOT_FOUND));

        ParticipatingUser user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "ユーザー情報が見つかりません。"));


        Optional<ParticipatingProject> existingParticipation = participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(site.getId(), project.getId());

        if (existingParticipation.isPresent()) {
        String currentStatus = existingParticipation.get().getStatus();

        if ("REJECTED".equals(currentStatus)) {
                return resubmitParticipation(existingParticipation.get(), request, companyId);
        } else {
                throw new CustomException(ResponseConstants.ALREADY_SUBMITTED,
                        ResponseConstants.MSG_ALREADY_SUBMITTED);
        }
        }

        validationService.validateEquipment(request.getEquipment());
        validationService.validateConfirmationItems(request.getConfirmationItems());
        validationService.validateSustainabilityCommitment(request.getSustainabilityCommitment());
        validationService.validateFinalAgreement(request.getFinalAgreement());

        SiteProjectRelation relation = saveSiteProjectRelation(site, project, companyId);
        EnR002Equipment savedEquipment = saveEquipment(request.getEquipment(), site);
        List<EnR002EquipmentFile> savedFiles = saveEquipmentFiles(request.getEquipment(), savedEquipment);

        // Key tmp disimpan terpisah — DB sudah memegang key final sejak commit.
        Map<String, String> tempKeyByFileId = request.getEquipment().getFiles().stream()
                .collect(Collectors.toMap(f -> f.getFileId(), f -> f.getS3Key()));

        // 7. Save ParticipatingProject
        ParticipatingProject participation = saveParticipatingProject(
            project, user.getCompany(), savedEquipment);

        final Integer participationId = participation.getId();

        // 8. afterCommit
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                savedFiles.forEach(fileEntity -> {
                    try {
                        fileUploadSolarService.moveToPermanent(
                                tempKeyByFileId.get(fileEntity.getFileId()),
                                fileEntity.getS3Key());
                    } catch (Exception e) {
                        log.error("S3 move failed. fileId={}, s3Key={}, error={}",
                                fileEntity.getFileId(), fileEntity.getS3Key(), e.getMessage());
                                }
                        });

                        try {
                                saveConfirmationItems(request.getConfirmationItems(), participationId, companyId);
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

                        // try {
                        //         String pdfS3Key = pdfService.createAndUpload(
                        //                 request.getEquipment(),
                        //                 request.getConfirmationItems(),
                        //                 request.getSustainabilityCommitment(),
                        //                 participation);
                        //         participatingProjectRepository.updatePdfS3Key(participationId, pdfS3Key);
                        // } catch (Exception e) {
                        //         log.error("PDF pipeline failed. participationId={}, error={}",
                        //                 participationId, e.getMessage());
                        //         }
                        }
                }
        );   
        ProjectJoinResponse response =ProjectJoinResponse.builder()
                .participationId(participationId)
                .equipmentId(savedEquipment.getId())
                .siteProjectRelationId(relation.getId())
                .build();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("プロジェクトへの参加が完了しました。");
        return response;
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
                // .pcsUnitCount(req.getPcsUnitCount())
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
                                // key final, bukan key tmp — lihat submitParticipation()
                                .s3Key(fileUploadSolarService.buildPermanentKey(
                                        f.getS3Key(),
                                        saved.getSite().getPublicId(),
                                        saved.getId()))
                                .build())
                        .collect(Collectors.toList()));
    }

    private ParticipatingProject saveParticipatingProject(Project project,
                                                                ParticipatingCompany company,
                                                                EnR002Equipment equipment) {
        ParticipatingProject participation = ParticipatingProject.builder()
                .project(project)
                .company(company)
                .equipment(equipment)
                .isEligible(false)
                .status("OPEN")
                .build();
        return participatingProjectRepository.save(participation);
        }

//      @Override
//      public List<MyParticipationResponse> getMyParticipations(String projectId, String companyId) {

//         Integer projectDbId = projectRepository.findByPublicIdWithFallback(projectId)
//                 .map(Project::getId)
//                 .orElseThrow(() -> new CustomException(
//                         ResponseConstants.NOT_FOUND, ResponseConstants.MSG_PROJECT_NOT_FOUND));

//         List<ParticipatingProject> participations = participatingProjectRepository
//                 .findByProject_IdAndEquipment_Site_Company_CompanyId(projectDbId, companyId);                
        
//         return participations.stream()
//                 .filter(p -> "APPROVED".equals(p.getStatus()))
//                 .map(p -> MyParticipationResponse.builder()
//                 .participationId(p.getId())
//                 .siteId(p.getSiteId())
//                 .siteName(p.get)
//                 .status(p.getStatus())
//                 .pdfStatus(p.getS3KeyPdfMembership)
//                 .build())
//         .collect(Collectors.toList());

    // ─────────────────────────────────────────────────────────────
    // PRIVATE — DynamoDB saves
    // ─────────────────────────────────────────────────────────────

    private void saveConfirmationItems(EnR002ConfirmationItemRequest req,
                                        Integer participationId, String companyId) {
        EnR002ApplicationConfirmation items = EnR002ApplicationConfirmation.builder() 
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
        EnR002SustainabilityCommitment commitment = EnR002SustainabilityCommitment.builder() 
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

    private ProjectJoinResponse resubmitParticipation(ParticipatingProject participation,
                                                    EnR002JoinProjectRequest request,
                                                    String companyId) {

        validationService.validateEquipment(request.getEquipment());
        validationService.validateConfirmationItems(request.getConfirmationItems());
        validationService.validateSustainabilityCommitment(request.getSustainabilityCommitment());
        validationService.validateFinalAgreement(request.getFinalAgreement());
        EnR002Equipment equipment = participation.getEquipment();

        // ── UPDATE equipment yang SAMA (bukan insert baru) ─────────────
        equipment.setEquipmentOpDate(request.getEquipment().getEquipmentOpDate());
        equipment.setNominalMaxOutput(request.getEquipment().getNominalMaxOutput());
        equipment.setPcsManufacturer(request.getEquipment().getPcsManufacturer());
        equipment.setPcsModel(request.getEquipment().getPcsModel());
        // equipment.setPcsUnitCount(request.getEquipment().getPcsUnitCount());
        equipment.setDeviceNo(request.getEquipment().getDeviceNo());
        equipment.setPcsStandby(request.getEquipment().getPcsStandby());
        equipment.setHasBattery(request.getEquipment().getHasBattery());
        equipment.setBatteryMaker(request.getEquipment().getBatteryMaker());
        equipment.setBatteryModel(request.getEquipment().getBatteryModel());
        equipment.setBatteryCapacity(request.getEquipment().getBatteryCapacity());
        equipment.setTotalInvestment(request.getEquipment().getTotalInvestment());
        equipment.setHasSubsidy(request.getEquipment().getHasSubsidy());
        equipment.setSubsidyName(request.getEquipment().getSubsidyName());
        equipment.setSubsidyAmount(request.getEquipment().getSubsidyAmount());
        equipment.setPreProjectUnitPrice(request.getEquipment().getPreProjectUnitPrice());
        equipmentRepository.save(equipment);

        // ── Replace ─────────────────────────
        List<EnR002EquipmentFile> oldFiles = equipmentFileRepository.findByEquipment_Id(equipment.getId());
        List<String> oldS3Keys = oldFiles.stream()
                .map(EnR002EquipmentFile::getS3Key)
                .collect(Collectors.toList());
        equipmentFileRepository.deleteAll(oldFiles);

        List<EnR002EquipmentFile> savedFiles = saveEquipmentFiles(request.getEquipment(), equipment);

        // Key tmp 
        Map<String, String> tempKeyByFileId = request.getEquipment().getFiles().stream()
                .collect(Collectors.toMap(f -> f.getFileId(), f -> f.getS3Key()));

        // ── Reset status relation → OPEN ─────────────────────────────────
        participation.setStatus("OPEN");
        participatingProjectRepository.save(participation);

        final Integer participationId = participation.getId();
        final Integer equipmentId = equipment.getId();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                        oldS3Keys.forEach(oldKey -> {
                                try {
                                        fileUploadSolarService.deletePermanentFile(oldKey, equipmentId);
                                } catch (Exception e) {
                                        log.error("S3 delete failed. s3Key={}, error={}",
                                                oldKey, e.getMessage());
                                }
                        });

                        savedFiles.forEach(fileEntity -> {
                                try {
                                        fileUploadSolarService.moveToPermanent(
                                                tempKeyByFileId.get(fileEntity.getFileId()),
                                                fileEntity.getS3Key());
                                } catch (Exception e) {
                                        log.error("S3 move failed. fileId={}, s3Key={}, error={}",
                                                fileEntity.getFileId(), fileEntity.getS3Key(),
                                                e.getMessage());
                                }
                        });

                        try {
                                saveConfirmationItems(request.getConfirmationItems(), participationId, companyId);
                        } catch (Exception e) {
                                log.error("ConfirmationItems save failed. error={}", e.getMessage());
                        }

                        try {
                                saveSustainabilityCommitment(request.getSustainabilityCommitment(),
                                        request.getFinalAgreement().getIsAgreeTerms(), participationId, companyId);
                        } catch (Exception e) {
                                log.error("SustainabilityCommitment save failed. error={}", e.getMessage());
                        }
                        }
                }
        );

        reviewRepository.findByParticipationId(participation.getId())
        .ifPresent(confirmation -> {
            confirmation.setPrimaryReviewerId(null);
            confirmation.setPrimaryApplReviewerName(null);
            confirmation.setPrimaryApplReviewerInkan(null);
            confirmation.setSecondaryReviewerId(null);
            confirmation.setSecondaryApplReviewerName(null);
            confirmation.setSecondaryApplReviewerInkan(null);
            reviewRepository.save(confirmation);
        });
        ProjectJoinResponse response =ProjectJoinResponse.builder()
                                                        .participationId(participationId)
                                                        .equipmentId(equipmentId)
                                                        //.siteProjectRelationId() //TODO: do we really need to return this value
                                                        .build();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("再申請が完了しました。");
        return response;
        }

        @Override
        public ProjectPdfDownloadResponse getMembershipPdfByCompany(String projectId, String companyId) {

        Integer projectDbId = projectRepository.findByPublicIdWithFallback(projectId)
                .map(Project::getId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, ResponseConstants.MSG_PROJECT_NOT_FOUND));

        Integer companyDbId = companyRepository.findByCompanyId(companyId)
                .map(ParticipatingCompany::getId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "会社情報が見つかりません。"));

        ParticipatingProject participation = participatingProjectRepository
                .findByCompanyIdAndProject_Id(companyDbId, projectDbId)
                .orElseThrow(() -> new CustomException(
                        ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

        String s3Key = participation.getS3KeyPdfMembership();
        if (s3Key == null || s3Key.isBlank()) {
                throw new CustomException("PDF_NOT_READY",
                        "PDFはまだ準備できていません。審査完了までしばらくお待ちください。");
        }

        String presignedUrl = pdfService.generatePresignedUrl(s3Key);

        return ProjectPdfDownloadResponse.builder()
                .pdfUrl(presignedUrl)
                .expiresIn(900)
                .build();
        }



        @Override
        @Transactional
    public  ProjectJoinResponse joinSite(String projectId,
                                        String companyId,
                                        Integer userId,
                                        String siteId) {
                        // first check that its the site has joined the project through the participating_project
                        //get the db id for project and company
              
                        Integer projectDbId = projectRepository.findByPublicIdWithFallback(projectId)
                        .map(com.kct.jcredit.core_backend.entity.Project::getId)
                        .orElse(0);
                        Integer companyDbId = //get the db id for company
                        companyRepository.findByCompanyId(companyId)
                                .map(com.kct.jcredit.core_backend.entity.ParticipatingCompany::getId)
                                .orElse(0);

                        Optional<ParticipatingProject> participationOpt = participatingProjectRepository
                                .findByCompanyIdAndProject_Id(companyDbId, projectDbId);
                        
                        //if null there is no link to this project, return error
                        if (!participationOpt.isPresent()) {
                                throw new CustomException(
                                ResponseConstants.NOT_FOUND,
                                ResponseConstants.MSG_NOT_FOUND);
                        }
                        ParticipatingProject participation = participationOpt.get();
                        //if exists check the status only allow new site to join if the status is eligible
                        if (!participation.getStatus().equals("APPROVED")) {
                                if(participationOpt.get().getStatus().equals("REJECTED")){
                                        throw new CustomException(
                                        ResponseConstants.REJECTED,
                                        "プロジェクト参加申請が却下されました。\n" + 
                                        "お手数ですが、入会届を再申請していただけますと幸いです。");
                                
                                } else if(participationOpt.get().getStatus().equals("OPEN")){
                                         throw new CustomException(
                                        ResponseConstants.OPEN,
                                        "プロジェクト参加申請が承認されるまで、サイトの参加はできません。");
                                } else {
                                         throw new CustomException(
                                        ResponseConstants.FORBIDDEN,
                                        ResponseConstants.MSG_UNAUTHORIZED);
                                }
                                
                        }
                        Site site = siteRepository
                                .findByPublicId(siteId)
                                .orElseThrow(() -> new CustomException(
                                        ResponseConstants.NOT_FOUND,
                                        ResponseConstants.MSG_SITE_NOT_FOUND));
                        Project project = projectRepository.findByPublicIdWithFallback(projectId)
                                .orElseThrow(() -> new CustomException(
                                        ResponseConstants.NOT_FOUND,
                                        ResponseConstants.MSG_PROJECT_NOT_FOUND));
                        
                        SiteProjectRelation relation = saveSiteProjectRelation(site, project, companyId);

                        ProjectJoinResponse response = ProjectJoinResponse.builder()       
                                                        .participationId(participation.getId())
                                                        .equipmentId(participation.getEquipment().getId())
                                                        .siteProjectRelationId(relation.getId())
                                                        .partipationStatus(participation.getStatus())
                                                        .build();

                        response.setStatusCode(ResponseConstants.OK);
                        response.setParticipationId(participationOpt.get().getId());
                        response.setSiteProjectRelationId(relation.getId());
                        response.setMessage("サイトの参加が完了しました。");

                        return response;



        }

//     @Override
//         public ProjectPdfDownloadResponse getPdfDownloadUrl(Integer participationId, String companyId) {
//         ParticipatingProject participation = participatingProjectRepository
//                 .findById(participationId)
//                 .orElseThrow(() -> new CustomException(
//                         ResponseConstants.NOT_FOUND, "参加情報が見つかりません。"));

//         // Auth check via Participation → User → Company
//         if (!participation.getUser().getCompany().getCompanyId().equals(companyId))
//                 throw new CustomException(ResponseConstants.FORBIDDEN,
//                         ResponseConstants.MSG_UNAUTHORIZED);

//         if (participation.getS3KeyPdfMembership() == null
//                 || participation.getS3KeyPdfMembership().isBlank()) {
//                 throw new CustomException("PDF_NOT_READY",
//                         "PDFを生成中です。しばらくしてから再度お試しください。");
//         }

//         String presignedUrl = pdfService.generatePresignedUrl(participation.getS3KeyPdfMembership());

//         return ProjectPdfDownloadResponse.builder()
//                 .pdfUrl(presignedUrl)
//                 .expiresIn(900)
//                 .build();
//         }
}
