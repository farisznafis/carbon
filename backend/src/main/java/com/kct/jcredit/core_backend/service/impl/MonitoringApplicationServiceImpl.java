package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.MonitoringReportRequest;
import com.kct.jcredit.core_backend.dto.response.CorporationMasterResponse;
import com.kct.jcredit.core_backend.dto.response.MonitoringReportResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectMasterResponse;
import com.kct.jcredit.core_backend.dto.response.details.CorporationMasterData;
import com.kct.jcredit.core_backend.dto.response.details.ProjectMasterData;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.repository.dynamodb.MonitoringReportRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.MonitoringApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringApplicationServiceImpl implements MonitoringApplicationService {

    private final ParticipatingCompanyRepository participatingCompanyRepository;
    private final ParticipatingUserRepository participatingUserRepository;
    private final ProjectRepository projectRepository;
    private final SiteRepository siteRepository;
    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final MonitoringReportRepository monitoringReportRepository;

    // ==================== Master Data Lookups (PostgreSQL) ====================

    @Override
    @Transactional(readOnly = true)
    public CorporationMasterResponse getCompanyMaster(String companyId, String requesterCompanyId, String role, String username) {
        CorporationMasterResponse response = new CorporationMasterResponse();

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        if (!requesterCompanyId.equals(company.getCompanyId())) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        if (!hasAccessToCompany(role, username, company)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        ParticipatingUser masterUser = participatingUserRepository
                .findByCompanyIdAndRole(company.getId(), RoleConstants.MASTER)
                .orElse(null);

        String picName = masterUser == null ? null : buildFullName(masterUser.getLastName(), masterUser.getFirstName());

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
                .picPosition(masterUser == null ? null : masterUser.getUserPosition())
                .picName(picName)
                .picEmail(masterUser == null ? null : masterUser.getEmail())
                .picPhoneNumber(masterUser == null ? null : masterUser.getPhoneNumber())
                .build();

        response.setData(data);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMasterResponse getProjectMaster(Integer projectId, String companyId, String role, String username) {
        ProjectMasterResponse response = new ProjectMasterResponse();

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        if (!hasAccessToProject(project, company, role, username)) {
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

    // ==================== DynamoDB Operations ====================

    @Override
    public MonitoringReportResponse submitMonitoringReport(
            Integer projectId,
            Integer siteId,
            MonitoringReportRequest request,
            String companyId,
            String role,
            String username) {
        MonitoringReportResponse response = new MonitoringReportResponse();

        // Validate site exists
        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        // Validate access
        if (!hasAccessToSite(site, companyId, role, username)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        // Build DynamoDB keys
        String pk = buildPk(siteId, projectId);
        String monitoringId = UUID.randomUUID().toString();
        String sk = buildSk(monitoringId);

        request.setPk(pk);
        request.setSk(sk);

        // Save to DynamoDB
        monitoringReportRepository.save(request);

        response.setData(request);
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("モニタリング報告書が正常に保存されました。");
        return response;
    }

    @Override
    public MonitoringReportResponse getMonitoringReport(
            Integer projectId,
            Integer siteId,
            String companyId,
            String role,
            String username) {
        MonitoringReportResponse response = new MonitoringReportResponse();

        // Validate site exists
        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        // Validate access
        if (!hasAccessToSite(site, companyId, role, username)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        // Fetch from DynamoDB (latest version)
        String pk = buildPk(siteId, projectId);
        MonitoringReportRequest saved = monitoringReportRepository.findLatestByPk(pk).orElse(null);
        if (saved == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage("モニタリング報告書が見つかりませんでした。");
            return response;
        }

        response.setData(saved);
        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    // ==================== Authorization Helpers ====================

    private boolean hasAccessToCompany(String role, String username, ParticipatingCompany company) {
        if (RoleConstants.MASTER.equals(role)) {
            return true;
        }
        if (RoleConstants.USER.equals(role)) {
            ParticipatingUser user = participatingUserRepository
                    .findByUsernameAndCompanyId(username, company.getId())
                    .orElse(null);
            return user != null && user.getSite() != null && user.getSite().getId() != null;
        }
        return false;
    }

    private boolean hasAccessToSite(Site site, String companyId, String role, String username) {
        if (site == null || site.getCompany() == null || site.getCompany().getCompanyId() == null) {
            return false;
        }
        if (!site.getCompany().getCompanyId().equals(companyId)) {
            return false;
        }
        if (RoleConstants.MASTER.equals(role)) {
            return true;
        }
        if (RoleConstants.USER.equals(role)) {
            ParticipatingUser user = participatingUserRepository
                    .findByUsernameAndCompanyId(username, site.getCompany().getId())
                    .orElse(null);
            if (user == null || user.getSite() == null || user.getSite().getId() == null) {
                return false;
            }
            return user.getSite().getId().equals(site.getId());
        }
        return false;
    }

    private boolean hasAccessToProject(Project project, ParticipatingCompany company, String role, String username) {
        if (project == null) {
            return false;
        }
        if (RoleConstants.MASTER.equals(role)) {
            List<SiteProjectRelation> relations = siteProjectRelationRepository.findByProject(project);
            return relations.stream()
                    .map(SiteProjectRelation::getSite)
                    .filter(Objects::nonNull)
                    .anyMatch(site -> site.getCompany() != null
                            && company.getId().equals(site.getCompany().getId()));
        }
        if (RoleConstants.USER.equals(role)) {
            ParticipatingUser user = participatingUserRepository
                    .findByUsernameAndCompanyId(username, company.getId())
                    .orElse(null);
            if (user == null || user.getSite() == null) {
                return false;
            }
            return siteProjectRelationRepository.existsByProjectAndSite(project, user.getSite());
        }
        return false;
    }

    // ==================== Key Builders ====================

    private String buildPk(Integer siteId, Integer projectId) {
        return String.format("SITE#%d#PROJ#%d", siteId, projectId);
    }

    private String buildSk(String monitoringId) {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        return String.format("MONITORING#%s#VERSION#%s", monitoringId, timestamp);
    }

    private String buildFullName(String lastName, String firstName) {
        String safeLast = lastName == null ? "" : lastName.trim();
        String safeFirst = firstName == null ? "" : firstName.trim();
        return (safeLast + " " + safeFirst).trim();
    }
}
