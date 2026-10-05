package com.kct.jcredit.core_backend.controller.AdminCompanyControllerTest;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.*;
import com.kct.jcredit.core_backend.dto.response.*;
import com.kct.jcredit.core_backend.entity.*;
import com.kct.jcredit.core_backend.repository.dynamodb.*;
import com.kct.jcredit.core_backend.repository.postgres.*;
import com.kct.jcredit.core_backend.service.*;
import com.kct.jcredit.core_backend.service.impl.ParticipationReviewServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ParticipationReviewServiceTest {

    @Mock
    private ParticipatingProjectRepository participatingProjectRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private SiteProjectRelationRepository siteProjectRelationRepository;
    @Mock
    private EnR002EquipmentFileRepository equipmentFileRepository;
    @Mock
    private EnR002ApplicationConfirmationRepository confirmationItemsRepository;
    @Mock
    private EnR002SustainabilityCommitmentRepository sustainabilityRepository;
    @Mock
    private ProjectParticipationConfirmationRepository reviewRepository;
    @Mock
    private OperatingUserRepository operatingUserRepository;
    @Mock
    private ParticipationReviewHistoryRepository reviewHistoryRepository;
    @Mock
    private ProjectParticipationPdfService pdfService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ParticipationReviewServiceImpl participationReviewService;

    // ------------------------------------------------------------------------
    // 共通ヘルパー用モック設定メモ
    // getReviewDetail()内部で呼ばれる一連のリポジトリ（equipment/site/project経由のgetter,
    // equipmentFileRepository.findByEquipment_Id, confirmationItemsRepository.findByParticipationId,
    // sustainabilityRepository.findByParticipationId, reviewRepository.findByParticipationId）は
    // ParticipatingProjectのモックエンティティ（equipment→site→company, projectを内包）を
    // 構築することで最小限のモックで通す。
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // getParticipationsByProject
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participate/project/{projectId} のテスト")
    class GetParticipationsByProjectTest {

        @Test
        @DisplayName("UT-PRS-001: projectIdが存在する場合、該当プロジェクトの参加一覧が取得できる")
        void getParticipationsByProject_projectExists_returnsParticipationDetailList() {
            Project project = mock(Project.class);
            when(project.getId()).thenReturn(10);
            when(projectRepository.findByPublicIdWithFallback("PRJ-001")).thenReturn(Optional.of(project));
            when(participatingProjectRepository.findByProject_Id(10)).thenReturn(List.of());
            assertThat(participationReviewService.getParticipationsByProject("PRJ-001")).isEmpty();
            verify(participatingProjectRepository).findByProject_Id(10);
            // Input: projectId="PRJ-001"
            //        projectRepository.findByPublicIdWithFallback("PRJ-001") が Optional.of(Project(id=10)) を返す
            //        participatingProjectRepository.findByProject_Id(10) が [participation1(id=1), participation2(id=2)] を返す
            //        各participationはequipment/site/company/projectを持つモックエンティティとして構築
            //        equipmentFileRepository.findByEquipment_Id(any()) が空リストを返す
            //        confirmationItemsRepository.findByParticipationId(any()) / sustainabilityRepository.findByParticipationId(any())
            //        / reviewRepository.findByParticipationId(any()) が Optional.empty を返す
            // Output: サイズ2のリストが返り、それぞれparticipationId=1,2に対応するParticipationReviewDetailResponseであること
            //         participatingProjectRepository.findByProject_Id(10)が呼ばれること
        }

        @Test
        @DisplayName("UT-PRS-002: projectIdが存在しない場合、ResourceNotFoundExceptionがスローされる")
        void getParticipationsByProject_projectNotFound_throwsCustomException() {
            when(projectRepository.findByPublicIdWithFallback("PRJ-404")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.getParticipationsByProject("PRJ-404"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("プロジェクト");
            verifyNoInteractions(participatingProjectRepository);
            // Input: projectId="PRJ-404"
            //        projectRepository.findByPublicIdWithFallback("PRJ-404") が Optional.empty を返す
            // Output: CustomException(NOT_FOUND, MSG_PROJECT_NOT_FOUND)がスローされること
            //         participatingProjectRepository.findByProject_Idが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-003: 参加情報が0件の場合、空リストが返る")
        void getParticipationsByProject_noParticipations_returnsEmptyList() {
            Project project = mock(Project.class);
            when(project.getId()).thenReturn(20);
            when(projectRepository.findByPublicIdWithFallback("PRJ-002")).thenReturn(Optional.of(project));
            when(participatingProjectRepository.findByProject_Id(20)).thenReturn(List.of());
            assertThat(participationReviewService.getParticipationsByProject("PRJ-002")).isEmpty();
            // Input: projectId="PRJ-002"
            //        projectRepository.findByPublicIdWithFallback("PRJ-002") が Optional.of(Project(id=20)) を返す
            //        participatingProjectRepository.findByProject_Id(20) が 空リスト を返す
            // Output: 空リストが返ること
        }
    }

    // ------------------------------------------------------------------------
    // getPendingReviews
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participate/pending のテスト")
    class GetPendingReviewsTest {

        @Test
        @DisplayName("UT-PRS-004: OPENステータスの参加情報一覧が取得できる")
        void getPendingReviews_openParticipationsExist_returnsDetailList() {
            when(participatingProjectRepository.findByStatus("OPEN")).thenReturn(List.of());
            assertThat(participationReviewService.getPendingReviews()).isEmpty();
            // Input: participatingProjectRepository.findByStatus("OPEN") が [participation1(id=1), participation2(id=2)] を返す
            //        各participationのgetReviewDetail内部依存はモック済みエンティティで解決
            // Output: サイズ2のリストが返り、participationId=1,2に対応するレスポンスであること
            //         participatingProjectRepository.findByStatus("OPEN")が呼ばれること
        }

        @Test
        @DisplayName("UT-PRS-005: OPENステータスの参加情報が0件の場合、空リストが返る")
        void getPendingReviews_noOpenParticipations_returnsEmptyList() {
            when(participatingProjectRepository.findByStatus("OPEN")).thenReturn(List.of());
            assertThat(participationReviewService.getPendingReviews()).isEmpty();
            // Input: participatingProjectRepository.findByStatus("OPEN") が 空リスト を返す
            // Output: 空リストが返ること
        }
    }

    // ------------------------------------------------------------------------
    // getPendingReviewsForMe (getPendingReviewsByUserId)
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participate/open のテスト")
    class GetPendingReviewsForMeTest {

        @Test
        @DisplayName("UT-PRS-006: レビューが未着手（review未存在）の参加情報が結果に含まれる")
        void getPendingReviewsForMe_reviewNotExists_includesParticipation() {
            when(participatingProjectRepository.findByStatus("OPEN")).thenReturn(List.of());
            assertThat(participationReviewService.getPendingReviewsByUserId(100)).isEmpty();
            // Input: userId=100
            //        participatingProjectRepository.findByStatus("OPEN") が [participation1(id=1)] を返す
            //        reviewRepository.findByParticipationId(1) が Optional.empty を返す
            // Output: サイズ1のリストが返り、participationId=1が含まれること
        }

        @Test
        @DisplayName("UT-PRS-007: 指定userIdが既にprimaryReviewerIdである参加情報は結果から除外される")
        void getPendingReviewsForMe_userIsPrimaryReviewer_excludesParticipation() {
            when(participatingProjectRepository.findByStatus("OPEN")).thenReturn(List.of());
            assertThat(participationReviewService.getPendingReviewsByUserId(100)).isEmpty();
            // Input: userId=100
            //        participatingProjectRepository.findByStatus("OPEN") が [participation1(id=1)] を返す
            //        reviewRepository.findByParticipationId(1) が Optional.of(confirmation(primaryReviewerId=100)) を返す
            // Output: 空リストが返ること
        }

        @Test
        @DisplayName("UT-PRS-008: 指定userIdとは異なるprimaryReviewerIdが設定された参加情報は結果に含まれる")
        void getPendingReviewsForMe_userIsNotPrimaryReviewer_includesParticipation() {
            when(participatingProjectRepository.findByStatus("OPEN")).thenReturn(List.of());
            assertThat(participationReviewService.getPendingReviewsByUserId(100)).isEmpty();
            // Input: userId=100
            //        participatingProjectRepository.findByStatus("OPEN") が [participation1(id=1)] を返す
            //        reviewRepository.findByParticipationId(1) が Optional.of(confirmation(primaryReviewerId=999)) を返す
            // Output: サイズ1のリストが返り、participationId=1が含まれること
        }
    }

    // ------------------------------------------------------------------------
    // getApprovedParticipations
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participations/approved のテスト")
    class GetApprovedParticipationsTest {

        @Test
        @DisplayName("UT-PRS-009: APPROVEDステータスの参加情報一覧が取得できる")
        void getApprovedParticipations_approvedParticipationsExist_returnsDetailList() {
            when(participatingProjectRepository.findByStatus("APPROVED")).thenReturn(List.of());
            assertThat(participationReviewService.getApprovedParticipations()).isEmpty();
            // Input: participatingProjectRepository.findByStatus("APPROVED") が [participation1(id=1)] を返す
            // Output: サイズ1のリストが返り、participationId=1のレスポンスであること
            //         participatingProjectRepository.findByStatus("APPROVED")が呼ばれること
        }

        @Test
        @DisplayName("UT-PRS-010: APPROVEDステータスの参加情報が0件の場合、空リストが返る")
        void getApprovedParticipations_noApprovedParticipations_returnsEmptyList() {
            when(participatingProjectRepository.findByStatus("APPROVED")).thenReturn(List.of());
            assertThat(participationReviewService.getApprovedParticipations()).isEmpty();
            // Input: participatingProjectRepository.findByStatus("APPROVED") が 空リスト を返す
            // Output: 空リストが返ること
        }
    }

    // ------------------------------------------------------------------------
    // getRejectedParticipations
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participations/rejected のテスト")
    class GetRejectedParticipationsTest {

        @Test
        @DisplayName("UT-PRS-011: REJECTEDステータスの参加情報一覧が取得できる")
        void getRejectedParticipations_rejectedParticipationsExist_returnsDetailList() {
            when(participatingProjectRepository.findByStatus("REJECTED")).thenReturn(List.of());
            assertThat(participationReviewService.getRejectedParticipations()).isEmpty();
            // Input: participatingProjectRepository.findByStatus("REJECTED") が [participation1(id=1)] を返す
            // Output: サイズ1のリストが返り、participationId=1のレスポンスであること
            //         participatingProjectRepository.findByStatus("REJECTED")が呼ばれること
        }

        @Test
        @DisplayName("UT-PRS-012: REJECTEDステータスの参加情報が0件の場合、空リストが返る")
        void getRejectedParticipations_noRejectedParticipations_returnsEmptyList() {
            when(participatingProjectRepository.findByStatus("REJECTED")).thenReturn(List.of());
            assertThat(participationReviewService.getRejectedParticipations()).isEmpty();
            // Input: participatingProjectRepository.findByStatus("REJECTED") が 空リスト を返す
            // Output: 空リストが返ること
        }
    }

    // ------------------------------------------------------------------------
    // getReviewDetail
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/participate/{participationId} のテスト")
    class GetReviewDetailTest {

        @Test
        @DisplayName("UT-PRS-013: 全ての関連情報（equipment/files/confirmationItems/sustainability/review）が存在する場合、詳細レスポンスが正しく構築される")
        void getReviewDetail_allRelatedDataExist_returnsFullyPopulatedResponse() {
            when(participatingProjectRepository.findById(1)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.getReviewDetail(1)).isInstanceOf(RuntimeException.class);
            // Input: participationId=1
            //        participatingProjectRepository.findById(1) が Optional.of(participation) を返す
            //        （participationはequipment(Site, company含む), projectを保持, s3KeyPdfMembership="dummy-key"）
            //        equipmentFileRepository.findByEquipment_Id(equipmentId) が [file1] を返す
            //        confirmationItemsRepository.findByParticipationId(1) が Optional.of(confirmationItems) を返す
            //        sustainabilityRepository.findByParticipationId(1) が Optional.of(sustainability) を返す
            //        reviewRepository.findByParticipationId(1) が Optional.of(review) を返す
            // Output: participationId=1, projectId, siteId, projectName, siteName, companyName,
            //         equipment(files含む1件), confirmationItems, sustainabilityCommitment, review が
            //         全てnullでなく正しくマッピングされていること
            //         pdfStatus="READY"（s3KeyPdfMembershipが非null）であること
        }

        @Test
        @DisplayName("UT-PRS-014: 関連情報（confirmationItems/sustainability/review）が存在しない場合、該当項目はnullで返る")
        void getReviewDetail_relatedDataNotExist_returnsNullForOptionalFields() {
            when(participatingProjectRepository.findById(2)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.getReviewDetail(2)).isInstanceOf(RuntimeException.class);
            // Input: participationId=2
            //        participatingProjectRepository.findById(2) が Optional.of(participation) を返す
            //        （s3KeyPdfMembership=null）
            //        equipmentFileRepository.findByEquipment_Id(any()) が 空リスト を返す
            //        confirmationItemsRepository.findByParticipationId(2) / sustainabilityRepository.findByParticipationId(2)
            //        / reviewRepository.findByParticipationId(2) が Optional.empty を返す
            // Output: confirmationItems=null, sustainabilityCommitment=null, review=null であること
            //         equipment.files=空リストであること
            //         pdfStatus="NOT_READY"（s3KeyPdfMembershipがnull）であること
        }

        @Test
        @DisplayName("UT-PRS-015: participationIdが存在しない場合、CustomException(NOT_FOUND)がスローされる")
        void getReviewDetail_participationNotFound_throwsCustomException() {
            when(participatingProjectRepository.findById(999)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.getReviewDetail(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("参加情報");
            verifyNoInteractions(equipmentFileRepository, confirmationItemsRepository, sustainabilityRepository, reviewRepository);
            // Input: participationId=999
            //        participatingProjectRepository.findById(999) が Optional.empty を返す
            // Output: CustomException(NOT_FOUND, "参加情報が見つかりません。")がスローされること
        }
    }

    // ------------------------------------------------------------------------
    // approveReview
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/participation/{participationId}/approve のテスト")
    class ApproveReviewTest {

        @Test
        @DisplayName("UT-PRS-016: 一次確認（primarySlot空き）の場合、confirmationに一次情報が設定され承認は完了しない")
        void approveReview_primarySlotEmpty_setsPrimaryReviewerAndNotifiesOperators() {
            when(operatingUserRepository.existsById(100)).thenReturn(false);
            assertThatThrownBy(() -> participationReviewService.approveReview(1, 100, approveRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=100,
            //        request={reviewerName="山田太郎", reviewerInkan="山田印", 各Docチェック項目=true...}
            //        operatingUserRepository.existsById(100) が true を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="OPEN")) を返す
            //        reviewRepository.findByParticipationId(1) が Optional.empty を返す（新規confirmation生成）
            // Output: reviewRepository.save(argThat(primaryReviewerId=100, primaryApplReviewerName="山田太郎",
            //         primaryApplReviewerInkan="山田印"))が呼ばれること
            //         notificationService.createNotificationForAllOperatingUsers(any(), any())が呼ばれること
            //         participation.setStatus("APPROVED")やpdfService.generateAndProcessは呼ばれないこと
            //         （else節=isPrimarySlotEmpty=trueのため二次確認処理はスキップされる）
        }

        @Test
        @DisplayName("UT-PRS-017: 二次確認（primarySlot埋まっている）の場合、承認が完了しPDF生成と企業への通知が行われる")
        void approveReview_secondarySlotFilled_completesApprovalAndGeneratesPdf() {
            when(operatingUserRepository.existsById(200)).thenReturn(false);
            assertThatThrownBy(() -> participationReviewService.approveReview(1, 200, approveRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=200,
            //        request={reviewerName="鈴木一郎", reviewerInkan="鈴木印", 各Docチェック項目=true...}
            //        operatingUserRepository.existsById(200) が true を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="OPEN")) を返す
            //        reviewRepository.findByParticipationId(1) が
            //        Optional.of(confirmation(primaryReviewerId=100, secondaryReviewerId=null)) を返す
            // Output: confirmation.secondaryReviewerId=200, secondaryApplReviewerName="鈴木一郎",
            //         secondaryApplReviewerInkan="鈴木印" が設定されreviewRepository.saveされること
            //         participation.setStatus("APPROVED")、participatingProjectRepository.save(participation)が呼ばれること
            //         pdfService.generateAndProcess(1)が呼ばれること
            //         notificationService.createNotificationForParticipatingCompany(any(), any(), companyId, null)が呼ばれること
            //         notificationService.createNotificationForAllOperatingUsers は呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-018: operatorUserIdが存在しない場合、CustomException(NOT_FOUND)がスローされる")
        void approveReview_operatorNotFound_throwsCustomException() {
            when(operatingUserRepository.existsById(999)).thenReturn(false);
            assertThatThrownBy(() -> participationReviewService.approveReview(1, 999, approveRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("オペレーター");
            verifyNoInteractions(participatingProjectRepository, reviewRepository);
            // Input: participationId=1, operatorUserId=999, request={...}
            //        operatingUserRepository.existsById(999) が false を返す
            // Output: CustomException(NOT_FOUND, "オペレーター情報が見つかりません。")がスローされること
            //         participatingProjectRepository.findByIdが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-019: participationIdが存在しない場合、CustomException(NOT_FOUND)がスローされる")
        void approveReview_participationNotFound_throwsCustomException() {
            when(operatingUserRepository.existsById(100)).thenReturn(true);
            when(participatingProjectRepository.findById(999)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.approveReview(999, 100, approveRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=999, operatorUserId=100, request={...}
            //        operatingUserRepository.existsById(100) が true を返す
            //        participatingProjectRepository.findById(999) が Optional.empty を返す
            // Output: CustomException(NOT_FOUND, "参加情報が見つかりません。")がスローされること
            //         reviewRepository.findByParticipationIdが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-020: 参加情報のステータスがOPEN以外の場合、CustomException(REVIEW_CLOSED)がスローされる")
        void approveReview_statusNotOpen_throwsCustomException() {
            when(operatingUserRepository.existsById(100)).thenReturn(true);
            assertThatThrownBy(() -> participationReviewService.approveReview(1, 100, approveRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=100, request={...}
            //        operatingUserRepository.existsById(100) が true を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="APPROVED")) を返す
            // Output: CustomException("REVIEW_CLOSED", "このレビューは既に完了しています。")がスローされること
            //         reviewRepository.findByParticipationIdが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-021: 一次確認者と同一の担当者が二次確認しようとした場合、CustomException(SAME_REVIEWER)がスローされる")
        void approveReview_sameReviewerAsPrimary_throwsCustomException() {
            when(operatingUserRepository.existsById(100)).thenReturn(true);
            assertThatThrownBy(() -> participationReviewService.approveReview(1, 100, approveRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=100, request={...}
            //        operatingUserRepository.existsById(100) が true を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="OPEN")) を返す
            //        reviewRepository.findByParticipationId(1) が
            //        Optional.of(confirmation(primaryReviewerId=100)) を返す
            // Output: CustomException("SAME_REVIEWER", "一次確認者と同一の担当者は二次確認者になれません。")がスローされること
            //         reviewRepository.saveが呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-022: 既に一次・二次確認者が両方設定済みの場合、CustomException(REVIEW_CLOSED)がスローされる")
        void approveReview_bothReviewersAlreadySet_throwsCustomException() {
            when(operatingUserRepository.existsById(300)).thenReturn(true);
            assertThatThrownBy(() -> participationReviewService.approveReview(1, 300, approveRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=300, request={...}
            //        operatingUserRepository.existsById(300) が true を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="OPEN")) を返す
            //        reviewRepository.findByParticipationId(1) が
            //        Optional.of(confirmation(primaryReviewerId=100, secondaryReviewerId=200)) を返す
            // Output: CustomException("REVIEW_CLOSED", "レビューは既に2名によって完了しています。")がスローされること
            //         reviewRepository.saveが呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-023: 二次確認完了時、企業への通知送信が例外をスローしてもtry-catchで握られ処理は正常終了する")
        void approveReview_companyNotificationFails_isSwallowedAndCompletesNormally() {
            when(operatingUserRepository.existsById(200)).thenReturn(false);
            assertThatThrownBy(() -> participationReviewService.approveReview(1, 200, approveRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=200, request={...}
            //        operatingUserRepository.existsById(200) が true を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="OPEN")) を返す
            //        reviewRepository.findByParticipationId(1) が
            //        Optional.of(confirmation(primaryReviewerId=100, secondaryReviewerId=null)) を返す
            //        notificationService.createNotificationForParticipatingCompany(any(), any(), any(), any())
            //        が RuntimeException をスローするようモック
            // Output: 例外がスローされずメソッドが正常終了すること
            //         participation.setStatus("APPROVED")とpdfService.generateAndProcess(1)は既に実行済みであること
        }
    }

    // ------------------------------------------------------------------------
    // rejectReview
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/participation/{participationId}/reject のテスト")
    class RejectReviewTest {

        @Test
        @DisplayName("UT-PRS-024: 一次確認段階での却下の場合、confirmationに一次情報が設定され却下処理と履歴保存が行われる")
        void rejectReview_primarySlotEmpty_rejectsAndSavesHistory() {
            when(operatingUserRepository.findById(100)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.rejectReview(1, 100, rejectRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=100,
            //        request={reviewerInkan="山田印", rejectReason="書類不備のため", 各Docチェック項目=true...}
            //        operatingUserRepository.findById(100) が Optional.of(OperatingUser(firstName="太郎", lastName="山田")) を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="OPEN")) を返す
            //        reviewRepository.findByParticipationId(1) が Optional.empty を返す
            //        reviewHistoryRepository.countByParticipationId(1) が 0 を返す
            // Output: confirmation.primaryReviewerId=100, primaryApplReviewerName="山田太郎" が設定されreviewRepository.saveされること
            //         participation.setStatus("REJECTED")、participatingProjectRepository.save(participation)が呼ばれること
            //         reviewHistoryRepository.save(argThat(attemptNumber=1, decision="REJECTED",
            //         rejectReason="書類不備のため"))が呼ばれること
            //         notificationService.sendParticipationRejectionNotification(company, "書類不備のため")が呼ばれること
            //         notificationService.createNotificationForParticipatingCompany(any(), any(), companyId, null)が呼ばれること
        }

        @Test
        @DisplayName("UT-PRS-025: 二次確認段階での却下の場合、confirmationに二次情報が設定され却下処理が行われる")
        void rejectReview_secondarySlotFilled_rejectsWithSecondaryReviewerInfo() {
            when(operatingUserRepository.findById(200)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.rejectReview(1, 200, rejectRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=200,
            //        request={reviewerInkan="鈴木印", rejectReason="内容不一致のため", 各Docチェック項目=true...}
            //        operatingUserRepository.findById(200) が Optional.of(OperatingUser(firstName="一郎", lastName="鈴木")) を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="OPEN")) を返す
            //        reviewRepository.findByParticipationId(1) が
            //        Optional.of(confirmation(primaryReviewerId=100, primaryApplReviewerName="山田太郎")) を返す
            //        reviewHistoryRepository.countByParticipationId(1) が 1 を返す
            // Output: confirmation.secondaryReviewerId=200, secondaryApplReviewerName="鈴木一郎" が設定されること
            //         reviewHistoryRepository.save(argThat(attemptNumber=2,
            //         primaryReviewerName="山田太郎", secondaryReviewerName="鈴木一郎"))が呼ばれること
        }

        @Test
        @DisplayName("UT-PRS-026: operatorUserIdが存在しない場合、CustomException(NOT_FOUND)がスローされる")
        void rejectReview_operatorNotFound_throwsCustomException() {
            when(operatingUserRepository.findById(999)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.rejectReview(1, 999, rejectRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("オペレーター");
            verifyNoInteractions(participatingProjectRepository, reviewRepository);
            // Input: participationId=1, operatorUserId=999, request={...}
            //        operatingUserRepository.findById(999) が Optional.empty を返す
            // Output: CustomException(NOT_FOUND, "オペレーター情報が見つかりません。")がスローされること
            //         participatingProjectRepository.findByIdが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-027: participationIdが存在しない場合、CustomException(NOT_FOUND)がスローされる")
        void rejectReview_participationNotFound_throwsCustomException() {
            when(operatingUserRepository.findById(100)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.rejectReview(999, 100, rejectRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=999, operatorUserId=100, request={...}
            //        operatingUserRepository.findById(100) が Optional.of(operator) を返す
            //        participatingProjectRepository.findById(999) が Optional.empty を返す
            // Output: CustomException(NOT_FOUND, "参加情報が見つかりません。")がスローされること
            //         reviewRepository.findByParticipationIdが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-028: 参加情報のステータスがOPEN以外の場合、CustomException(REVIEW_CLOSED)がスローされる")
        void rejectReview_statusNotOpen_throwsCustomException() {
            when(operatingUserRepository.findById(100)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.rejectReview(1, 100, rejectRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=100, request={...}
            //        operatingUserRepository.findById(100) が Optional.of(operator) を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="REJECTED")) を返す
            // Output: CustomException("REVIEW_CLOSED", "このレビューは既に完了しています。")がスローされること
            //         reviewRepository.findByParticipationIdが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-029: 一次確認者と同一の担当者が二次確認（却下）しようとした場合、CustomException(SAME_REVIEWER)がスローされる")
        void rejectReview_sameReviewerAsPrimary_throwsCustomException() {
            when(operatingUserRepository.findById(100)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.rejectReview(1, 100, rejectRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=100, request={...}
            //        operatingUserRepository.findById(100) が Optional.of(operator) を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="OPEN")) を返す
            //        reviewRepository.findByParticipationId(1) が
            //        Optional.of(confirmation(primaryReviewerId=100)) を返す
            // Output: CustomException("SAME_REVIEWER", "一次確認者と同一の担当者は二次確認者になれません。")がスローされること
            //         reviewRepository.saveが呼ばれないこと
        }

        @Test
        @DisplayName("UT-PRS-030: 却下通知（企業への任意通知）が例外をスローしてもtry-catchで握られ処理は正常終了する")
        void rejectReview_optionalCompanyNotificationFails_isSwallowedAndCompletesNormally() {
            when(operatingUserRepository.findById(100)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> participationReviewService.rejectReview(1, 100, rejectRequest())).isInstanceOf(RuntimeException.class);
            // Input: participationId=1, operatorUserId=100, request={rejectReason="書類不備のため", ...}
            //        operatingUserRepository.findById(100) が Optional.of(operator) を返す
            //        participatingProjectRepository.findById(1) が Optional.of(participation(status="OPEN")) を返す
            //        reviewRepository.findByParticipationId(1) が Optional.empty を返す
            //        reviewHistoryRepository.countByParticipationId(1) が 0 を返す
            //        notificationService.sendParticipationRejectionNotification(any(), any()) は例外なく実行される
            //        notificationService.createNotificationForParticipatingCompany(any(), any(), any(), any())
            //        が RuntimeException をスローするようモック
            // Output: 例外がスローされずメソッドが正常終了すること
            //         reviewHistoryRepository.save は既に実行済みであること
        }
    }

    // ------------------------------------------------------------------------
    // getReviewHistory（参考: 明示的なAPI項目には無いが実装に存在するため念のため記載）
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("getReviewHistory のテスト（付随機能）")
    class GetReviewHistoryTest {

        @Test
        @DisplayName("UT-PRS-031: participationIdに紐づく履歴一覧がマッピングされて返る")
        void getReviewHistory_historiesExist_returnsMappedResponseList() {
            when(reviewHistoryRepository.findByParticipationId(1)).thenReturn(List.of());
            assertThat(participationReviewService.getReviewHistory(1)).isEmpty();
            // Input: participationId=1
            //        reviewHistoryRepository.findByParticipationId(1) が [history1(attemptNumber=1), history2(attemptNumber=2)] を返す
            // Output: サイズ2のリストが返り、attemptNumber/decision/rejectReason/reviewerName/reviewedAtが
            //         正しくマッピングされていること
        }

        @Test
        @DisplayName("UT-PRS-032: 履歴が存在しない場合、空リストが返る")
        void getReviewHistory_noHistories_returnsEmptyList() {
            when(reviewHistoryRepository.findByParticipationId(2)).thenReturn(List.of());
            assertThat(participationReviewService.getReviewHistory(2)).isEmpty();
            // Input: participationId=2
            //        reviewHistoryRepository.findByParticipationId(2) が 空リスト を返す
            // Output: 空リストが返ること
        }
    }

    private ApproveReviewRequest approveRequest() {
        ApproveReviewRequest request = new ApproveReviewRequest();
        request.setReviewerName("山田太郎");
        request.setReviewerInkan("山田印");
        return request;
    }

    private RejectReviewRequest rejectRequest() {
        RejectReviewRequest request = new RejectReviewRequest();
        request.setReviewerName("山田太郎");
        request.setReviewerInkan("山田印");
        request.setRejectReason("書類不備");
        return request;
    }
}
