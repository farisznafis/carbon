package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CreateOperatorUserRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateOperatorUserRequest;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.CompanyInfoResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
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
public class OperatorUserServiceImpl implements UserService {

    private final OperatingUserRepository operatorUserRepository;
    private final OperatorCompanyRepository OperatorCompanyRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final UsernameAvailabilityService usernameAvailabilityService;
    private static final int MAX_USERS_PER_COMPANY = 50; // Maximum number of users allowed per company

    // @Override
    // public OperatorUserProfileResponse getUserProfile(String username) {
    //     OperatorUserProfileResponse response = OperatorUserProfileResponse.builder().build();
    //     try {
    //         OperatingUser user = operatorUserRepository.findByUsername(username).orElse(null);
    //         if (user == null) {
    //             response.setStatusCode(ResponseConstants.NOT_FOUND);
    //             response.setMessage(ResponseConstants.MSG_USER_NOT_FOUND);
    //             return response;
    //         }

    //         OperatorCompany company = user.getCompany();

    //         CompanyInfoResponse companyInfo = null;
    //         if (company != null) {
    //             companyInfo = CompanyInfoResponse.builder()
    //                 .companyId(company.getCompanyId())
    //                 .nameJA(company.getNameJa())
    //                 .nameFurigana(company.getNameFurigana())
    //                 .nameEN(company.getNameEn())
    //                 .email(company.getEmail())
    //                 .phoneNumber(company.getPhoneNumber())
    //                 .fax(company.getFax())
    //                 .corpNumber(company.getCorpNumber())
    //                 .address(String.format("%s %s %s %s", company.getPrefecture(), company.getMunicipality(), company.getBlocknum(), company.getBuildingName()))
    //                 .isSuspended(company.isSuspended())
    //                 .build();
    //         }

    //         UserInfoResponse profile = UserInfoResponse.builder()
    //             .id(user.getId().toString())
    //             .username(user.getUsername())
    //             .name(user.getLastName() + " " + user.getFirstName())
    //             .firstName(user.getFirstName())
    //             .firstNameKana(user.getFirstNameKana())
    //             .lastName(user.getLastName())
    //             .lastNameKana(user.getLastNameKana())
    //             .email(user.getEmail())
    //             .phoneNumber(user.getPhoneNumber())
    //             .userPosition(user.getUserPosition())
    //             .role(user.getRole())
    //             .siteId(user.getSite() != null ? user.getSite().getId().toString() : null)
    //             .siteName(user.getSite() != null ? user.getSite().getSiteName() : null)
    //             .companyId(user.getCompany().getCompanyId())
    //             .company(companyInfo)
    //             .build();
            
    //         response.setProfile(profile);
    //         response.setStatusCode(ResponseConstants.OK);

    //         return response;

    //     } catch (Exception e) {
    //         response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
    //         response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
    //         return response;
    //     }
    // }

    @Override
    public OperatorUserListResponse getUsersByCompany(String companyId) {
        OperatorUserListResponse response = new OperatorUserListResponse();
        // Check company exists
        var company = OperatorCompanyRepository.findByCompanyId(companyId)
                .orElse(null);

        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage("該当する会社が見つかりませんでした。");
            return response;
        }
        
