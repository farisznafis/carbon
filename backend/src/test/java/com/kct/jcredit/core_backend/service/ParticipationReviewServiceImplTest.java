// package com.kct.jcredit.core_backend.service;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.time.LocalDateTime;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Optional;

// import com.kct.jcredit.core_backend.entity.*;
// import com.kct.jcredit.core_backend.repository.*;
// import com.kct.jcredit.core_backend.dto.response.*;
// import com.kct.jcredit.core_backend.constant.ResponseConstants;
// import com.kct.jcredit.core_backend.dto.request.ReviewParticipationRequest;
// import com.kct.jcredit.core_backend.dto.response.ParticipationReviewDetailResponse;
// import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
// import com.kct.jcredit.core_backend.entity.ProjectParticipationConfirmation;
// import com.kct.jcredit.core_backend.entity.Project;
// import com.kct.jcredit.core_backend.entity.Site;
// import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
// import com.kct.jcredit.core_backend.entity.EnR002Equipment;
// import com.kct.jcredit.core_backend.exception.CustomException;
// import com.kct.jcredit.core_backend.service.impl.ParticipationReviewServiceImpl;
// import com.kct.jcredit.core_backend.entity.ParticipatingProject;
// import com.kct.jcredit.core_backend.repository.dynamodb.*;
// import com.kct.jcredit.core_backend.repository.postgres.*;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.junit.jupiter.api.Assertions.assertThrows;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class ParticipationReviewServiceImplTest {

//     @Mock private SiteProjectRelationRepository siteProjectRelationRepository;
//     @Mock private ParticipatingProjectRepository participatingProjectRepository;
//     @Mock private EnR002EquipmentFileRepository equipmentFileRepository;
//     @Mock private EnR002ApplicationConfirmationRepository confirmationItemsRepository;
//     @Mock private EnR002SustainabilityCommitmentRepository sustainabilityRepository;
//     @Mock private ProjectParticipationConfirmationRepository reviewRepository;

//     @InjectMocks
//     private ParticipationReviewServiceImpl service;

//     private Site site;
//     private Project project;
//     private ParticipatingCompany company;
//     private EnR002Equipment equipment;
//     private SiteProjectRelation relation;
//     private ParticipatingProject participation;

//     @BeforeEach
//     void setUp() {
//         company = new ParticipatingCompany();
//         company.setNameJa("テスト株式会社");

//         site = new Site();
//         site.setId(1);
//         site.setSiteName("テストサイト");
//         site.setCompany(company);

//         project = new Project();
//         project.setId(2);
//         project.setProjectName("テストプロジェクト");

//         equipment = new EnR002Equipment();
//         equipment.setId(10);
//         equipment.setSite(site);

//         relation = new SiteProjectRelation();
//         relation.setSite(site);
//         relation.setProject(project);
//         relation.setStatus("PENDING");

//         participation = ParticipatingProject.builder()
//                 .project(project)
//                 .equipment(equipment)
//                 .isEligible(true)
//                 .build();
//         participation.setId(100);
//         participation.setCreatedAt(LocalDateTime.now());
//     }

//     // ========================================================
//     // getPendingReviews() -> memanggil findPendingParticipations() secara internal
//     // ========================================================

//     @Test
//     void shouldReturnPendingReviewsWhenStatusIsOpen() {
//         relation.setStatus("OPEN");

//         when(siteProjectRelationRepository.findByStatus("OPEN"))
//                 .thenReturn(List.of(relation));
//         when(participatingProjectRepository.findByEquipment_Site_IdAndProject_Id(1, 2))
//                 .thenReturn(Optional.of(participation));
//         when(participatingProjectRepository.findById(100))
//                 .thenReturn(Optional.of(participation));
//         when(equipmentFileRepository.findByEquipment_Id(10))
//                 .thenReturn(List.of());
//         when(confirmationItemsRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         when(sustainabilityRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         when(reviewRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());

//         List<ParticipationReviewDetailResponse> result = service.getPendingReviews();

//         assertThat(result).hasSize(1);
//         assertThat(result.get(0).getParticipationId()).isEqualTo(100);
//         assertThat(result.get(0).getReview()).isNull(); // belum direview, sesuai desain
//     }

//     @Test
//     void shouldReturnEmptyWhenNoMatchingParticipationFound() {
//         relation.setStatus("OPEN");

//         when(siteProjectRelationRepository.findByStatus("OPEN"))
//                 .thenReturn(List.of(relation));
//         when(participatingProjectRepository.findByEquipment_Site_IdAndProject_Id(1, 2))
//                 .thenReturn(Optional.empty()); // tidak ada equipment terdaftar

//         List<ParticipationReviewDetailResponse> result = service.getPendingReviews();

//         assertThat(result).isEmpty();
//     }

//     // ========================================================
//     // getPendingReviewsByUserId() -> filter berdasarkan primaryReviewerId
//     // ========================================================

