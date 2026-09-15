package com.kct.jcredit.core_backend.service.projectplan;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.CorporationMasterData;
import com.kct.jcredit.core_backend.dto.response.details.ProjectMasterData;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.util.AddressFormatter;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContext;
import com.kct.jcredit.core_backend.service.projectplan.ProjectPlanContextService.ProjectContextStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Master data used to prefill the plan, read from PostgreSQL.
 *
 * <p>Split out of the old per-form services so those could be deleted; the
 * two endpoints it backs are consumed by the frontend and are unchanged.
 */
@Service
@RequiredArgsConstructor
public class ProjectPlanAutofillService {

    private final OperatorCompanyRepository operatorCompanyRepository;
    private final OperatingUserRepository operatingUserRepository;
    private final ProjectPlanContextService projectPlanContextService;

    /** Operator-company master data used to prefill a project application. */
    @Transactional(readOnly = true)
    public CorporationMasterResponse getCorporationMaster(
            String requesterCompanyId, String role, String username) {

        CorporationMasterResponse response = new CorporationMasterResponse();

        OperatorCompany company =
                operatorCompanyRepository.findByCompanyId(requesterCompanyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        if (!projectPlanContextService.canAccessCompany(role, username, company)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        OperatingUser operatorUser = operatingUserRepository
                .findByCompany_IdAndRole(company.getId(), RoleConstants.MASTER)
                .orElse(null);

        CorporationMasterData data = CorporationMasterData.builder()
                .corpNumber(company.getCorpNumber())
                // 法人代表者の氏名 — the representative, matching the plan page.
                .corpName(buildFullName(company.getRepLastName(), company.getRepFirstName()))
                .corpPosition(company.getRepPosition())
                .prefecture(company.getPrefecture())
                .city(company.getMunicipality())
                .address(AddressFormatter.joinAddressAfterCity(
                        company.getBlocknum(), company.getBuildingName()))
                .representativePosition(company.getRepPosition())
                .representativeName(
                        buildFullName(company.getRepLastName(), company.getRepFirstName()))
                .picPosition(company.getRepPosition())
                .picName(operatorUser == null
                        ? null
                        : buildFullName(operatorUser.getLastName(), operatorUser.getFirstName()))
                .picEmail(operatorUser == null ? null : operatorUser.getEmail())
                .picPhoneNumber(operatorUser == null
                        ? null
                        : ProjectPlanSqlContext.normalizePhoneNumber(operatorUser.getPhoneNumber()))
                .build();

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    /** Project master data, after checking operator ownership and user access. */
    @Transactional(readOnly = true)
    public ProjectMasterResponse getProjectMaster(
            String projectId, String companyId, String role, String username) {

        ProjectMasterResponse response = new ProjectMasterResponse();

        ProjectContext context =
                projectPlanContextService.resolveProject(projectId, companyId, role, username);
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
                // プロジェクト実施場所の分類 is operator-entered from a fixed list; there
                // is no column to autofill it from.
                .location(null)
                .regionalCategory(project.getRegionalCategory())
                .build();

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    private static void applyContextError(BaseResponse response, ProjectContext context) {
        if (context.status() == ProjectContextStatus.NOT_FOUND) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return;
        }
        response.setStatusCode(ResponseConstants.FORBIDDEN);
        response.setMessage(ResponseConstants.MSG_FORBIDDEN);
    }

    private static String buildFullName(String lastName, String firstName) {
        String safeLast = lastName == null ? "" : lastName.trim();
        String safeFirst = firstName == null ? "" : firstName.trim();
        return (safeLast + " " + safeFirst).trim();
    }
}
