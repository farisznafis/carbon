package com.kct.jcredit.core_backend.service.project;


import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.UpdateProjectRequest;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.impl.ProjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProjectService - Update (PATCH) Tests")
class ProjectServiceUpdateTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project dummyProject;

    @BeforeEach
    void setUp() {
 
        dummyProject = Project.builder()
                .projectName("古いプロジェクト名")
                .projectNumber("P421")
                .baselineEmission(3200.0)
                .postProjectEmission(1950.0)
                .emissionReduction(1250.0)
                .build();
        dummyProject.setId(12345);
    }

    @Test
    @DisplayName("TC-UP-01: Update project success (Partial update / PATCH)")
    void updateProject_success() {
        String targetProjectId = "PROJ12345";
        
        UpdateProjectRequest updateReq = new UpdateProjectRequest();
        updateReq.setId(targetProjectId);
        updateReq.setProjectName("更新されたプロジェクト名"); 
        updateReq.setProcessedBy("operator_update");

        when(projectRepository.findByPublicId(targetProjectId)).thenReturn(Optional.of(dummyProject));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = projectService.updateProject(updateReq);

        assertNotNull(response);
        assertEquals(ResponseConstants.OK, response.getStatusCode());
        assertNotNull(response.getProject());

        assertEquals("更新されたプロジェクト名", response.getProject().getProjectName());

        assertEquals("P421", response.getProject().getProjectNumber());
        assertEquals("1250.0", response.getProject().getEmissionReduction());

        verify(projectRepository, times(1)).findByPublicId(targetProjectId);
        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    @DisplayName("TC-UP-02: Update project returns 404 when project not found")
    void updateProject_notFound() {
        // Given
        UpdateProjectRequest updateReq = new UpdateProjectRequest();
        updateReq.setId("PROJ0999");
        
        when(projectRepository.findByPublicId("PROJ0999")).thenReturn(Optional.empty());

        // When
        ProjectResponse response = projectService.updateProject(updateReq);

        // Then
        assertNotNull(response);
        assertEquals(ResponseConstants.NOT_FOUND, response.getStatusCode());
        assertEquals(ResponseConstants.MSG_PROJECT_NOT_FOUND, response.getMessage());

        verify(projectRepository, never()).save(any(Project.class));
    }
}
