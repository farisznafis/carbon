package com.kct.jcredit.core_backend.controller.PartiCompanyControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.DeleteCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.DeletionRequestResponse;
import com.kct.jcredit.core_backend.entity.CompanyDeletionRequest;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.repository.postgres.CompanyDeletionRequestRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.RecordCancelRequestRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.CompanyDeletionService;

/**
 * CompanyDeletionService unit test.
 * 対象メソッド: createDeletionRequest, approveDeletionRequest, rejectDeletionRequest, softDeleteCompanyAndChildren
 */
@ExtendWith(MockitoExtension.class)
class CompanyDeletionServiceTest {

    @Mock private ParticipatingCompanyRepository participatingCompanyRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private ParticipatingUserRepository participatingUserRepository;
    @Mock private SiteProjectRelationRepository siteProjectRelationRepository;
    @Mock private SiteR002MemberDataRepository siteR002MemberDataRepository;
    @Mock private EnR002EquipmentFileRepository siteR002EquipmentFileRepository;
    @Mock private EnR002ReductionResultRepository reductionResultRepository;
    @Mock private EnR002ReductionResultTotalRepository reductionResultTotalRepository;
    @Mock private RecordCancelRequestRepository recordCancelRequestRepository;
    @Mock private CompanyDeletionRequestRepository deletionRequestRepository;

    @InjectMocks
    private CompanyDeletionService companyDeletionService;

    private ParticipatingCompany company;

    @BeforeEach
    void setUp() {
        company = new ParticipatingCompany();
        company.setId(100);
        company.setCompanyId("COMP001");
        company.setNameJa("株式会社サンプル");
    }

    private CompanyDeletionRequest pendingRequest(String requestedBy) {
        CompanyDeletionRequest request = new CompanyDeletionRequest();
        request.setId(1);
        request.setCompanyId("COMP001");
        request.setRequestedBy(requestedBy);
        request.setRequestedAt(LocalDateTime.of(2026, 6, 19, 10, 30, 0));
        request.setReason("事業都合により脱退");
        request.setStatus("PENDING");
        return request;
    }

    // ========================================================
    // createDeletionRequest
    // ========================================================
    @Nested
    @DisplayName("createDeletionRequestのテスト")
    class CreateDeletionRequestTest {

        @Test
        @DisplayName("UT-CDS-001: companyIdの会社が存在しない場合、NOT_FOUNDを返す")
        void companyNotFound_returnsNotFound() {
            when(participatingCompanyRepository.findByCompanyId("COMP999")).thenReturn(Optional.empty());

            DeletionRequestResponse response =
                    companyDeletionService.createDeletionRequest("COMP999", "master_user", "事業都合により脱退");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
            verify(deletionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-CDS-002: 会社が既に削除済み（deletedAt設定済み）の場合、BAD_REQUESTを返す")
        void companyAlreadyDeleted_returnsBadRequest() {
            company.setDeletedAt(LocalDateTime.now());
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));

            DeletionRequestResponse response =
                    companyDeletionService.createDeletionRequest("COMP001", "master_user", "事業都合により脱退");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_ALREADY_DELETED);
            assertThat(response.getCompanyId()).isEqualTo("COMP001");
            assertThat(response.getCompanyName()).isEqualTo("株式会社サンプル");
            verify(deletionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-CDS-003: 既にPENDING状態の削除申請が存在する場合、BAD_REQUESTを返す")
        void pendingRequestAlreadyExists_returnsBadRequest() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.existsByCompanyIdAndStatus("COMP001", "PENDING")).thenReturn(true);

            DeletionRequestResponse response =
                    companyDeletionService.createDeletionRequest("COMP001", "master_user", "事業都合により脱退");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_ALREADY_REQUESTED);
            assertThat(response.getCompanyId()).isEqualTo("COMP001");
            assertThat(response.getCompanyName()).isEqualTo("株式会社サンプル");
            verify(deletionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-CDS-004: 正常に削除申請が作成できた場合、PENDING状態のリクエストが保存される")
        void valid_createsPendingRequest() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.existsByCompanyIdAndStatus("COMP001", "PENDING")).thenReturn(false);

