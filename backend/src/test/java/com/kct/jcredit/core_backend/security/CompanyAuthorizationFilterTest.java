package com.kct.jcredit.core_backend.security;

import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.enums.CompanyStatus;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyAuthorizationFilterTest {

    private static final String TOKEN = "operator-token";
    private static final String OPERATOR_USERNAME = "op_admin01";
    private static final String OPERATOR_COMPANY_ID = "OP000000001";

    @Mock
    private ParticipatingCompanyRepository participatingCompanyRepository;

    @Mock
    private ParticipatingUserRepository participatingUserRepository;

    @Mock
    private OperatingUserRepository operatingUserRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FilterChain filterChain;

    private CompanyAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CompanyAuthorizationFilter(
                participatingCompanyRepository,
                participatingUserRepository,
                operatingUserRepository,
                jwtUtil
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        OPERATOR_USERNAME,
                        TOKEN,
                        List.of(new SimpleGrantedAuthority("ROLE_operator"))
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void operatorWithSameNumericIdAsSuspendedParticipatingCompanyIsAuthorized() throws Exception {
        stubOperatorClaims(1, OPERATOR_COMPANY_ID);
        OperatingUser operator = buildOperator(1, true, true, OPERATOR_COMPANY_ID);
        when(operatingUserRepository.findForAuthorization(OPERATOR_USERNAME, OPERATOR_COMPANY_ID))
                .thenReturn(Optional.of(operator));

        MockHttpServletRequest request = protectedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(participatingCompanyRepository);
        assertEquals(200, response.getStatus());
    }

    @Test
    void inactiveOperatingUserIsForbidden() throws Exception {
        stubOperatorClaims(1, OPERATOR_COMPANY_ID);
        OperatingUser operator = buildOperator(1, false, true, OPERATOR_COMPANY_ID);
        when(operatingUserRepository.findForAuthorization(OPERATOR_USERNAME, OPERATOR_COMPANY_ID))
                .thenReturn(Optional.of(operator));

        MockHttpServletResponse response = executeProtectedRequest();

        assertForbidden(response);
        verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(participatingCompanyRepository);
    }

    @Test
    void inactiveOperatorCompanyIsForbidden() throws Exception {
        stubOperatorClaims(1, OPERATOR_COMPANY_ID);
        OperatingUser operator = buildOperator(1, true, false, OPERATOR_COMPANY_ID);
        when(operatingUserRepository.findForAuthorization(OPERATOR_USERNAME, OPERATOR_COMPANY_ID))
                .thenReturn(Optional.of(operator));

        MockHttpServletResponse response = executeProtectedRequest();

        assertForbidden(response);
        verifyNoInteractions(participatingCompanyRepository);
    }

    @Test
    void mismatchedOperatorUserIdIsForbidden() throws Exception {
        stubOperatorClaims(1, OPERATOR_COMPANY_ID);
        OperatingUser operator = buildOperator(2, true, true, OPERATOR_COMPANY_ID);
        when(operatingUserRepository.findForAuthorization(OPERATOR_USERNAME, OPERATOR_COMPANY_ID))
                .thenReturn(Optional.of(operator));

        MockHttpServletResponse response = executeProtectedRequest();

        assertForbidden(response);
        verifyNoInteractions(participatingCompanyRepository);
    }

    @Test
    void mismatchedOperatorBusinessCompanyIdIsForbidden() throws Exception {
        String mismatchedCompanyId = "OP000000999";
        stubOperatorClaims(1, mismatchedCompanyId);
        when(operatingUserRepository.findForAuthorization(OPERATOR_USERNAME, mismatchedCompanyId))
                .thenReturn(Optional.empty());

        MockHttpServletResponse response = executeProtectedRequest();

        assertForbidden(response);
        verifyNoInteractions(participatingCompanyRepository);
    }

    @Test
    void suspendedParticipatingCompanyStillReturnsSuspended() throws Exception {
        String participatingCompanyId = "PC000000001";
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(participatingCompanyId);
        when(jwtUtil.extractRole(TOKEN)).thenReturn(RoleConstants.USER);

        ParticipatingCompany company = new ParticipatingCompany();
        company.setId(1);
        company.setCompanyId(participatingCompanyId);
        company.setCompanyStatus(CompanyStatus.SUSPENDED);
        when(participatingCompanyRepository.findByCompanyId(participatingCompanyId))
                .thenReturn(Optional.of(company));

        MockHttpServletResponse response = executeProtectedRequest();

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"statusCode\":\"SUSPENDED\""));
        verifyNoInteractions(operatingUserRepository);
    }

    private void stubOperatorClaims(Integer userId, String companyId) {
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn(companyId);
        when(jwtUtil.extractRole(TOKEN)).thenReturn(RoleConstants.OPERATOR);
        when(jwtUtil.extractUserId(TOKEN)).thenReturn(userId);
        when(jwtUtil.extractUsername(TOKEN)).thenReturn(OPERATOR_USERNAME);
    }

    private OperatingUser buildOperator(
            Integer userId,
            boolean userActive,
            boolean companyActive,
            String companyId) {
        OperatorCompany company = OperatorCompany.builder()
                .companyId(companyId)
                .build();
        company.setId(1);
        company.setIsActive(companyActive);

        OperatingUser operator = OperatingUser.builder()
                .username(OPERATOR_USERNAME)
                .role(RoleConstants.OPERATOR)
                .company(company)
                .build();
        operator.setId(userId);
        operator.setIsActive(userActive);
        return operator;
    }

    private MockHttpServletRequest protectedRequest() {
        return new MockHttpServletRequest("GET", "/api/project");
    }

    private MockHttpServletResponse executeProtectedRequest() throws Exception {
        MockHttpServletRequest request = protectedRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        return response;
    }

    private void assertForbidden(MockHttpServletResponse response) throws Exception {
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"statusCode\":\"FORBIDDEN\""));
    }
}
