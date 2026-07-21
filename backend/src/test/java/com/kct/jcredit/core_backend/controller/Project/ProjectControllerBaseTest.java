package com.kct.jcredit.core_backend.controller.Project;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import com.kct.jcredit.core_backend.controller.ProjectController;
import com.kct.jcredit.core_backend.dto.request.CreateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.details.ProjectDtoReq;
import com.kct.jcredit.core_backend.dto.response.CreateProjectResponse;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.ParticipationProjectService;
import com.kct.jcredit.core_backend.service.ProjectMonitoringService;
import com.kct.jcredit.core_backend.service.ProjectUploadDocumentService;
import com.kct.jcredit.core_backend.util.JwtUtil;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.constant.ResponseConstants;

import java.util.Collections;

/**
 * - ProjectControllerCreateTest
 * - ProjectControllerSolarUploadTest
 * - ProjectControllerDocumentTest
 * - ProjectControllerMonitoringTest
 * - ProjectControllerSolarRegisterTest
 * - ProjectControllerUpdateTest
 */
@ExtendWith(MockitoExtension.class)
public abstract class ProjectControllerBaseTest {

    // ==================== Mock Dependencies ====================
    
    @Mock
    protected ProjectService projectService;
    
    @Mock
    protected ParticipationProjectService equipmentSolarService;
    
    @Mock
    protected ProjectUploadDocumentService projectUploadDocumentService;
    
    @Mock
    protected ProjectMonitoringService projectMonitoringService;
    
    @Mock
    protected JwtUtil jwtUtil;
    
    @Mock
    protected Authentication authentication;
    
    // ==================== Controller (Inject Mocks) ====================
    
    @InjectMocks
    protected ProjectController controller;
    
    // ==================== Test Data Constants ====================
    
    protected static final String VALID_TOKEN = "valid.jwt.token";
    protected static final String INVALID_TOKEN = "invalid.token";
    protected static final String OPERATOR_COMPANY_ID = "1";
    protected static final String OPERATOR_USERNAME = "operator01";
    
    // ==================== Setup ====================
    
    @BeforeEach
    void baseSetUp() {
        setupDefaultJwtBehavior();
    }
    
    protected void setupDefaultJwtBehavior() {
        lenient().when(authentication.getCredentials()).thenReturn(VALID_TOKEN);
        lenient().when(jwtUtil.extractCompanyId(VALID_TOKEN)).thenReturn(OPERATOR_COMPANY_ID);
        lenient().when(jwtUtil.extractUsername(VALID_TOKEN)).thenReturn(OPERATOR_USERNAME);
    }
    
    protected void setupJwtCompanyId(String companyId) {
        lenient().when(jwtUtil.extractCompanyId(anyString())).thenReturn(companyId);
    }
    
    protected void setupJwtUsername(String username) {
        lenient().when(jwtUtil.extractUsername(anyString())).thenReturn(username);
    }

    protected HttpStatus getHttpStatusFromCode(String statusCode) {
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        switch (statusCode) {
            case ResponseConstants.OK:
                return HttpStatus.OK;
            case "201":
                return HttpStatus.CREATED;
            case "400":
                return HttpStatus.BAD_REQUEST;
            case "401":
                return HttpStatus.UNAUTHORIZED;
            case "403":
                return HttpStatus.FORBIDDEN;
            case "404":
                return HttpStatus.NOT_FOUND;
            case "409":
                return HttpStatus.CONFLICT;
            case "500":
                return HttpStatus.INTERNAL_SERVER_ERROR;
            default:
                return HttpStatus.OK;
        }
    }
    
    // ==================== Test Data Builders ====================

    protected static class TestDataBuilder {
        public static CreateProjectRequest buildValidCreateProjectRequest() {
            CreateProjectRequest request = new CreateProjectRequest();
            
            ProjectDtoReq projectData = new ProjectDtoReq();
            projectData.setProjectName("家庭における太陽光発電設備の導入プロジェクト");
            projectData.setProjectNumber("P001");
            projectData.setProjectStatus("作成済み");
            projectData.setRepFirstName("太郎");
            projectData.setRepFirstNameKana("タロウ");
            projectData.setRepLastName("山田");
            projectData.setRepLastNameKana("ヤマダ");
            projectData.setRepEmail("taro.yamada@example.co.jp");
            projectData.setRepPhoneNumber("+819012345678");
            projectData.setRepPosition("代表取締役");
            projectData.setPicFirstName("花子");
            projectData.setPicFirstNameKana("ハナコ");
            projectData.setPicLastName("佐藤");
            projectData.setPicLastNameKana("サトウ");
            projectData.setPicEmail("hanako.sato@example.co.jp");
            projectData.setPicPhoneNumber("+819087654321");
            projectData.setPicPosition("プロジェクトマネージャー");
            projectData.setDataManagementPic("田中次郎");
            projectData.setRegionalCategory("関東");
            projectData.setMethodologyVersion("Ver.1.0");
            projectData.setMethodCategory("EN-S");
            projectData.setMethodNumber("001");
            projectData.setMethodName("太陽光発電設備の導入");
            projectData.setMethodMainEmission("CO2排出削減");
            projectData.setMethodCombiFeasibility(true);
            projectData.setMethodDetails("家庭用太陽光発電による排出削減");
            projectData.setBaselineEmissions(3200.0);
            projectData.setPostProjectEmission(1950.0);
            projectData.setNewMembersTotal(120);
            projectData.setWithdrawnMembers(5);
            projectData.setCumulativeMembers(115);
            
            request.setProject(Collections.singletonList(projectData));
            
            return request;
        }
        
        public static CreateProjectResponse buildSuccessCreateProjectResponse() {
            CreateProjectResponse response = new CreateProjectResponse();
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage("プロジェクトが正常に作成されました");
            
            ProjectDetailResponse projectDetail = new ProjectDetailResponse();
            projectDetail.setId("PRJ001");
            
            response.setProject(Collections.singletonList(projectDetail));
            
            return response;
        }

        public static ProjectResponse buildSuccessProjectResponse() {
            ProjectResponse response = new ProjectResponse();
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage("Success");
            
            ProjectDetailResponse projectData = new ProjectDetailResponse();
            projectData.setId("PRJ001");
            projectData.setProjectName("Test Solar Project 2025");
            projectData.setProjectNumber("P001");
            projectData.setProjectStatus("作成済み");
            
            response.setProject(projectData);
            
            return response;
        }
        
        public static CreateProjectResponse buildErrorResponse(String statusCode, String message) {
            CreateProjectResponse response = new CreateProjectResponse();
            response.setStatusCode(statusCode);
            response.setMessage(message);
            return response;
        }
    }
}