            CompanyDeletionRequest saved = pendingRequest("master_user");
            when(deletionRequestRepository.save(any(CompanyDeletionRequest.class))).thenReturn(saved);

            DeletionRequestResponse response =
                    companyDeletionService.createDeletionRequest("COMP001", "master_user", "事業都合により脱退");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_SUBMITTED);
            assertThat(response.getRequestId()).isEqualTo(1L);
            assertThat(response.getCompanyId()).isEqualTo("COMP001");
            assertThat(response.getCompanyName()).isEqualTo("株式会社サンプル");
            assertThat(response.getRequestedBy()).isEqualTo("master_user");
            assertThat(response.getReason()).isEqualTo("事業都合により脱退");
            assertThat(response.getStatus()).isEqualTo("PENDING");

            ArgumentCaptor<CompanyDeletionRequest> captor = ArgumentCaptor.forClass(CompanyDeletionRequest.class);
            verify(deletionRequestRepository).save(captor.capture());
            CompanyDeletionRequest captured = captor.getValue();
            assertThat(captured.getCompanyId()).isEqualTo("COMP001");
            assertThat(captured.getRequestedBy()).isEqualTo("master_user");
            assertThat(captured.getStatus()).isEqualTo("PENDING");
            assertThat(captured.getReason()).isEqualTo("事業都合により脱退");
        }

        @Test
        @DisplayName("UT-CDS-005: 保存処理中に例外が発生した場合、INTERNAL_SERVER_ERRORを返す")
        void saveThrowsException_returnsInternalServerError() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.existsByCompanyIdAndStatus("COMP001", "PENDING")).thenReturn(false);
            when(deletionRequestRepository.save(any(CompanyDeletionRequest.class)))
                    .thenThrow(new RuntimeException("DB error"));

            DeletionRequestResponse response =
                    companyDeletionService.createDeletionRequest("COMP001", "master_user", "事業都合により脱退");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
        }
    }

    // ========================================================
    // approveDeletionRequest
    // ========================================================
    @Nested
    @DisplayName("approveDeletionRequestのテスト")
    class ApproveDeletionRequestTest {

        @Test
        @DisplayName("UT-CDS-006: companyIdの会社が存在しない場合、NOT_FOUNDを返す")
        void companyNotFound_returnsNotFound() {
            when(participatingCompanyRepository.findByCompanyId("COMP999")).thenReturn(Optional.empty());

            DeletionRequestResponse response =
                    companyDeletionService.approveDeletionRequest("COMP999", "operator_admin");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
            verify(deletionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-CDS-007: PENDING状態の削除申請が存在しない場合、NOT_FOUNDを返す")
        void requestNotFound_returnsNotFound() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.findByCompanyIdAndStatus("COMP001", "PENDING"))
                    .thenReturn(Optional.empty());

            DeletionRequestResponse response =
                    companyDeletionService.approveDeletionRequest("COMP001", "operator_admin");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_NOT_FOUND);
            assertThat(response.getCompanyId()).isEqualTo("COMP001");
            assertThat(response.getCompanyName()).isEqualTo("株式会社サンプル");
            verify(deletionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-CDS-008: 申請者自身が承認しようとした場合、BAD_REQUESTを返す（自己承認禁止）")
        void approverIsRequester_returnsBadRequest() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.findByCompanyIdAndStatus("COMP001", "PENDING"))
                    .thenReturn(Optional.of(pendingRequest("master_user")));

            DeletionRequestResponse response =
                    companyDeletionService.approveDeletionRequest("COMP001", "master_user");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_CANNOT_APPROVE_OWN_REQUEST);
            assertThat(response.getCompanyId()).isEqualTo("COMP001");
            verify(deletionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-CDS-009: 正常に承認された場合、申請がAPPROVEDに更新され関連データが論理削除される")
        void valid_approvesRequestAndSoftDeletes() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.findByCompanyIdAndStatus("COMP001", "PENDING"))
                    .thenReturn(Optional.of(pendingRequest("master_user")));
            when(deletionRequestRepository.save(any(CompanyDeletionRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            DeletionRequestResponse response =
                    companyDeletionService.approveDeletionRequest("COMP001", "operator_admin");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_APPROVED);
            assertThat(response.getRequestId()).isEqualTo(1L);
            assertThat(response.getCompanyId()).isEqualTo("COMP001");
            assertThat(response.getStatus()).isEqualTo("APPROVED");
            assertThat(response.getDecidedBy()).isEqualTo("operator_admin");
            assertThat(response.getDecidedAt()).isNotNull();

            ArgumentCaptor<CompanyDeletionRequest> captor = ArgumentCaptor.forClass(CompanyDeletionRequest.class);
            verify(deletionRequestRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("APPROVED");
            assertThat(captor.getValue().getDecidedBy()).isEqualTo("operator_admin");

            // カスケード論理削除（softDeleteCompanyAndChildren内部処理）の呼び出し検証
            verify(reductionResultRepository).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(reductionResultTotalRepository).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(recordCancelRequestRepository).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(siteR002EquipmentFileRepository).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(siteR002MemberDataRepository).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(siteProjectRelationRepository).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(siteRepository).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(participatingCompanyRepository).softDeleteByCompanyId("COMP001", "operator_admin");
        }

        @Test
        @DisplayName("UT-CDS-010: 承認後のカスケード削除処理で例外が発生した場合、申請はAPPROVEDのままレスポンスはINTERNAL_SERVER_ERRORを返す")
        void cascadeDeleteFails_returnsInternalServerErrorButRequestApproved() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.findByCompanyIdAndStatus("COMP001", "PENDING"))
                    .thenReturn(Optional.of(pendingRequest("master_user")));
            when(deletionRequestRepository.save(any(CompanyDeletionRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            doThrow(new RuntimeException("DB error"))
                    .when(reductionResultRepository).softDeleteByCompanyId(eq("COMP001"), anyString());

            DeletionRequestResponse response =
                    companyDeletionService.approveDeletionRequest("COMP001", "operator_admin");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            // 申請自体は承認処理（save）が先に実行されているため、ステータス表示はAPPROVEDのまま
            assertThat(response.getStatus()).isEqualTo("APPROVED");
            verify(participatingCompanyRepository, never()).softDeleteByCompanyId(anyString(), anyString());
        }
    }

    // ========================================================
    // rejectDeletionRequest
    // ========================================================
    @Nested
    @DisplayName("rejectDeletionRequestのテスト")
    class RejectDeletionRequestTest {

        @Test
        @DisplayName("UT-CDS-011: companyIdの会社が存在しない場合、NOT_FOUNDを返す")
        void companyNotFound_returnsNotFound() {
            when(participatingCompanyRepository.findByCompanyId("COMP999")).thenReturn(Optional.empty());

            DeletionRequestResponse response =
                    companyDeletionService.rejectDeletionRequest("COMP999", "operator_admin", "提出書類に不備があるため却下");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
            verify(deletionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-CDS-012: PENDING状態の削除申請が存在しない場合、NOT_FOUNDを返す")
        void requestNotFound_returnsNotFound() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.findByCompanyIdAndStatus("COMP001", "PENDING"))
                    .thenReturn(Optional.empty());

            DeletionRequestResponse response =
                    companyDeletionService.rejectDeletionRequest("COMP001", "operator_admin", "提出書類に不備があるため却下");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_NOT_FOUND);
            assertThat(response.getCompanyId()).isEqualTo("COMP001");
            verify(deletionRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("UT-CDS-013: 正常に却下された場合、申請がREJECTEDに更新されrejectionReasonが保存される")
        void valid_rejectsRequest() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.findByCompanyIdAndStatus("COMP001", "PENDING"))
                    .thenReturn(Optional.of(pendingRequest("master_user")));
            when(deletionRequestRepository.save(any(CompanyDeletionRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            DeletionRequestResponse response = companyDeletionService.rejectDeletionRequest(
                    "COMP001", "operator_admin", "提出書類に不備があるため却下");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_DELETION_REQUEST_REJECTED);
            assertThat(response.getRequestId()).isEqualTo(1L);
            assertThat(response.getCompanyId()).isEqualTo("COMP001");
            assertThat(response.getStatus()).isEqualTo("REJECTED");
            assertThat(response.getDecidedBy()).isEqualTo("operator_admin");
            assertThat(response.getRejectionReason()).isEqualTo("提出書類に不備があるため却下");

            ArgumentCaptor<CompanyDeletionRequest> captor = ArgumentCaptor.forClass(CompanyDeletionRequest.class);
            verify(deletionRequestRepository).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("REJECTED");
            assertThat(captor.getValue().getRejectionReason()).isEqualTo("提出書類に不備があるため却下");

            // 却下時はカスケード削除処理が実行されないこと
            verify(participatingCompanyRepository, never()).softDeleteByCompanyId(anyString(), anyString());
        }

        @Test
        @DisplayName("UT-CDS-014: rejectionReasonがnullでも却下処理が成功する（必須チェックなし）")
        void nullRejectionReason_stillSucceeds() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            when(deletionRequestRepository.findByCompanyIdAndStatus("COMP001", "PENDING"))
                    .thenReturn(Optional.of(pendingRequest("master_user")));
            when(deletionRequestRepository.save(any(CompanyDeletionRequest.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            DeletionRequestResponse response =
                    companyDeletionService.rejectDeletionRequest("COMP001", "operator_admin", null);

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getRejectionReason()).isNull();
        }
    }

    // ========================================================
    // softDeleteCompanyAndChildren
    // ========================================================
    @Nested
    @DisplayName("softDeleteCompanyAndChildrenのテスト")
    class SoftDeleteCompanyAndChildrenTest {

        @Test
        @DisplayName("UT-CDS-015: companyIdの会社が存在しない場合、NOT_FOUNDを返す")
        void companyNotFound_returnsNotFound() {
            when(participatingCompanyRepository.findByCompanyId("COMP999")).thenReturn(Optional.empty());

            DeleteCompanyResponse response =
                    companyDeletionService.softDeleteCompanyAndChildren("COMP999", "operator_admin");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
        }

        @Test
        @DisplayName("UT-CDS-016: 会社が既に削除済みの場合、BAD_REQUESTを返す")
        void alreadyDeleted_returnsBadRequest() {
            company.setDeletedAt(LocalDateTime.now());
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));

            DeleteCompanyResponse response =
                    companyDeletionService.softDeleteCompanyAndChildren("COMP001", "operator_admin");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_COMPANY_ALREADY_DELETED);
            verify(participatingCompanyRepository, never()).softDeleteByCompanyId(anyString(), anyString());
        }

        @Test
        @DisplayName("UT-CDS-017: 正常にカスケード論理削除が実行された場合、全関連リポジトリが呼び出される")
        void valid_cascadesSoftDeleteAllRelatedData() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));

            DeleteCompanyResponse response =
                    companyDeletionService.softDeleteCompanyAndChildren("COMP001", "operator_admin");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_DELETE_SUCCESS);
            assertThat(response.getCompanyId()).isEqualTo("COMP001");
            assertThat(response.getCompanyName()).isEqualTo("株式会社サンプル");

            verify(reductionResultRepository, times(1)).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(reductionResultTotalRepository, times(1)).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(recordCancelRequestRepository, times(1)).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(siteR002EquipmentFileRepository, times(1)).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(siteR002MemberDataRepository, times(1)).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(siteProjectRelationRepository, times(1)).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(siteRepository, times(1)).softDeleteByCompanyId("COMP001", "operator_admin");
            verify(participatingCompanyRepository, times(1)).softDeleteByCompanyId("COMP001", "operator_admin");
        }

        @Test
        @DisplayName("UT-CDS-018: カスケード削除処理中に例外が発生した場合、INTERNAL_SERVER_ERRORを返す")
        void cascadeDeleteThrowsException_returnsInternalServerError() {
            when(participatingCompanyRepository.findByCompanyId("COMP001")).thenReturn(Optional.of(company));
            doThrow(new RuntimeException("DB error"))
                    .when(siteRepository).softDeleteByCompanyId(eq("COMP001"), anyString());

            DeleteCompanyResponse response =
                    companyDeletionService.softDeleteCompanyAndChildren("COMP001", "operator_admin");

            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
            // 例外発生箇所以降の処理（participatingCompanyのsoftDelete等）は呼ばれない
            verify(participatingCompanyRepository, never()).softDeleteByCompanyId(anyString(), anyString());
        }
    }
}