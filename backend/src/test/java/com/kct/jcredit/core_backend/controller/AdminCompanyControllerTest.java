package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CompanyListQuery;
import com.kct.jcredit.core_backend.dto.request.CreateCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.request.ResolveCompanySuspensionRequest;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyDetailResponse;
import com.kct.jcredit.core_backend.dto.response.AdminCompanyListResponse;
import com.kct.jcredit.core_backend.dto.response.CompanySuspensionResponse;
import com.kct.jcredit.core_backend.enums.SuspensionReasonCode;
import com.kct.jcredit.core_backend.service.AdminCompanyQueryService;
import com.kct.jcredit.core_backend.service.CompanyApprovalService;
import com.kct.jcredit.core_backend.service.CompanyLifecycleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCompanyControllerTest {

    @Mock
    private CompanyApprovalService companyApprovalService;

    @Mock
    private AdminCompanyQueryService adminCompanyQueryService;

    @Mock
    private CompanyLifecycleService companyLifecycleService;

    @InjectMocks
    private AdminCompanyController controller;

    @Test
    void getCompaniesReturnsUnifiedCompanyPage() {
        CompanyListQuery query = new CompanyListQuery();
        AdminCompanyListResponse serviceResponse = new AdminCompanyListResponse();
        serviceResponse.setStatusCode(ResponseConstants.OK);
        when(adminCompanyQueryService.findCompanies(query)).thenReturn(serviceResponse);

        ResponseEntity<AdminCompanyListResponse> response = controller.getCompanies(query);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(serviceResponse);
        verify(adminCompanyQueryService).findCompanies(query);
    }

    @Test
    void getCompanyDetailDelegatesToQueryService() throws NoSuchMethodException {
        AdminCompanyDetailResponse serviceResponse = new AdminCompanyDetailResponse();
        serviceResponse.setStatusCode(ResponseConstants.OK);
        when(adminCompanyQueryService.findCompanyDetail("COMP00000001")).thenReturn(serviceResponse);

        ResponseEntity<AdminCompanyDetailResponse> response = controller.getCompanyDetail("COMP00000001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(serviceResponse);
        verify(adminCompanyQueryService).findCompanyDetail("COMP00000001");

        Method endpoint = AdminCompanyController.class.getMethod("getCompanyDetail", String.class);
        assertThat(endpoint.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasAnyRole('operator')");
    }

    @Test
    void createSuspensionDelegatesToLifecycleService() {
        CreateCompanySuspensionRequest request = new CreateCompanySuspensionRequest();
        request.setReasonCode(SuspensionReasonCode.POLICY_VIOLATION);
        request.setReasonDetail("Policy violation");
        CompanySuspensionResponse serviceResponse = new CompanySuspensionResponse();
        serviceResponse.setStatusCode(ResponseConstants.OK);
        when(companyLifecycleService.createSuspension("COMP00000001", request))
                .thenReturn(serviceResponse);

        ResponseEntity<CompanySuspensionResponse> response = controller.createSuspension(
                "COMP00000001",
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(serviceResponse);
    }

    @Test
    void resolveSuspensionDelegatesToLifecycleService() {
        ResolveCompanySuspensionRequest request = new ResolveCompanySuspensionRequest();
        request.setResolutionNote("Resolved");
        CompanySuspensionResponse serviceResponse = new CompanySuspensionResponse();
        serviceResponse.setStatusCode(ResponseConstants.OK);
        when(companyLifecycleService.resolveSuspension("COMP00000001", 7, request))
                .thenReturn(serviceResponse);

        ResponseEntity<CompanySuspensionResponse> response = controller.resolveSuspension(
                "COMP00000001",
                7,
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(serviceResponse);
    }
}