//     @Test
//     void shouldIncludeParticipationWhenNotYetReviewedByAnyone() {
//         relation.setStatus("OPEN");

//         when(siteProjectRelationRepository.findByStatus("OPEN"))
//                 .thenReturn(List.of(relation));
//         when(participatingProjectRepository.findByEquipment_Site_IdAndProject_Id(1, 2))
//                 .thenReturn(Optional.of(participation));
//         when(participatingProjectRepository.findById(100))
//                 .thenReturn(Optional.of(participation));
//         when(equipmentFileRepository.findByEquipment_Id(10))
//                 .thenReturn(List.of());
//         when(confirmationItemsRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         when(sustainabilityRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         // dipanggil 2x: sekali di filter(), sekali di getReviewDetail()
//         when(reviewRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());

//         List<ParticipationReviewDetailResponse> result = service.getPendingReviewsByUserId(999);

//         assertThat(result).hasSize(1);
//     }

//     @Test
//     void shouldExcludeParticipationWhenAlreadyReviewedBySameUser() {
//         relation.setStatus("OPEN");

//         ProjectParticipationConfirmation existingReview = ProjectParticipationConfirmation.builder()
//                 .pk("PART#100")
//                 .primaryReviewerId(999)
//                 .build();

//         when(siteProjectRelationRepository.findByStatus("OPEN"))
//                 .thenReturn(List.of(relation));
//         when(participatingProjectRepository.findByEquipment_Site_IdAndProject_Id(1, 2))
//                 .thenReturn(Optional.of(participation));
//         when(reviewRepository.findByParticipationId(100))
//                 .thenReturn(Optional.of(existingReview));

//         List<ParticipationReviewDetailResponse> result = service.getPendingReviewsByUserId(999);

//         assertThat(result).isEmpty(); // sudah direview oleh user yang sama -> di-skip
//     }

//     @Test
//     void shouldIncludeParticipationWhenReviewedByDifferentUser() {
//         relation.setStatus("OPEN");

//         ProjectParticipationConfirmation existingReview = ProjectParticipationConfirmation.builder()
//                 .pk("PART#100")
//                 .primaryReviewerId(111) // reviewer lain, bukan user 999
//                 .build();

//         when(siteProjectRelationRepository.findByStatus("OPEN"))
//                 .thenReturn(List.of(relation));
//         when(participatingProjectRepository.findByEquipment_Site_IdAndProject_Id(1, 2))
//                 .thenReturn(Optional.of(participation));
//         when(participatingProjectRepository.findById(100))
//                 .thenReturn(Optional.of(participation));
//         when(equipmentFileRepository.findByEquipment_Id(10))
//                 .thenReturn(List.of());
//         when(confirmationItemsRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         when(sustainabilityRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         when(reviewRepository.findByParticipationId(100))
//                 .thenReturn(Optional.of(existingReview));

//         List<ParticipationReviewDetailResponse> result = service.getPendingReviewsByUserId(999);

//         assertThat(result).hasSize(1); // reviewer beda -> tetap dianggap pending untuk user 999
//     }

//     // ========================================================
//     // getReviewDetail()
//     // ========================================================

//     @Test
//     void shouldReturnDetailWithNullReviewWhenNotYetReviewed() {
//         when(participatingProjectRepository.findById(100))
//                 .thenReturn(Optional.of(participation));
//         when(equipmentFileRepository.findByEquipment_Id(10))
//                 .thenReturn(List.of());
//         when(confirmationItemsRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         when(sustainabilityRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         when(reviewRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());

//         ParticipationReviewDetailResponse detail = service.getReviewDetail(100);

//         assertThat(detail.getParticipationId()).isEqualTo(100);
//         assertThat(detail.getProjectName()).isEqualTo("テストプロジェクト");
//         assertThat(detail.getSiteName()).isEqualTo("テストサイト");
//         assertThat(detail.getCompanyName()).isEqualTo("テスト株式会社");
//         assertThat(detail.getReview()).isNull();
//     }

//     @Test
//     void shouldReturnDetailWithReviewWhenAlreadyReviewed() {
//         ProjectParticipationConfirmation existingReview = ProjectParticipationConfirmation.builder()
//                 .pk("PART#100")
//                 .isSolarContractDoc(true)
//                 .primaryApplReviewerName("田中")
//                 .primaryReviewerId(999)
//                 .build();

//         when(participatingProjectRepository.findById(100))
//                 .thenReturn(Optional.of(participation));
//         when(equipmentFileRepository.findByEquipment_Id(10))
//                 .thenReturn(List.of());
//         when(confirmationItemsRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         when(sustainabilityRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());
//         when(reviewRepository.findByParticipationId(100))
//                 .thenReturn(Optional.of(existingReview));

//         ParticipationReviewDetailResponse detail = service.getReviewDetail(100);

//         assertThat(detail.getReview()).isNotNull();
//         assertThat(detail.getReview().getIsSolarContractDoc()).isTrue();
//         assertThat(detail.getReview().getPrimaryApplReviewerName()).isEqualTo("田中");
//     }

