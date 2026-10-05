package com.kct.jcredit.core_backend.projectcontrollertest;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.MemberTermsInfoResponse;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.impl.ParticipationProjectServiceImpl;

// Scope note: this file covers ONLY getMemberTermsInfo, the single method of
// ParticipationProjectServiceImpl reachable from ProjectController
// (GET /project/{projectId}/member-terms-info).
//
// PAIRED FILE: the same production class is also tested from the SiteProjectController
// round (submitParticipation, joinSite, getMembershipPdfByCompany - blocks UT-SPS-06 to
// 09). A change to ParticipationProjectServiceImpl needs both files checked.
//
// NOTE: only projectRepository is declared as a mock, because getMemberTermsInfo touches
// nothing else. Mockito's constructor injection fills the remaining dependencies with
// null, which is safe here and keeps the file from listing fifteen unused mocks.
@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationProjectServiceImpl のテスト（member-terms-info）")
class ParticipationProjectServiceImplTest {

    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private ParticipationProjectServiceImpl participationService;

    private static final String PROJECT_ID = "PUB-PJ-001";

    // ─────────────────────────────────────────────────────────────
    // Fixtures
    // ─────────────────────────────────────────────────────────────

    private OperatorCompany operatorCompanyMock() {
        OperatorCompany company = mock(OperatorCompany.class);
        lenient().when(company.getNameJa()).thenReturn("運営株式会社");
        lenient().when(company.getRepLastName()).thenReturn("熊本");
        lenient().when(company.getRepFirstName()).thenReturn("太郎");
        return company;
    }

    private Project projectMock(OperatorCompany operatorCompany) {
        Project project = mock(Project.class);
        lenient().when(project.getProjectName()).thenReturn("熊本太陽光プロジェクト");
        lenient().when(project.getMethodName()).thenReturn("太陽光発電");
        lenient().when(project.getUsePurpose()).thenReturn("自家消費");
        lenient().when(project.getOperatorCompany()).thenReturn(operatorCompany);
        lenient().when(project.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 4, 1, 9, 0));
        return project;
    }

    // ==================================================================
    // 13. getMemberTermsInfo
    // ==================================================================

    @Test
    @DisplayName("UT-PS-13-01: 正常 - 会員規約表示用のプロジェクト・運営者情報が返る")
    void getMemberTermsInfo_valid_returnsProjectAndOperatorInfo() {
        Project project = projectMock(operatorCompanyMock());
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));

        MemberTermsInfoResponse response =
                participationService.getMemberTermsInfo(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getProjectName()).isEqualTo("熊本太陽光プロジェクト");
        assertThat(response.getOperatorNameJa()).isEqualTo("運営株式会社");
        assertThat(response.getProjectOverview()).isEqualTo("太陽光発電");
        assertThat(response.getUsePurpose()).isEqualTo("自家消費");
    }

    @Test
    @DisplayName("UT-PS-13-02: 異常 - プロジェクトが存在しない場合NOT_FOUNDを返す")
    void getMemberTermsInfo_projectNotFound_returnsNotFound() {
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.empty());

        MemberTermsInfoResponse response =
                participationService.getMemberTermsInfo(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
        assertThat(response.getProjectName()).isNull();
    }

    @Test
    @DisplayName("UT-PS-13-03: 異常 - 運営会社が未設定の場合、NPEではなくNOT_FOUNDを返す")
    void getMemberTermsInfo_operatorCompanyMissing_returnsNotFoundNotNpe() {
        // These are the membership terms a member company consents to, so the page cannot
        // be shown without naming the operator the agreement is with. A project with no
        // operator company is incomplete data, not a blank field on the page.
        Project project = projectMock(null);
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));

        MemberTermsInfoResponse response =
                participationService.getMemberTermsInfo(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
    }

    @Test
    @DisplayName("UT-PS-13-04: 正常 - 代表者名は姓→名の順で連結される")
    void getMemberTermsInfo_representativeNameIsFamilyNameFirst() {
        // Japanese name order, matching how UserServiceImpl builds a full name. Reversed,
        // the operator's representative appears under the wrong name on a page that forms
        // part of an agreement.
        Project project = projectMock(operatorCompanyMock());
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));

        MemberTermsInfoResponse response =
                participationService.getMemberTermsInfo(PROJECT_ID);

        assertThat(response.getAdminRepName()).isEqualTo("熊本太郎");
    }

    @Test
    @DisplayName("UT-PS-13-05: 準正常 - 代表者の姓名が未設定でも空文字として連結される")
    void getMemberTermsInfo_missingRepNameParts_producesEmptyString() {
        // safe() turns nulls into empty strings, so a company with no representative on
        // file yields an empty name rather than the literal "nullnull".
        OperatorCompany company = mock(OperatorCompany.class);
        lenient().when(company.getNameJa()).thenReturn("運営株式会社");
        lenient().when(company.getRepLastName()).thenReturn(null);
        lenient().when(company.getRepFirstName()).thenReturn(null);

        Project project = projectMock(company);
        when(projectRepository.findByPublicIdWithFallback(PROJECT_ID))
                .thenReturn(Optional.of(project));

        MemberTermsInfoResponse response =
                participationService.getMemberTermsInfo(PROJECT_ID);

        assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
        assertThat(response.getAdminRepName()).isEmpty();
    }
}
