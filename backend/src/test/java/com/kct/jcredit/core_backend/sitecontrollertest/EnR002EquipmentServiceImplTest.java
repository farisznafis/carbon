package com.kct.jcredit.core_backend.sitecontrollertest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.transaction.reactive.TransactionSynchronization;
// import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentChangeRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FileItemRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentHistoryListResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentListResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.EnR002FileUploadService;
import com.kct.jcredit.core_backend.service.ParticipationProjectValidationService;
import com.kct.jcredit.core_backend.service.impl.EnR002EquipmentServiceImpl;
import com.kct.jcredit.core_backend.service.impl.ParticipationProjectValidationServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnR002EquipmentServiceImpl のテスト")
class EnR002EquipmentServiceImplTest {

    @Mock
    private SiteRepository siteRepository;
    @Mock
    private SiteProjectRelationRepository siteProjectRelationRepository;
    @Mock
    private ParticipatingProjectRepository participatingProjectRepository;
    @Mock
    private EnR002EquipmentRepository equipmentRepository;
    @Mock
    private EnR002EquipmentFileRepository equipmentFileRepository;
    @Mock
    private EnR002FileUploadService fileUploadService;
    @Mock
    private ParticipationProjectValidationService validationService;
    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private EnR002EquipmentServiceImpl equipmentServiceImpl;

    private static final String COMPANY_ID = "COMP001";
    private static final String OTHER_COMPANY_ID = "COMP999";
    private static final String SITE_ID = "PUB-SITE-001";
    private static final String PROJECT_ID = "PUB-PROJ-001";
    private static final Integer SITE_DB_ID = 10;
    private static final Integer PROJECT_DB_ID = 20;
    private static final Integer EQUIPMENT_ID = 100;

    // Two distinct file types, taken positionally so the test does not depend on
    // specific enum constant names. Replace with the real constants
    // (e.g. EquipmentFileType.XXX) once confirmed, for readability.
    private static final EquipmentFileType FILE_TYPE_A = EquipmentFileType.values()[0];
    private static final EquipmentFileType FILE_TYPE_B = EquipmentFileType.values()[1];

    /** Builds a Site mock whose company matches the given companyId. */
    private Site siteWithCompany(String companyId) {
        ParticipatingCompany company = mock(ParticipatingCompany.class);
        lenient().when(company.getCompanyId()).thenReturn(companyId);
        Site site = mock(Site.class);
        lenient().when(site.getCompany()).thenReturn(company);
        lenient().when(site.getId()).thenReturn(SITE_DB_ID);
        lenient().when(site.getPublicId()).thenReturn(SITE_ID);
        return site;
    }

    /** Stubs everything toResponse() needs for the given equipment. */
    private void stubToResponse(EnR002Equipment equipment, Integer id, Site site) {
        lenient().when(equipment.getId()).thenReturn(id);
        lenient().when(equipment.getSite()).thenReturn(site);
        lenient().when(equipment.getPreviousEquipment()).thenReturn(null);
        lenient().when(equipmentFileRepository.findByEquipment_Id(id))
                .thenReturn(Collections.emptyList());
        lenient().when(participatingProjectRepository.findByEquipment_Id(id))
                .thenReturn(Collections.emptyList());
    }

    // ==================================================================
    // 09. addEquipment
    // ==================================================================

    @Test
    @DisplayName("UT-STS-09-01: 正常 - ファイルなしで設備が保存されOKを返す（保存内容を検証）")
    void addEquipment_validRequestNoFiles_savesAndReturnsOk() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        request.setPcsManufacturer("MakerA");
        request.setPcsModel("Model-X");
        request.setFiles(null);

        Site site = siteWithCompany(COMPANY_ID);
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(PROJECT_DB_ID);

