package com.kct.jcredit.core_backend.service.projectplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Autofill of the operator-company master data.
 *
 * <p>This endpoint once carried its own copy of the company access check. The
 * copies drifted, so an admin got 200 from {@code /autofill/{projectId}} and
 * 403 from {@code /autofill} in the same page load. The check is shared now,
 * and these tests pin the endpoint rather than only the helper — a future
 * private copy would pass a helper test and still break this.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectPlanAutofillServiceTest {

    private static final String COMPANY_ID = "COMP1";
    private static final String USERNAME = "taro";

    @Mock private OperatorCompanyRepository operatorCompanyRepository;
    @Mock private OperatingUserRepository operatingUserRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private OperatorCompany company;
    @Mock private OperatingUser member;

    private ProjectPlanAutofillService service;

    @BeforeEach
    void setUp() {
        ProjectPlanContextService contextService =
                new ProjectPlanContextService(projectRepository, operatingUserRepository);
        service = new ProjectPlanAutofillService(
                operatorCompanyRepository, operatingUserRepository, contextService);

        when(company.getId()).thenReturn(7);
        when(company.getCompanyId()).thenReturn(COMPANY_ID);
        when(operatorCompanyRepository.findByCompanyId(COMPANY_ID))
                .thenReturn(Optional.of(company));
        when(operatingUserRepository.findByUsernameAndCompany_Id(USERNAME, 7))
                .thenReturn(Optional.of(member));
    }

    private CorporationMasterResponse asRole(String role) {
        return service.getCorporationMaster(COMPANY_ID, role, USERNAME);
    }

    @Test
    void anOperatorOfTheCompanyGetsTheMasterData() {
        assertThat(asRole(RoleConstants.OPERATOR).getStatusCode())
                .isEqualTo(ResponseConstants.OK);
    }

    @Test
    void corpNameIsTheRepresentativeNotTheCompany() {
        // 法人代表者の役職・氏名 on the official 登録申請書.
        when(company.getNameJa()).thenReturn("株式会社グリーンエナジー");
        when(company.getRepLastName()).thenReturn("山田");
        when(company.getRepFirstName()).thenReturn("太郎");

        assertThat(asRole(RoleConstants.ADMIN).getData().getCorpName()).isEqualTo("山田 太郎");
    }

    @Test
    void anAdminOfTheCompanyGetsItToo() {
        assertThat(asRole(RoleConstants.ADMIN).getStatusCode())
                .isEqualTo(ResponseConstants.OK);
    }

    @Test
    void otherRolesAreStillRefused() {
        assertThat(asRole(RoleConstants.MASTER).getStatusCode())
                .isEqualTo(ResponseConstants.FORBIDDEN);
        assertThat(asRole(RoleConstants.USER).getStatusCode())
                .isEqualTo(ResponseConstants.FORBIDDEN);
    }

    @Test
    void someoneNotRegisteredInTheCompanyIsRefused() {
        when(operatingUserRepository.findByUsernameAndCompany_Id("stranger", 7))
                .thenReturn(Optional.empty());

        assertThat(service.getCorporationMaster(COMPANY_ID, RoleConstants.ADMIN, "stranger")
                .getStatusCode())
                .isEqualTo(ResponseConstants.FORBIDDEN);
    }

    @Test
    void anUnknownCompanyIsNotFoundRatherThanForbidden() {
        when(operatorCompanyRepository.findByCompanyId("NOPE")).thenReturn(Optional.empty());

        assertThat(service.getCorporationMaster("NOPE", RoleConstants.ADMIN, USERNAME)
                .getStatusCode())
                .isEqualTo(ResponseConstants.NOT_FOUND);
    }
}
