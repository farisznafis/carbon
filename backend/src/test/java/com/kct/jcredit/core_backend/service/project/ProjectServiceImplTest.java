package com.kct.jcredit.core_backend.service.project;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CreateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.details.ProjectDtoReq;
import com.kct.jcredit.core_backend.dto.response.CreateProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.service.impl.ProjectServiceImpl;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectServiceImpl - Unit Tests")
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private OperatorCompanyRepository operatorCompanyRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    // ==================== Variabel Dummy ====================
    private OperatorCompany dummyCompany;
    private Project dummyProject;
    private CreateProjectRequest createRequest;
    private ProjectDtoReq projectDtoReq;

    @BeforeEach
    void setUp() {
        // 1. Setup Dummy OperatorCompany
        dummyCompany = new OperatorCompany();
        dummyCompany.setId(42);
        dummyCompany.setNameJa("株式会社グリーンエナジー"); 

        // 2. Setup Dummy Project 
        dummyProject = Project.builder()
                .projectName("太陽光発電導入プロジェクト")
                .projectNumber("P421")
                .baselineEmission(3200.0)
                .postProjectEmission(1950.0)
                .emissionReduction(1250.0)
                .operatorCompany(dummyCompany)
                .build();
        dummyProject.setId(12345);
        dummyProject.setCreatedBy("operator_test");

        // 3. Setup Dummy Request createProject
        projectDtoReq = new ProjectDtoReq();
        projectDtoReq.setOperatorCompanyId("42");
        projectDtoReq.setProjectName("太陽光発電導入プロジェクト");
        projectDtoReq.setProjectNumber("P421");
        projectDtoReq.setBaselineEmissions(3200.0);
        projectDtoReq.setPostProjectEmission(1950.0);

        createRequest = new CreateProjectRequest();
        createRequest.setCreatedBy("operator_test");
        createRequest.setProject(List.of(projectDtoReq));
    }

    // ==========================================================
    // Skenario Test: CREATE PROJECT
    // ==========================================================

    @Test
    @DisplayName("TC-CP-01: Create project success with emission calculation")
    void createProject_success_withEmissionCalculation() {
        // Given
        when(operatorCompanyRepository.findById(42)).thenReturn(Optional.of(dummyCompany));
        when(projectRepository.saveAll(any())).thenReturn(List.of(dummyProject));

        CreateProjectResponse response = projectService.createProject(createRequest);

        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getProject());
        assertEquals(1, response.getProject().size());
        assertEquals("1250.0", response.getProject().get(0).getEmissionReduction());

        verify(operatorCompanyRepository, times(1)).findById(42);
        verify(projectRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-CP-02: Create project success without emission calculation (null value)")
    void createProject_success_withoutEmissionCalculation() {
        projectDtoReq.setPostProjectEmission(null);
        Project dummyNullEmission = Project.builder()
                .baselineEmission(3200.0)
                .postProjectEmission(null)
                .emissionReduction(null)
                .operatorCompany(dummyCompany)
                .build();
        dummyNullEmission.setId(12345);

        when(operatorCompanyRepository.findById(42)).thenReturn(Optional.of(dummyCompany));
        when(projectRepository.saveAll(any())).thenReturn(List.of(dummyNullEmission));

        CreateProjectResponse response = projectService.createProject(createRequest);

        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNull(response.getProject().get(0).getEmissionReduction(), "Emission reduction should be null because data is incomplete");
        
        verify(projectRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("TC-CP-03: Create project throws exception when operator company not found")
    void createProject_operatorCompanyNotFound_throwsException() {
        projectDtoReq.setOperatorCompanyId("999");
        when(operatorCompanyRepository.findById(999)).thenReturn(Optional.empty());

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
            projectService.createProject(createRequest);
        });

        assertEquals(ResponseConstants.MSG_UNEXPEXTED_ERROR, exception.getMessage());

        verify(projectRepository, never()).saveAll(any());
    }

    // ==========================================================
    // Skenario Test: GET PROJECT
    // ==========================================================

    @Test
    @DisplayName("TC-GP-01: Get project details successfully")
    void getProject_success() {
        // Given
        Integer targetProjectId = 12345;
        String dummyUserCompanyId = "COMP-001";
        
        when(projectRepository.findById(targetProjectId)).thenReturn(Optional.of(dummyProject));

        ProjectResponse response = projectService.getProject(targetProjectId, dummyUserCompanyId);

        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getProject());
        
        assertEquals("12345", response.getProject().getId());
        assertEquals("太陽光発電導入プロジェクト", response.getProject().getProjectName());
        assertEquals("株式会社グリーンエナジー", response.getProject().getCompanyName()); 

        verify(projectRepository, times(1)).findById(targetProjectId);
    }

    @Test
    @DisplayName("TC-GP-02: Get project returns 404 when project not found")
    void getProject_notFound() {
        Integer targetProjectId = 999;
        String dummyUserCompanyId = "COMP-001";
        
        when(projectRepository.findById(targetProjectId)).thenReturn(Optional.empty());

        ProjectResponse response = projectService.getProject(targetProjectId, dummyUserCompanyId);

        assertNotNull(response);
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_PROJECT_NOT_FOUND, response.getMessage());
        assertNull(response.getProject(), "Project details should be null when project is not found");

        verify(projectRepository, times(1)).findById(targetProjectId);
    }

    
}