        List<OperatorUser> users =
                OperatuserRepository.findByCompany_Id(company.getId());

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
    public BaseResponse validateCreateOperatorUser(String masterCompanyId, String username, CreateOperatorUserRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        OperatorCompany company = OperatorCompanyRepository.findByCompanyId(masterCompanyId)
                .orElse(null);
        if (company == null) {
            errors.put("companyId", ResponseConstants.MSG_NOT_FOUND);
            return validationError(errors);
        }

        if (userRepository.countByCompany_Id(company.getId()) >= MAX_USERS_PER_COMPANY) {
            errors.put("companyId", ResponseConstants.MSG_LIMIT_EXCEEDED);
        }

        if (request.getSiteId() != null) {
            Site site = siteRepository.findById(request.getSiteId()).orElse(null);
            if (site == null) {
                errors.put("siteId", ResponseConstants.MSG_NOT_FOUND);
            } else {
                if (!masterCompanyId.equals(site.getCompany().getCompanyId())) {
                    errors.put("siteId", ResponseConstants.MSG_FORBIDDEN);
                } else if (userRepository.countByCompany_IdAndSite_Id(company.getId(), request.getSiteId()) >= MAX_USERS_PER_SITE) {
                    errors.put("siteId", ResponseConstants.MSG_LIMIT_EXCEEDED);
                }
            }
        }

        if (userRepository.existsByEmail(request.getEmail())
                || userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            errors.put("email", ResponseConstants.MSG_USED_EMAIL);
        }
        if (userRepository.existsByUsername(request.getUsername())
                || (usernameAvailabilityService != null
                    && !usernameAvailabilityService.isAvailable(request.getUsername()))) {
            errors.put("username", ResponseConstants.MSG_USED_USERNAME);
        }

        return errors.isEmpty()
                ? BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS)
                : validationError(errors);
    }

    @Override
    @Transactional
    public CreateUserResponse createUser(String masterCompanyId, String username, CreateUserRequest request) {
        CreateUserResponse response = new CreateUserResponse();
        OperatorCompany company = OperatorCompanyRepository.findByCompanyId(masterCompanyId)
        .orElse(null);

        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }
        // Check if the user count for the company exceeds the limit
        long userCountForCompany = userRepository.countByCompany_Id(company.getId());
        if (userCountForCompany >= MAX_USERS_PER_COMPANY) {
            response.setStatusCode(ResponseConstants.LIMIT_EXCEEDED);
            response.setMessage(ResponseConstants.MSG_LIMIT_EXCEEDED);
            return response;
        }

        Site site = null;
        // 1. サイトが指定されている場合は存在確認
        if(request.getSiteId() != null) {
            site = siteRepository.findById(request.getSiteId())
                .orElse(null);
            if (site == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.MSG_NOT_FOUND);
                return response;
            }
        
            // 権限チェック: Masterは自分の会社のサイトにのみユーザーを作成可能
            if (!masterCompanyId.equals(site.getCompany().getCompanyId())) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response; 
            }

            if(userRepository.countByCompany_IdAndSite_Id(company.getId(), request.getSiteId()) >= MAX_USERS_PER_SITE) {
                response.setStatusCode(ResponseConstants.LIMIT_EXCEEDED);
                response.setMessage(ResponseConstants.MSG_LIMIT_EXCEEDED);
                return response;
            }
            
        }

        // 6. メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.getEmail())
                || userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            response.setStatusCode(ResponseConstants.USED_EMAIL);
            response.setMessage(ResponseConstants.MSG_USED_EMAIL);
            return response;
        }
        // 7. ユーザー名の重複チェック
        if (userRepository.existsByUsername(request.getUsername())) {
            response.setStatusCode(ResponseConstants.USED_USERNAME);
            response.setMessage(ResponseConstants.MSG_USED_USERNAME);
            return response;
        }
        String availableUsername = usernameAvailabilityService == null
                ? request.getUsername().trim()
                : usernameAvailabilityService.lockAndRequireAvailable(request.getUsername());
        LocalDateTime currentTime = LocalDateTime.now();
        // 8. 新しいユーザーを作成
        OperatorUser newUser = OperatorUser.builder()
                .username(availableUsername)
                .firstName(request.getFirstName())
                .firstNameKana(request.getFirstNameKana())
                .lastName(request.getLastName())
                .lastNameKana(request.getLastNameKana())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .userPosition(request.getUserPosition())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(RoleConstants.USER)
                .loginAttempts(0)
                .accountLockedUntil(null)
                .site(site)
                .company(company)  // Masterと同じ会社ID
                .build();
        newUser.setIsActive(true);
        newUser.setCreatedAt(currentTime);
        newUser.setUpdatedAt(currentTime);
        
        OperatorUser savedUser = userRepository.save(newUser);
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("ユーザーが正常に作成されました。");
        
        // 10. レスポンスにサイト情報とユーザー情報を設定
        SiteResponse siteResponse = !Objects.isNull(site) ? mapToSiteResponse(site) : null;
        UserInfoResponse userResponse = mapToUserInfoResponse(savedUser);

        response.setSite(siteResponse);
        response.setUser(userResponse);
        
        return response;
    }

    // @Override
    // @Transactional(readOnly = true)
    // public BaseResponse validateUpdateUser(String masterCompanyId,
    //                                        String username,
    //                                        UpdateUserRequest request) {
    //     Map<String, String> errors = new LinkedHashMap<>();

    //     OperatorCompany masterCompany = OperatorCompanyRepository
    //             .findByCompanyId(masterCompanyId)
    //             .orElse(null);
    //     if (masterCompany == null) {
    //         errors.put("companyId", ResponseConstants.MSG_NOT_FOUND);
    //         return validationError(errors);
    //     }

    //     OperatorUser requestingUser = userRepository
    //             .findByUsernameAndCompany_Id(username, masterCompany.getId())
    //             .orElse(null);
    //     if (requestingUser == null) {
    //         errors.put("username", ResponseConstants.MSG_USER_NOT_FOUND);
    //         return validationError(errors);
    //     }

    //     boolean isMaster = RoleConstants.MASTER.equals(requestingUser.getRole());
    //     OperatorUser targetUser = requestingUser;
    //     if (isMaster && request.getUserId() != null) {
    //         targetUser = userRepository.findById(request.getUserId()).orElse(null);
    //         if (targetUser == null) {
    //             errors.put("UserId", ResponseConstants.MSG_USER_NOT_FOUND);
    //             return validationError(errors);
    //         }
    //     }

    //     boolean isSelf = targetUser.getId().equals(requestingUser.getId());
    //     Site currentSite = targetUser.getSite();
    //     Integer currentSiteId = currentSite != null ? currentSite.getId() : null;
    //     Integer requestedSiteId = request.getSiteId();
    //     boolean isSiteChange = isMaster
    //             && !isSelf
    //             && !(requestedSiteId == null && currentSiteId == null)
    //             && !Objects.equals(requestedSiteId, currentSiteId);

    //     if (!isMaster && request.getUserId() != null && !request.getUserId().equals(requestingUser.getId())) {
    //         errors.put("UserId", ResponseConstants.MSG_FORBIDDEN);
    //     }
    //     if (!isMaster && requestedSiteId != null) {
    //         errors.put("siteId", ResponseConstants.MSG_FORBIDDEN);
    //     }
    //     if (isMaster && isSelf && requestedSiteId != null) {
    //         errors.put("siteId", ResponseConstants.MSG_BAD_REQUEST);
    //     }
    //     if (!isCompanyAligned(masterCompanyId, masterCompany, requestingUser, targetUser)) {
    //         errors.put("UserId", ResponseConstants.MSG_FORBIDDEN);
    //     }

    //     if (isSiteChange && requestedSiteId != null) {
    //         Site requestedSite = siteRepository.findById(requestedSiteId).orElse(null);
    //         if (requestedSite == null) {
    //             errors.put("siteId", ResponseConstants.MSG_SITE_NOT_FOUND);
    //         } else if (!masterCompanyId.equals(requestedSite.getCompany().getCompanyId())) {
    //             errors.put("siteId", ResponseConstants.MSG_FORBIDDEN);
    //         } else if (userRepository.countByCompany_IdAndSite_Id(masterCompany.getId(), requestedSiteId) >= MAX_USERS_PER_SITE) {
    //             errors.put("siteId", ResponseConstants.MSG_LIMIT_EXCEEDED);
    //         }
    //     }

    //     return errors.isEmpty()
    //             ? BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS)
    //             : validationError(errors);
    // }

    // @Override
    // @Transactional // FIX #6: wrap the entire operation in a transaction
    // public UpdateUserResponse updateUser(String masterCompanyId,
    //                                      String username,
    //                                      UpdateUserRequest request) {

    //     UpdateUserResponse response = new UpdateUserResponse();

    //     try {
    //         // ── Resolve requesting user ───────────────────────────────────────
    //         OperatorCompany masterCompany = OperatorCompanyRepository
    //                 .findByCompanyId(masterCompanyId)
    //                 .orElseThrow(() -> new IllegalStateException("想定外エラーが発せしました。"));

    //         OperatorUser requestingUser = userRepository
    //                 .findByUsernameAndCompany_Id(username, masterCompany.getId())
    //                 .orElseThrow(() -> new IllegalStateException("想定外エラーが発せしました。"));

    //         // ── Resolve target user ───────────────────────────────────────────
    //         OperatorUser targetUser = resolveTargetUser(request, requestingUser, response);
    //         if (targetUser == null) return response; // NOT_FOUND already set

    //         // ── Build the 4 flags cleanly up front ───────────────────────────
    //         boolean isMaster     = "master".equals(requestingUser.getRole());
    //         boolean isSelf       = targetUser.getId().equals(requestingUser.getId());
    //         boolean isPIC        = isSitePIC(targetUser.getSite(), targetUser);

    //         Site    currentSite      = targetUser.getSite();
    //         Integer currentSiteId    = (currentSite != null) ? currentSite.getId() : null;
    //         Integer requestedSiteId  = request.getSiteId();
    //         boolean isSiteChange = isMaster
    //                                 && !isSelf
    //                                 && !(requestedSiteId == null && currentSiteId == null)  // both null → no change
    //                                 && !Objects.equals(requestedSiteId, currentSiteId);     // same id → no change

    //         // ── GATE 1: Non-master editing someone else → FORBIDDEN ───────────
    //         // Covers cases: 1, 3, 9, 11
    //         if (!isMaster && !isSelf) {
    //             // FIX #7: was silently falling through to self-edit before
    //             response.setStatusCode(ResponseConstants.FORBIDDEN);
    //             response.setMessage(ResponseConstants.MSG_FORBIDDEN);
    //             return response;
    //         }

    //         // ── GATE 2: Non-master attempting a site change → FORBIDDEN ───────
    //         // Covers cases: 10, 12
    //         if (!isMaster && requestedSiteId != null) {
    //             response.setStatusCode(ResponseConstants.FORBIDDEN);
    //             response.setMessage("拠点の変更はマスターユーザーのみ可能です。");
    //             return response;
    //         }

    //         // ── GATE 3: Master changing own site → BAD REQUEST ────────────────
    //         // Covers cases: 14, 16
    //         // FIX #3: was never explicitly caught before
    //         if (isMaster && isSelf && requestedSiteId != null) {
    //             response.setStatusCode(ResponseConstants.BAD_REQUEST);
    //             response.setMessage("マスターユーザーは自身の拠点を変更できません。");
    //             return response;
    //         }

    //         // ── Security: verify all parties belong to the same company ───────
    //         if (!isCompanyAligned(masterCompanyId, masterCompany, requestingUser, targetUser)) {
    //             response.setStatusCode(ResponseConstants.FORBIDDEN);
    //             response.setMessage(ResponseConstants.MSG_FORBIDDEN);
    //             return response;
    //         }

    //         // ── Route to correct handler ──────────────────────────────────────
    //         List<SiteResponse> updatedSites = new ArrayList<>();

    //         if (isMaster) {
    //             handleMasterUpdate(request, masterCompany, requestingUser,
    //                                targetUser, isSelf, isPIC, isSiteChange,
    //                                currentSite, requestedSiteId, updatedSites, response);
    //         } else {
    //             handleSelfUpdate(request, targetUser, isPIC,
    //                              currentSite, updatedSites, response);
    //         }

    //     } catch (IllegalStateException e) {
    //         response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
    //         response.setMessage(e.getMessage());
    //     } catch (Exception e) {
    //         response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
    //         response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);
    //     }

    //     return response;
    // }



    @Override
    @Transactional
    public void changePassword(String targetUsername, String newPassword) {
        OperatorUser user = userRepository.findByUsername(targetUsername)
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
    private boolean updateUserInfo(OperatorUser user, UpdateUserRequest request) {
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
     * Check if user is the PIC (Person In Charge) of the site
     * potential downside: if PIC email is changed outside this flow, check may fail
     * if other users share email with the PIC, false positive may occur
     */
    private boolean isSitePIC(Site site, OperatorUser user) {
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
     * Map OperatorUser entity to UserInfoResponse DTO
     */
    private UserInfoResponse mapToUserInfoResponse(OperatorUser user) {
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
