package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import com.kct.jcredit.core_backend.dto.request.DeleteUserSite;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.*;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
import com.kct.jcredit.core_backend.entity.ParticipatingUser;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
import com.kct.jcredit.core_backend.service.TokenBlacklistService;
import com.kct.jcredit.core_backend.service.UsernameAvailabilityService;
import com.kct.jcredit.core_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final ParticipatingUserRepository userRepository;
    private final ParticipatingCompanyRepository participatingCompanyRepository;
    private final SiteRepository siteRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final UsernameAvailabilityService usernameAvailabilityService;
    private static final int MAX_USERS_PER_SITE = 5; // Maximum number of users allowed per site
    private static final int MAX_USERS_PER_COMPANY = 50; // Maximum number of users allowed per company

    @Override
    public UserProfileResponse getUserProfile(String username) {
        UserProfileResponse response = UserProfileResponse.builder().build();
        try {
            ParticipatingUser user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_USER_NOT_FOUND);
                return response;
            }

            ParticipatingCompany company = user.getCompany();

            CompanyInfoResponse companyInfo = null;
            if (company != null) {
                companyInfo = CompanyInfoResponse.builder()
                    .companyId(company.getCompanyId())
                    .nameJA(company.getNameJa())
                    .nameFurigana(company.getNameFurigana())
                    .nameEN(company.getNameEn())
                    .email(company.getEmail())
                    .phoneNumber(company.getPhoneNumber())
                    .fax(company.getFax())
                    .corpNumber(company.getCorpNumber())
                    .address(String.format("%s %s %s %s", company.getPrefecture(), company.getMunicipality(), company.getBlocknum(), company.getBuildingName()))
                    .isSuspended(company.isSuspended())
                    .build();
            }

            UserInfoResponse profile = UserInfoResponse.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .name(user.getLastName() + " " + user.getFirstName())
                .firstName(user.getFirstName())
                .firstNameKana(user.getFirstNameKana())
                .lastName(user.getLastName())
                .lastNameKana(user.getLastNameKana())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .userPosition(user.getUserPosition())
                .role(user.getRole())
                .siteId(user.getSite() != null ? user.getSite().getId().toString() : null)
                .siteName(user.getSite() != null ? user.getSite().getSiteName() : null)
                .companyId(user.getCompany().getCompanyId())
                .company(companyInfo)
                .build();
            
            response.setProfile(profile);
            response.setStatusCode(ResponseConstants.OK);

            return response;

        } catch (Exception e) {
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
            return response;
        }
    }

    @Override
    public UserListResponse getUsersByCompany(String companyId) {
        UserListResponse response = new UserListResponse();
        // Check company exists
        var company = participatingCompanyRepository.findByCompanyId(companyId)
                .orElse(null);

        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage("該当する会社が見つかりませんでした。");
            return response;
        }
        
        List<ParticipatingUser> users =
                userRepository.findByCompany_Id(company.getId());

        // Map entities → DTO
        List<UserInfoResponse> userInfoList = users.stream()
            .map(this::mapToUserInfoResponse)
            .toList();

        response.setUsers(userInfoList);
        response.setStatusCode(ResponseConstants.OK);

        return response;
    }


    @Override
    @Transactional(readOnly = true)
    public BaseResponse validateUpdateUser(String masterCompanyId,
                                           String username,
                                           UpdateUserRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        ParticipatingCompany masterCompany = participatingCompanyRepository
                .findByCompanyId(masterCompanyId)
                .orElse(null);
        if (masterCompany == null) {
            errors.put("companyId", ResponseConstants.MSG_NOT_FOUND);
            return validationError(errors);
        }

        ParticipatingUser requestingUser = userRepository
                .findByUsernameAndCompany_Id(username, masterCompany.getId())
                .orElse(null);
        if (requestingUser == null) {
            errors.put("username", ResponseConstants.MSG_USER_NOT_FOUND);
            return validationError(errors);
        }

        boolean isMaster = RoleConstants.MASTER.equals(requestingUser.getRole());
        ParticipatingUser targetUser = requestingUser;
        if (isMaster && request.getUserId() != null) {
            targetUser = userRepository.findById(request.getUserId()).orElse(null);
            if (targetUser == null) {
                errors.put("UserId", ResponseConstants.MSG_USER_NOT_FOUND);
                return validationError(errors);
            }
        }

        boolean isSelf = targetUser.getId().equals(requestingUser.getId());
        Site currentSite = targetUser.getSite();
        Integer currentSiteId = currentSite != null ? currentSite.getId() : null;
        Integer requestedSiteId = request.getSiteId();
        boolean isSiteChange = isMaster
                && !isSelf
                && !(requestedSiteId == null && currentSiteId == null)
                && !Objects.equals(requestedSiteId, currentSiteId);

        if (!isMaster && request.getUserId() != null && !request.getUserId().equals(requestingUser.getId())) {
            errors.put("UserId", ResponseConstants.MSG_FORBIDDEN);
        }
        if (!isMaster && requestedSiteId != null) {
            errors.put("siteId", ResponseConstants.MSG_FORBIDDEN);
        }
        if (isMaster && isSelf && requestedSiteId != null) {
            errors.put("siteId", ResponseConstants.MSG_BAD_REQUEST);
        }
        if (!isCompanyAligned(masterCompanyId, masterCompany, requestingUser, targetUser)) {
            errors.put("UserId", ResponseConstants.MSG_FORBIDDEN);
        }

        if (isSiteChange && requestedSiteId != null) {
            Site requestedSite = siteRepository.findById(requestedSiteId).orElse(null);
            if (requestedSite == null) {
                errors.put("siteId", ResponseConstants.MSG_SITE_NOT_FOUND);
            } else if (!masterCompanyId.equals(requestedSite.getCompany().getCompanyId())) {
                errors.put("siteId", ResponseConstants.MSG_FORBIDDEN);
            } else if (userRepository.countByCompany_IdAndSite_Id(masterCompany.getId(), requestedSiteId) >= MAX_USERS_PER_SITE) {
                errors.put("siteId", ResponseConstants.MSG_LIMIT_EXCEEDED);
            }
        }

        return errors.isEmpty()
                ? BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS)
                : validationError(errors);
    }

    @Override
    @Transactional // FIX #6: wrap the entire operation in a transaction
    public UpdateUserResponse updateUser(String masterCompanyId,
                                         String username,
                                         UpdateUserRequest request) {

        UpdateUserResponse response = new UpdateUserResponse();
        System.out.println("UpdateUserRequest: " + request);

        try {
            // ── Resolve requesting user ───────────────────────────────────────
            ParticipatingCompany masterCompany = participatingCompanyRepository
                    .findByCompanyId(masterCompanyId)
                    .orElseThrow(() -> new IllegalStateException("想定外エラーが発せしました。"));

            ParticipatingUser requestingUser = userRepository
                    .findByUsernameAndCompany_Id(username, masterCompany.getId())
                    .orElseThrow(() -> new IllegalStateException("想定外エラーが発せしました。"));

            // ── Resolve target user ───────────────────────────────────────────
            ParticipatingUser targetUser = resolveTargetUser(request, requestingUser, response);
            if (targetUser == null) return response; // NOT_FOUND already set

            // ── Build the 4 flags cleanly up front ───────────────────────────
            boolean isMaster     = "master".equals(requestingUser.getRole());
            boolean isSelf       = targetUser.getId().equals(requestingUser.getId());
            boolean isPIC        = isSitePIC(targetUser.getSite(), targetUser);

            Site    currentSite      = targetUser.getSite();
            Integer currentSiteId    = (currentSite != null) ? currentSite.getId() : null;
            Integer requestedSiteId  = request.getSiteId();
            boolean isSiteChange = isMaster
                                    && !isSelf
                                    && !(requestedSiteId == null && currentSiteId == null)  // both null → no change
                                    && !Objects.equals(requestedSiteId, currentSiteId);     // same id → no change

            // ── GATE 1: Non-master editing someone else → FORBIDDEN ───────────
            // Covers cases: 1, 3, 9, 11
            if (!isMaster && !isSelf) {
                // FIX #7: was silently falling through to self-edit before
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response;
            }

            // ── GATE 2: Non-master attempting a site change → FORBIDDEN ───────
            // Covers cases: 10, 12
            if (!isMaster && requestedSiteId != null) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage("拠点の変更はマスターユーザーのみ可能です。");
                return response;
            }

            // ── GATE 3: Master changing own site → BAD REQUEST ────────────────
            // Covers cases: 14, 16
            // FIX #3: was never explicitly caught before
            if (isMaster && isSelf && requestedSiteId != null) {
                response.setStatusCode(ResponseConstants.BAD_REQUEST);
                response.setMessage("マスターユーザーは自身の拠点を変更できません。");
                return response;
            }

            // ── Security: verify all parties belong to the same company ───────
            if (!isCompanyAligned(masterCompanyId, masterCompany, requestingUser, targetUser)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response;
            }

            // ── Route to correct handler ──────────────────────────────────────
            List<SiteResponse> updatedSites = new ArrayList<>();

            if (isMaster) {
                handleMasterUpdate(request, masterCompany, requestingUser,
                                   targetUser, isSelf, isPIC, isSiteChange,
                                   currentSite, requestedSiteId, updatedSites, response);
            } else {
                handleSelfUpdate(request, targetUser, isPIC,
                                 currentSite, updatedSites, response);
            }

        } catch (IllegalStateException e) {
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
        }

        return response;
    }
    
 // ─────────────────────────────────────────────────────────────────────────
    // TARGET USER RESOLUTION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the target user from the request.
     * Master with a userId targets that user, everyone else targets themselves.
     */
    private ParticipatingUser resolveTargetUser(UpdateUserRequest request,
                                                 ParticipatingUser requestingUser,
                                                 UpdateUserResponse response) {
        boolean isMaster = "master".equals(requestingUser.getRole());

        if (isMaster && request.getUserId() != null) {
            ParticipatingUser target = userRepository.findById(request.getUserId()).orElse(null);
            if (target == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_USER_NOT_FOUND);
                return null;
            }
            return target;
        }

        return requestingUser;
    }


    // ─────────────────────────────────────────────────────────────────────────
    // MASTER UPDATE ROUTER
    // ─────────────────────────────────────────────────────────────────────────

    private void handleMasterUpdate(UpdateUserRequest request,
                                     ParticipatingCompany masterCompany,
                                     ParticipatingUser requestingUser,
                                     ParticipatingUser targetUser,
                                     boolean isSelf, boolean isPIC, boolean isSiteChange,
                                     Site currentSite, Integer requestedSiteId,
                                     List<SiteResponse> updatedSites,
                                     UpdateUserResponse response) {
        if (isSelf) {
            // Cases 6, 8 — master editing self, cascade to all PIC sites
            handleMasterSelfUpdate(request, masterCompany, requestingUser,
                                   updatedSites, response);

        } else if (isSiteChange) {
            // Case 13, 15 — master moving a user to a different site
            handleMasterEditWithSiteChange(request, masterCompany, targetUser,requestingUser,
                                              currentSite, requestedSiteId,
                                              updatedSites, response, isPIC);
        } else {
            // Case 5, 7— master editing a regular user, no site change
            handleMasterEditUser(request, targetUser, currentSite, updatedSites, response, isPIC);
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // SELF UPDATE  (non-master, isSelf guaranteed)
    // ─────────────────────────────────────────────────────────────────────────

    private void handleSelfUpdate(UpdateUserRequest request,
                                   ParticipatingUser targetUser,
                                   boolean isPIC, Site currentSite,
                                   List<SiteResponse> updatedSites,
                                   UpdateUserResponse response) {
        if (isPIC) {
            // Case 4 — PIC editing self, must sync site PIC fields
            handlePicSelfUpdate(request, targetUser, currentSite, updatedSites, response);
        } else {
            // Case 2 — regular user editing own info only
            handleRegularSelfUpdate(request, targetUser, response);
        }
    }


    // ═════════════════════════════════════════════════════════════════════════
    // LEAF METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Case 2 — Regular user editing own basic info, no site involvement.
     */
    private void handleRegularSelfUpdate(UpdateUserRequest request,
                                          ParticipatingUser targetUser,
                                          UpdateUserResponse response) {
        updateUserInfo(targetUser, request);
        targetUser.setUpdatedAt(LocalDateTime.now());
        ParticipatingUser saved = userRepository.save(targetUser);

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("ユーザー情報が正常に更新されました。");
        response.setUser(mapToUserInfoResponse(saved));
        response.setUpdatedSites(List.of());
    }

    /**
     * Case 4 — PIC editing self.
     * User info updates AND the site's PIC fields must stay in sync.
     * FIX #4: isPIC check now uses oldEmail captured BEFORE updateUserInfo mutates the entity.
     */
    private void handlePicSelfUpdate(UpdateUserRequest request,
                                      ParticipatingUser targetUser,
                                      Site currentSite,
                                      List<SiteResponse> updatedSites,
                                      UpdateUserResponse response) {
        updateUserInfo(targetUser, request);

        // Sync site PIC — use request-override overload (email may have changed)
        updateSitePIC(currentSite, targetUser);
        Site savedSite = siteRepository.save(currentSite);
        updatedSites.add(mapToSiteResponse(savedSite));

        targetUser.setUpdatedAt(LocalDateTime.now());
        ParticipatingUser saved = userRepository.save(targetUser);

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("ユーザー情報とサイトPIC情報が正常に更新されました。");
        response.setUser(mapToUserInfoResponse(saved));
        response.setUpdatedSites(updatedSites);
    }

    /**
     * Cases 5, 7 — Master editing a user, no site change.
     * Mirrors case 4 but actor != target. If PIC change PIC info
     */
    private void handleMasterEditUser(UpdateUserRequest request,
                                      ParticipatingUser targetUser,
                                      Site currentSite,
                                      List<SiteResponse> updatedSites,
                                      UpdateUserResponse response,boolean isPIC) {
        updateUserInfo(targetUser, request);
        
        System.out.println("isPIC: " + isPIC);
        if(isPIC){
        updateSitePIC(currentSite, targetUser);
        Site savedSite = siteRepository.save(currentSite);
        updatedSites.add(mapToSiteResponse(savedSite));
        }

        targetUser.setUpdatedAt(LocalDateTime.now());
        ParticipatingUser saved = userRepository.save(targetUser);

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("ユーザー情報が正常に更新されました。");
        response.setUser(mapToUserInfoResponse(saved));
        response.setUpdatedSites(updatedSites);
    }


    /**
     * Cases 6, 8 — Master editing self.
     * Must cascade updated info to ALL sites where this master is listed as PIC.
     * FIX #4: oldEmail captured before mutation so PIC lookup still finds the right sites.
     */
    private void handleMasterSelfUpdate(UpdateUserRequest request,
                                         ParticipatingCompany masterCompany,
                                         ParticipatingUser requestingUser,
                                         List<SiteResponse> updatedSites,
                                         UpdateUserResponse response) {
        // Capture old email BEFORE mutation for the site PIC lookup
        String oldEmail = requestingUser.getEmail();

        updateUserInfo(requestingUser, request);

        // Find ALL sites where this master is PIC (by old email) and cascade the update
        List<Site> masterPICSites = siteRepository.findByCompanyIdAndPicEmail(
                masterCompany.getId(), oldEmail);

        for (Site site : masterPICSites) {
            updateSitePIC(site, requestingUser);
            Site savedSite = siteRepository.save(site);
            updatedSites.add(mapToSiteResponse(savedSite));
        }

        requestingUser.setUpdatedAt(LocalDateTime.now());
        ParticipatingUser saved = userRepository.save(requestingUser);

        //update company 法人 info
        ParticipatingCompany company = requestingUser.getCompany();
        company.setRepFirstName(request.getFirstName());
        company.setRepFirstNameKana(request.getFirstNameKana());
        company.setRepLastName(request.getLastName());
        company.setRepLastNameKana(request.getLastNameKana());
        company.setEmail(request.getEmail());
        company.setRepPosition(request.getUserPosition());
        company.setRepPhoneNumber(request.getPhoneNumber());
        company.setUpdatedBy(requestingUser.getUsername());
        company.setUpdatedAt(LocalDateTime.now());

        participatingCompanyRepository.save(company);

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("マスターユーザー情報と関連サイトPIC情報が正常に更新されました。");
        response.setUser(mapToUserInfoResponse(saved));
        response.setUpdatedSites(updatedSites);
    }

    /**
     * Case 15 and 13 — Master moving a user to a different site. 
     * if the user was PIC, site PIC must become Master User, new 
     * 
     */
    private void handleMasterEditWithSiteChange(UpdateUserRequest request,
                                                    ParticipatingCompany masterCompany,
                                                    ParticipatingUser targetUser,
                                                    ParticipatingUser masterUser,
                                                    Site currentSite,
                                                    Integer requestedSiteId,
                                                    List<SiteResponse> updatedSites,
                                                    UpdateUserResponse response, boolean isPIC) {
                                                       
        Site newSite;
        if (requestedSiteId == null) {
            newSite = null; // intentional unassign
        } else {
            newSite = resolveSite(requestedSiteId, masterCompany, response);
            if (newSite == null) return; 
            if(userRepository.countByCompany_IdAndSite_Id(masterCompany.getId(), requestedSiteId) >= MAX_USERS_PER_SITE) {
                response.setStatusCode(ResponseConstants.LIMIT_EXCEEDED);
                response.setMessage(ResponseConstants.MSG_LIMIT_EXCEEDED);
                return;
            }
        }
     
        if (currentSite != null && isPIC) {
            updateSitePIC(currentSite, masterUser);
            Site savedOldSite = siteRepository.save(currentSite);
            updatedSites.add(mapToSiteResponse(savedOldSite));
        }

        // Update user fields first so PIC sync picks up new values
        updateUserInfo(targetUser, request);

        targetUser.setSite(newSite);
        targetUser.setUpdatedAt(LocalDateTime.now());
        ParticipatingUser saved = userRepository.save(targetUser);

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("ユーザー情報、サイトPIC情報、サイト割り当てが正常に更新されました。");
        response.setUser(mapToUserInfoResponse(saved));
        response.setUpdatedSites(updatedSites);
    }


    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Resolves and validates a site by ID, confirming it belongs to the master's company.
     * Returns null and sets response if site not found or company mismatch.
     */
    private Site resolveSite(Integer siteId, ParticipatingCompany masterCompany,
                              UpdateUserResponse response) {
        if (siteId == null) return null;

        Site site = siteRepository.findById(siteId).orElse(null);
        if (site == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage("該当する拠点が見つかりませんでした。");
            return null;
        }
        if (!masterCompany.getCompanyId().equals(site.getCompany().getCompanyId())) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return null;
        }
        return site;
    }

    /**
     * Ensures the requesting user, target user, and sites all belong to the same company.
     */
    private boolean isCompanyAligned(String masterCompanyId,
                                      ParticipatingCompany masterCompany,
                                      ParticipatingUser requestingUser,
                                      ParticipatingUser targetUser) {
        if (!masterCompanyId.equals(targetUser.getCompany().getCompanyId())) return false;
        if (!masterCompanyId.equals(requestingUser.getCompany().getCompanyId())) return false;
        if (targetUser.getSite() != null &&
            !masterCompanyId.equals(targetUser.getSite().getCompany().getCompanyId())) return false;
        return true;
    }

    // ... updateUserInfo(), updateSitePIC(), isSitePIC(),
    //     mapToSiteResponse(), mapToUserInfoResponse(), getProjectsForSite()
    //     → these are all correct as-is and carry over unchanged
    @Override
    public UserProfileResponse getUserProfileById(Integer userId, String requestingRole, String requestingCompanyId) {
        UserProfileResponse response = UserProfileResponse.builder().build();
        try {
            ParticipatingUser user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_USER_NOT_FOUND);
                return response;
            }

            if ("master".equals(requestingRole)) {
                if (!requestingCompanyId.equals(user.getCompany().getCompanyId())) {
                    response.setStatusCode(ResponseConstants.FORBIDDEN);
                    response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                    return response;
                }
            }

            ParticipatingCompany company = user.getCompany();

            CompanyInfoResponse companyInfo = null;
            if (company != null) {
                companyInfo = CompanyInfoResponse.builder()
                    .companyId(company.getCompanyId())
                    .nameJA(company.getNameJa())
                    .nameFurigana(company.getNameFurigana())
                    .nameEN(company.getNameEn())
                    .email(company.getEmail())
                    .phoneNumber(company.getPhoneNumber())
                    .fax(company.getFax())
                    .corpNumber(company.getCorpNumber())
                    .address(String.format("%s %s %s %s", company.getPrefecture(), company.getMunicipality(), company.getBlocknum(), company.getBuildingName()))
                    .isSuspended(company.isSuspended())
                    .build();
            }

            UserInfoResponse profile = mapToUserInfoResponse(user);
            profile.setCompany(companyInfo);

            response.setProfile(profile);
            response.setStatusCode(ResponseConstants.OK);

        } catch (Exception e) {
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
        }
        return response;
    }

    @Override
    @Transactional
    public void changePassword(String targetUsername, String newPassword) {
        ParticipatingUser user = userRepository.findByUsername(targetUsername)
            .orElseThrow(() -> new IllegalArgumentException(ResponseConstants.MSG_USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Event-driven cache refresh for lazy JWT validation
        tokenBlacklistService.recordPasswordChange(user.getUsername());
    }

    /**
     * Update user info with non-null fields from request
     * @return true if any field was changed
     */
    private boolean updateUserInfo(ParticipatingUser user, UpdateUserRequest request) {
        boolean changed = false;

        if (StringUtils.isNotBlank(request.getFirstName()) && !request.getFirstName().equals(user.getFirstName())) {
            user.setFirstName(request.getFirstName());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getFirstNameKana()) && !request.getFirstNameKana().equals(user.getFirstNameKana())) {
            user.setFirstNameKana(request.getFirstNameKana());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getLastName()) && !request.getLastName().equals(user.getLastName())) {
            user.setLastName(request.getLastName());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getLastNameKana()) && !request.getLastNameKana().equals(user.getLastNameKana())) {
            user.setLastNameKana(request.getLastNameKana());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getPhoneNumber()) && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getUserPosition()) && !request.getUserPosition().equals(user.getUserPosition())) {
            user.setUserPosition(request.getUserPosition());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
            changed = true;
        }
            
        return changed;
    }


    /**
     * Sync site PIC fields from user entity.
     * Must be called after updateUserInfo() has already been applied to the user,
     * so that the user entity reflects the final merged state.
     */
    private void updateSitePIC(Site site, ParticipatingUser user) {
        site.setPicFirstName(user.getFirstName());
        site.setPicFirstNameKana(user.getFirstNameKana());
        site.setPicLastName(user.getLastName());
        site.setPicLastNameKana(user.getLastNameKana());
        site.setPicEmail(user.getEmail());
        site.setPicPhoneNumber(user.getPhoneNumber());
        site.setPicPosition(user.getUserPosition());
        site.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Check if user is the PIC (Person In Charge) of the site
     * potential downside: if PIC email is changed outside this flow, check may fail
     * if other users share email with the PIC, false positive may occur
     */
    private boolean isSitePIC(Site site, ParticipatingUser user) {
         if (site == null) return false;
        return site.getPicEmail() != null && site.getPicEmail().equals(user.getEmail());
        
      
    }

    /**
     * Map Site entity to SiteResponse DTO
     */
    private SiteResponse mapToSiteResponse(Site site) {
    return SiteResponse.builder()
            .siteName(site.getSiteName())
            .postalCode(site.getPostalCode())
            .prefecture(site.getPrefecture())
            .municipality(site.getMunicipality())
            .blocknum(site.getBlocknum())  
            .buildingName(site.getBuildingName())  
            .picFirstName(site.getPicFirstName())
            .picLastName(site.getPicLastName())
            .projects(getProjectsForSite(site))
            .build();
}

    /**
     * Map ParticipatingUser entity to UserInfoResponse DTO
     */
    private UserInfoResponse mapToUserInfoResponse(ParticipatingUser user) {
        String fullName = StringUtils.trim(StringUtils.joinWith(" ",
            StringUtils.defaultString(user.getLastName()),
            StringUtils.defaultString(user.getFirstName())
        ));

        return UserInfoResponse.builder()
                .id(String.valueOf(user.getId()))
                .username(user.getUsername())
                .name(fullName.isEmpty() ? null : fullName)
                .firstName(user.getFirstName())
                .firstNameKana(user.getFirstNameKana())
                .lastName(user.getLastName())
                .lastNameKana(user.getLastNameKana())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .userPosition(user.getUserPosition())
                .role(user.getRole())
                .siteId(user.getSite() != null ? String.valueOf(user.getSite().getId()) : null)
                .siteName(user.getSite() != null ? user.getSite().getSiteName() : null)
                .companyId(user.getCompany() != null ? String.valueOf(user.getCompany().getCompanyId()) : null)
                .build();
    }

    private List<String> getProjectsForSite(Site site) {
        return site.getSiteProjectRelations() == null ? List.of() :
                        site.getSiteProjectRelations().stream()
                                .map(SiteProjectRelation::getProject)
                                .map(Project::getProjectName)
                                .collect(Collectors.toList());
    }

    private BaseResponse validationError(Map<String, String> errors) {
        BaseResponse response = BaseResponse.validationError(errors);
        response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
        return response;
    }
}
