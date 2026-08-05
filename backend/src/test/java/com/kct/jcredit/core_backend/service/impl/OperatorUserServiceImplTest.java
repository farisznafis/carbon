package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.constant.PermissionCode;
import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import com.kct.jcredit.core_backend.dto.response.CreateUserResponse;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.OperatingUser;
import com.kct.jcredit.core_backend.entity.Permission;
import com.kct.jcredit.core_backend.repository.postgres.OperatorCompanyRepository;
import com.kct.jcredit.core_backend.repository.postgres.OperatingUserRepository;
import com.kct.jcredit.core_backend.repository.postgres.PermissionRepository;
import com.kct.jcredit.core_backend.service.TokenBlacklistService;
import com.kct.jcredit.core_backend.service.UsernameAvailabilityService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperatorUserServiceImplTest {

    @Test
    void createOperatorUserAssignsPermissionsFromRequest() {
        OperatingUserRepository operatingUserRepository = mock(OperatingUserRepository.class);
        OperatorCompanyRepository operatorCompanyRepository = mock(OperatorCompanyRepository.class);
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        UsernameAvailabilityService usernameAvailabilityService = mock(UsernameAvailabilityService.class);

        OperatorUserServiceImpl service = new OperatorUserServiceImpl(
                operatingUserRepository,
                operatorCompanyRepository,
                permissionRepository,
                passwordEncoder,
                tokenBlacklistService,
                usernameAvailabilityService
        );

        OperatorCompany company = new OperatorCompany();
        company.setId(1);
        company.setCompanyId("OP001");
        when(operatorCompanyRepository.findByCompanyId("OP001")).thenReturn(Optional.of(company));
        when(operatingUserRepository.countByCompany_Id(1)).thenReturn(0L);
        when(operatingUserRepository.existsByEmail(any())).thenReturn(false);
        when(operatingUserRepository.existsByUsername(any())).thenReturn(false);
        when(usernameAvailabilityService.lockAndRequireAvailable("op-user")).thenReturn("op-user");
        when(passwordEncoder.encode("Password123")).thenReturn("encoded");

        Permission permission = Permission.builder()
                .code(PermissionCode.PROJECT_READ)
                .displayName("プロジェクト参照")
                .category("PROJECT")
                .active(true)
                .build();
        when(permissionRepository.findByCodeIn(List.of(PermissionCode.PROJECT_READ, PermissionCode.OPERATOR_USER_CREATE)))
                .thenReturn(List.of(permission));

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("op-user");
        request.setFirstName("太郎");
        request.setFirstNameKana("タロウ");
        request.setLastName("運用");
        request.setLastNameKana("ウンヨウ");
        request.setEmail("operator@example.com");
        request.setPhoneNumber("09012345678");
        request.setUserPosition("管理者");
        request.setPassword("Password123");
        request.setRole("operator");
        request.setPermissionCodes(List.of("PROJECT_READ", "OPERATOR_USER_CREATE"));

        when(operatingUserRepository.save(any(OperatingUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUserResponse response = service.createOperatorUser("OP001", "creator", request);

        assertEquals("OK", response.getStatusCode());
        ArgumentCaptor<OperatingUser> captor = ArgumentCaptor.forClass(OperatingUser.class);
        verify(operatingUserRepository).save(captor.capture());
        assertTrue(captor.getValue().getPermissions().stream().anyMatch(p -> p.getCode() == PermissionCode.PROJECT_READ));
    }
}
