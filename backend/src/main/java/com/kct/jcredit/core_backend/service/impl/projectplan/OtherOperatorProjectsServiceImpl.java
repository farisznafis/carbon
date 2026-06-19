package com.kct.jcredit.core_backend.service.impl.projectplan;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.response.OtherOperatorProjectsResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.dynamodb.OtherOperatorProjectsRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.projectplan.OtherOperatorProjectsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtherOperatorProjectsServiceImpl implements OtherOperatorProjectsService {

    private static final String OTHER_OPERATOR_PROJECTS_SK = "OTHER_OPERATOR_PROJECTS";

    private final OperatingUserRepository operatorUserRepository;
    private final ProjectRepository projectRepository;
    private final OtherOperatorProjectsRepository otherOperatorProjectsRepository;

    @Override
    @Transactional
    public OtherOperatorProjectsResponse submitOtherOperatorProjects(
            Integer projectId,
            OtherOperatorProjectsRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    @Override
    @Transactional
    public OtherOperatorProjectsResponse updateOtherOperatorProjects(
            Integer projectId,
            OtherOperatorProjectsRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    @Override
    @Transactional(readOnly = true)
    public OtherOperatorProjectsResponse getOtherOperatorProjects(
            Integer projectId,
            String companyId,
            String role,
            String username) {
        OtherOperatorProjectsResponse response = new OtherOperatorProjectsResponse();

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
        OtherOperatorProjectsRequest data = otherOperatorProjectsRepository
                .findByPkAndSk(pk, OTHER_OPERATOR_PROJECTS_SK)
                .orElseGet(OtherOperatorProjectsRequest::new);

        data.setPk(pk);
        data.setSk(OTHER_OPERATOR_PROJECTS_SK);

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private OtherOperatorProjectsResponse validateAndSave(
            Integer projectId,
            OtherOperatorProjectsRequest request,
            String companyId,
            String role,
            String username) {
        OtherOperatorProjectsResponse response = new OtherOperatorProjectsResponse();

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

        return saveOtherOperatorProjects(projectId, request);
    }

    private OtherOperatorProjectsResponse saveOtherOperatorProjects(
            Integer projectId,
            OtherOperatorProjectsRequest request) {
        String pk = buildPk(projectId);

        request.setPk(pk);
        request.setSk(OTHER_OPERATOR_PROJECTS_SK);

        otherOperatorProjectsRepository.save(request);

        OtherOperatorProjectsResponse response = new OtherOperatorProjectsResponse();
        response.setData(request);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private boolean hasAccessToProject(Project project, String companyId, String role, String username) {
        if (project == null) {
            return false;
        }
        if (!RoleConstants.OPERATOR.equals(role)) {
            return false;
        }
        OperatingUser user = operatorUserRepository
                .findByUsernameAndCompanyId(username, companyId)
                .orElse(null);
        if (user == null) {
            return false;
        }
        OperatorCompany operatorCompany = project.getOperatorCompany();
        return operatorCompany != null && companyId.equals(operatorCompany.getCompanyId());
    }

    private String buildPk(Integer projectId) {
        return String.valueOf(projectId);
    }
}
