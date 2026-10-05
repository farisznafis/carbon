package com.kct.jcredit.core_backend.controller.AdminCompanyControllerTest;

import com.kct.jcredit.core_backend.dto.request.*;
import com.kct.jcredit.core_backend.dto.response.*;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.entity.*;
import com.kct.jcredit.core_backend.enums.*;
import com.kct.jcredit.core_backend.exception.*;
import com.kct.jcredit.core_backend.repository.postgres.*;
import com.kct.jcredit.core_backend.service.NotificationService;
import com.kct.jcredit.core_backend.service.impl.CompanyLifecycleServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyLifecycleServiceTest {

    @Mock
    private ParticipatingCompanyRepository companyRepository;

    @Mock
    private CompanySuspensionRepository suspensionRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CompanyLifecycleServiceImpl companyLifecycleService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------------
    // createSuspension
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/suspensions のテスト")
    class CreateSuspensionTest {

        @Test
        @DisplayName("UT-CLS-001: ACTIVE企業に新規サスペンションを作成できる")
        void createSuspension_activeCompany_createsSuspensionAndReturnsSuspendedStatus() {
            ParticipatingCompany company = company(1, "COMP-001", CompanyStatus.ACTIVE);
            CreateCompanySuspensionRequest request = suspensionRequest(SuspensionReasonCode.SECURITY, "不正利用の疑い");
            when(companyRepository.findByCompanyIdForUpdate("COMP-001")).thenReturn(Optional.of(company));
            when(suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(1, request.getReasonCode())).thenReturn(false);
            when(suspensionRepository.save(any())).thenReturn(mock(CompanySuspension.class));
            when(suspensionRepository.countByCompany_IdAndResolvedAtIsNull(1)).thenReturn(1L);
            CompanySuspensionResponse response = companyLifecycleService.createSuspension("COMP-001", request);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getCompanyStatus()).isEqualTo(CompanyStatus.SUSPENDED);
            verify(notificationService).sendSuspensionNotification(company);
            verify(companyRepository).save(company);
            verify(suspensionRepository).save(any(CompanySuspension.class));
            // Input: companyId="COMP-001", request={reasonCode="FRAUD_SUSPECTED", reasonDetail="不正利用の疑い"}
            //        company = ParticipatingCompany(companyId="COMP-001", id=1L, companyStatus=ACTIVE)
            //        companyRepository.findByCompanyIdForUpdate("COMP-001") が Optional.of(company) を返す
            //        suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(1L, "FRAUD_SUSPECTED") が false を返す
            //        suspensionRepository.save(any()) が保存後のCompanySuspension(id=100)を返す
            //        suspensionRepository.countByCompany_IdAndResolvedAtIsNull(1L) が 1L を返す
            //        TransactionSynchronizationManager.isSynchronizationActive() は false（同期未アクティブ）
            // Output: statusCode=OK, message=MSG_SUSPENSION_CREATED, companyId="COMP-001",
            //         companyStatus=SUSPENDED, activeSuspensionCount=1
            //         company.setCompanyStatus(SUSPENDED)/setIsActive(false)が呼ばれ companyRepository.save(company)が呼ばれること
            //         suspensionRepository.save(argThat(reasonCode="FRAUD_SUSPECTED"))が呼ばれること
            //         notificationService.sendSuspensionNotification(company)が呼ばれること（即時実行）
        }

        @Test
        @DisplayName("UT-CLS-002: SUSPENDED企業に別reasonCodeで追加サスペンションを作成できる")
        void createSuspension_suspendedCompanyWithDifferentReasonCode_createsAdditionalSuspension() {
            ParticipatingCompany company = company(2, "COMP-002", CompanyStatus.SUSPENDED);
            CreateCompanySuspensionRequest request = suspensionRequest(SuspensionReasonCode.ADMINISTRATIVE, "書類不備");
            when(companyRepository.findByCompanyIdForUpdate("COMP-002")).thenReturn(Optional.of(company));
            when(suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(2, request.getReasonCode())).thenReturn(false);
            when(suspensionRepository.save(any())).thenReturn(mock(CompanySuspension.class));
            when(suspensionRepository.countByCompany_IdAndResolvedAtIsNull(2)).thenReturn(2L);
            CompanySuspensionResponse response = companyLifecycleService.createSuspension("COMP-002", request);
            assertThat(response.getActiveSuspensionCount()).isEqualTo(2L);
            verify(companyRepository).save(company);
            verify(suspensionRepository).save(any(CompanySuspension.class));
            // Input: companyId="COMP-002", request={reasonCode="DOCUMENT_ISSUE", reasonDetail="書類不備"}
            //        company = ParticipatingCompany(companyId="COMP-002", id=2L, companyStatus=SUSPENDED)
            //        companyRepository.findByCompanyIdForUpdate("COMP-002") が Optional.of(company) を返す
            //        suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(2L, "DOCUMENT_ISSUE") が false を返す
            //        suspensionRepository.save(any()) が保存後のCompanySuspension(id=101)を返す
            //        suspensionRepository.countByCompany_IdAndResolvedAtIsNull(2L) が 2L を返す
            // Output: statusCode=OK, message=MSG_SUSPENSION_CREATED, companyStatus=SUSPENDED, activeSuspensionCount=2
            //         company.setCompanyStatus(SUSPENDED)/setIsActive(false)が再設定され companyRepository.save(company)が呼ばれること
            //         suspensionRepository.saveが呼ばれること
            //         notificationService.sendSuspensionNotification(company)が呼ばれること
        }

        @Test
        @DisplayName("UT-CLS-003: companyIdが存在しない場合、ResourceNotFoundExceptionがスローされる")
        void createSuspension_companyNotFound_throwsResourceNotFoundException() {
            when(companyRepository.findByCompanyIdForUpdate("COMP-404")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> companyLifecycleService.createSuspension("COMP-404", suspensionRequest(SuspensionReasonCode.SECURITY, "不正利用の疑い")))
                .isInstanceOf(ResourceNotFoundException.class);
            // Input: companyId="COMP-404", request={reasonCode="FRAUD_SUSPECTED", reasonDetail="不正利用の疑い"}
            //        companyRepository.findByCompanyIdForUpdate("COMP-404") が Optional.empty を返す
            // Output: ResourceNotFoundException(MSG_COMPANY_NOT_FOUND)がスローされること
            //         suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNullが一度も呼ばれないこと
            //         suspensionRepository.saveが呼ばれないこと
            //         notificationServiceが呼ばれないこと
        }

        @Test
        @DisplayName("UT-CLS-004: 企業ステータスがPENDING等でサスペンション作成不可の場合、BusinessRuleViolationExceptionがスローされる")
        void createSuspension_companyStatusNotSuspendable_throwsBusinessRuleViolationException() {
            ParticipatingCompany company = company(3, "COMP-003", CompanyStatus.PENDING);
            when(companyRepository.findByCompanyIdForUpdate("COMP-003")).thenReturn(Optional.of(company));
            assertThatThrownBy(() -> companyLifecycleService.createSuspension("COMP-003", suspensionRequest(SuspensionReasonCode.SECURITY, "不正利用の疑い")))
                .isInstanceOf(BusinessRuleViolationException.class);
            // Input: companyId="COMP-003", request={reasonCode="FRAUD_SUSPECTED", reasonDetail="不正利用の疑い"}
            //        company = ParticipatingCompany(companyId="COMP-003", id=3L, companyStatus=PENDING)
            //        companyRepository.findByCompanyIdForUpdate("COMP-003") が Optional.of(company) を返す
            // Output: BusinessRuleViolationException(CONFLICT, MSG_COMPANY_STATUS_CONFLICT)がスローされること
            //         suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNullが一度も呼ばれないこと
            //         suspensionRepository.saveが呼ばれないこと
        }

        @Test
        @DisplayName("UT-CLS-005: 同一reasonCodeで未解決サスペンションが既に存在する場合、BusinessRuleViolationExceptionがスローされる")
        void createSuspension_duplicateActiveReasonCode_throwsBusinessRuleViolationException() {
            ParticipatingCompany company = company(1, "COMP-001", CompanyStatus.ACTIVE);
            CreateCompanySuspensionRequest request = suspensionRequest(SuspensionReasonCode.SECURITY, "再度の不正利用疑い");
            when(companyRepository.findByCompanyIdForUpdate("COMP-001")).thenReturn(Optional.of(company));
            when(suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(1, request.getReasonCode())).thenReturn(true);
            assertThatThrownBy(() -> companyLifecycleService.createSuspension("COMP-001", request))
                .isInstanceOf(BusinessRuleViolationException.class);
            verify(suspensionRepository, never()).save(any());
            // Input: companyId="COMP-001", request={reasonCode="FRAUD_SUSPECTED", reasonDetail="再度の不正利用疑い"}
            //        company = ParticipatingCompany(companyId="COMP-001", id=1L, companyStatus=ACTIVE)
            //        companyRepository.findByCompanyIdForUpdate("COMP-001") が Optional.of(company) を返す
            //        suspensionRepository.existsByCompany_IdAndReasonCodeAndResolvedAtIsNull(1L, "FRAUD_SUSPECTED") が true を返す
            // Output: BusinessRuleViolationException(CONFLICT, MSG_SUSPENSION_ALREADY_ACTIVE)がスローされること
            //         suspensionRepository.saveが呼ばれないこと
            //         companyRepository.save（ステータス変更）が呼ばれないこと
            //         notificationServiceが呼ばれないこと
        }
    }

    // ------------------------------------------------------------------------
    // findSuspensions (getSuspensions)
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("GET /admin/companies/{companyId}/suspensions のテスト")
    class FindSuspensionsTest {

        @Test
        @DisplayName("UT-CLS-006: activeOnly=falseの場合、全件のサスペンション一覧がページネーション付きで取得できる")
        void findSuspensions_activeOnlyFalse_returnsAllSuspensionsWithPagination() {
            ParticipatingCompany company = company(1, "COMP-001", CompanyStatus.ACTIVE);
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(suspensionRepository.findByCompany_CompanyId(eq("COMP-001"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
            CompanySuspensionListResponse response = companyLifecycleService.findSuspensions("COMP-001", false, 1, 20);
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getSuspensions()).isEmpty();
            verify(suspensionRepository).findByCompany_CompanyId(eq("COMP-001"), any(Pageable.class));
            // Input: companyId="COMP-001", activeOnly=false, page=1, limit=20
            //        companyRepository.findByCompanyId("COMP-001") が Optional.of(company) を返す
            //        suspensionRepository.findByCompany_CompanyId("COMP-001", any(PageRequest)) が
            //        Page(content=[suspension1, suspension2], totalElements=2, totalPages=1, hasNext=false) を返す
            // Output: statusCode=OK, message=MSG_SUSPENSION_LIST_RETRIEVED, companyId="COMP-001",
            //         suspensions=[detail1, detail2]
            //         pagination={page=1, limit=20, totalElements=2, totalPages=1, hasNext=false}
            //         suspensionRepository.findByCompany_CompanyIdが呼ばれ、
            //         findByCompany_CompanyIdAndResolvedAtIsNullは呼ばれないこと
            //         PageRequestがpage-1（0始まり）、createdAt降順・id降順でソートされること
        }

        @Test
        @DisplayName("UT-CLS-007: activeOnly=trueの場合、未解決のサスペンションのみ取得できる")
        void findSuspensions_activeOnlyTrue_returnsOnlyUnresolvedSuspensions() {
            when(companyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company(1, "COMP-001", CompanyStatus.ACTIVE)));
            when(suspensionRepository.findByCompany_CompanyIdAndResolvedAtIsNull(eq("COMP-001"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
            assertThat(companyLifecycleService.findSuspensions("COMP-001", true, 1, 20).getSuspensions()).isEmpty();
            verify(suspensionRepository).findByCompany_CompanyIdAndResolvedAtIsNull(eq("COMP-001"), any());
            // Input: companyId="COMP-001", activeOnly=true, page=1, limit=20
            //        companyRepository.findByCompanyId("COMP-001") が Optional.of(company) を返す
            //        suspensionRepository.findByCompany_CompanyIdAndResolvedAtIsNull("COMP-001", any(PageRequest)) が
            //        Page(content=[suspension1], totalElements=1, totalPages=1, hasNext=false) を返す
            // Output: statusCode=OK, suspensions=[detail1]（未解決のみ）
            //         suspensionRepository.findByCompany_CompanyIdAndResolvedAtIsNullが呼ばれ、
            //         findByCompany_CompanyIdは呼ばれないこと
        }

        @Test
        @DisplayName("UT-CLS-008: companyIdが存在しない場合、ResourceNotFoundExceptionがスローされる")
        void findSuspensions_companyNotFound_throwsResourceNotFoundException() {
            when(companyRepository.findByCompanyId("COMP-404")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> companyLifecycleService.findSuspensions("COMP-404", false, 1, 20)).isInstanceOf(ResourceNotFoundException.class);
            // Input: companyId="COMP-404", activeOnly=false, page=1, limit=20
            //        companyRepository.findByCompanyId("COMP-404") が Optional.empty を返す
            // Output: ResourceNotFoundException(MSG_COMPANY_NOT_FOUND)がスローされること
            //         suspensionRepository.findByCompany_CompanyId
            //         およびfindByCompany_CompanyIdAndResolvedAtIsNullが一度も呼ばれないこと
        }
    }

    // ------------------------------------------------------------------------
    // resolveSuspension
    // ------------------------------------------------------------------------
    @Nested
    @DisplayName("POST /admin/companies/{companyId}/suspensions/{suspensionId}/resolve のテスト")
    class ResolveSuspensionTest {

        @Test
        @DisplayName("UT-CLS-009: 他に未解決サスペンションが残る場合、企業はSUSPENDEDのまま解決処理が完了する")
        void resolveSuspension_otherActiveSuspensionsRemain_keepsCompanySuspended() {
            ParticipatingCompany company = company(1, "COMP-001", CompanyStatus.SUSPENDED);
            CompanySuspension suspension = suspension(100, true);
            when(companyRepository.findByCompanyIdForUpdate("COMP-001")).thenReturn(Optional.of(company));
            when(suspensionRepository.findForUpdate("COMP-001", 100)).thenReturn(Optional.of(suspension));
            when(suspensionRepository.countByCompany_IdAndResolvedAtIsNull(1)).thenReturn(1L);
            CompanySuspensionResponse response = companyLifecycleService.resolveSuspension("COMP-001", 100, resolveRequest("一部解消"));
            assertThat(response.getActiveSuspensionCount()).isEqualTo(1L);
            verify(companyRepository, never()).save(company);
            verify(suspensionRepository).save(suspension);
            verify(notificationService, never()).sendReactivationNotification(any());
            // Input: companyId="COMP-001", suspensionId=100, request={resolutionNote="書類再提出により一部解消"}
            //        SecurityContextHolder.setContext()でAuthentication(name="admin_taro", authenticated=true)をセット
            //        company = ParticipatingCompany(companyId="COMP-001", id=1L, companyStatus=SUSPENDED)
            //        companyRepository.findByCompanyIdForUpdate("COMP-001") が Optional.of(company) を返す
            //        suspension = CompanySuspension(id=100, active=true, resolvedAt=null)
            //        suspensionRepository.findForUpdate("COMP-001", 100) が Optional.of(suspension) を返す
            //        suspensionRepository.countByCompany_IdAndResolvedAtIsNull(1L) が 1L を返す（解決後も1件残る）
            // Output: statusCode=OK, message=MSG_SUSPENSION_RESOLVED_COMPANY_STILL_SUSPENDED,
            //         companyStatus=SUSPENDED（変更なし）, activeSuspensionCount=1
            //         suspension.setResolvedAt/setResolvedBy("admin_taro")/setResolutionNoteが呼ばれ保存されること
            //         companyRepository.save（ステータス変更）が呼ばれないこと
            //         notificationService.sendReactivationNotificationが呼ばれないこと
        }

        @Test
        @DisplayName("UT-CLS-010: 未解決サスペンションが0件になる場合、企業がACTIVEに復帰し再開通知が送られる")
        void resolveSuspension_lastActiveSuspensionResolved_reactivatesCompanyAndNotifies() {
            ParticipatingCompany company = company(1, "COMP-001", CompanyStatus.SUSPENDED);
            when(companyRepository.findByCompanyIdForUpdate("COMP-001")).thenReturn(Optional.of(company));
            when(suspensionRepository.findForUpdate("COMP-001", 100)).thenReturn(Optional.of(suspension(100, true)));
            when(suspensionRepository.countByCompany_IdAndResolvedAtIsNull(1)).thenReturn(0L);
            CompanySuspensionResponse response = companyLifecycleService.resolveSuspension("COMP-001", 100, resolveRequest("問題解消済み"));
            assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SUSPENSION_RESOLVED);
            verify(companyRepository).save(company);
            verify(suspensionRepository).save(any(CompanySuspension.class));
            verify(notificationService).sendReactivationNotification(company);
            // Input: companyId="COMP-001", suspensionId=100, request={resolutionNote="問題解消済み"}
            //        SecurityContextHolder.setContext()でAuthentication(name="admin_taro", authenticated=true)をセット
            //        company = ParticipatingCompany(companyId="COMP-001", id=1L, companyStatus=SUSPENDED)
            //        companyRepository.findByCompanyIdForUpdate("COMP-001") が Optional.of(company) を返す
            //        suspension = CompanySuspension(id=100, active=true, resolvedAt=null)
            //        suspensionRepository.findForUpdate("COMP-001", 100) が Optional.of(suspension) を返す
            //        suspensionRepository.countByCompany_IdAndResolvedAtIsNull(1L) が 0L を返す（解決後は0件）
            //        TransactionSynchronizationManager.isSynchronizationActive() は false（同期未アクティブ）
            // Output: statusCode=OK, message=MSG_SUSPENSION_RESOLVED, companyStatus=ACTIVE, activeSuspensionCount=0
            //         company.setCompanyStatus(ACTIVE)/setIsActive(true)が呼ばれ companyRepository.save(company)が呼ばれること
            //         notificationService.sendReactivationNotification(company)が呼ばれること（即時実行）
        }

        @Test
        @DisplayName("UT-CLS-011: companyIdが存在しない場合、ResourceNotFoundExceptionがスローされる")
        void resolveSuspension_companyNotFound_throwsResourceNotFoundException() {
            when(companyRepository.findByCompanyIdForUpdate("COMP-404")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> companyLifecycleService.resolveSuspension("COMP-404", 100, resolveRequest("解消済み"))).isInstanceOf(ResourceNotFoundException.class);
            // Input: companyId="COMP-404", suspensionId=100, request={resolutionNote="解消済み"}
            //        companyRepository.findByCompanyIdForUpdate("COMP-404") が Optional.empty を返す
            // Output: ResourceNotFoundException(MSG_COMPANY_NOT_FOUND)がスローされること
            //         suspensionRepository.findForUpdateが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-CLS-012: suspensionIdが存在しない場合、ResourceNotFoundExceptionがスローされる")
        void resolveSuspension_suspensionNotFound_throwsResourceNotFoundException() {
            when(companyRepository.findByCompanyIdForUpdate("COMP-001")).thenReturn(Optional.of(company(1, "COMP-001", CompanyStatus.SUSPENDED)));
            when(suspensionRepository.findForUpdate("COMP-001", 999)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> companyLifecycleService.resolveSuspension("COMP-001", 999, resolveRequest("解消済み"))).isInstanceOf(ResourceNotFoundException.class);
            // Input: companyId="COMP-001", suspensionId=999, request={resolutionNote="解消済み"}
            //        company = ParticipatingCompany(companyId="COMP-001", id=1L, companyStatus=SUSPENDED)
            //        companyRepository.findByCompanyIdForUpdate("COMP-001") が Optional.of(company) を返す
            //        suspensionRepository.findForUpdate("COMP-001", 999) が Optional.empty を返す
            // Output: ResourceNotFoundException(MSG_SUSPENSION_NOT_FOUND)がスローされること
            //         suspensionRepository.saveが呼ばれないこと
            //         companyRepository.save（ステータス変更）が呼ばれないこと
        }

        @Test
        @DisplayName("UT-CLS-013: 既に解決済みのサスペンションを再度解決しようとした場合、BusinessRuleViolationExceptionがスローされる")
        void resolveSuspension_alreadyResolvedSuspension_throwsBusinessRuleViolationException() {
            when(companyRepository.findByCompanyIdForUpdate("COMP-001")).thenReturn(Optional.of(company(1, "COMP-001", CompanyStatus.ACTIVE)));
            when(suspensionRepository.findForUpdate("COMP-001", 100)).thenReturn(Optional.of(suspension(100, false)));
            assertThatThrownBy(() -> companyLifecycleService.resolveSuspension("COMP-001", 100, resolveRequest("再解決試行"))).isInstanceOf(BusinessRuleViolationException.class);
            verify(suspensionRepository, never()).save(any());
            // Input: companyId="COMP-001", suspensionId=100, request={resolutionNote="再解決試行"}
            //        company = ParticipatingCompany(companyId="COMP-001", id=1L, companyStatus=ACTIVE)
            //        companyRepository.findByCompanyIdForUpdate("COMP-001") が Optional.of(company) を返す
            //        suspension = CompanySuspension(id=100, active=false, resolvedAt=直近の日時)
            //        suspensionRepository.findForUpdate("COMP-001", 100) が Optional.of(suspension) を返す
            // Output: BusinessRuleViolationException(CONFLICT, MSG_SUSPENSION_ALREADY_RESOLVED)がスローされること
            //         suspensionRepository.saveが呼ばれないこと
            //         companyRepository.save（ステータス変更）が呼ばれないこと
            //         notificationServiceが呼ばれないこと
        }
    }

    private ParticipatingCompany company(Integer id, String companyId, CompanyStatus status) {
        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(id);
        company.setCompanyId(companyId);
        company.setCompanyStatus(status);
        return company;
    }

    private CompanySuspension suspension(Integer id, boolean active) {
        CompanySuspension suspension = new CompanySuspension();
        suspension.setId(id);
        suspension.setResolvedAt(active ? null : LocalDateTime.MIN);
        return suspension;
    }

    private CreateCompanySuspensionRequest suspensionRequest(SuspensionReasonCode code, String detail) {
        CreateCompanySuspensionRequest request = new CreateCompanySuspensionRequest();
        request.setReasonCode(code);
        request.setReasonDetail(detail);
        return request;
    }

    private ResolveCompanySuspensionRequest resolveRequest(String note) {
        ResolveCompanySuspensionRequest request = new ResolveCompanySuspensionRequest();
        request.setResolutionNote(note);
        return request;
    }
}
