package com.kct.jcredit.core_backend.service.impl.projectplan;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectApplicationResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;
import com.kct.jcredit.core_backend.dto.response.details.CorporationMasterData;
import com.kct.jcredit.core_backend.dto.response.details.ProjectMasterData;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.dynamodb.ProjectApplicationRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.projectplan.ProjectApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectApplicationServiceImpl implements ProjectApplicationService {

    private static final String PROJECT_APPLICATION_SK = "PROJECT_APPLICATION";

    private final OperatorCompanyRepository operatorCompanyRepository;
    private final OperatingUserRepository operatorUserRepository;
    private final ProjectRepository projectRepository;
    private final ProjectApplicationRepository projectApplicationRepository;

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

        if (!hasAccessToCompany(role, username, requesterCompanyId)) {
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

    @Override
    @Transactional(readOnly = true)
    public ProjectMasterResponse getProjectMaster(Integer projectId, String companyId, String role, String username) {
        ProjectMasterResponse response = new ProjectMasterResponse();

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        if (!hasAccessToProject(project, companyId, role, username)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

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

    @Override
    @Transactional
    public ProjectApplicationResponse submitProjectApplication(
            Integer projectId,
            ProjectApplicationRequest request,
            String companyId,
            String role,
            String username) {
        ProjectApplicationResponse response = new ProjectApplicationResponse();

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        if (!hasAccessToProject(project, companyId, role, username)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        return saveProjectApplication(projectId, request);
    }

    @Override
    @Transactional
    public ProjectApplicationResponse updateProjectApplication(
            Integer projectId,
            ProjectApplicationRequest request,
            String companyId,
            String role,
            String username) {
        ProjectApplicationResponse response = new ProjectApplicationResponse();

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        if (!hasAccessToProject(project, companyId, role, username)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        return saveProjectApplication(projectId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectApplicationResponse getProjectApplication(
            Integer projectId,
            String companyId,
            String role,
            String username) {
        ProjectApplicationResponse response = new ProjectApplicationResponse();

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        if (!hasAccessToProject(project, companyId, role, username)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        String pk = buildPk(projectId);
        ProjectApplicationRequest data = projectApplicationRepository
                .findByPkAndSk(pk, PROJECT_APPLICATION_SK)
                .orElseGet(ProjectApplicationRequest::new);

        applySqlMasterFields(data, project, companyId);
        data.setPk(pk);
        data.setSk(PROJECT_APPLICATION_SK);

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private boolean hasAccessToCompany(String role, String username, String companyId) {
        OperatorCompany company = operatorCompanyRepository.findByCompanyId(companyId).orElse(null);
        
        if (!RoleConstants.OPERATOR.equals(role)) {
            return false;
        }
        OperatingUser user = operatorUserRepository
                .findByUsernameAndCompany_Id(username, company.getId())
                .orElse(null);
        return user != null;
    }

    private boolean hasAccessToProject(Project project, String companyId, String role, String username) {
        if (project == null) {
            return false;
        }
        OperatorCompany company = project.getOperatorCompany();
        if (company == null) {
            company = operatorCompanyRepository.findByCompanyId(companyId).orElse(null);
        }
        if (!RoleConstants.OPERATOR.equals(role)) {
            return false;
        }
        OperatingUser user = operatorUserRepository
                .findByUsernameAndCompany_Id(username, company.getId())
                .orElse(null);
        if (user == null) {
            return false;
        }
        OperatorCompany operatorCompany = project.getOperatorCompany();
        return operatorCompany != null && companyId.equals(operatorCompany.getCompanyId());
    }

    private ProjectApplicationResponse saveProjectApplication(Integer projectId, ProjectApplicationRequest request) {
        String pk = buildPk(projectId);

        request.setPk(pk);
        request.setSk(PROJECT_APPLICATION_SK);

        projectApplicationRepository.save(request);

        ProjectApplicationResponse response = new ProjectApplicationResponse();
        response.setData(request);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private void applySqlMasterFields(ProjectApplicationRequest request, Project project, String companyId) {
        OperatorCompany company = project.getOperatorCompany();
        if (company == null) {
            company = operatorCompanyRepository.findByCompanyId(companyId).orElse(null);
        }

        OperatingUser masterUser = operatorUserRepository
                .findByCompany_IdAndRole(company.getId(), RoleConstants.MASTER)
                .orElse(null);

        if (company != null) {
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
        }

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

    private String buildPk(Integer projectId) {
        return String.valueOf(projectId);
    }

    private String buildFullName(String lastName, String firstName) {
        String safeLast = lastName == null ? "" : lastName.trim();
        String safeFirst = firstName == null ? "" : firstName.trim();
        return (safeLast + " " + safeFirst).trim();
    }
}
