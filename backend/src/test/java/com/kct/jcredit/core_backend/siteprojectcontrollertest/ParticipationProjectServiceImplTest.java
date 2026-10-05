package com.kct.jcredit.core_backend.siteprojectcontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.ProjectJoinResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectPdfDownloadResponse;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002ApplicationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.EnR002SustainabilityCommitmentRepository;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectParticipationConfirmationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.service.NotificationMessages;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.ProjectParticipationPdfService;
import com.kct.jcredit.core_backend.service.impl.ParticipationProjectServiceImpl;
import com.kct.jcredit.core_backend.util.ParticipationSelection;
@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationProjectServiceImpl のテスト")
class ParticipationProjectServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private ParticipatingCompanyRepository companyRepository;
    @Mock private ParticipatingUserRepository userRepository;
    @Mock private SiteProjectRelationRepository siteProjectRelationRepository;
    @Mock private EnR002EquipmentRepository equipmentRepository;
    @Mock private EnR002EquipmentFileRepository equipmentFileRepository;
    @Mock private EnR002ApplicationConfirmationRepository confirmationRepository;
    @Mock private EnR002SustainabilityCommitmentRepository sustainabilityRepository;
    @Mock private ParticipatingProjectRepository participatingProjectRepository;
    @Mock private ProjectParticipationConfirmationRepository reviewRepository;
    @Mock private EnR002FileUploadService fileUploadSolarService;
    @Mock private ParticipationProjectValidationService validationService;
    @Mock private ProjectParticipationPdfService pdfService;
    @Mock private NotificationService notificationService;
    // 実バンドルを使う。文面は mail/messages.properties から読む。
    @Spy private NotificationMessages messages = new NotificationMessages();

    @InjectMocks
    private ParticipationProjectServiceImpl participationService;

    private static final String COMPANY_ID = "COMP001";
    private static final String OTHER_COMPANY_ID = "COMP999";
    private static final String SITE_ID = "PUB-SITE-001";
    private static final String PROJECT_ID = "PUB-PROJ-001";
    private static final Integer USER_ID = 5;
    private static final Integer SITE_DB_ID = 10;
    private static final Integer PROJECT_DB_ID = 20;
    private static final Integer COMPANY_DB_ID = 1;
    private static final Integer EQUIPMENT_DB_ID = 100;
    private static final Integer PARTICIPATION_ID = 77;
    private static final Integer RELATION_ID = 88;

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private Site siteOwnedBy(String companyId) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getCompanyId()).thenReturn(companyId);
        lenient().when(company.getId()).thenReturn(COMPANY_DB_ID);
        Site site = mock(Site.class);
        lenient().when(site.getCompany()).thenReturn(company);
        lenient().when(site.getId()).thenReturn(SITE_DB_ID);
        lenient().when(site.getPublicId()).thenReturn(SITE_ID);
        return site;
    }

    private Project projectMock() {
        Project project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(PROJECT_DB_ID);
        lenient().when(project.getProjectName()).thenReturn("太陽光プロジェクトA");
        return project;
    }

    private EnR002FileItemRequest fileItem(String fileId, EquipmentFileType type) {
        EnR002FileItemRequest f = new EnR002FileItemRequest();
        f.setFileId(fileId);
        f.setFileType(type);
        f.setS3Key("tmp/equipments/" + fileId + "/" + type.getValue() + "_doc.pdf");
        f.setFileName(type.getValue() + "_doc.pdf");
        return f;
    }

    private EnR002JoinProjectRequest joinRequest() {
        EnR002EquipmentRequest equipment = new EnR002EquipmentRequest();
        equipment.setSiteId(SITE_ID);
        equipment.setPcsManufacturer("MakerA");
        equipment.setPcsModel("Model-X");
        equipment.setFiles(new ArrayList<>(List.of(
                fileItem("F1", EquipmentFileType.SOLAR_CONTRACT),
                fileItem("F2", EquipmentFileType.PANEL_CATALOG))));

        EnR002FinalAgreementRequest finalAgreement = new EnR002FinalAgreementRequest();
        finalAgreement.setIsAgreeTerms(true);

        EnR002JoinProjectRequest request = new EnR002JoinProjectRequest();
        request.setEquipment(equipment);
        request.setConfirmationItems(new EnR002ConfirmationItemRequest());
        request.setSustainabilityCommitment(new EnR002SustainabilityCommitmentRequest());
        request.setFinalAgreement(finalAgreement);
        return request;
    }

    /** Stubs every repository call the happy path of submitParticipation makes. */
    private EnR002Equipment stubHappySubmitPath(Site site, Project project) {
        ParticipatingCompany userCompany = mock(ParticipatingCompany.class);
        ParticipatingUser user = mock(ParticipatingUser.class);
        lenient().when(user.getCompany()).thenReturn(userCompany);

        EnR002Equipment savedEquipment = mock(EnR002Equipment.class);
        lenient().when(savedEquipment.getId()).thenReturn(EQUIPMENT_DB_ID);
        lenient().when(savedEquipment.getSite()).thenReturn(site);

        SiteProjectRelation savedRelation = mock(SiteProjectRelation.class);
        lenient().when(savedRelation.getId()).thenReturn(RELATION_ID);

        ParticipatingProject savedParticipation = mock(ParticipatingProject.class);
        lenient().when(savedParticipation.getId()).thenReturn(PARTICIPATION_ID);

        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(Optional.empty());
        when(siteProjectRelationRepository.save(any(SiteProjectRelation.class)))
                .thenReturn(savedRelation);
        when(equipmentRepository.save(any(EnR002Equipment.class))).thenReturn(savedEquipment);
        lenient().when(fileUploadSolarService.buildPermanentKey(anyString(), eq(SITE_ID), eq(EQUIPMENT_DB_ID)))
                .thenAnswer(inv -> "sites/" + SITE_ID + "/equipments/" + EQUIPMENT_DB_ID + "/doc.pdf");
        when(equipmentFileRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(participatingProjectRepository.save(any(ParticipatingProject.class)))
                .thenReturn(savedParticipation);

        return savedEquipment;
    }

    // ==================================================================
    // 06. submitParticipation (入会届)
    // ==================================================================

    @Test
    @DisplayName("UT-SPS-06-01: 正常 - 入会届の提出が成功しOKと各IDを返す")
    void submitParticipation_validRequest_returnsOk() {
        Site site = siteOwnedBy(COMPANY_ID);
        Project project = projectMock();
        stubHappySubmitPath(site, project);

        try (MockedStatic<TransactionSynchronizationManager> tx =
                     mockStatic(TransactionSynchronizationManager.class)) {

            ProjectJoinResponse response = participationService.submitParticipation(
                    PROJECT_ID, COMPANY_ID, USER_ID, joinRequest());

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getParticipationId()).isEqualTo(PARTICIPATION_ID);
            assertThat(response.getEquipmentId()).isEqualTo(EQUIPMENT_DB_ID);
            assertThat(response.getSiteProjectRelationId()).isEqualTo(RELATION_ID);

            verify(siteProjectRelationRepository, times(1)).save(any(SiteProjectRelation.class));
            verify(equipmentRepository, times(1)).save(any(EnR002Equipment.class));
            verify(participatingProjectRepository, times(1)).save(any(ParticipatingProject.class));
        }
    }

    @Test
    @DisplayName("UT-SPS-06-02: 正常 - 4種類の入力検証が全て実行される")
    void submitParticipation_runsAllFourValidations() {
        Site site = siteOwnedBy(COMPANY_ID);
        Project project = projectMock();
        stubHappySubmitPath(site, project);
        EnR002JoinProjectRequest request = joinRequest();

        try (MockedStatic<TransactionSynchronizationManager> tx =
                     mockStatic(TransactionSynchronizationManager.class)) {

            participationService.submitParticipation(PROJECT_ID, COMPANY_ID, USER_ID, request);

            verify(validationService, times(1)).validateEquipment(request.getEquipment());
            verify(validationService, times(1)).validateConfirmationItems(request.getConfirmationItems());
            verify(validationService, times(1))
                    .validateSustainabilityCommitment(request.getSustainabilityCommitment());
            verify(validationService, times(1)).validateFinalAgreement(request.getFinalAgreement());
        }
    }

    @Test
    @DisplayName("UT-SPS-06-03: 異常 - 拠点が存在しない場合NOT_FOUNDをスローする")
    void submitParticipation_siteNotFound_throwsNotFound() {
        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.submitParticipation(
                        PROJECT_ID, COMPANY_ID, USER_ID, joinRequest()));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
        verifyNoInteractions(projectRepository, validationService, equipmentRepository);
    }

    @Test
    @DisplayName("UT-SPS-06-04: 異常 - 拠点が他社のものである場合FORBIDDENをスローする（会社スコープ検証）")
    void submitParticipation_siteFromOtherCompany_throwsForbidden() {
        Site site = siteOwnedBy(OTHER_COMPANY_ID);
        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.of(site));

        assertThrows(CustomException.class,
                () -> participationService.submitParticipation(
                        PROJECT_ID, COMPANY_ID, USER_ID, joinRequest()));

        verifyNoInteractions(projectRepository, validationService, equipmentRepository);
    }

    @Test
    @DisplayName("UT-SPS-06-05: 異常 - プロジェクトが存在しない場合NOT_FOUNDをスローする")
    void submitParticipation_projectNotFound_throwsNotFound() {
        Site site = siteOwnedBy(COMPANY_ID);
        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> participationService.submitParticipation(
                        PROJECT_ID, COMPANY_ID, USER_ID, joinRequest()));

        verifyNoInteractions(userRepository, validationService, equipmentRepository);
    }

    @Test
    @DisplayName("UT-SPS-06-06: 異常 - ユーザーが存在しない場合NOT_FOUNDをスローする")
    void submitParticipation_userNotFound_throwsNotFound() {
        Site site = siteOwnedBy(COMPANY_ID);
        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.of(site));
        // The helper stubs its own mock, so it must be built before when(...) opens a
        // stubbing - calling it inside thenReturn(...) trips UnfinishedStubbingException.
        Project project = projectMock();
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> participationService.submitParticipation(
                        PROJECT_ID, COMPANY_ID, USER_ID, joinRequest()));

        verifyNoInteractions(validationService, equipmentRepository);
    }

    @Test
    @DisplayName("UT-SPS-06-07: 異常 - 既にOPEN状態で提出済みの場合ALREADY_SUBMITTEDをスローする")
    void submitParticipation_alreadySubmitted_throwsAlreadySubmitted() {
        Site site = siteOwnedBy(COMPANY_ID);
        Project project = projectMock();
        ParticipatingProject existing = mock(ParticipatingProject.class);
        when(existing.getStatus()).thenReturn("OPEN");

        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(ParticipatingUser.class)));
        when(participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(existing));

        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.submitParticipation(
                        PROJECT_ID, COMPANY_ID, USER_ID, joinRequest()));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_ALREADY_SUBMITTED);
        verifyNoInteractions(validationService, equipmentRepository);
    }

    @Test
    @DisplayName("UT-SPS-06-08: 正常 - REJECTED状態の場合は再申請として同一設備を更新し新規relationは作らない")
    void submitParticipation_rejectedStatus_resubmitsWithoutNewRelation() {
        Site site = siteOwnedBy(COMPANY_ID);
        Project project = projectMock();

        EnR002Equipment existingEquipment = mock(EnR002Equipment.class);
        lenient().when(existingEquipment.getId()).thenReturn(EQUIPMENT_DB_ID);
        lenient().when(existingEquipment.getSite()).thenReturn(site);

        ParticipatingProject existing = mock(ParticipatingProject.class);
        when(existing.getStatus()).thenReturn("REJECTED");
        when(existing.getEquipment()).thenReturn(existingEquipment);
        lenient().when(existing.getId()).thenReturn(PARTICIPATION_ID);

        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(ParticipatingUser.class)));
        when(participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(existing));
        when(equipmentFileRepository.findByEquipment_Id(EQUIPMENT_DB_ID))
                .thenReturn(Collections.emptyList());
        lenient().when(fileUploadSolarService.buildPermanentKey(anyString(), eq(SITE_ID), eq(EQUIPMENT_DB_ID)))
                .thenReturn("sites/" + SITE_ID + "/equipments/" + EQUIPMENT_DB_ID + "/doc.pdf");
        when(equipmentFileRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(reviewRepository.findByParticipationId(PARTICIPATION_ID)).thenReturn(Optional.empty());

        try (MockedStatic<TransactionSynchronizationManager> tx =
                     mockStatic(TransactionSynchronizationManager.class)) {

            ProjectJoinResponse response = participationService.submitParticipation(
                    PROJECT_ID, COMPANY_ID, USER_ID, joinRequest());

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            // The same equipment row is updated, not a new one inserted.
            verify(equipmentRepository, times(1)).save(existingEquipment);
            // Status returns to OPEN so the review flow starts again.
            verify(existing, times(1)).setStatus("OPEN");
            // The site is already linked to the project, so no new relation is created.
            verifyNoInteractions(siteProjectRelationRepository);
        }
    }

    @Test
    @DisplayName("UT-SPS-06-09: 正常 - 再申請時に審査者情報がリセットされる")
    void submitParticipation_resubmit_resetsReviewerFields() {
        Site site = siteOwnedBy(COMPANY_ID);
        Project project = projectMock();

        EnR002Equipment existingEquipment = mock(EnR002Equipment.class);
        lenient().when(existingEquipment.getId()).thenReturn(EQUIPMENT_DB_ID);
        lenient().when(existingEquipment.getSite()).thenReturn(site);

        ParticipatingProject existing = mock(ParticipatingProject.class);
        when(existing.getStatus()).thenReturn("REJECTED");
        when(existing.getEquipment()).thenReturn(existingEquipment);
        when(existing.getId()).thenReturn(PARTICIPATION_ID);

        ProjectParticipationConfirmation review = mock(ProjectParticipationConfirmation.class);

        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID)).thenReturn(Optional.of(project));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(ParticipatingUser.class)));
        when(participatingProjectRepository
                .findByEquipment_Site_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(existing));
        when(equipmentFileRepository.findByEquipment_Id(EQUIPMENT_DB_ID))
                .thenReturn(Collections.emptyList());
        lenient().when(fileUploadSolarService.buildPermanentKey(anyString(), eq(SITE_ID), eq(EQUIPMENT_DB_ID)))
                .thenReturn("sites/x/equipments/1/doc.pdf");
        when(equipmentFileRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(reviewRepository.findByParticipationId(PARTICIPATION_ID)).thenReturn(Optional.of(review));

        try (MockedStatic<TransactionSynchronizationManager> tx =
                     mockStatic(TransactionSynchronizationManager.class)) {

            participationService.submitParticipation(PROJECT_ID, COMPANY_ID, USER_ID, joinRequest());

            verify(review).setPrimaryReviewerId(null);
            verify(review).setPrimaryApplReviewerName(null);
            verify(review).setSecondaryReviewerId(null);
            verify(review).setSecondaryApplReviewerName(null);
            verify(reviewRepository, times(1)).save(review);
        }
    }

    @Test
    @DisplayName("UT-SPS-06-10: 正常 - コミット後にファイル移動と通知が実行される")
    void submitParticipation_afterCommit_movesFilesAndNotifies() {
        Site site = siteOwnedBy(COMPANY_ID);
        Project project = projectMock();
        stubHappySubmitPath(site, project);

        EnR002EquipmentFile savedFile = mock(EnR002EquipmentFile.class);
        when(savedFile.getFileId()).thenReturn("F1");
        when(savedFile.getS3Key()).thenReturn("sites/" + SITE_ID + "/equipments/100/doc.pdf");
        when(equipmentFileRepository.saveAll(anyList())).thenReturn(List.of(savedFile));

        try (MockedStatic<TransactionSynchronizationManager> tx =
                     mockStatic(TransactionSynchronizationManager.class)) {

            participationService.submitParticipation(PROJECT_ID, COMPANY_ID, USER_ID, joinRequest());

            // Capture the registered callback and run it as the transaction manager would.
            ArgumentCaptor<TransactionSynchronization> captor =
                    ArgumentCaptor.forClass(TransactionSynchronization.class);
            tx.verify(() -> TransactionSynchronizationManager
                    .registerSynchronization(captor.capture()));
            captor.getValue().afterCommit();

            // Files are promoted from tmp to their permanent key only after commit.
            verify(fileUploadSolarService, times(1))
                    .moveToPermanent("tmp/equipments/F1/solar_contract_doc.pdf",
                            "sites/" + SITE_ID + "/equipments/100/doc.pdf");
            // Both the applicant and the operating staff are notified.
            verify(notificationService, times(1))
                    .createNotification(anyString(), anyString(), eq(USER_ID), isNull());
            verify(notificationService, times(1))
                    .createNotificationForAllOperatingUsers(anyString(), anyString());
        }
    }

    @Test
    @DisplayName("UT-SPS-06-11: 異常 - 確認事項・事業者情報はトランザクション内で保存されるべき【要修正】")
    void submitParticipation_confirmationAndCommitment_shouldBeSavedInsideTransaction() {
        // Pattern B: both writes currently happen inside afterCommit, wrapped in a
        // try/catch that only logs. A DynamoDB failure therefore leaves a participation
        // that looks successful but can never produce a PDF - generateAndProcess later
        // throws DATA_MISSING for exactly these two records.
        // They are the content of the 入会届 itself, so they must roll back with it.
        Site site = siteOwnedBy(COMPANY_ID);
        Project project = projectMock();
        stubHappySubmitPath(site, project);

        try (MockedStatic<TransactionSynchronizationManager> tx =
                     mockStatic(TransactionSynchronizationManager.class)) {

            participationService.submitParticipation(PROJECT_ID, COMPANY_ID, USER_ID, joinRequest());

            // Asserted without running afterCommit: these must already be persisted.
            verify(confirmationRepository, times(1)).save(any());
            verify(sustainabilityRepository, times(1)).save(any());
        }
    }

    // ==================================================================
    // 07. joinSite
    // ==================================================================

    private ParticipatingProject participationWithStatus(String status) {
        EnR002Equipment equipment = mock(EnR002Equipment.class);
        lenient().when(equipment.getId()).thenReturn(EQUIPMENT_DB_ID);
        ParticipatingProject participation = mock(ParticipatingProject.class);
        lenient().when(participation.getStatus()).thenReturn(status);
        lenient().when(participation.getId()).thenReturn(PARTICIPATION_ID);
        lenient().when(participation.getEquipment()).thenReturn(equipment);
        return participation;
    }

    private void stubJoinSiteLookups(ParticipatingProject participation) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getId()).thenReturn(COMPANY_DB_ID);

        Project project = projectMock();
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(participatingProjectRepository
                .findByCompanyIdAndProject_IdOrderByIdAsc(COMPANY_DB_ID, PROJECT_DB_ID))
                .thenReturn(List.of(participation));
    }

    @Test
    @DisplayName("UT-SPS-07-01: 正常 - 承認済み参加がある場合、拠点がプロジェクトに紐づけられOKを返す")
    void joinSite_approvedParticipation_savesRelation() {
        ParticipatingProject participation = participationWithStatus("APPROVED");
        stubJoinSiteLookups(participation);

        Site site = siteOwnedBy(COMPANY_ID);
        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.of(site));

        SiteProjectRelation savedRelation = mock(SiteProjectRelation.class);
        when(savedRelation.getId()).thenReturn(RELATION_ID);
        when(siteProjectRelationRepository.save(any(SiteProjectRelation.class)))
                .thenReturn(savedRelation);

        try (MockedStatic<ParticipationSelection> selection =
                     mockStatic(ParticipationSelection.class)) {
            selection.when(() -> ParticipationSelection.preferred(anyList()))
                    .thenReturn(Optional.of(participation));

            ProjectJoinResponse response =
                    participationService.joinSite(PROJECT_ID, COMPANY_ID, USER_ID, SITE_ID);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getParticipationId()).isEqualTo(PARTICIPATION_ID);
            assertThat(response.getSiteProjectRelationId()).isEqualTo(RELATION_ID);
            verify(siteProjectRelationRepository, times(1)).save(any(SiteProjectRelation.class));
        }
    }

    @Test
    @DisplayName("UT-SPS-07-02: 異常 - 参加情報が存在しない場合NOT_FOUNDをスローする")
    void joinSite_noParticipation_throwsNotFound() {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        Project project = projectMock();
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(participatingProjectRepository
                .findByCompanyIdAndProject_IdOrderByIdAsc(COMPANY_DB_ID, PROJECT_DB_ID))
                .thenReturn(Collections.emptyList());

        assertThrows(CustomException.class,
                () -> participationService.joinSite(PROJECT_ID, COMPANY_ID, USER_ID, SITE_ID));

        verifyNoInteractions(siteProjectRelationRepository);
    }

    @Test
    @DisplayName("UT-SPS-07-03: 異常 - 参加が承認待ち(OPEN)の場合、OPENコードでスローされ拠点は登録されない")
    void joinSite_openStatus_throwsOpen() {
        ParticipatingProject participation = participationWithStatus("OPEN");
        stubJoinSiteLookups(participation);

        try (MockedStatic<ParticipationSelection> selection =
                     mockStatic(ParticipationSelection.class)) {
            selection.when(() -> ParticipationSelection.preferred(anyList()))
                    .thenReturn(Optional.of(participation));

            CustomException ex = assertThrows(CustomException.class,
                    () -> participationService.joinSite(PROJECT_ID, COMPANY_ID, USER_ID, SITE_ID));

            assertThat(ex.getMessage()).contains("承認されるまで");
            verifyNoInteractions(siteRepository, siteProjectRelationRepository);
        }
    }

    @Test
    @DisplayName("UT-SPS-07-04: 異常 - 参加が却下済み(REJECTED)の場合、REJECTEDコードでスローされる")
    void joinSite_rejectedStatus_throwsRejected() {
        ParticipatingProject participation = participationWithStatus("REJECTED");
        stubJoinSiteLookups(participation);

        try (MockedStatic<ParticipationSelection> selection =
                     mockStatic(ParticipationSelection.class)) {
            selection.when(() -> ParticipationSelection.preferred(anyList()))
                    .thenReturn(Optional.of(participation));

            CustomException ex = assertThrows(CustomException.class,
                    () -> participationService.joinSite(PROJECT_ID, COMPANY_ID, USER_ID, SITE_ID));

            assertThat(ex.getMessage()).contains("再申請");
            verifyNoInteractions(siteRepository, siteProjectRelationRepository);
        }
    }

    @Test
    @DisplayName("UT-SPS-07-05: 異常 - 想定外のステータスの場合FORBIDDENをスローする")
    void joinSite_unexpectedStatus_throwsForbidden() {
        ParticipatingProject participation = participationWithStatus("SUSPENDED");
        stubJoinSiteLookups(participation);

        try (MockedStatic<ParticipationSelection> selection =
                     mockStatic(ParticipationSelection.class)) {
            selection.when(() -> ParticipationSelection.preferred(anyList()))
                    .thenReturn(Optional.of(participation));

            CustomException ex = assertThrows(CustomException.class,
                    () -> participationService.joinSite(PROJECT_ID, COMPANY_ID, USER_ID, SITE_ID));

            assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_UNAUTHORIZED);
            verifyNoInteractions(siteProjectRelationRepository);
        }
    }

    @Test
    @DisplayName("UT-SPS-07-06: 異常 - 拠点が存在しない場合NOT_FOUNDをスローする")
    void joinSite_siteNotFound_throwsNotFound() {
        ParticipatingProject participation = participationWithStatus("APPROVED");
        stubJoinSiteLookups(participation);
        when(siteRepository.findByPublicId("UNKNOWN-SITE")).thenReturn(Optional.empty());

        try (MockedStatic<ParticipationSelection> selection =
                     mockStatic(ParticipationSelection.class)) {
            selection.when(() -> ParticipationSelection.preferred(anyList()))
                    .thenReturn(Optional.of(participation));

            assertThrows(CustomException.class,
                    () -> participationService.joinSite(
                            PROJECT_ID, COMPANY_ID, USER_ID, "UNKNOWN-SITE"));

            verifyNoInteractions(siteProjectRelationRepository);
        }
    }

    @Test
    @DisplayName("UT-SPS-07-07: 異常 - 他社の拠点はプロジェクトに紐づけられないべき【要修正】")
    void joinSite_siteFromOtherCompany_shouldThrowForbidden() {
        // Pattern B: submitParticipation checks
        //   site.getCompany().getCompanyId().equals(companyId)
        // but joinSite does not. A master of company A can therefore attach company B's
        // site to project P, and that site's generation data would then be aggregated
        // into A's J-Credit registration.
        ParticipatingProject participation = participationWithStatus("APPROVED");
        stubJoinSiteLookups(participation);

        Site otherCompanySite = siteOwnedBy(OTHER_COMPANY_ID);
        when(siteRepository.findByPublicId(SITE_ID)).thenReturn(Optional.of(otherCompanySite));

        try (MockedStatic<ParticipationSelection> selection =
                     mockStatic(ParticipationSelection.class)) {
            selection.when(() -> ParticipationSelection.preferred(anyList()))
                    .thenReturn(Optional.of(participation));

            assertThrows(CustomException.class,
                    () -> participationService.joinSite(PROJECT_ID, COMPANY_ID, USER_ID, SITE_ID),
                    "他社の拠点を指定した場合はFORBIDDENで拒否されるべき");

            verify(siteProjectRelationRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("UT-SPS-07-08: 準正常 - 未知のprojectIdがorElse(0)で握り潰され参加情報なしとして報告される")
    void joinSite_unknownProject_reportedAsMissingParticipation() {
        // NOTE: projectDbId falls back to 0 instead of raising NOT_FOUND, so the caller
        // is told "participation not found" when the real cause is "project not found".
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        when(company.getId()).thenReturn(COMPANY_DB_ID);

        when(projectRepository.findByPublicIdWithFallback("UNKNOWN-PJ")).thenReturn(Optional.empty());
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(participatingProjectRepository
                .findByCompanyIdAndProject_IdOrderByIdAsc(COMPANY_DB_ID, 0))
                .thenReturn(Collections.emptyList());

        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.joinSite("UNKNOWN-PJ", COMPANY_ID, USER_ID, SITE_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
    }

    // ==================================================================
    // 08. getMembershipPdfByCompany (入会届PDF)
    // ==================================================================

    private ParticipatingProject participationWithPdfKey(String s3Key) {
        ParticipatingProject p = mock(ParticipatingProject.class);
        lenient().when(p.getS3KeyPdfMembership()).thenReturn(s3Key);
        return p;
    }

    private void stubPdfLookups(List<ParticipatingProject> participations) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getId()).thenReturn(COMPANY_DB_ID);

        Project project = projectMock();
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(companyRepository.findByCompanyId(COMPANY_ID)).thenReturn(Optional.of(company));
        when(participatingProjectRepository
                .findByCompanyIdAndProject_IdOrderByIdAsc(COMPANY_DB_ID, PROJECT_DB_ID))
                .thenReturn(participations);
    }

    @Test
    @DisplayName("UT-SPS-08-01: 正常 - PDFが存在する場合presigned URLと有効期限を返す")
    void getMembershipPdfByCompany_pdfExists_returnsUrl() {
        stubPdfLookups(List.of(participationWithPdfKey("participation/20/10/77/draft.pdf")));
        when(pdfService.generatePresignedUrl("participation/20/10/77/draft.pdf"))
                .thenReturn("https://s3.example.com/signed");

        ProjectPdfDownloadResponse response =
                participationService.getMembershipPdfByCompany(PROJECT_ID, COMPANY_ID);

        assertThat(response.getPdfUrl()).isEqualTo("https://s3.example.com/signed");
        assertThat(response.getExpiresIn()).isEqualTo(900);
    }

    @Test
    @DisplayName("UT-SPS-08-02: 準正常 - 複数の参加行がある場合、最初にPDFキーを持つ行が採用される")
    void getMembershipPdfByCompany_multipleRows_picksFirstWithKey() {
        stubPdfLookups(List.of(
                participationWithPdfKey(null),
                participationWithPdfKey("   "),
                participationWithPdfKey("participation/20/10/79/draft.pdf")));
        when(pdfService.generatePresignedUrl("participation/20/10/79/draft.pdf"))
                .thenReturn("https://s3.example.com/signed");

        ProjectPdfDownloadResponse response =
                participationService.getMembershipPdfByCompany(PROJECT_ID, COMPANY_ID);

        assertThat(response.getPdfUrl()).isEqualTo("https://s3.example.com/signed");
    }

    @Test
    @DisplayName("UT-SPS-08-03: 異常 - プロジェクトが存在しない場合NOT_FOUNDをスローする")
    void getMembershipPdfByCompany_projectNotFound_throwsNotFound() {
        when(projectRepository.findByPublicIdWithFallback("UNKNOWN-PJ")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.getMembershipPdfByCompany("UNKNOWN-PJ", COMPANY_ID));

        assertThat(ex.getMessage()).isEqualTo(ResponseConstants.MSG_PROJECT_NOT_FOUND);
        verifyNoInteractions(pdfService);
    }

    @Test
    @DisplayName("UT-SPS-08-04: 異常 - 会社情報が存在しない場合NOT_FOUNDをスローする")
    void getMembershipPdfByCompany_companyNotFound_throwsNotFound() {
        Project project = projectMock();
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));
        when(companyRepository.findByCompanyId(OTHER_COMPANY_ID)).thenReturn(Optional.empty());

        assertThrows(CustomException.class,
                () -> participationService.getMembershipPdfByCompany(PROJECT_ID, OTHER_COMPANY_ID));

        verifyNoInteractions(pdfService);
    }

    @Test
    @DisplayName("UT-SPS-08-05: 異常 - 参加情報が存在しない場合NOT_FOUNDをスローする")
    void getMembershipPdfByCompany_noParticipation_throwsNotFound() {
        stubPdfLookups(Collections.emptyList());

        assertThrows(CustomException.class,
                () -> participationService.getMembershipPdfByCompany(PROJECT_ID, COMPANY_ID));

        verifyNoInteractions(pdfService);
    }

    @Test
    @DisplayName("UT-SPS-08-06: 異常 - どの参加行にもPDFキーがない場合PDF_NOT_READYをスローする")
    void getMembershipPdfByCompany_noPdfKey_throwsPdfNotReady() {
        stubPdfLookups(List.of(participationWithPdfKey(null), participationWithPdfKey("  ")));

        CustomException ex = assertThrows(CustomException.class,
                () -> participationService.getMembershipPdfByCompany(PROJECT_ID, COMPANY_ID));

        assertThat(ex.getMessage()).contains("まだ準備できていません");
        verifyNoInteractions(pdfService);
    }

    @Test
    @DisplayName("UT-SPS-08-07: 異常 - レスポンスにstatusCodeが設定されるべき【要修正】")
    void getMembershipPdfByCompany_shouldSetStatusCode() {
        // Pattern B: the response is built without setStatusCode(OK). Since the
        // controller now maps the status through ResponseUtil.getHttpStatus(
        // response.getStatusCode()), a null status breaks the success path in production.
        stubPdfLookups(List.of(participationWithPdfKey("participation/20/10/77/draft.pdf")));
        when(pdfService.generatePresignedUrl(anyString())).thenReturn("https://s3.example.com/signed");

        ProjectPdfDownloadResponse response =
                participationService.getMembershipPdfByCompany(PROJECT_ID, COMPANY_ID);

        assertThat(response.getStatusCode())
                .as("コントローラーがResponseUtilでマッピングするため、statusCodeは必須")
                .isEqualTo(ResponseConstants.OK);
    }
}