        EnR002Equipment saved = mock(EnR002Equipment.class);
        stubToResponse(saved, 500, site);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicId(PROJECT_ID)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.findBySite_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(mock(SiteProjectRelation.class)));
        when(equipmentRepository.save(any(EnR002Equipment.class))).thenReturn(saved);
        when(equipmentFileRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.addEquipment(SITE_ID, COMPANY_ID, PROJECT_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

        ArgumentCaptor<EnR002Equipment> captor = ArgumentCaptor.forClass(EnR002Equipment.class);
        verify(equipmentRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getPcsManufacturer()).isEqualTo("MakerA");
        assertThat(captor.getValue().getPcsModel()).isEqualTo("Model-X");
        // The field-level validation helper must be invoked before saving.
        verify(validationService, times(1)).validateEquipment(request);
    }

    @Test
    @DisplayName("UT-STS-09-02: 正常 - ファイルありの場合、恒久キーが生成されファイルが保存される")
    void addEquipment_withFiles_savesFilesWithPermanentKey() {
        EnR002FileItemRequest fileItem = new EnR002FileItemRequest();
        fileItem.setFileId("F1");
        fileItem.setS3Key("tmp/equipments/abc.pdf");

        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        request.setFiles(List.of(fileItem));

        Site site = siteWithCompany(COMPANY_ID);
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(PROJECT_DB_ID);

        EnR002Equipment saved = mock(EnR002Equipment.class);
        stubToResponse(saved, 500, site);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicId(PROJECT_ID)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.findBySite_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(mock(SiteProjectRelation.class)));
        when(equipmentRepository.save(any(EnR002Equipment.class))).thenReturn(saved);
        when(fileUploadService.buildPermanentKey("tmp/equipments/abc.pdf", SITE_ID, 500))
                .thenReturn("equipments/10/500/abc.pdf");
        when(equipmentFileRepository.saveAll(anyList()))
                .thenReturn(List.of(mock(EnR002EquipmentFile.class)));

        try (MockedStatic<TransactionSynchronizationManager> txManager =
                     mockStatic(TransactionSynchronizationManager.class)) {

            EnR002EquipmentResponse response =
                    equipmentServiceImpl.addEquipment(SITE_ID, COMPANY_ID, PROJECT_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(fileUploadService, times(1))
                    .buildPermanentKey("tmp/equipments/abc.pdf", SITE_ID, 500);
            // The S3 move is deferred until after commit, never inside the transaction.
            verify(fileUploadService, never()).moveToPermanent(anyString(), anyString());
            txManager.verify(() -> TransactionSynchronizationManager
                    .registerSynchronization(any(TransactionSynchronization.class)), times(1));
        }
    }

    @Test
    @DisplayName("UT-STS-09-03: 異常 - 拠点が存在しない場合NOT_FOUNDを返し以降の処理は行わない")
    void addEquipment_siteNotFound_returnsNotFound() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.addEquipment("UNKNOWN", COMPANY_ID, PROJECT_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
        verifyNoInteractions(projectRepository, equipmentRepository, validationService);
    }

    @Test
    @DisplayName("UT-STS-09-04: 異常 - プロジェクトが存在しない場合NOT_FOUNDを返す")
    void addEquipment_projectNotFound_returnsNotFound() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        Site site = siteWithCompany(COMPANY_ID);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicId("UNKNOWN-PJ")).thenReturn(Optional.empty());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.addEquipment(SITE_ID, COMPANY_ID, "UNKNOWN-PJ", request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_PROJECT_NOT_FOUND);
        verifyNoInteractions(equipmentRepository, validationService);
    }

    @Test
    @DisplayName("UT-STS-09-05: 異常 - 拠点が他社のものである場合FORBIDDENを返す（会社スコープ検証）")
    void addEquipment_otherCompanySite_returnsForbidden() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        Site site = siteWithCompany(OTHER_COMPANY_ID);
        Project project = mock(Project.class);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicId(PROJECT_ID)).thenReturn(Optional.of(project));

        EnR002EquipmentResponse response =
                equipmentServiceImpl.addEquipment(SITE_ID, COMPANY_ID, PROJECT_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_UNAUTHORIZED);
        verifyNoInteractions(siteProjectRelationRepository, equipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-09-06: 異常 - 拠点がプロジェクトに未参加の場合SITE_NOT_JOINEDを返す")
    void addEquipment_siteNotJoined_returnsSiteNotJoined() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        Site site = siteWithCompany(COMPANY_ID);
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(PROJECT_DB_ID);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicId(PROJECT_ID)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.findBySite_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(Optional.empty());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.addEquipment(SITE_ID, COMPANY_ID, PROJECT_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.SITE_NOT_JOINED);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_JOINED);
        verifyNoInteractions(equipmentRepository, validationService);
    }

    @Test
    @DisplayName("UT-STS-09-07: 異常 - 保存時に例外が発生した場合INTERNAL_SERVER_ERRORを返す")
    void addEquipment_saveThrows_returnsInternalServerError() {
        EnR002EquipmentRequest request = new EnR002EquipmentRequest();
        Site site = siteWithCompany(COMPANY_ID);
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(PROJECT_DB_ID);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(projectRepository.findByPublicId(PROJECT_ID)).thenReturn(Optional.of(project));
        when(siteProjectRelationRepository.findBySite_IdAndProject_Id(SITE_DB_ID, PROJECT_DB_ID))
                .thenReturn(Optional.of(mock(SiteProjectRelation.class)));
        when(equipmentRepository.save(any(EnR002Equipment.class)))
                .thenThrow(new RuntimeException("DB error"));

        EnR002EquipmentResponse response =
                equipmentServiceImpl.addEquipment(SITE_ID, COMPANY_ID, PROJECT_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
    }

    // ==================================================================
    // 10. getEquipmentsBySite
    // ==================================================================

    @Test
    @DisplayName("UT-STS-10-01: 正常 - 設備が存在する場合OKと設備一覧を返す")
    void getEquipmentsBySite_hasEquipments_returnsOk() {
        Site site = siteWithCompany(COMPANY_ID);

        EnR002Equipment eq1 = mock(EnR002Equipment.class);
        EnR002Equipment eq2 = mock(EnR002Equipment.class);
        stubToResponse(eq1, 101, site);
        stubToResponse(eq2, 102, site);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findBySite_Id(SITE_DB_ID)).thenReturn(List.of(eq1, eq2));

        EnR002EquipmentListResponse response =
                equipmentServiceImpl.getEquipmentsBySite(SITE_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getEquipments()).hasSize(2);
        assertThat(response.getEquipments().get(0).getId()).isEqualTo(101);
    }

    @Test
    @DisplayName("UT-STS-10-02: 準正常 - 設備が0件の場合OKと空リストを返す")
    void getEquipmentsBySite_noEquipments_returnsOkWithEmptyList() {
        Site site = siteWithCompany(COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findBySite_Id(SITE_DB_ID)).thenReturn(Collections.emptyList());

        EnR002EquipmentListResponse response =
                equipmentServiceImpl.getEquipmentsBySite(SITE_ID, COMPANY_ID);

        // NOTE: 拠点一覧(endpoint 01)は0件をNOT_FOUND扱いにするが、ここは空リスト+OK。
        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getEquipments()).isEmpty();
    }

    @Test
    @DisplayName("UT-STS-10-03: 異常 - 拠点が存在しない場合NOT_FOUNDを返す")
    void getEquipmentsBySite_siteNotFound_returnsNotFound() {
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        EnR002EquipmentListResponse response =
                equipmentServiceImpl.getEquipmentsBySite("UNKNOWN", COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verifyNoInteractions(equipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-10-04: 異常 - 拠点が他社のものである場合FORBIDDENを返す（会社スコープ検証）")
    void getEquipmentsBySite_otherCompanySite_returnsForbidden() {
        Site site = siteWithCompany(OTHER_COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        EnR002EquipmentListResponse response =
                equipmentServiceImpl.getEquipmentsBySite(SITE_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verifyNoInteractions(equipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-10-05: 異常 - リポジトリが例外をスローする場合INTERNAL_SERVER_ERRORを返す")
    void getEquipmentsBySite_repositoryThrows_returnsInternalServerError() {
        when(siteRepository.findByPublicIdWithFallback(SITE_ID))
                .thenThrow(new RuntimeException("DB error"));

        EnR002EquipmentListResponse response =
                equipmentServiceImpl.getEquipmentsBySite(SITE_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
    }

    // ==================================================================
    // 11. getEquipmentById
    // ==================================================================

    @Test
    @DisplayName("UT-STS-11-01: 正常 - 設備が存在し拠点に属する場合OKと設備情報を返す")
    void getEquipmentById_validEquipment_returnsOk() {
        Site site = siteWithCompany(COMPANY_ID);
        EnR002Equipment equipment = mock(EnR002Equipment.class);
        stubToResponse(equipment, EQUIPMENT_ID, site);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));

        EnR002EquipmentResponse response =
                equipmentServiceImpl.getEquipmentById(SITE_ID, EQUIPMENT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getId()).isEqualTo(EQUIPMENT_ID);
    }

    @Test
    @DisplayName("UT-STS-11-02: 異常 - 拠点が存在しない場合NOT_FOUNDを返す")
    void getEquipmentById_siteNotFound_returnsNotFound() {
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.getEquipmentById("UNKNOWN", EQUIPMENT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
        verifyNoInteractions(equipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-11-03: 異常 - 拠点が他社のものである場合FORBIDDENを返す")
    void getEquipmentById_otherCompanySite_returnsForbidden() {
        Site site = siteWithCompany(OTHER_COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        EnR002EquipmentResponse response =
                equipmentServiceImpl.getEquipmentById(SITE_ID, EQUIPMENT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verifyNoInteractions(equipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-11-04: 異常 - 設備が存在しない場合NOT_FOUND(EQUIPMENT)を返す")
    void getEquipmentById_equipmentNotFound_returnsNotFound() {
        Site site = siteWithCompany(COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(999)).thenReturn(Optional.empty());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.getEquipmentById(SITE_ID, 999, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("UT-STS-11-05: 異常 - 設備が別拠点に属する場合NOT_FOUNDを返す（拠点跨ぎアクセス防止）")
    void getEquipmentById_equipmentFromOtherSite_returnsNotFound() {
        Site site = siteWithCompany(COMPANY_ID);
        Site otherSite = mock(Site.class);
        when(otherSite.getId()).thenReturn(77);

        EnR002Equipment equipment = mock(EnR002Equipment.class);
        when(equipment.getSite()).thenReturn(otherSite);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(equipment));

        EnR002EquipmentResponse response =
                equipmentServiceImpl.getEquipmentById(SITE_ID, EQUIPMENT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND);
    }

    // ==================================================================
    // 12. changeEquipment
    // ==================================================================

    @Test
    @DisplayName("UT-STS-12-01: 正常 - 未指定項目は旧設備から継承され、新世代が保存される")
    void changeEquipment_validRequest_inheritsUnspecifiedFields() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        request.setChangeDate(LocalDate.of(2026, 6, 1));
        request.setPcsManufacturer("NewMaker"); // specified -> use new value
        request.setPcsModel(null);              // not specified -> inherit

        Site site = siteWithCompany(COMPANY_ID);

        EnR002Equipment oldEquipment = mock(EnR002Equipment.class);
        when(oldEquipment.getSite()).thenReturn(site);
        when(oldEquipment.getChangeDate()).thenReturn(null);
        when(oldEquipment.getEquipmentOpDate()).thenReturn(LocalDate.of(2025, 1, 1));
        when(oldEquipment.getPcsModel()).thenReturn("OldModel");

        EnR002Equipment savedNew = mock(EnR002Equipment.class);
        stubToResponse(savedNew, 200, site);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(oldEquipment));
        when(equipmentRepository.findByPreviousEquipment_Id(EQUIPMENT_ID)).thenReturn(Optional.empty());
        when(equipmentRepository.save(any(EnR002Equipment.class))).thenReturn(savedNew);
        when(equipmentFileRepository.findByEquipment_Id(EQUIPMENT_ID)).thenReturn(Collections.emptyList());
        when(equipmentFileRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        try (MockedStatic<ParticipationProjectValidationServiceImpl.EquipmentUpdateValidator> validator =
                     mockStatic(ParticipationProjectValidationServiceImpl.EquipmentUpdateValidator.class)) {

            EnR002EquipmentResponse response = equipmentServiceImpl.changeEquipment(
                    SITE_ID, EQUIPMENT_ID, COMPANY_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

            ArgumentCaptor<EnR002Equipment> captor = ArgumentCaptor.forClass(EnR002Equipment.class);
            verify(equipmentRepository, times(1)).save(captor.capture());
            EnR002Equipment built = captor.getValue();
            assertThat(built.getPcsManufacturer()).isEqualTo("NewMaker");
            assertThat(built.getPcsModel()).isEqualTo("OldModel");         // inherited
            assertThat(built.getEquipmentOpDate()).isEqualTo(LocalDate.of(2025, 1, 1)); // never changed
            assertThat(built.getChangeDate()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(built.getPreviousEquipment()).isEqualTo(oldEquipment);
        }
    }

    @Test
    @DisplayName("UT-STS-12-02: 正常 - 差し替え対象のfileTypeは継承されず、それ以外は継承される")
    void changeEquipment_inheritsOnlyNonReplacedFileTypes() {
        EnR002FileItemRequest newFile = new EnR002FileItemRequest();
        newFile.setFileId("F-NEW");
        newFile.setFileType(FILE_TYPE_A);
        newFile.setS3Key("tmp/equipments/new.pdf");

        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        request.setChangeDate(LocalDate.of(2026, 6, 1));
        request.setFiles(List.of(newFile));

        Site site = siteWithCompany(COMPANY_ID);

        EnR002Equipment oldEquipment = mock(EnR002Equipment.class);
        when(oldEquipment.getSite()).thenReturn(site);
        when(oldEquipment.getChangeDate()).thenReturn(null);
        when(oldEquipment.getEquipmentOpDate()).thenReturn(LocalDate.of(2025, 1, 1));

        EnR002Equipment savedNew = mock(EnR002Equipment.class);
        stubToResponse(savedNew, 200, site);

        // Old file of the SAME type as the replacement -> must NOT be inherited.
        EnR002EquipmentFile replacedOld = mock(EnR002EquipmentFile.class);
        when(replacedOld.getFileType()).thenReturn(FILE_TYPE_A);
        // Old file of a DIFFERENT type -> must be inherited.
        EnR002EquipmentFile keptOld = mock(EnR002EquipmentFile.class);
        when(keptOld.getFileType()).thenReturn(FILE_TYPE_B);
        when(keptOld.getFileId()).thenReturn("F-OLD");
        when(keptOld.getS3Key()).thenReturn("equipments/10/100/old.jpg");

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(oldEquipment));
        when(equipmentRepository.findByPreviousEquipment_Id(EQUIPMENT_ID)).thenReturn(Optional.empty());
        when(equipmentRepository.save(any(EnR002Equipment.class))).thenReturn(savedNew);
        when(equipmentFileRepository.findByEquipment_Id(EQUIPMENT_ID))
                .thenReturn(List.of(replacedOld, keptOld));
        when(equipmentFileRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(equipmentFileRepository.save(any(EnR002EquipmentFile.class)))
                .thenReturn(mock(EnR002EquipmentFile.class));
        when(fileUploadService.buildPermanentKey("tmp/equipments/new.pdf", SITE_ID, 200))
                .thenReturn("equipments/10/200/new.pdf");

        try (MockedStatic<ParticipationProjectValidationServiceImpl.EquipmentUpdateValidator> validator =
                     mockStatic(ParticipationProjectValidationServiceImpl.EquipmentUpdateValidator.class);
             MockedStatic<TransactionSynchronizationManager> txManager =
                     mockStatic(TransactionSynchronizationManager.class)) {

            EnR002EquipmentResponse response = equipmentServiceImpl.changeEquipment(
                    SITE_ID, EQUIPMENT_ID, COMPANY_ID, request);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

            ArgumentCaptor<List<EnR002EquipmentFile>> inheritedCaptor = ArgumentCaptor.forClass(List.class);
            verify(equipmentFileRepository, atLeastOnce()).saveAll(inheritedCaptor.capture());
            List<EnR002EquipmentFile> inherited = inheritedCaptor.getAllValues().get(0);
            assertThat(inherited).hasSize(1);
            assertThat(inherited.get(0).getFileId()).isEqualTo("F-OLD");
        }
    }

    @Test
    @DisplayName("UT-STS-12-03: 異常 - 拠点が存在しない場合NOT_FOUNDを返す")
    void changeEquipment_siteNotFound_returnsNotFound() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.changeEquipment("UNKNOWN", EQUIPMENT_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verifyNoInteractions(equipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-12-04: 異常 - 拠点が他社のものである場合FORBIDDENを返す")
    void changeEquipment_otherCompanySite_returnsForbidden() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        Site site = siteWithCompany(OTHER_COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        EnR002EquipmentResponse response =
                equipmentServiceImpl.changeEquipment(SITE_ID, EQUIPMENT_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verifyNoInteractions(equipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-12-05: 異常 - 変更対象の設備が存在しない場合NOT_FOUNDを返す")
    void changeEquipment_equipmentNotFound_returnsNotFound() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        Site site = siteWithCompany(COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(999)).thenReturn(Optional.empty());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.changeEquipment(SITE_ID, 999, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND);
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-12-06: 異常 - 設備が別拠点に属する場合NOT_FOUNDを返す")
    void changeEquipment_equipmentFromOtherSite_returnsNotFound() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        Site site = siteWithCompany(COMPANY_ID);
        Site otherSite = mock(Site.class);
        when(otherSite.getId()).thenReturn(77);

        EnR002Equipment oldEquipment = mock(EnR002Equipment.class);
        when(oldEquipment.getSite()).thenReturn(otherSite);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(oldEquipment));

        EnR002EquipmentResponse response =
                equipmentServiceImpl.changeEquipment(SITE_ID, EQUIPMENT_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-12-07: 異常 - 既に後継設備が存在する場合EQUIPMENT_ALREADY_CHANGEDを返す")
    void changeEquipment_successorExists_returnsAlreadyChanged() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        Site site = siteWithCompany(COMPANY_ID);

        EnR002Equipment oldEquipment = mock(EnR002Equipment.class);
        when(oldEquipment.getSite()).thenReturn(site);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(oldEquipment));
        when(equipmentRepository.findByPreviousEquipment_Id(EQUIPMENT_ID))
                .thenReturn(Optional.of(mock(EnR002Equipment.class)));

        EnR002EquipmentResponse response =
                equipmentServiceImpl.changeEquipment(SITE_ID, EQUIPMENT_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.EQUIPMENT_ALREADY_CHANGED);
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-12-08: 異常 - changeDateが旧設備の開始日と同日の場合INVALID_CHANGE_DATEを返す（境界値）")
    void changeEquipment_changeDateEqualsStartDate_returnsInvalidChangeDate() {
        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        // Boundary: isAfter is strict, so the same date must be rejected.
        request.setChangeDate(LocalDate.of(2025, 1, 1));

        Site site = siteWithCompany(COMPANY_ID);
        EnR002Equipment oldEquipment = mock(EnR002Equipment.class);
        when(oldEquipment.getSite()).thenReturn(site);
        when(oldEquipment.getChangeDate()).thenReturn(null);
        when(oldEquipment.getEquipmentOpDate()).thenReturn(LocalDate.of(2025, 1, 1));

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(oldEquipment));
        when(equipmentRepository.findByPreviousEquipment_Id(EQUIPMENT_ID)).thenReturn(Optional.empty());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.changeEquipment(SITE_ID, EQUIPMENT_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INVALID_CHANGE_DATE);
        verify(equipmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-STS-12-09: 異常 - 新規ファイルのs3Keyがtmp配下でない場合INVALID_FILE_REFERENCEを返す")
    void changeEquipment_nonTempS3Key_returnsInvalidFileReference() {
        EnR002FileItemRequest badFile = new EnR002FileItemRequest();
        badFile.setFileId("F-BAD");
        badFile.setFileType(FILE_TYPE_A);
        badFile.setS3Key("equipments/10/100/existing.pdf"); // not under tmp/equipments/

        EnR002EquipmentChangeRequest request = new EnR002EquipmentChangeRequest();
        request.setChangeDate(LocalDate.of(2026, 6, 1));
        request.setFiles(List.of(badFile));

        Site site = siteWithCompany(COMPANY_ID);
        EnR002Equipment oldEquipment = mock(EnR002Equipment.class);
        when(oldEquipment.getSite()).thenReturn(site);
        when(oldEquipment.getChangeDate()).thenReturn(null);
        when(oldEquipment.getEquipmentOpDate()).thenReturn(LocalDate.of(2025, 1, 1));

        EnR002Equipment savedNew = mock(EnR002Equipment.class);
        lenient().when(savedNew.getId()).thenReturn(200);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(oldEquipment));
        when(equipmentRepository.findByPreviousEquipment_Id(EQUIPMENT_ID)).thenReturn(Optional.empty());
        when(equipmentRepository.save(any(EnR002Equipment.class))).thenReturn(savedNew);
        when(equipmentFileRepository.findByEquipment_Id(EQUIPMENT_ID)).thenReturn(Collections.emptyList());
        when(equipmentFileRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        EnR002EquipmentResponse response =
                equipmentServiceImpl.changeEquipment(SITE_ID, EQUIPMENT_ID, COMPANY_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INVALID_FILE_REFERENCE);
        assertThat(response.getMessage()).contains("equipments/10/100/existing.pdf");
        verify(fileUploadService, never()).moveToPermanent(anyString(), anyString());
    }

    // ==================================================================
    // 13. getEquipmentHistory
    // ==================================================================

    @Test
    @DisplayName("UT-STS-13-01: 正常 - 変更履歴が複数ある場合、時系列順に全世代を返す")
    void getEquipmentHistory_multipleVersions_returnsChronologicalChain() {
        Site site = siteWithCompany(COMPANY_ID);

        EnR002Equipment gen1 = mock(EnR002Equipment.class);
        EnR002Equipment gen2 = mock(EnR002Equipment.class);
        stubToResponse(gen1, 100, site);
        stubToResponse(gen2, 200, site);

        when(gen1.getChangeDate()).thenReturn(null);
        when(gen1.getEquipmentOpDate()).thenReturn(LocalDate.of(2025, 1, 1));
        when(gen2.getChangeDate()).thenReturn(LocalDate.of(2026, 6, 1));
        when(gen2.getPreviousEquipment()).thenReturn(gen1);
        when(gen2.getSite()).thenReturn(site);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        // Target is the newest version; the chain is walked backwards then forwards.
        when(equipmentRepository.findById(200)).thenReturn(Optional.of(gen2));
        when(equipmentRepository.findByPreviousEquipment_Id(100)).thenReturn(Optional.of(gen2));
        when(equipmentRepository.findByPreviousEquipment_Id(200)).thenReturn(Optional.empty());

        EnR002EquipmentHistoryListResponse response =
                equipmentServiceImpl.getEquipmentHistory(SITE_ID, 200, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getHistory()).hasSize(2);
        // Oldest first.
        assertThat(response.getHistory().get(0).getId()).isEqualTo(100);
        assertThat(response.getHistory().get(1).getId()).isEqualTo(200);
        // gen1 ends the day before gen2 starts; gen2 is still active.
        assertThat(response.getHistory().get(0).getMonitoringEndDate())
                .isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(response.getHistory().get(0).getIsActive()).isFalse();
        assertThat(response.getHistory().get(1).getMonitoringEndDate()).isNull();
        assertThat(response.getHistory().get(1).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("UT-STS-13-02: 準正常 - 変更履歴がない場合は1世代のみを返しisActiveはtrueになる")
    void getEquipmentHistory_singleVersion_returnsOneActiveEntry() {
        Site site = siteWithCompany(COMPANY_ID);

        EnR002Equipment only = mock(EnR002Equipment.class);
        stubToResponse(only, EQUIPMENT_ID, site);
        when(only.getChangeDate()).thenReturn(null);
        when(only.getEquipmentOpDate()).thenReturn(LocalDate.of(2025, 1, 1));

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(only));
        when(equipmentRepository.findByPreviousEquipment_Id(EQUIPMENT_ID)).thenReturn(Optional.empty());

        EnR002EquipmentHistoryListResponse response =
                equipmentServiceImpl.getEquipmentHistory(SITE_ID, EQUIPMENT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getHistory()).hasSize(1);
        assertThat(response.getHistory().get(0).getMonitoringStartDate())
                .isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(response.getHistory().get(0).getIsActive()).isTrue();
        assertThat(response.getHistory().get(0).getPreviousEquipmentId()).isNull();
    }

    @Test
    @DisplayName("UT-STS-13-03: 異常 - 拠点が存在しない場合NOT_FOUNDを返す")
    void getEquipmentHistory_siteNotFound_returnsNotFound() {
        when(siteRepository.findByPublicIdWithFallback("UNKNOWN")).thenReturn(Optional.empty());

        EnR002EquipmentHistoryListResponse response =
                equipmentServiceImpl.getEquipmentHistory("UNKNOWN", EQUIPMENT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        verifyNoInteractions(equipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-13-04: 異常 - 拠点が他社のものである場合FORBIDDENを返す")
    void getEquipmentHistory_otherCompanySite_returnsForbidden() {
        Site site = siteWithCompany(OTHER_COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));

        EnR002EquipmentHistoryListResponse response =
                equipmentServiceImpl.getEquipmentHistory(SITE_ID, EQUIPMENT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
        verifyNoInteractions(equipmentRepository);
    }

    @Test
    @DisplayName("UT-STS-13-05: 異常 - 設備が存在しない場合NOT_FOUND(EQUIPMENT)を返す")
    void getEquipmentHistory_equipmentNotFound_returnsNotFound() {
        Site site = siteWithCompany(COMPANY_ID);
        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(999)).thenReturn(Optional.empty());

        EnR002EquipmentHistoryListResponse response =
                equipmentServiceImpl.getEquipmentHistory(SITE_ID, 999, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_EQUIPMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("UT-STS-13-06: 異常 - 設備が別拠点に属する場合NOT_FOUNDを返す（拠点跨ぎアクセス防止）")
    void getEquipmentHistory_equipmentFromOtherSite_returnsNotFound() {
        Site site = siteWithCompany(COMPANY_ID);
        Site otherSite = mock(Site.class);
        when(otherSite.getId()).thenReturn(77);

        EnR002Equipment target = mock(EnR002Equipment.class);
        when(target.getSite()).thenReturn(otherSite);

        when(siteRepository.findByPublicIdWithFallback(SITE_ID)).thenReturn(Optional.of(site));
        when(equipmentRepository.findById(EQUIPMENT_ID)).thenReturn(Optional.of(target));

        EnR002EquipmentHistoryListResponse response =
                equipmentServiceImpl.getEquipmentHistory(SITE_ID, EQUIPMENT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-STS-13-07: 異常 - リポジトリが例外をスローする場合INTERNAL_SERVER_ERRORを返す")
    void getEquipmentHistory_repositoryThrows_returnsInternalServerError() {
        when(siteRepository.findByPublicIdWithFallback(SITE_ID))
                .thenThrow(new RuntimeException("DB error"));

        EnR002EquipmentHistoryListResponse response =
                equipmentServiceImpl.getEquipmentHistory(SITE_ID, EQUIPMENT_ID, COMPANY_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
    }

    
}