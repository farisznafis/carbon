package com.kct.jcredit.core_backend.service.impl.projectplan;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectApplicationResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.CorporationMasterData;
import com.kct.jcredit.core_backend.dto.response.details.ProjectMasterData;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectApplicationRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.service.projectplan.ProjectApplicationService;
import com.kct.jcredit.core_backend.service.impl.projectplan.ProjectPlanContextService.ProjectContext;
import com.kct.jcredit.core_backend.service.impl.projectplan.ProjectPlanContextService.ProjectContextStatus;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the main project-application form and its autofill data.
 *
 * Operator-company and project master data are read from PostgreSQL. The
 * editable application form is stored in the shared project-plan DynamoDB table
 * under a form-specific static SK.
 */
@Service
@RequiredArgsConstructor
public class ProjectApplicationServiceImpl implements ProjectApplicationService {

    private static final String PROJECT_APPLICATION_SK = "PROJECT_APPLICATION";

    private final OperatorCompanyRepository operatorCompanyRepository;
    private final OperatingUserRepository operatorUserRepository;
    private final ProjectPlanContextService projectPlanContextService;
    private final ProjectApplicationRepository projectApplicationRepository;

    /**
     * Returns operator-company master data used to prefill a project application.
     */
    @Override
    @Transactional(readOnly = true)
    public CorporationMasterResponse getCorporationMaster(String requesterCompanyId, String role, String username) {
        CorporationMasterResponse response = new CorporationMasterResponse();

        OperatorCompany company = operatorCompanyRepository.findByCompanyId(requesterCompanyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        if (!hasAccessToCompany(role, username, company)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        OperatingUser operatorUser = operatorUserRepository
            .findByCompany_IdAndRole(company.getId(), RoleConstants.MASTER)
            .orElse(null);

        String picName = operatorUser == null ? null : buildFullName(operatorUser.getLastName(), operatorUser.getFirstName());

        CorporationMasterData data = CorporationMasterData.builder()
                .corpNumber(company.getCorpNumber())
                .corpName(company.getNameJa())
                .corpPosition(company.getRepPosition())
                .prefecture(company.getPrefecture())
                .municipality(company.getMunicipality())
                .blocknum(company.getBlocknum())
                .buildingName(company.getBuildingName())
                .representativePosition(company.getRepPosition())
                .representativeName(buildFullName(company.getRepLastName(), company.getRepFirstName()))
                .picPosition(company.getRepPosition())
                .picName(picName)
                .picEmail(operatorUser == null ? null : operatorUser.getEmail())
                .picPhoneNumber(operatorUser == null ? null : operatorUser.getPhoneNumber())
                .build();

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    /**
     * Returns project master data after checking operator ownership and user access.
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectMasterResponse getProjectMaster(String projectId, String companyId, String role, String username) {
        ProjectMasterResponse response = new ProjectMasterResponse();

        ProjectContext context = projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        Project project = context.project();
        ProjectMasterData data = ProjectMasterData.builder()
            .projectId(project.getId())
                .projectName(project.getProjectName())
                .methodologyCategory(project.getMethodCategory())
                .methodologyNumber(project.getMethodNumber())
                .methodology(project.getMethodName())
                .location(null)
                .regionalCategory(project.getRegionalCategory())
                .build();

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    /**
     * Creates or replaces the project-application form for a project.
     */
    @Override
    @Transactional
    public ProjectApplicationResponse submitProjectApplication(
            String projectId,
            ProjectApplicationRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    /**
     * Replaces the existing project-application form for a project.
     */
    @Override
    @Transactional
    public ProjectApplicationResponse updateProjectApplication(
            String projectId,
            ProjectApplicationRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    /**
     * Returns the stored application with current PostgreSQL master fields overlaid.
     */
    @Override
    @Transactional(readOnly = true)
    public ProjectApplicationResponse getProjectApplication(
            String projectId,
            String companyId,
            String role,
            String username) {
        ProjectApplicationResponse response = new ProjectApplicationResponse();

        ProjectContext context = projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }

        String pk = buildPk(projectId);
        ProjectApplicationRequest data = projectApplicationRepository
                .findByPkAndSk(pk, PROJECT_APPLICATION_SK)
                .orElseGet(ProjectApplicationRequest::new);

        // PostgreSQL remains authoritative for master fields shown in this form.
        applySqlMasterFields(data, context.project());
        data.setPk(pk);
        data.setSk(PROJECT_APPLICATION_SK);

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private boolean hasAccessToCompany(String role, String username, OperatorCompany company) {
        if (!RoleConstants.OPERATOR.equals(role)) {
            return false;
        }
        OperatingUser user = operatorUserRepository
                .findByUsernameAndCompany_Id(username, company.getId())
                .orElse(null);
        return user != null;
    }

    private ProjectApplicationResponse validateAndSave(
            String projectId,
            ProjectApplicationRequest request,
            String companyId,
            String role,
            String username) {
        ProjectApplicationResponse response = new ProjectApplicationResponse();
        ProjectContext context = projectPlanContextService.resolveProject(projectId, companyId, role, username);
        if (!context.isAuthorized()) {
            applyContextError(response, context);
            return response;
        }
        return saveProjectApplication(projectId, request);
    }

    private ProjectApplicationResponse saveProjectApplication(String projectId, ProjectApplicationRequest request) {
        String pk = buildPk(projectId);

        // DynamoDB keys are server-managed and cannot be supplied by clients.
        request.setPk(pk);
        request.setSk(PROJECT_APPLICATION_SK);

        projectApplicationRepository.save(request);

        ProjectApplicationResponse response = new ProjectApplicationResponse();
        response.setData(request);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private void applySqlMasterFields(ProjectApplicationRequest request, Project project) {
        OperatorCompany company = project.getOperatorCompany();

        OperatingUser masterUser = operatorUserRepository
                .findByCompany_IdAndRole(company.getId(), RoleConstants.MASTER)
                .orElse(null);

        request.setProjectOperatorKana(company.getNameFurigana());
        request.setProjectOperatorNameJa(company.getNameJa());
        request.setProjectOperatorNameEn(company.getNameEn());
        request.setCorpNumber(company.getCorpNumber());
        request.setCorpPosition(company.getRepPosition());
        request.setCorpName(company.getNameJa());
        request.setPrefecture(company.getPrefecture());
        request.setMunicipality(company.getMunicipality());
        request.setBlocknum(company.getBlocknum());
        request.setBuildingName(company.getBuildingName());
        request.setRepresentativePosition(company.getRepPosition());
        request.setRepresentativeName(buildFullName(company.getRepLastName(), company.getRepFirstName()));
        request.setPicPosition(company.getRepPosition());

        if (masterUser != null) {
            request.setPicName(buildFullName(masterUser.getLastName(), masterUser.getFirstName()));
            request.setPicEmail(masterUser.getEmail());
            request.setPicPhoneNumber(masterUser.getPhoneNumber());
        }

        request.setProjectNumber(project.getProjectNumber());
        request.setProjectName(project.getProjectName());
        request.setMethodologyCategory(project.getMethodCategory());
        request.setMethodologyNumber(project.getMethodNumber());
        request.setMethodology(project.getMethodName());
        request.setRegionalCategory(project.getRegionalCategory());
    }

    private void applyContextError(BaseResponse response, ProjectContext context) {
        if (context.status() == ProjectContextStatus.NOT_FOUND) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return;
        }
        response.setStatusCode(ResponseConstants.FORBIDDEN);
        response.setMessage(ResponseConstants.MSG_FORBIDDEN);
    }

    private String buildPk(String projectId) {
        return String.valueOf(projectId);
    }

    private String buildFullName(String lastName, String firstName) {
        String safeLast = lastName == null ? "" : lastName.trim();
        String safeFirst = firstName == null ? "" : firstName.trim();
        return (safeLast + " " + safeFirst).trim();
    }
}