//     @Test
//     void shouldThrowExceptionWhenParticipationIdNotFound() {
//         when(participatingProjectRepository.findById(999))
//                 .thenReturn(Optional.empty());

//         assertThrows(CustomException.class, () -> service.getReviewDetail(999));
//     }

//     // ========================================================
//     // submitReview()
//     // ========================================================

//     private ReviewParticipationRequest buildRequest(String decision) {
//         return ReviewParticipationRequest.builder()
//                 .isSolarContractDoc(true)
//                 .isPcsSpecDoc(true)
//                 .isBatterySpecDoc(true)
//                 .isOperationStartDoc(true)
//                 .isGenerationDataDoc(true)
//                 .isReversePowerDoc(true)
//                 .isInvestmentProofDoc(true)
//                 .isApplicationCompleteCheck(true)
//                 .isFormsAttachCheck(true)
//                 .isEmissionsReductionActivityListCheck(true)
//                 .isOriginalSpecCheck(true)
//                 .isSubsidyCheck(true)
//                 .decision(decision)
//                 .build();
//     }

//     @Test
//     void shouldSaveNewReviewWhenSubmittedForTheFirstTime() {
//         relation.setStatus("PENDING");

//         when(participatingProjectRepository.findById(100))
//                 .thenReturn(Optional.of(participation));
//         when(siteProjectRelationRepository.findBySite_IdAndProject_Id(1, 2))
//                 .thenReturn(Optional.of(relation));
//         when(reviewRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());

//         ReviewParticipationRequest request = buildRequest("APPROVE");

//         service.submitReview(100, request);

//         verify(reviewRepository, times(1)).save(argThat(saved ->
//                 "PART#100".equals(saved.getPk()) && saved.getIsSolarContractDoc()
//         ));
//         // decision bukan "OPEN" dan slot primary masih kosong sebelum save
//         // -> relation status TIDAK diupdate ke REVIEWED (karena isPrimarySlotEmpty true saat submit pertama)
//         verify(siteProjectRelationRepository, never()).save(any());
//     }

//     @Test
//     void shouldSetRelationBackToOpenWhenDecisionIsOpen() {
//         relation.setStatus("PENDING");

//         when(participatingProjectRepository.findById(100))
//                 .thenReturn(Optional.of(participation));
//         when(siteProjectRelationRepository.findBySite_IdAndProject_Id(1, 2))
//                 .thenReturn(Optional.of(relation));
//         when(reviewRepository.findByParticipationId(100))
//                 .thenReturn(Optional.empty());

//         ReviewParticipationRequest request = buildRequest("OPEN");
//         request.setRejectReason("資料が不足しています");

//         service.submitReview(100, request);

//         verify(siteProjectRelationRepository, times(1)).save(argThat(saved ->
//                 "OPEN".equals(saved.getStatus())
//         ));
//     }

//     @Test
//     void shouldSetRelationToReviewedWhenSecondReviewerSubmits() {
//         relation.setStatus("PENDING");

//         ProjectParticipationConfirmation existingReview = ProjectParticipationConfirmation.builder()
//                 .pk("PART#100")
//                 .primaryReviewerId(111)
//                 .primaryApplReviewerName("田中")
//                 .build();

//         when(participatingProjectRepository.findById(100))
//                 .thenReturn(Optional.of(participation));
//         when(siteProjectRelationRepository.findBySite_IdAndProject_Id(1, 2))
//                 .thenReturn(Optional.of(relation));
//         when(reviewRepository.findByParticipationId(100))
//                 .thenReturn(Optional.of(existingReview));

//         ReviewParticipationRequest request = buildRequest("APPROVE");

//         service.submitReview(100, request);

//         verify(siteProjectRelationRepository, times(1)).save(argThat(saved ->
//                 "REVIEWED".equals(saved.getStatus())
//         ));
//     }

//     @Test
//     void shouldThrowExceptionWhenRelationStatusIsNotPending() {
//         relation.setStatus("REVIEWED"); // sudah selesai direview

//         when(participatingProjectRepository.findById(100))
//                 .thenReturn(Optional.of(participation));
//         when(siteProjectRelationRepository.findBySite_IdAndProject_Id(1, 2))
//                 .thenReturn(Optional.of(relation));

//         ReviewParticipationRequest request = buildRequest("APPROVE");

//         CustomException ex = assertThrows(CustomException.class,
//                 () -> service.submitReview(100, request));

//         assertThat(ex.getMessage()).contains("既に完了しています");
//         verify(reviewRepository, never()).save(any());
//     }

//     @Test
//     void shouldThrowExceptionWhenParticipationNotFoundOnSubmit() {
//         when(participatingProjectRepository.findById(999))
//                 .thenReturn(Optional.empty());

//         ReviewParticipationRequest request = buildRequest("APPROVE");

//         assertThrows(CustomException.class, () -> service.submitReview(999, request));
//     }
// }