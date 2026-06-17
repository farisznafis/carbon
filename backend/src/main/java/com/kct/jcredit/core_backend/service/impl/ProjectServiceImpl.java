package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.request.CreateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateProjectRequest;
import com.kct.jcredit.core_backend.dto.request.details.ProjectDtoReq;
import com.kct.jcredit.core_backend.dto.response.CreateProjectResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectResponse;
import com.kct.jcredit.core_backend.dto.response.details.ProjectDetailResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateProjectStatusResponse;
import com.kct.jcredit.core_backend.dto.request.UpdateProjectStatusRequest;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.base.PaginationDetailResponse;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.util.ProjectSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final SiteRepository siteRepository;
    private final ProjectRepository projectRepository;
    private final OperatorCompanyRepository operatorCompanyRepository;
    
    

    ZoneId USER_ZONE = ZoneId.of("Asia/Tokyo");
    ZoneId DB_ZONE   = ZoneId.of("Asia/Tokyo");

    @Override
    public ProjectListInfoResponse getProjectsBySiteIdAndCompanyId(Integer siteId, String companyId) {
        ProjectListInfoResponse response = new ProjectListInfoResponse();

        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        if (!site.getCompany().getCompanyId().equals(companyId)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_UNAUTHORIZED);
            return response;
        }

        response.setProjects(new ArrayList<>());
        Map<Integer, String> joinedProjectIdStatusMap = site.getSiteProjectRelations().stream()
            .collect(Collectors.toMap(
                rel -> rel.getProject().getId(), SiteProjectRelation::getStatus
            ));

        List<Project> projects = projectRepository.findAll();

        for (Project project : projects) {
            ProjectDetailResponse resp = ProjectDetailResponse.builder()
                    .id(String.valueOf(project.getId()))
                    .projectName(project.getProjectName())
                    .emissionReduction(project.getEmissionReduction() != null ?
                            String.valueOf(project.getEmissionReduction()) : "0")
                    .baselineEmission(project.getBaselineEmission() != null ?
                            String.valueOf(project.getBaselineEmission()) : "0")
                    .postProjectEmission(project.getPostProjectEmission() != null ?
                            String.valueOf(project.getPostProjectEmission()) : "0")
                    .regionalCategory(project.getRegionalCategory())
                    .methodologyVersion(project.getMethodologyVersion())
                    .methodologyNumber(project.getMethodNumber())
                    .methodologyName(project.getMethodName())
                    .methodologyMainEmission(project.getMethodMainEmission())
                    .isJoined(joinedProjectIdStatusMap.containsKey(project.getId()))
                    .projectStatus(joinedProjectIdStatusMap.get(project.getId()))
                    .build();

            response.getProjects().add(resp);
        }
        response.setStatusCode(ResponseConstants.OK);

        return response;
    }

    @Override
    @Transactional
    public CreateProjectResponse createProject(CreateProjectRequest request) {
        Map<String, String> duplicateErrors = validateDuplicateProjectData(request.getProject());
        if (!duplicateErrors.isEmpty()) {
            CreateProjectResponse response = new CreateProjectResponse();
            response.setStatusCode(ResponseConstants.CONFLICT);
            response.setMessage(ResponseConstants.MSG_DUPLICATE_INPUT_DATA);
            response.setErrors(duplicateErrors);
            return response;
        }

        List<Project> projectList = request.getProject().stream()
            .map(projectDto -> convertProjectDtoToEntity(projectDto, request.getCreatedBy(), request.getOperatorCompanyId()))
            .collect(Collectors.toList());

        CreateProjectResponse response = generateProjectListResponse(projectRepository.saveAll(projectList));

        response.setStatusCode(ResponseConstants.OK);

        return response;
    }

    private Map<String, String> validateDuplicateProjectData(List<ProjectDtoReq> projectDtos) {
        Map<String, String> errors = new LinkedHashMap<>();
        Set<String> seenProjectNumbers = new HashSet<>();
        Set<String> duplicatedProjectNumbersInRequest = new HashSet<>();
        Set<String> seenMethodNumbers = new HashSet<>();
        Set<String> duplicatedMethodNumbersInRequest = new HashSet<>();

        for (int i = 0; i < projectDtos.size(); i++) {
            ProjectDtoReq projectDto = projectDtos.get(i);
            String projectNumber = projectDto.getProjectNumber();
            if (projectNumber == null || projectNumber.isBlank()) {
                // Bean validation reports blank values; duplicate validation only checks provided values.
            } else if (!seenProjectNumbers.add(projectNumber)) {
                duplicatedProjectNumbersInRequest.add(projectNumber);
                errors.put("project[" + i + "].projectNumber", ResponseConstants.MSG_PROJECT_NUMBER_DUPLICATED_IN_REQUEST);
            } else if (projectRepository.existsByProjectNumber(projectNumber)) {
                errors.put("project[" + i + "].projectNumber", ResponseConstants.MSG_PROJECT_NUMBER_ALREADY_USED);
            }

            String methodNumber = projectDto.getMethodNumber();
            if (methodNumber == null || methodNumber.isBlank()) {
                // Bean validation reports blank values; duplicate validation only checks provided values.
            } else if (!seenMethodNumbers.add(methodNumber)) {
                duplicatedMethodNumbersInRequest.add(methodNumber);
                errors.put("project[" + i + "].methodNumber", ResponseConstants.MSG_METHOD_NUMBER_DUPLICATED_IN_REQUEST);
            } else if (projectRepository.existsByMethodNumber(methodNumber)) {
                errors.put("project[" + i + "].methodNumber", ResponseConstants.MSG_METHOD_NUMBER_ALREADY_USED);
            }
        }

        for (int i = 0; i < projectDtos.size(); i++) {
            ProjectDtoReq projectDto = projectDtos.get(i);
            if (duplicatedProjectNumbersInRequest.contains(projectDto.getProjectNumber())) {
                errors.putIfAbsent("project[" + i + "].projectNumber", ResponseConstants.MSG_PROJECT_NUMBER_DUPLICATED_IN_REQUEST);
            }
            if (duplicatedMethodNumbersInRequest.contains(projectDto.getMethodNumber())) {
                errors.putIfAbsent("project[" + i + "].methodNumber", ResponseConstants.MSG_METHOD_NUMBER_DUPLICATED_IN_REQUEST);
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(UpdateProjectRequest request) {
        ProjectResponse response = new ProjectResponse();
        Project project = projectRepository.findById(request.getId()).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        // PATCH: overwrite only non-null fields
        if (request.getProjectName() != null) project.setProjectName(request.getProjectName());
        if (request.getBaselineEmissions() != null) project.setBaselineEmission(request.getBaselineEmissions());
        if (request.getPostProjectEmission() != null) project.setPostProjectEmission(request.getPostProjectEmission());

        if (request.getRepFirstName() != null) project.setRepFirstName(request.getRepFirstName());
        if (request.getRepFirstNameKana() != null) project.setRepFirstNameKana(request.getRepFirstNameKana());
        if (request.getRepLastName() != null) project.setRepLastName(request.getRepLastName());
        if (request.getRepLastNameKana() != null) project.setRepLastNameKana(request.getRepLastNameKana());
        if (request.getRepEmail() != null) project.setRepEmail(request.getRepEmail());
        if (request.getRepPhoneNumber() != null) project.setRepPhoneNumber(request.getRepPhoneNumber());
        if (request.getRepPosition() != null) project.setRepPosition(request.getRepPosition());

        if (request.getPicFirstName() != null) project.setPicFirstName(request.getPicFirstName());
        if (request.getPicFirstNameKana() != null) project.setPicFirstNameKana(request.getPicFirstNameKana());
        if (request.getPicLastName() != null) project.setPicLastName(request.getPicLastName());
        if (request.getPicLastNameKana() != null) project.setPicLastNameKana(request.getPicLastNameKana());
        if (request.getPicEmail() != null) project.setPicEmail(request.getPicEmail());
        if (request.getPicPhoneNumber() != null) project.setPicPhoneNumber(request.getPicPhoneNumber());
        if (request.getPicPosition() != null) project.setPicPosition(request.getPicPosition());
        if (request.getDataManagementPic() != null) project.setDataManagementPic(request.getDataManagementPic());

        if (request.getRegionalCategory() != null) project.setRegionalCategory(request.getRegionalCategory());

        if (request.getMethodologyVersion() != null) project.setMethodologyVersion(request.getMethodologyVersion());
        if (request.getMethodCategory() != null) project.setMethodCategory(request.getMethodCategory());
        if (request.getMethodNumber() != null) project.setMethodNumber(request.getMethodNumber());
        if (request.getMethodName() != null) project.setMethodName(request.getMethodName());
        if (request.getMethodMainEmission() != null) project.setMethodMainEmission(request.getMethodMainEmission());
        if (request.getMethodCombiFeasibility() != null) project.setMethodCombiFeasibility(request.getMethodCombiFeasibility());
        if (request.getMethodDetails() != null) project.setMethodDetails(request.getMethodDetails());

        project.setUpdatedBy(request.getProcessedBy());

        response.setProject(generateUpdateProjectResponse(request, projectRepository.save(project)));
        response.setStatusCode(ResponseConstants.OK);

        return response;
    }

    private ProjectDetailResponse generateUpdateProjectResponse(UpdateProjectRequest req, Project project) {
        ProjectDetailResponse detailResponse = new ProjectDetailResponse();
        if (req.getProjectName() != null) detailResponse.setProjectName(project.getProjectName());
        if (req.getBaselineEmissions() != null) detailResponse.setBaselineEmission(String.valueOf(project.getBaselineEmission()));
        if (req.getPostProjectEmission() != null) detailResponse.setPostProjectEmission(String.valueOf(project.getPostProjectEmission()));

        if (req.getRepFirstName() != null) detailResponse.setRepFirstName(project.getRepFirstName());
        if (req.getRepFirstNameKana() != null) detailResponse.setRepFirstNameKana(project.getRepFirstNameKana());
        if (req.getRepLastName() != null) detailResponse.setRepLastName(project.getRepLastName());
        if (req.getRepLastNameKana() != null) detailResponse.setRepLastNameKana(project.getRepLastNameKana());
        if (req.getRepEmail() != null) detailResponse.setRepEmail(project.getRepEmail());
        if (req.getRepPhoneNumber() != null) detailResponse.setRepPhoneNumber(project.getRepPhoneNumber());
        if (req.getRepPosition() != null) detailResponse.setRepPosition(project.getRepPosition());

        if (req.getPicFirstName() != null) detailResponse.setPicFirstName(project.getPicFirstName());
        if (req.getPicFirstNameKana() != null) detailResponse.setPicFirstNameKana(project.getPicFirstNameKana());
        if (req.getPicLastName() != null) detailResponse.setPicLastName(project.getPicLastName());
        if (req.getPicLastNameKana() != null) detailResponse.setPicLastNameKana(project.getPicLastNameKana());
        if (req.getPicEmail() != null) detailResponse.setPicEmail(project.getPicEmail());
        if (req.getPicPhoneNumber() != null) detailResponse.setPicPhoneNumber(project.getPicPhoneNumber());
        if (req.getPicPosition() != null) detailResponse.setPicPosition(project.getPicPosition());

        if (req.getDataManagementPic() != null) detailResponse.setDataManagementPic(project.getDataManagementPic());
        if (req.getRegionalCategory() != null) detailResponse.setRegionalCategory(project.getRegionalCategory());

        if (req.getMethodologyVersion() != null) detailResponse.setMethodologyVersion(project.getMethodologyVersion());
        if (req.getMethodCategory() != null) detailResponse.setMethodologyCategory(project.getMethodCategory());
        if (req.getMethodNumber() != null) detailResponse.setMethodologyNumber(project.getMethodNumber());
        if (req.getMethodName() != null) detailResponse.setMethodologyName(project.getMethodName());
        if (req.getMethodMainEmission() != null) detailResponse.setMethodologyMainEmission(project.getMethodMainEmission());
        if (req.getMethodCombiFeasibility() != null) detailResponse.setMethodCombiFeasibility(project.getMethodCombiFeasibility());
        if (req.getMethodDetails() != null) detailResponse.setMethodDetails(project.getMethodDetails());

        return detailResponse;
    }

    private CreateProjectResponse generateProjectListResponse(List<Project> projectList) {
        CreateProjectResponse response = new CreateProjectResponse();

        List<ProjectDetailResponse> projectDetailList = projectList.stream()
            .map(p -> ProjectDetailResponse.builder()
                    .id(p.getId().toString())
                    .projectName(p.getProjectName())
                    .projectNumber(p.getProjectNumber())
                    .projectStatus(p.getProjectStatus())
                    .emissionReduction(p.getEmissionReduction() != null ? p.getEmissionReduction().toString() : null)
                    .baselineEmission(p.getBaselineEmission() != null ? p.getBaselineEmission().toString() : null)
                    .postProjectEmission(p.getPostProjectEmission() != null ? p.getPostProjectEmission().toString() : null)
                    .repFirstName(p.getRepFirstName())
                    .repFirstNameKana(p.getRepFirstNameKana())
                    .repLastName(p.getRepLastName())
                    .repLastNameKana(p.getRepLastNameKana())
                    .repEmail(p.getRepEmail())
                    .repPhoneNumber(p.getRepPhoneNumber())
                    .repPosition(p.getRepPosition())
                    .picFirstName(p.getPicFirstName())
                    .picFirstNameKana(p.getPicFirstNameKana())
                    .picLastName(p.getPicLastName())
                    .picLastNameKana(p.getPicLastNameKana())
                    .picEmail(p.getPicEmail())
                    .picPhoneNumber(p.getPicPhoneNumber())
                    .picPosition(p.getPicPosition())
                    .dataManagementPic(p.getDataManagementPic())
                    .companyName(p.getOperatorCompany() != null ? p.getOperatorCompany().getNameJa() : null)
                    .regionalCategory(p.getRegionalCategory())
                    .newMembersTotal(p.getNewMembersTotal())
                    .withdrawnMembers(p.getWithdrawnMembers())
                    .cumulativeMembers(p.getCumulativeMembersTotal())
                    .membersTotal(p.getMembersTotal())
                    .methodologyVersion(p.getMethodologyVersion())
                    .methodologyCategory(p.getMethodCategory())
                    .methodologyNumber(p.getMethodNumber())
                    .methodologyName(p.getMethodName())
                    .methodologyMainEmission(p.getMethodMainEmission())
                    .methodCombiFeasibility(p.getMethodCombiFeasibility())
                    .methodDetails(p.getMethodDetails())
                    .operatorCompanyId(
                            p.getOperatorCompany() != null ? p.getOperatorCompany().getId() : null
                    )
                    .isJoined(false) // or set from your own logic
                    .build()
                )
            .collect(Collectors.toList());

        response.setProject(projectDetailList);

        return response;
    }

    private Project convertProjectDtoToEntity(ProjectDtoReq projectDto, String username, String operatorCompanyId) {
        OperatorCompany operatorCompany = null;
        String dtoOperatorCompanyId = projectDto.getOperatorCompanyId();
        if (dtoOperatorCompanyId != null && !dtoOperatorCompanyId.isBlank()) {
            operatorCompany = resolveOperatorCompany(dtoOperatorCompanyId);
        } else if (operatorCompanyId != null && !operatorCompanyId.isBlank()) {
            operatorCompany = operatorCompanyRepository.findByCompanyId(operatorCompanyId)
                .orElseThrow(() -> {
                    log.error("ProjectServiceImpl.createProject()関数 : エラーcompany Id " + operatorCompanyId);
                    return new UnsupportedOperationException(ResponseConstants.MSG_UNEXPEXTED_ERROR);
                });
        } else {
            log.error("ProjectServiceImpl.createProject()関数 : company Id is missing");
            throw new UnsupportedOperationException(ResponseConstants.MSG_UNEXPEXTED_ERROR);
        }
        
        Project project = Project.builder().build();

        Double emissionReduction = null;
        if (projectDto.getBaselineEmissions() != null && projectDto.getPostProjectEmission() != null) {
            emissionReduction = projectDto.getBaselineEmissions() - projectDto.getPostProjectEmission();
        }

        project = Project.builder()
            .projectName(projectDto.getProjectName())
            .projectNumber(projectDto.getProjectNumber())
            .projectStatus(projectDto.getProjectStatus())
            .repFirstName(projectDto.getRepFirstName())
            .repFirstNameKana(projectDto.getRepFirstNameKana())
            .repLastName(projectDto.getRepLastName())
            .repLastNameKana(projectDto.getRepLastNameKana())
            .repEmail(projectDto.getRepEmail())
            .repPhoneNumber(projectDto.getRepPhoneNumber())
            .repPosition(projectDto.getRepPosition())
            .picFirstName(projectDto.getPicFirstName())
            .picFirstNameKana(projectDto.getPicFirstNameKana())
            .picLastName(projectDto.getPicLastName())
            .picLastNameKana(projectDto.getPicLastNameKana())
            .picEmail(projectDto.getPicEmail())
            .picPhoneNumber(projectDto.getPicPhoneNumber())
            .picPosition(projectDto.getPicPosition())
            .dataManagementPic(projectDto.getDataManagementPic())
            .emissionReduction(emissionReduction)
            .baselineEmission(projectDto.getBaselineEmissions())
            .postProjectEmission(projectDto.getPostProjectEmission())
            .regionalCategory(projectDto.getRegionalCategory())
            .newMembersTotal(projectDto.getNewMembersTotal())
            .withdrawnMembers(projectDto.getWithdrawnMembers())
            .cumulativeMembersTotal(projectDto.getCumulativeMembers())
            .membersTotal(projectDto.getMembersTotal())
            .methodologyVersion(projectDto.getMethodologyVersion())
            .methodCategory(projectDto.getMethodCategory())
            .methodNumber(projectDto.getMethodNumber())
            .methodName(projectDto.getMethodName())
            .methodMainEmission(projectDto.getMethodMainEmission())
            .methodCombiFeasibility(projectDto.getMethodCombiFeasibility())
            .methodDetails(projectDto.getMethodDetails())
            .operatorCompany(operatorCompany)
            .build();
        project.setCreatedBy(username);

        return project;
    }

    private OperatorCompany resolveOperatorCompany(String operatorCompanyId) {
        try {
            Integer id = Integer.valueOf(operatorCompanyId);
            return operatorCompanyRepository.findById(id)
                .orElseGet(() -> operatorCompanyRepository.findByCompanyId(operatorCompanyId)
                    .orElseThrow(() -> {
                        log.error("ProjectServiceImpl.createProject()関数 : エラーcompany Id " + operatorCompanyId);
                        return new UnsupportedOperationException(ResponseConstants.MSG_UNEXPEXTED_ERROR);
                    }));
        } catch (NumberFormatException ex) {
            return operatorCompanyRepository.findByCompanyId(operatorCompanyId)
                .orElseThrow(() -> {
                    log.error("ProjectServiceImpl.createProject()関数 : エラーcompany Id " + operatorCompanyId);
                    return new UnsupportedOperationException(ResponseConstants.MSG_UNEXPEXTED_ERROR);
                });
        }
    }

    @Override
    public ProjectResponse getProject(Integer projectId, String userCompanyId) {
        ProjectResponse response = new ProjectResponse();
        Project project = projectRepository.findById(projectId).orElse(null);

        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        ProjectDetailResponse dto = convertToProjectDetailResponse(project);

        response.setProject(dto);
        response.setStatusCode(ResponseConstants.OK);

        return response;
    }


    private ProjectDetailResponse convertToProjectDetailResponse(Project project) {
        ProjectDetailResponse dto =  ProjectDetailResponse.builder()
            .id(project.getId() != null ? project.getId().toString() : null)
            .projectName(project.getProjectName())
            .projectNumber(project.getProjectNumber())
            .projectStatus(project.getProjectStatus())
            .emissionReduction(String.valueOf(project.getEmissionReduction()))
            .baselineEmission(String.valueOf(project.getBaselineEmission()))
            .postProjectEmission(String.valueOf(project.getPostProjectEmission()))
            .repFirstName(project.getRepFirstName())
            .repFirstNameKana(project.getRepFirstNameKana())
            .repLastName(project.getRepLastName())
            .repLastNameKana(project.getRepLastNameKana())
            .repEmail(project.getRepEmail())
            .repPhoneNumber(project.getRepPhoneNumber())
            .repPosition(project.getRepPosition())
            .picFirstName(project.getPicFirstName())
            .picFirstNameKana(project.getPicFirstNameKana())
            .picLastName(project.getPicLastName())
            .picLastNameKana(project.getPicLastNameKana())
            .picEmail(project.getPicEmail())
            .picPhoneNumber(project.getPicPhoneNumber())
            .picPosition(project.getPicPosition())
            .dataManagementPic(project.getDataManagementPic())
            .companyName(project.getOperatorCompany().getNameJa())
            .regionalCategory(project.getRegionalCategory())
            .newMembersTotal(project.getNewMembersTotal())
            .withdrawnMembers(project.getWithdrawnMembers())
            .cumulativeMembers(project.getCumulativeMembersTotal())
            .membersTotal(project.getMembersTotal())
            .methodologyVersion(project.getMethodologyVersion())
            .methodologyCategory(project.getMethodCategory())
            .methodologyNumber(project.getMethodNumber())
            .methodologyName(project.getMethodName())
            .methodologyMainEmission(project.getMethodMainEmission())
            .methodCombiFeasibility(project.getMethodCombiFeasibility())
            .methodDetails(project.getMethodDetails())
            .operatorCompanyId(project.getOperatorCompany().getId())
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .build();
        return dto;
    }

    @Override
    public ProjectListInfoResponse findByFilter(
            int page,
            int limit,
            String status,
            String methodology,
            LocalDate createdFrom,
            LocalDate createdTo,
            LocalDate updatedFrom,
            LocalDate updatedTo,
            String search,
            String sort,
            String sortOrder
    ) {
        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                Sort.by("desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC, sort)
        );

        Specification<Project> spec = Specification.unrestricted();

        if (status != null && !status.isBlank()) {
            spec = spec.and(ProjectSpecification.hasStatus(status));
        }
        if (methodology != null && !methodology.isBlank()) {
            spec = spec.and(ProjectSpecification.hasMethodology(methodology));
        }
        if (createdFrom != null) {
            spec = spec.and(ProjectSpecification.createdAtFrom(createdFrom.atStartOfDay()));
        }
        if (createdTo != null) {
            spec = spec.and(ProjectSpecification.createdAtTo(createdTo.atStartOfDay())); 
        }
        if (updatedFrom != null) {
            spec = spec.and(ProjectSpecification.updatedAtFrom(updatedFrom.atStartOfDay()));
        }
        if (updatedTo != null) {
            spec = spec.and(ProjectSpecification.updatedAtTo(updatedTo.atStartOfDay()));
        }
        if (search != null && !search.isBlank()) {
            spec = spec.and(ProjectSpecification.containsKeyword(search));
        }

        Page<Project> pageResult = projectRepository.findAll(spec, pageable);

        List<ProjectDetailResponse> projectDtos = pageResult.getContent().stream()
                .map(this::convertToProjectDetailResponse)
                .toList();

        PaginationDetailResponse pagination = new PaginationDetailResponse(
                page,
                limit,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                null,
                true
        );

        ProjectListInfoResponse response = new ProjectListInfoResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setProjects(projectDtos);
        response.setPagination(pagination);
        return response;
    }

    @Override
    public UpdateProjectStatusResponse updateProjectStatus(String newStatus, Integer projectId) {
        UpdateProjectStatusResponse response = new UpdateProjectStatusResponse();
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
            return response;
        }

        if(project.getIsActive() != null && !project.getIsActive()) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
        if(project.getProjectStatus().equals(newStatus)) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage("プロジェクトはすでにこのステータスです。");
            return response;
        }
        String previousStatus = project.getProjectStatus();
        

        project.setProjectStatus(newStatus);

        Project updatedProject = projectRepository.save(project);

        response.setUpdatedAt(updatedProject.getUpdatedAt());
        response.setProjectName(updatedProject.getProjectName());
        response.setNewStatus(updatedProject.getProjectStatus());
        response.setOldStatus(previousStatus);
        response.setStatusCode(ResponseConstants.OK);

        return response;
    }
}
