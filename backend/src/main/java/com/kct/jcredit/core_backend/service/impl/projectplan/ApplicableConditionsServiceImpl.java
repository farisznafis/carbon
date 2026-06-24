package com.kct.jcredit.core_backend.service.impl.projectplan;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.response.ApplicableConditionsResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.dynamodb.ApplicableConditionsRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.service.projectplan.ApplicableConditionsService;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicableConditionsServiceImpl implements ApplicableConditionsService {

    private static final String APPLICABLE_CONDITIONS_SK = "APPLICABLE_CONDITIONS";

    private final OperatingUserRepository operatorUserRepository;
    private final ProjectRepository projectRepository;
    private final ApplicableConditionsRepository applicableConditionsRepository;
    private final OperatorCompanyRepository operatorCompanyRepository;

    @Override
    @Transactional
    public ApplicableConditionsResponse submitApplicableConditions(
            Integer projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    @Override
    @Transactional
    public ApplicableConditionsResponse updateApplicableConditions(
            Integer projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username) {
        return validateAndSave(projectId, request, companyId, role, username);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicableConditionsResponse getApplicableConditions(
            Integer projectId,
            String companyId,
            String role,
            String username) {
        ApplicableConditionsResponse response = new ApplicableConditionsResponse();

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
        ApplicableConditionsRequest data = applicableConditionsRepository
                .findByPkAndSk(pk, APPLICABLE_CONDITIONS_SK)
                .orElseGet(ApplicableConditionsRequest::new);

        applySqlMasterFields(data, project);
        data.setPk(pk);
        data.setSk(APPLICABLE_CONDITIONS_SK);

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private ApplicableConditionsResponse validateAndSave(
            Integer projectId,
            ApplicableConditionsRequest request,
            String companyId,
            String role,
            String username) {
        ApplicableConditionsResponse response = new ApplicableConditionsResponse();

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

        return saveApplicableConditions(projectId, request);
    }

    private ApplicableConditionsResponse saveApplicableConditions(
            Integer projectId,
            ApplicableConditionsRequest request) {
        String pk = buildPk(projectId);

        request.setPk(pk);
        request.setSk(APPLICABLE_CONDITIONS_SK);

        applicableConditionsRepository.save(request);

        ApplicableConditionsResponse response = new ApplicableConditionsResponse();
        response.setData(request);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private void applySqlMasterFields(ApplicableConditionsRequest request, Project project) {
        request.setMethodologyCode(buildMethodologyCode(project));
        request.setMethodologyVersion(project.getMethodologyVersion());

        OperatorCompany company = project.getOperatorCompany();
        if (company != null) {
            request.setBusinessName(company.getNameJa());
            request.setRepresentativePosition(company.getRepPosition());
            request.setRepresentativeName(buildFullName(company.getRepLastName(), company.getRepFirstName()));
        }
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

    private String buildPk(Integer projectId) {
        return String.valueOf(projectId);
    }

    private String buildMethodologyCode(Project project) {
        String category = project.getMethodCategory();
        String number = project.getMethodNumber();
        if (category == null || category.isBlank()) {
            return number;
        }
        if (number == null || number.isBlank()) {
            return category;
        }
        return category + "-" + number;
    }

    private String buildFullName(String lastName, String firstName) {
        String safeLast = lastName == null ? "" : lastName.trim();
        String safeFirst = firstName == null ? "" : firstName.trim();
        return (safeLast + " " + safeFirst).trim();
    }
}
