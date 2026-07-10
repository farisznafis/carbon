package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CreateDeletionRequest;
import com.kct.jcredit.core_backend.dto.request.PartiCompanyRegisterRequest;
import com.kct.jcredit.core_backend.dto.request.UpdatePartiCompanyRequest;
import com.kct.jcredit.core_backend.dto.response.DeletionRequestResponse;
import com.kct.jcredit.core_backend.dto.response.UpdatePartiCompanyResponse;
import com.kct.jcredit.core_backend.dto.response.details.PartiCompanyResponseDetails;
import com.kct.jcredit.core_backend.service.CompanyDeletionService;
import com.kct.jcredit.core_backend.service.PartiCompanyRegisterService;
import com.kct.jcredit.core_backend.service.PartiCompanyService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartiCompanyControllerTest {

    @Mock
    private PartiCompanyService companyService;

    @Mock
    private PartiCompanyRegisterService registerService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CompanyDeletionService companyDeletionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PartiCompanyController controller;

    @Test
    void updateCompanyReturnsServiceResponse() {
        UpdatePartiCompanyRequest request = new UpdatePartiCompanyRequest();
        request.setCompanyId("COMP00000001");
        UpdatePartiCompanyResponse serviceResponse = new UpdatePartiCompanyResponse();
        serviceResponse.setStatusCode(ResponseConstants.OK);
        when(companyService.updateCompany(request.getCompanyId(), request)).thenReturn(serviceResponse);

        ResponseEntity<UpdatePartiCompanyResponse> response = controller.updateCompany(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(serviceResponse);
    }

    @Test
    void signupReturnsCreatedResponse() {
        PartiCompanyRegisterRequest request = new PartiCompanyRegisterRequest();
        PartiCompanyResponseDetails serviceResponse = new PartiCompanyResponseDetails(
                ResponseConstants.OK,
                "登録申請を受け付けました。",
                "COMP00000001",
                "PENDING"
        );
        when(registerService.register(request)).thenReturn(serviceResponse);

        ResponseEntity<PartiCompanyResponseDetails> response = controller.signup(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(serviceResponse);
    }

    @Test
    void createDeletionRequestUsesActorFromJwt() {
        CreateDeletionRequest request = new CreateDeletionRequest();
        request.setReason("No longer needed");
        DeletionRequestResponse serviceResponse = new DeletionRequestResponse();
        serviceResponse.setStatusCode(ResponseConstants.OK);
        when(authentication.getCredentials()).thenReturn("token");
        when(jwtUtil.extractUsername("token")).thenReturn("master@example.com");
        when(companyDeletionService.createDeletionRequest(
                "COMP00000001",
                "master@example.com",
                request.getReason()
        )).thenReturn(serviceResponse);

        ResponseEntity<DeletionRequestResponse> response = controller.createDeletionRequest(
                authentication,
                "COMP00000001",
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(companyDeletionService).createDeletionRequest(
                "COMP00000001",
                "master@example.com",
                request.getReason()
        );
    }
}
