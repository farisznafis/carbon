package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CreateSiteRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateSiteRequest;
import com.kct.jcredit.core_backend.dto.response.CreateSiteResponse;
import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.SiteInfoResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.EquipmentItemSolarResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteDataResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.entity.EnR002EquipmentFile;
import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
import com.kct.jcredit.core_backend.repository.postgres.EnR002EquipmentFileRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.SiteService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;
    private final ParticipatingCompanyRepository participatingCompanyRepository;
    private final ProjectRepository projectRepository;
    private final SiteR002MemberDataRepository siteR002MemberDataRepository;
    private final EnR002EquipmentFileRepository siteR002EquipmentFileRepository;
    private final SiteProjectRelationRepository siteProjectRelationRepository;
    private final ParticipatingUserRepository participatingUserRepository;
    private static final Integer MAX_SITES_PER_COMPANY = 20; // Maximum number of sites allowed per company

    @Override
    public SiteListResponse getSitesByCompanyId(String companyId) {
        SiteListResponse response = SiteListResponse.builder().build();

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId)
            .orElse(null);

        if (Objects.isNull(company)) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        
        List<Site> sites = siteRepository.findByCompany(company);
        

        if (sites.isEmpty()) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        List<SiteResponse> siteList = sites.stream().map(site -> SiteResponse.builder()
                .id(String.valueOf(site.getId()))
                .siteName(site.getSiteName())
                .postalCode(site.getPostalCode())
                .prefecture(site.getPrefecture())
                .municipality(site.getMunicipality())
                .blocknum(site.getBlocknum())
                .buildingName(site.getBuildingName())
                .picFirstName(site.getPicFirstName())
                .picLastName(site.getPicLastName())
                .projects(site.getSiteProjectRelations() == null ? List.of() :
                        site.getSiteProjectRelations().stream()
                                .map(SiteProjectRelation::getProject)
                                .map(Project::getProjectName)
                                .collect(Collectors.toList()))
            .data(null)
                .build()
        ).collect(Collectors.toList());

        response.setSiteList(siteList);
        response.setStatusCode("OK");
        return response;
    }

    @Override
    public SiteInfoResponse getSiteById(int siteId) {
        SiteInfoResponse response = new SiteInfoResponse();
        SiteResponse siteDetails = new SiteResponse();

        try {
            Optional<Site> siteOpt = siteRepository.findById(siteId);
            if (siteOpt.isEmpty()) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }

            Site site = siteOpt.get();

            SiteDataResponse data = new SiteDataResponse();

            //To be updated
            data.setJCredit("1");
            data.setSumEmissionReduction("0.5");
            data.setSumEmission("1.5");

            siteDetails.setId(String.valueOf(site.getId()));
            siteDetails.setSiteName(site.getSiteName());
            siteDetails.setPostalCode(site.getPostalCode());
            siteDetails.setPrefecture(site.getPrefecture());
            siteDetails.setMunicipality(site.getMunicipality());
            siteDetails.setBlocknum(site.getBlocknum());
            siteDetails.setBuildingName(site.getBuildingName());
            siteDetails.setPicFirstName(site.getPicFirstName());
            siteDetails.setPicLastName(site.getPicLastName());
                siteDetails.setProjects(site.getSiteProjectRelations() == null ? List.of() :
                    site.getSiteProjectRelations().stream()
                        .map(SiteProjectRelation::getProject)
                        .map(Project::getProjectName)
                        .collect(Collectors.toList()));
            siteDetails.setData(data);

            response.setSite(siteDetails);
            response.setStatusCode(ResponseConstants.OK);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse validateCreateSite(CreateSiteRequest request, String companyId) {
        Map<String, String> errors = new LinkedHashMap<>();

        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (Objects.isNull(company)) {
            errors.put("companyId", ResponseConstants.MSG_NOT_FOUND);
            return validationError(errors);
        }

        if (siteRepository.countByCompany_Id(company.getId()) >= MAX_SITES_PER_COMPANY) {
            errors.put("companyId", ResponseConstants.MSG_LIMIT_EXCEEDED);
        }

        ParticipatingUser user = participatingUserRepository.findById(request.getUserId()).orElse(null);
        if (Objects.isNull(user)) {
            errors.put("userId", ResponseConstants.MSG_USER_NOT_FOUND);
            return validationError(errors);
        }

        if (user.getCompany() == null || !user.getCompany().getId().equals(company.getId())) {
            errors.put("userId", ResponseConstants.MSG_FORBIDDEN);
        }
        if (!RoleConstants.MASTER.equals(user.getRole()) && !Objects.isNull(user.getSite())) {
            errors.put("userId", ResponseConstants.MSG_BAD_REQUEST);
        }

        return errors.isEmpty()
                ? BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS)
                : validationError(errors);
    }

    @Override
    @Transactional
    public CreateSiteResponse createSite(CreateSiteRequest request, String companyId) {
        CreateSiteResponse response = new CreateSiteResponse();
        ParticipatingCompany company = participatingCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (Objects.isNull(company)) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }
        
        if(siteRepository.countByCompany_Id(company.getId()) >= MAX_SITES_PER_COMPANY){
            response.setStatusCode(ResponseConstants.LIMIT_EXCEEDED);
            response.setMessage(ResponseConstants.MSG_LIMIT_EXCEEDED);
            return response;
        }

        ParticipatingUser user = participatingUserRepository.findById(request.getUserId()).orElse(null);
            
        if (Objects.isNull(user)) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_USER_NOT_FOUND);
            return response;
        }

        if(!user.getCompany().getId().equals(company.getId())){
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
        //check that user's site is not null
        if(!RoleConstants.MASTER.equals(user.getRole()) && !Objects.isNull(user.getSite())){
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_BAD_REQUEST);
            return response;
        }

        Site site = Site.builder()
                .siteName(request.getSiteName())
                .postalCode(request.getPostalCode())
                .prefecture(request.getPrefecture())
                .municipality(request.getMunicipality())
                .blocknum(request.getBlocknum())
                .buildingName(request.getBuildingName())
                .company(company)
                .picFirstName(user.getFirstName())
                .picFirstNameKana(user.getFirstNameKana())
                .picLastName(user.getLastName())
                .picLastNameKana(user.getLastNameKana())
                .picEmail(user.getEmail())
                .picPhoneNumber(user.getPhoneNumber())
                .picPosition(user.getUserPosition())
                .build();
        site.setCreatedBy(company.getNameJa());
        try {
            Site savedSite = siteRepository.save(site);
            response.setSiteId(savedSite.getId());
            user.setSite(site);
            participatingUserRepository.save(user);
            response.setStatusCode(ResponseConstants.OK);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse validateUpdateSite(String siteId, String companyId, UpdateSiteRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        Integer parsedSiteId = parseSiteId(siteId, errors);
        if (parsedSiteId == null) {
            return validationError(errors);
        }

        Site site = siteRepository.findById(parsedSiteId).orElse(null);
        if (Objects.isNull(site)) {
            errors.put("siteId", ResponseConstants.MSG_NOT_FOUND);
            return validationError(errors);
        }

        if (site.getCompany() == null || !site.getCompany().getCompanyId().equals(companyId)) {
            errors.put("siteId", ResponseConstants.MSG_FORBIDDEN);
        }

        if (request.getPICUserId() != null) {
            ParticipatingUser user = participatingUserRepository.findById(request.getPICUserId()).orElse(null);
            if (Objects.isNull(user)) {
                errors.put("PICUserId", ResponseConstants.MSG_USER_NOT_FOUND);
            } else {
                if (user.getCompany() == null || !user.getCompany().getCompanyId().equals(companyId)) {
                    errors.put("PICUserId", ResponseConstants.MSG_FORBIDDEN);
                }
                if (!RoleConstants.MASTER.equals(user.getRole())
                        && !Objects.isNull(user.getSite())
                        && !user.getSite().getId().equals(site.getId())) {
                    errors.put("PICUserId", ResponseConstants.MSG_BAD_REQUEST);
                }
            }
        }

        return errors.isEmpty()
                ? BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS)
                : validationError(errors);
    }

    @Override
    @Transactional
    public SiteInfoResponse updateSite(String siteId, String companyId, UpdateSiteRequest request) {
        //StringUtils.isEmpty(value) use this for null object logic (its concise)

        SiteInfoResponse response = new SiteInfoResponse();
        
        // Find site by ID
        Site site = siteRepository.findById(Integer.parseInt(siteId)).orElse(null);
        
        if (Objects.isNull(site)) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }
        
        // Check if site belongs to the company (security check)
        if (!site.getCompany().getCompanyId().equals(companyId)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
        //check if PIC can change to selected user
        ParticipatingUser user = participatingUserRepository.findById(request.getPICUserId()).orElse(null);

            
        if (!Objects.isNull(user)) {
            if(!user.getCompany().getCompanyId().equals(companyId)){
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
            }
            //check that user's site is the current site being updated or is master
            if(!RoleConstants.MASTER.equals(user.getRole()) && !Objects.isNull(user.getSite()) && !user.getSite().getId().equals(site.getId())){
                response.setStatusCode(ResponseConstants.BAD_REQUEST);
                response.setMessage(ResponseConstants.MSG_BAD_REQUEST);
                return response;
            }
        }
        // Update site adress with request data
        Site updatedSite = updateSite(site, request, user);
        
        // Build response using SiteResponse
        SiteResponse siteResponse = SiteResponse.builder()
            .id(String.valueOf(updatedSite.getId()))
            .siteName(updatedSite.getSiteName())
            .postalCode(updatedSite.getPostalCode())
            .prefecture(updatedSite.getPrefecture())
            .municipality(updatedSite.getMunicipality())
            .blocknum(updatedSite.getBlocknum())
            .buildingName(updatedSite.getBuildingName())
            .picFirstName(updatedSite.getPicFirstName())
            .picLastName(updatedSite.getPicLastName())
            .build();

        
        response.setStatusCode(ResponseConstants.OK);
        response.setSite(siteResponse);

        return response;
    }

    @Override
    public EquipmentSolarRegisterResponse getSiteProjectEquipment(
            Integer siteId,
            Integer projectId,
            String companyId,
            String role,
            String username) {
        EquipmentSolarRegisterResponse response = new EquipmentSolarRegisterResponse();        

        try {
            Site site = siteRepository.findById(siteId).orElse(null);
            if (site == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }

            Project project = projectRepository.findById(projectId).orElse(null);
            if (project == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_PROJECT_NOT_FOUND);
                return response;
            }

            if (!hasAccessToSite(site, companyId, role, username)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response;
            }

            SiteProjectRelation relation = siteProjectRelationRepository.findByProjectIdAndSiteId(projectId, siteId);
            if (relation == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }

            SiteR002MemberData memberData = siteR002MemberDataRepository
                    .findByProject_IdAndSite_Id(projectId, siteId)
                    .orElse(null);
            if (memberData == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
                return response;
            }

            List<EnR002EquipmentFile> equipmentFiles = siteR002EquipmentFileRepository
                    .findByMemberData_Id(memberData.getId());
            if (equipmentFiles.isEmpty() && !memberData.getId().equals(siteId)) {
                equipmentFiles = siteR002EquipmentFileRepository.findByMemberData_Id(siteId);
            }

            List<String> evidenceDocuments = equipmentFiles.stream()
                    .map(EnR002EquipmentFile::getS3Key)
                    .map(this::extractFileNameFroms3Key)
                    .filter(Objects::nonNull)
                    .filter(name -> !name.isBlank())
                    .collect(Collectors.toList());

            EquipmentItemSolarResponse item = EquipmentItemSolarResponse.builder()
                    .equipmentId(memberData.getId())
                    .pcsManufacturer(memberData.getPcsManufacturer())
                    .pcsModel(memberData.getPcsModel())
                    .powerCondUnitCount(memberData.getPowerCondUnitCount())
                    .deviceNo(memberData.getDeviceNo())
                    .powerCondStandby(memberData.getPowerCondStandby())
                    // .categoryBc(memberData.getCategoryBc()) category bc in other table/take it from dynamo
                    .nominalMaxOutput(memberData.getNominalMaxOutput())
                    .ictEquipmentPowerConsumption(memberData.getIctEquipmentPowerConsumption())
                    .additionalEquipment(memberData.getAdditionalEquipment())
                    .additionalEquipmentDate(memberData.getAdditionalEquipmentDate())
                    .additionalEquipmentType(memberData.getAdditionalEquipmentType())
                    .additionalUnitCount(memberData.getAdditionalUnitCount())
                    .additionalDeviceUniqueNo(memberData.getAdditionalDeviceUniqueNo())
                    // .equipmentId(evidenceDocuments)
                    .build();

            response.setEquipments(List.of(item));
            response.setStatusCode(ResponseConstants.OK);
            return response;
        } catch (Exception e) {
            log.error("SiteServiceImpl.getSiteProjectEquipment error", e);
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
            return response;
        }
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
                    .findByUsernameAndCompany_Id(username, site.getCompany().getId())
                    .orElse(null);
            if (user == null || user.getSite() == null || user.getSite().getId() == null) {
                return false;
            }
            return user.getSite().getId().equals(site.getId());
        }

        return false;
    }

    private String extractFileNameFroms3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }

        String key = s3Key.startsWith("/") ? s3Key.substring(1) : s3Key;
        int lastSlash = key.lastIndexOf('/');
        String baseName = lastSlash >= 0 ? key.substring(lastSlash + 1) : key;

        if (baseName.startsWith("file_")) {
            int secondUnderscore = baseName.indexOf('_', "file_".length());
            if (secondUnderscore > 0 && secondUnderscore + 1 < baseName.length()) {
                return baseName.substring(secondUnderscore + 1);
            }
        }

        return baseName;
    }


    private Site updateSite(Site site, UpdateSiteRequest request, ParticipatingUser user) {
        if (StringUtils.isNotBlank(request.getSiteName())) {
            site.setSiteName(request.getSiteName());
        }
        if (StringUtils.isNotBlank(request.getPostalCode())) {
            site.setPostalCode(request.getPostalCode());
        }
        if (StringUtils.isNotBlank(request.getPrefecture())) {
            site.setPrefecture(request.getPrefecture());
        }
        if (StringUtils.isNotBlank(request.getMunicipality())) {
            site.setMunicipality(request.getMunicipality());
        }
        if (StringUtils.isNotBlank(request.getBlocknum())) {
            site.setBlocknum(request.getBlocknum());
        }
        if (StringUtils.isNotBlank(request.getBuildingName())) {
            site.setBuildingName(request.getBuildingName());
        }
        if(!Objects.isNull(user)){
            site.setPicEmail(user.getEmail());
            site.setPicFirstName(user.getFirstName());
            site.setPicFirstNameKana(user.getFirstNameKana());
            site.setPicLastName(user.getLastName());
            site.setPicLastNameKana(user.getLastNameKana());
            site.setPicPhoneNumber(user.getPhoneNumber());
            site.setPicPosition(user.getUserPosition());
        }
        site.setUpdatedAt(LocalDateTime.now());

        return siteRepository.save(site);
    }

    private Integer parseSiteId(String siteId, Map<String, String> errors) {
        try {
            return Integer.parseInt(siteId);
        } catch (NumberFormatException e) {
            errors.put("siteId", ResponseConstants.MSG_BAD_REQUEST);
            return null;
        }
    }

    private BaseResponse validationError(Map<String, String> errors) {
        BaseResponse response = BaseResponse.validationError(errors);
        response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
        return response;
    }
}
