package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import com.kct.jcredit.core_backend.dto.request.DeleteUserSite;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.*;
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
import com.kct.jcredit.core_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

            ParticipatingCompany company = user.getSite() != null ? user.getSite().getCompany() : null;

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
                    .address(company.getAddress())
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
                .companyId(user.getCompanyId())
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
                userRepository.findByCompanyId(companyId);

        // Map entities → DTO
        List<UserInfoResponse> userInfoList = users.stream()
            .map(this::mapToUserInfoResponse)
            .toList();

        response.setUsers(userInfoList);
        response.setStatusCode(ResponseConstants.OK);

        return response;
    }

    @Override
    @Transactional
    public DeleteUserSiteResponse removeUserSiteConnection(DeleteUserSite request) {
        DeleteUserSiteResponse response = new DeleteUserSiteResponse();

        ParticipatingUser user = userRepository.findById(Integer.parseInt(request.getUserId())).orElse(null);
        if (user == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_USER_NOT_FOUND);
            return response;
        }

        Site site = siteRepository.findById(Integer.parseInt(request.getSiteId())).orElse(null);
        if (site == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_SITE_NOT_FOUND);
            return response;
        }

        user.setSite(null);
        userRepository.save(user);

        ParticipatingUser siteMaster = userRepository
            .findByCompanyIdAndRole(site.getCompany().getCompanyId(), RoleConstants.MASTER).orElse(null);
        if (siteMaster == null) {
            response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR);
            response.setMessage(ResponseConstants.MSG_UNEXPEXTED_ERROR);
            log.error("UserServiceImpl.removeUserSiteConnection : Master User Not Found!");
            return response;
        }

        site.setPicEmail(siteMaster.getEmail());
        site.setPicFirstName(siteMaster.getFirstName());
        site.setPicFirstNameKana(siteMaster.getFirstNameKana());
        site.setPicLastName(siteMaster.getLastName());
        site.setPicLastNameKana(siteMaster.getLastNameKana());
        site.setPicPhoneNumber(siteMaster.getPhoneNumber());
        site.setPicPosition(siteMaster.getUserPosition());
        siteRepository.save(site);

        response.setStatusCode(ResponseConstants.OK);
        return response;
    }

    @Override
    public CreateUserResponse createUser(String masterCompanyId, String username, CreateUserRequest request) {
        CreateUserResponse response = new CreateUserResponse();
        
        // サイトを取得 & 存在チェック
        Site site = null;
        if (request.getSiteId() != null) {
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
        // 現在のユーザー（マスター）情報を取得
        ParticipatingUser masterUser = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("想定外のエラーが発生しました。"));

        // 4. サイトのPICがマスターユーザー自身であることを確認
        if (!isSitePIC(site, masterUser)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }
        }
        // 6. メールアドレスの重複チェック
        if (userRepository.existsByEmail(request.getEmail())) {
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
        LocalDateTime currentTime = LocalDateTime.now();
        // 8. 新しいユーザーを作成
        ParticipatingUser newUser = ParticipatingUser.builder()
                .username(request.getUsername())
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
                .companyId(masterCompanyId)  // Masterと同じ会社ID
                .build();
        newUser.setIsActive(true);
        newUser.setCreatedAt(currentTime);
        newUser.setUpdatedAt(currentTime);
        
        ParticipatingUser savedUser = userRepository.save(newUser);

        // 9. サイトのPIC情報を新規ユーザーに更新
        Site savedSite = null;
        if (site != null) {
            updateSitePIC(site, savedUser);
            savedSite = siteRepository.save(site);
        }

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("ユーザーが正常に作成されました。");
        
        // 10. レスポンスにサイト情報とユーザー情報を設定
        SiteResponse siteResponse = savedSite != null ? mapToSiteResponse(savedSite) : null;
        UserInfoResponse userResponse = mapToUserInfoResponse(savedUser);

        response.setSite(siteResponse);
        response.setUser(userResponse);
        
        return response;
    }

    @Override
    public UpdateUserResponse updateUser(String masterCompanyId,
            String username, UpdateUserRequest request) {
        
                //想定外エラが出る
                //too long spread it out a bit
                //handle master case differently 

        UpdateUserResponse response = new UpdateUserResponse();
        List<SiteResponse> updatedSites = new ArrayList<>();

        try {
            // 1. リクエスト元のユーザーを取得
            ParticipatingUser requestingUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalStateException("想定外エラーが発せしました。"));
            ParticipatingCompany masterCompany = participatingCompanyRepository.findByCompanyId(masterCompanyId)
            .orElseThrow(() -> new IllegalStateException("想定外エラーが発せしました。"));
        

            // 2. 更新対象のユーザーを決定
            ParticipatingUser targetUser;
            boolean isMasterRequest = "master".equals(requestingUser.getRole());
            
            if (isMasterRequest && request.getUserId() != null) {
                // マスターが他のユーザーを更新する場合
                targetUser = userRepository.findById(request.getUserId()).orElse(null);
                if (targetUser == null) {
                    response.setStatusCode(ResponseConstants.NOT_FOUND);
                    response.setMessage(ResponseConstants.NOT_FOUND);
                    return response;
                }
            } else {
                // ユーザーが自分自身を更新する場合
                targetUser = requestingUser;
            }

            // サイト変更の有無を確認
            Site currentSite = targetUser.getSite();
            if (currentSite == null && !isMasterRequest) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage(ResponseConstants.NOT_FOUND);
                return response;
            }
            
            Integer currentSiteId = (currentSite != null) ? currentSite.getId() : null;
            Integer requestedSiteId = request.getSiteId();
            boolean siteChangeRequested = (requestedSiteId != null && !requestedSiteId.equals(currentSiteId));
            
            // 4. 一般ユーザーがサイト変更を要求している場合はエラー
            if (siteChangeRequested && !isMasterRequest) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.FORBIDDEN);
                return response;
            }
            
            // 5. サイト変更がない場合、基本情報のみ更新
            if (!siteChangeRequested) {
                //マスターがPICとなっているすべてのサイトを検索する必要がある
                String oldEmail = targetUser.getEmail();

                boolean userInfoChanged = updateUserInfo(targetUser, request);
                
                // ユーザーがサイトのPICの場合、サイト情報も更新
                if (userInfoChanged && currentSite != null && isSitePIC(currentSite, targetUser)) {
                    updateSitePIC(currentSite, targetUser, request);
                    Site savedSite = siteRepository.save(currentSite);
                    updatedSites.add(mapToSiteResponse(savedSite));
                }

                
                // CONDITION: マスターが自分自身を更新している場合のみ実行
                if (userInfoChanged && "master".equals(targetUser.getRole()) && targetUser.equals(requestingUser)) {
                    // 変更前のメールアドレスでマスターがPICとなっている全サイトを検索
     
                    List<Site> masterPICSites = siteRepository.findByCompanyIdAndPicEmail(
                        masterCompany.getId(), 
                        oldEmail
                    );
                    
                    // 各サイトのPIC情報を更新
                    for (Site site : masterPICSites) {
                        // 現在のサイトは既に更新済みなのでスキップ
                        if (site.getId().equals(currentSiteId)) {
                            continue;
                        }
                        // サイトのPIC情報を新しいマスター情報で更新
                        updateSitePIC(site, targetUser, request);
                        Site savedSite = siteRepository.save(site);
                        updatedSites.add(mapToSiteResponse(savedSite));
                    }
                }


                targetUser.setUpdatedAt(LocalDateTime.now());
                ParticipatingUser savedUser = userRepository.save(targetUser);
                
                response.setStatusCode(ResponseConstants.OK);
                response.setMessage("ユーザー情報が正常に更新されました。");
                response.setUser(mapToUserInfoResponse(savedUser));
                response.setUpdatedSites(updatedSites);
                
                return response;
            }
            
            // ここからはサイト変更処理（マスターユーザーのみ）
            
            // 6. 新しいサイトを取得
            Site newSite = siteRepository.findById(requestedSiteId).orElse(null);
            if (newSite == null) {
                response.setStatusCode(ResponseConstants.NOT_FOUND);
                response.setMessage("該当する拠点が見つかりませんでした。");
                return response;
            }
            
            // 7. 会社IDの一致確認（セキュリティチェック）
            if (!masterCompanyId.equals(currentSite.getCompany().getCompanyId()) ||
                !masterCompanyId.equals(newSite.getCompany().getCompanyId()) ||
                !masterCompanyId.equals(targetUser.getCompanyId())) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                return response;
            }
            
            // 8. 新サイトに既存ユーザーがいないことを確認（マスターがPICである必要がある）
            if (!isSitePIC(newSite, requestingUser)) {
                response.setStatusCode(ResponseConstants.FORBIDDEN);
                response.setMessage("この拠点は既に担当者がいるのでユーザを割り当てることができません。");
                return response;
            }
            
            // 9. 現在のサイトのPIC処理
            if (isSitePIC(currentSite, targetUser)) {
                // 現在のユーザーがPICの場合、マスターをPICに設定
                updateSitePIC(currentSite, requestingUser); // マスターをPICに
                Site savedCurrentSite = siteRepository.save(currentSite);
                updatedSites.add(mapToSiteResponse(savedCurrentSite));
            }
            
            // 10. ユーザー情報の更新
            updateUserInfo(targetUser, request);
            
            // 11. 新サイトのPIC情報を更新
            updateSitePIC(newSite, targetUser, request);
            Site savedNewSite = siteRepository.save(newSite);
            updatedSites.add(mapToSiteResponse(savedNewSite));
            
            // 12. ユーザーのサイト情報を更新
            targetUser.setSite(newSite);
            targetUser.setUpdatedAt(LocalDateTime.now());
            ParticipatingUser savedUser = userRepository.save(targetUser);
            
            // 13. レスポンス作成
            response.setStatusCode(ResponseConstants.OK);
            response.setMessage("ユーザー情報とサイト割り当てが正常に更新されました。");
            response.setUser(mapToUserInfoResponse(savedUser));
            response.setUpdatedSites(updatedSites);
            
        } catch (NumberFormatException e) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage("ユーザーIDの形式が正しくありません。");    
        }
        return response;
    }

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
                if (!requestingCompanyId.equals(user.getCompanyId())) {
                    response.setStatusCode(ResponseConstants.FORBIDDEN);
                    response.setMessage(ResponseConstants.MSG_FORBIDDEN);
                    return response;
                }
            }

            ParticipatingCompany company = user.getSite() != null ? user.getSite().getCompany() : null;

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
                    .address(company.getAddress())
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
     * Update site PIC from existing user (all fields)
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
     * Update site PIC from user with request overrides
     * Uses request values if provided, otherwise uses user's current values
     */
    private void updateSitePIC(Site site, ParticipatingUser user, UpdateUserRequest request) {
        site.setPicFirstName(request.getFirstName() != null ? request.getFirstName() : user.getFirstName());
        site.setPicFirstNameKana(request.getFirstNameKana() != null ? request.getFirstNameKana() : user.getFirstNameKana());
        site.setPicLastName(request.getLastName() != null ? request.getLastName() : user.getLastName());
        site.setPicLastNameKana(request.getLastNameKana() != null ? request.getLastNameKana() : user.getLastNameKana());
        site.setPicEmail(request.getEmail() != null ? request.getEmail() : user.getEmail());
        site.setPicPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : user.getPhoneNumber());
        site.setPicPosition(request.getUserPosition() != null ? request.getUserPosition() : user.getUserPosition());
        site.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Check if user is the PIC (Person In Charge) of the site
     * potential downside: if PIC email is changed outside this flow, check may fail
     * if other users share email with the PIC, false positive may occur
     */
    private boolean isSitePIC(Site site, ParticipatingUser user) {
        // Check by email 
        boolean isPIC = site.getPicEmail() != null && site.getPicEmail().equals(user.getEmail());
        
        return isPIC;
    }

    /**
     * Map Site entity to SiteResponse DTO
     */
    private SiteResponse mapToSiteResponse(Site site) {
    return SiteResponse.builder()
            .id(String.valueOf(site.getId()))
            .siteName(site.getSiteName())
            .postalCode(site.getPostalCode())
            .prefecture(site.getPrefecture())
            .city(site.getCity())
            .town(site.getTown())
            .block(site.getBlocknum())  
            .address(site.getAddress())  
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
                .companyId(user.getCompanyId())
                .build();
    }

    private List<String> getProjectsForSite(Site site) {
        return site.getSiteProjectRelations() == null ? List.of() :
                        site.getSiteProjectRelations().stream()
                                .map(SiteProjectRelation::getProject)
                                .map(Project::getProjectName)
                                .collect(Collectors.toList());
    }
}
