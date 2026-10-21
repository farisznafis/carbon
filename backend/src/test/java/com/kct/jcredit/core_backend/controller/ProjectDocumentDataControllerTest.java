package com.kct.jcredit.core_backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.service.documentdata.ProjectDocumentDataService;
import com.kct.jcredit.core_backend.service.documentdata.ProjectDocumentDataService.Caller;
import com.kct.jcredit.core_backend.util.JwtUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;

/** The 書類データ取得 endpoint: who may call it and what it hands to the service. */
@ExtendWith(MockitoExtension.class)
class ProjectDocumentDataControllerTest {

    private static final String TOKEN = "document-data-test-token";

    @Mock private ProjectDocumentDataService service;
    @Mock private JwtUtil jwtUtil;
    @Mock private Authentication authentication;

    @InjectMocks
    private ProjectDocumentDataController controller;

    private void prepareAuthentication() {
        when(authentication.getCredentials()).thenReturn(TOKEN);
        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_operator"),
                new SimpleGrantedAuthority("PROJECT_READ"),
                new SimpleGrantedAuthority("MONITORING_READ")))
                .when(authentication).getAuthorities();
        when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP-001");
        when(jwtUtil.extractRole(TOKEN)).thenReturn("operator");
        when(jwtUtil.extractUsername(TOKEN)).thenReturn("operator01");
    }

    @Test
    void handsTheServiceTheCallerAsTheTokenStatesIt() {
        prepareAuthentication();
        ProjectDocumentDataResponse expected = response(ResponseConstants.OK);
        when(service.getData(any(), any(), any())).thenReturn(expected);

        var response = controller.getDocumentData(authentication, "PROJ1", ProjectFileType.MONITORING_REPORT);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(service).getData(
                "PROJ1",
                ProjectFileType.MONITORING_REPORT,
                new Caller("COMP-001", "operator", "operator01",
                        Set.of("ROLE_operator", "PROJECT_READ", "MONITORING_READ")));
    }

    @Test
    void answersWithTheHttpStatusOfTheServiceResult() {
        prepareAuthentication();
        when(service.getData(any(), any(), any())).thenReturn(response(ResponseConstants.FORBIDDEN));
        assertThat(controller.getDocumentData(authentication, "PROJ1", ProjectFileType.PROJECT_PLAN)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        when(service.getData(any(), any(), any())).thenReturn(response(ResponseConstants.NOT_FOUND));
        assertThat(controller.getDocumentData(authentication, "PROJ1", ProjectFileType.PROJECT_PLAN)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void isOpenOnlyToAdminsAndToOperatorsHoldingProjectRead() throws Exception {
        var method = ProjectDocumentDataController.class.getDeclaredMethod(
                "getDocumentData", Authentication.class, String.class, ProjectFileType.class);

        assertThat(method.getAnnotation(PreAuthorize.class).value()).isEqualTo(
                "hasRole('admin') or (hasRole('operator') and hasAuthority("
                        + "T(com.kct.jcredit.core_backend.constant.PermissionCode).PROJECT_READ.name()))");
        assertThat(method.getAnnotation(GetMapping.class).value())
                .containsExactly("/{projectId}/document-data/{fileType}");
    }

    private static ProjectDocumentDataResponse response(String statusCode) {
        ProjectDocumentDataResponse response = new ProjectDocumentDataResponse();
        response.setStatusCode(statusCode);
        return response;
    }
}
