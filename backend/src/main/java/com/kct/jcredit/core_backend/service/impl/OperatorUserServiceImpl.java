package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.PermissionCode;
import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateUserRequest;
import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UpdateUserResponse;
import com.kct.jcredit.core_backend.dto.response.UserListResponse;
import com.kct.jcredit.core_backend.dto.response.UserProfileResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.UserInfoResponse;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Permission;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.PermissionRepository;
import com.kct.jcredit.core_backend.service.OperatorUserService;
import com.kct.jcredit.core_backend.service.TokenBlacklistService;
import com.kct.jcredit.core_backend.service.UsernameAvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperatorUserServiceImpl implements OperatorUserService {

    private final OperatingUserRepository operatingUserRepository;
    private final OperatorCompanyRepository operatorCompanyRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final UsernameAvailabilityService usernameAvailabilityService;
    private static final int MAX_USERS_PER_COMPANY = 50;


    @Override
    public UserListResponse getOperatorUsersByCompany(String companyId) {
        UserListResponse response = new UserListResponse();
        OperatorCompany company = operatorCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        List<OperatingUser> users = operatingUserRepository.findByCompany_Id(company.getId());
        response.setUsers(users.stream().map(this::mapToUserInfoResponse).collect(Collectors.toList()));
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("運用者ユーザー一覧を取得しました。");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse validateCreateOperatorUser(String companyId, String username, CreateUserRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        OperatorCompany company = operatorCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            errors.put("companyId", ResponseConstants.MSG_NOT_FOUND);
            return validationError(errors);
        }

        if (operatingUserRepository.countByCompany_Id(company.getId()) >= MAX_USERS_PER_COMPANY) {
            errors.put("companyId", ResponseConstants.MSG_LIMIT_EXCEEDED);
        }

        if (StringUtils.isBlank(request.getEmail())
                || operatingUserRepository.existsByEmail(request.getEmail())) {
            errors.put("email", ResponseConstants.MSG_USED_EMAIL);
        }

        if (StringUtils.isBlank(request.getUsername())
                || operatingUserRepository.existsByUsername(request.getUsername())
                || (usernameAvailabilityService != null && !usernameAvailabilityService.isAvailable(request.getUsername()))) {
            errors.put("username", ResponseConstants.MSG_USED_USERNAME);
        }

        if (StringUtils.isNotBlank(request.getRole()) && !isSupportedRole(request.getRole())) {
            errors.put("role", "役割は admin または operator を指定してください。");
        }

        Set<String> requestedPermissionCodes = normalizePermissionCodes(request.getPermissionCodes());
        if (!requestedPermissionCodes.isEmpty()) {
            Set<PermissionCode> lookupCodes = requestedPermissionCodes.stream().map(this::toPermissionCode).collect(Collectors.toSet());
            if (lookupCodes.size() != requestedPermissionCodes.size()) {
                errors.put("permissionCodes", "権限コードに不正な値が含まれています。");
            } else {
                List<Permission> permissions = permissionRepository.findByCodeIn(lookupCodes);
                if (permissions.size() != lookupCodes.size()) {
                    errors.put("permissionCodes", "指定された権限コードの一部が見つかりませんでした。");
                }
            }
        }

        return errors.isEmpty() ? BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS) : validationError(errors);
    }

    @Override
    @Transactional
    public CreateUserResponse createOperatorUser(String companyId, String username, CreateUserRequest request) {
        CreateUserResponse response = new CreateUserResponse();
        OperatorCompany company = operatorCompanyRepository.findByCompanyId(companyId).orElse(null);
        if (company == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_NOT_FOUND);
            return response;
        }

        if (operatingUserRepository.countByCompany_Id(company.getId()) >= MAX_USERS_PER_COMPANY) {
            response.setStatusCode(ResponseConstants.LIMIT_EXCEEDED);
            response.setMessage(ResponseConstants.MSG_LIMIT_EXCEEDED);
            return response;
        }

        if (StringUtils.isBlank(request.getEmail())
                || operatingUserRepository.existsByEmail(request.getEmail())
                || operatingUserRepository.existsByEmailIgnoreCase(request.getEmail())) {
            response.setStatusCode(ResponseConstants.USED_EMAIL);
            response.setMessage(ResponseConstants.MSG_USED_EMAIL);
            return response;
        }

        if (StringUtils.isBlank(request.getUsername())
                || operatingUserRepository.existsByUsername(request.getUsername())
                || (usernameAvailabilityService != null && !usernameAvailabilityService.isAvailable(request.getUsername()))) {
            response.setStatusCode(ResponseConstants.USED_USERNAME);
            response.setMessage(ResponseConstants.MSG_USED_USERNAME);
            return response;
        }

        Set<Permission> permissions = resolvePermissions(request.getPermissionCodes());
        if (permissions == null) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage("指定された権限コードに不正な値が含まれています。");
            return response;
        }

        String availableUsername = usernameAvailabilityService == null
                ? request.getUsername().trim()
                : usernameAvailabilityService.lockAndRequireAvailable(request.getUsername());
        LocalDateTime currentTime = LocalDateTime.now();

        OperatingUser newUser = OperatingUser.builder()
                .username(availableUsername)
                .firstName(request.getFirstName())
                .firstNameKana(request.getFirstNameKana())
                .lastName(request.getLastName())
                .lastNameKana(request.getLastNameKana())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(normalizeRole(request.getRole()))
                .company(company)
                .build();
        newUser.setIsActive(true);
        newUser.setCreatedAt(currentTime);
        newUser.setUpdatedAt(currentTime);
        newUser.setCreatedBy(username);
        newUser.setUpdatedBy(username);
        newUser.setPermissions(permissions);

        OperatingUser savedUser = operatingUserRepository.save(newUser);
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("運用者ユーザーを作成しました。");
        response.setUser(mapToUserInfoResponse(savedUser));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public BaseResponse validateUpdateOperatorUser(String companyId, String username, UpdateUserRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (request.getUserId() == null) {
            errors.put("userId", ResponseConstants.MSG_BAD_REQUEST);
            return validationError(errors);
        }

        OperatingUser targetUser = operatingUserRepository.findById(request.getUserId()).orElse(null);
        if (targetUser == null || targetUser.getCompany() == null || !companyId.equals(targetUser.getCompany().getCompanyId())) {
            errors.put("userId", ResponseConstants.MSG_FORBIDDEN);
            return validationError(errors);
        }

        // if (StringUtils.isNotBlank(request.getRole()) && !isSupportedRole(request.getRole())) {
        //     errors.put("role", "役割は admin または operator を指定してください。");
        // }

        Set<String> requestedPermissionCodes = normalizePermissionCodes(request.getPermissionCodes());
        if (!requestedPermissionCodes.isEmpty()) {
            Set<PermissionCode> lookupCodes = requestedPermissionCodes.stream().map(this::toPermissionCode).collect(Collectors.toSet());
            if (lookupCodes.size() != requestedPermissionCodes.size()) {
                errors.put("permissionCodes", "権限コードに不正な値が含まれています。");
            } else {
                List<Permission> permissions = permissionRepository.findByCodeIn(lookupCodes);
                if (permissions.size() != lookupCodes.size()) {
                    errors.put("permissionCodes", "指定された権限コードの一部が見つかりませんでした。");
                }
            }
        }

        return errors.isEmpty() ? BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS) : validationError(errors);
    }

    @Override
    @Transactional
    public UpdateUserResponse updateUser(String companyId, String username, UpdateUserRequest request) {
        UpdateUserResponse response = new UpdateUserResponse();
        if (request.getUserId() == null) {
            response.setStatusCode(ResponseConstants.BAD_REQUEST);
            response.setMessage(ResponseConstants.MSG_BAD_REQUEST);
            return response;
        }

        OperatingUser targetUser = operatingUserRepository.findById(request.getUserId()).orElse(null);
        if (targetUser == null || targetUser.getCompany() == null || !companyId.equals(targetUser.getCompany().getCompanyId())) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        boolean changed = false;
        changed |= updateBaseFields(targetUser, request);

        // if (StringUtils.isNotBlank(request.getRole())) {
        //     String newRole = normalizeRole(request.getRole());
        //     if (!StringUtils.equals(newRole, targetUser.getRole())) {
        //         targetUser.setRole(newRole);
        //         changed = true;
        //     }
        // }

        if (request.getPermissionCodes() != null) {
            Set<Permission> resolvedPermissions = resolvePermissions(request.getPermissionCodes());
            if (resolvedPermissions == null) {
                response.setStatusCode(ResponseConstants.BAD_REQUEST);
                response.setMessage("指定された権限コードに不正な値が含まれています。");
                return response;
            }
            targetUser.replacePermissions(resolvedPermissions);
            changed = true;
        }

        if (changed) {
            targetUser.setUpdatedBy(username);
            targetUser.setUpdatedAt(LocalDateTime.now());
            operatingUserRepository.save(targetUser);
        }

        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(changed ? "運用者ユーザーを更新しました。" : "更新内容がありませんでした。");
        response.setUser(mapToUserInfoResponse(targetUser));
        return response;
    }

    @Override
    @Transactional
    public void changePassword(String targetUsername, String newPassword) {
        OperatingUser user = operatingUserRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new IllegalArgumentException(ResponseConstants.MSG_USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        operatingUserRepository.save(user);
        tokenBlacklistService.recordPasswordChange(user.getUsername());
    }

    @Override
    public UserProfileResponse getOperatorUserProfileById(Integer userId, Integer requestingId, String requestingRole, String requestingCompanyId) {
        UserProfileResponse response = new UserProfileResponse();
        OperatingUser user = operatingUserRepository.findById(userId).orElse(null);
        if (user == null) {
            response.setStatusCode(ResponseConstants.NOT_FOUND);
            response.setMessage(ResponseConstants.MSG_USER_NOT_FOUND);
            return response;
        }

        if (!requestingRole.equalsIgnoreCase(RoleConstants.ADMIN) && !user.getId().equals(requestingId)) {
            response.setStatusCode(ResponseConstants.FORBIDDEN);
            response.setMessage(ResponseConstants.MSG_FORBIDDEN);
            return response;
        }

        response.setProfile(mapToUserInfoResponse(user));
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage("運用者ユーザー情報を取得しました。");
        return response;
    }

    private boolean updateBaseFields(OperatingUser user, UpdateUserRequest request) {
        boolean changed = false;
        if (StringUtils.isNotBlank(request.getFirstName()) && !StringUtils.equals(request.getFirstName(), user.getFirstName())) {
            user.setFirstName(request.getFirstName());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getFirstNameKana()) && !StringUtils.equals(request.getFirstNameKana(), user.getFirstNameKana())) {
            user.setFirstNameKana(request.getFirstNameKana());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getLastName()) && !StringUtils.equals(request.getLastName(), user.getLastName())) {
            user.setLastName(request.getLastName());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getLastNameKana()) && !StringUtils.equals(request.getLastNameKana(), user.getLastNameKana())) {
            user.setLastNameKana(request.getLastNameKana());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getEmail()) && !StringUtils.equals(request.getEmail(), user.getEmail())) {
            user.setEmail(request.getEmail());
            changed = true;
        }
        if (StringUtils.isNotBlank(request.getPhoneNumber()) && !StringUtils.equals(request.getPhoneNumber(), user.getPhoneNumber())) {
            user.setPhoneNumber(request.getPhoneNumber());
            changed = true;
        }
        return changed;
    }

    private Set<Permission> resolvePermissions(List<String> rawCodes) {
        Set<String> normalizedCodes = normalizePermissionCodes(rawCodes);
        if (normalizedCodes.isEmpty()) {
            return new HashSet<>();
        }

        Set<PermissionCode> codes = normalizedCodes.stream().map(this::toPermissionCode).collect(Collectors.toSet());
        if (codes.size() != normalizedCodes.size()) {
            return null;
        }

        List<Permission> permissions = permissionRepository.findByCodeIn(codes);
        if (permissions.size() != codes.size()) {
            return null;
        }

        return new HashSet<>(permissions);
    }

    private Set<String> normalizePermissionCodes(List<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return Set.of();
        }
        return rawCodes.stream()
                .filter(StringUtils::isNotBlank)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isSupportedRole(String role) {
        if (StringUtils.isBlank(role)) {
            return false;
        }
        String normalized = normalizeRole(role);
        return RoleConstants.ADMIN.equals(normalized) || RoleConstants.OPERATOR.equals(normalized);
    }

    private String normalizeRole(String role) {
        if (StringUtils.isBlank(role)) {
            return RoleConstants.OPERATOR;
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    private PermissionCode toPermissionCode(String code) {
          try {
        return PermissionCode.valueOf(code.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
        return null;
    }
    }

    private UserInfoResponse mapToUserInfoResponse(OperatingUser user) {
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
                .role(user.getRole() != null ? user.getRole().toLowerCase(Locale.ROOT) : null)
                .companyId(user.getCompany() != null ? user.getCompany().getCompanyId() : null)
                .permissions(resolveDisplayPermissions(user)) 
                .build();
    }

    private List<String> resolveDisplayPermissions(OperatingUser user) {
        // adminは全権限を保有している扱い（DB上のpermissionsに関係なくバイパス）
        if (RoleConstants.ADMIN.equalsIgnoreCase(user.getRole())) {
            return Arrays.stream(PermissionCode.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
        }

        // operatorは実際に紐づいているpermissionのみ返す
        if (user.getPermissions() == null) {
            return Collections.emptyList();
        }

        return user.getPermissions().stream()
                .map(permission -> permission.getCode().name())
                .sorted()
                .collect(Collectors.toList());
    }

    private BaseResponse validationError(Map<String, String> errors) {
        BaseResponse response = BaseResponse.validationError(errors);
        response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
        return response;
    }
}
