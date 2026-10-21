package com.kct.jcredit.core_backend.controller.AdminCompanyControllerTest;
import com.kct.jcredit.core_backend.constant.*;
import com.kct.jcredit.core_backend.dto.request.*;
import com.kct.jcredit.core_backend.dto.response.*;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.entity.*;
import com.kct.jcredit.core_backend.repository.postgres.*;
import com.kct.jcredit.core_backend.service.*;
import com.kct.jcredit.core_backend.service.impl.OperatorUserServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorUserServiceTest {

    @Mock private OperatingUserRepository operatingUserRepository;
    @Mock private OperatorCompanyRepository operatorCompanyRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private UsernameAvailabilityService usernameAvailabilityService;

    @InjectMocks
    private OperatorUserServiceImpl operatorUserService;

    // =========================================================
    // GET /admin/companies/operator-user
    // =========================================================
    @Nested
    @DisplayName("GET /admin/companies/operator-user のテスト")
    class GetOperatorUsersByCompanyTest {

        @Test
        @DisplayName("UT-OUS-001: companyIdが存在しない場合、NOT_FOUNDを返し一覧取得処理は実行されない")
        void getOperatorUsersByCompany_nonExistingCompanyId_returnsNotFound() {
            when(operatorCompanyRepository.findByCompanyId("COMP-404")).thenReturn(Optional.empty());
            UserListResponse response = operatorUserService.getOperatorUsersByCompany("COMP-404");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            verifyNoInteractions(operatingUserRepository);
            // Input: companyId="COMP-404"
            //        operatorCompanyRepository.findByCompanyId(...) が Optional.empty を返すようモック
            // Output: statusCode=NOT_FOUND, message=MSG_NOT_FOUND
            //         operatingUserRepository.findByCompany_Id が一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-002: 企業に紐づくユーザーが存在しない場合、OKかつ空リストが返る")
        void getOperatorUsersByCompany_noUsers_returnsOkWithEmptyList() {
            OperatorCompany company = company(1, "COMP-001");
            when(operatorCompanyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(operatingUserRepository.findByCompany_Id(1)).thenReturn(List.of());
            UserListResponse response = operatorUserService.getOperatorUsersByCompany("COMP-001");
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            assertThat(response.getUsers()).isEmpty();
            // Input: companyId="COMP-001"
            //        operatorCompanyRepository.findByCompanyId(...) が company(id=1)を返すようモック
            //        operatingUserRepository.findByCompany_Id(1) が 空リストを返すようモック
            // Output: statusCode=OK, message="運用者ユーザー一覧を取得しました。"
            //         users= 空リスト
        }

        @Test
        @DisplayName("UT-OUS-003: adminロールのユーザーが含まれる場合、permissionsに全PermissionCodeが返る")
        void getOperatorUsersByCompany_adminUser_returnsAllPermissionCodes() {
            OperatorCompany company = company(1, "COMP-001");
            OperatingUser user = user(10, company, RoleConstants.ADMIN);
            when(operatorCompanyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(operatingUserRepository.findByCompany_Id(1)).thenReturn(List.of(user));
            UserListResponse response = operatorUserService.getOperatorUsersByCompany("COMP-001");
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getUsers().get(0).getRole()).isEqualTo(RoleConstants.ADMIN);
            verify(operatingUserRepository).findByCompany_Id(1);
            // Input: companyId="COMP-001"
            //        company存在、users=[role="admin"のOperatingUser(permissions=空Set)]
            // Output: statusCode=OK
            //         users[0].permissions が PermissionCode.values() 全件（name()の集合）と一致すること
            //         users[0].role="admin"
        }

        @Test
        @DisplayName("UT-OUS-004: operatorロールのユーザーが含まれる場合、紐づくpermissionのみ昇順で返る")
        void getOperatorUsersByCompany_operatorUser_returnsOwnPermissionsSorted() {
            OperatorCompany company = company(1, "COMP-001");
            OperatingUser user = user(10, company, RoleConstants.OPERATOR);
            when(operatorCompanyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(operatingUserRepository.findByCompany_Id(1)).thenReturn(List.of(user));
            UserListResponse response = operatorUserService.getOperatorUsersByCompany("COMP-001");
            assertThat(response.getUsers()).hasSize(1);
            assertThat(response.getUsers().get(0).getRole()).isEqualTo(RoleConstants.OPERATOR);
            // Input: companyId="COMP-001"
            //        company存在、users=[role="operator"のOperatingUser(permissions={CODE_B, CODE_A})]
            // Output: statusCode=OK
            //         users[0].permissions=["CODE_A","CODE_B"]（アルファベット昇順）
        }

        @Test
        @DisplayName("UT-OUS-005: 複数ユーザーが存在する場合、全件がレスポンスに含まれる")
        void getOperatorUsersByCompany_multipleUsers_returnsAllMapped() {
            OperatorCompany company = company(1, "COMP-001");
            when(operatorCompanyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(operatingUserRepository.findByCompany_Id(1)).thenReturn(List.of(user(1, company, RoleConstants.ADMIN), user(2, company, RoleConstants.OPERATOR)));
            assertThat(operatorUserService.getOperatorUsersByCompany("COMP-001").getUsers()).hasSize(2);
            // Input: companyId="COMP-001"
            //        company存在、users=[user1(admin), user2(operator)]
            // Output: statusCode=OK, users.size=2
            //         各要素のid/username/name/email/role等が入力userと一致すること
        }
    }

    // =========================================================
    // POST /admin/companies/operator-user
    //   - validateCreateOperatorUser
    // =========================================================
    @Nested
    @DisplayName("POST /admin/companies/operator-user のバリデーションテスト（validateCreateOperatorUser）")
    class ValidateCreateOperatorUserTest {

        @Test
        @DisplayName("UT-OUS-006: 全項目が有効な場合、バリデーション成功が返る")
        void validateCreateOperatorUser_allValid_returnsSuccess() {
            OperatorCompany company = company(1, "COMP-001");
            when(operatorCompanyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(operatingUserRepository.countByCompany_Id(1)).thenReturn(0L);
            lenient().when(usernameAvailabilityService.isAvailable(anyString())).thenReturn(true);
            BaseResponse response = operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("new@example.com", "newuser", List.of()));
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: companyId="COMP-001", username="admin1",
            //        request(email="new@example.com", username="newuser", permissionCodes=["CODE_A"])
            //        company存在、countByCompany_Id=10、existsByEmail=false、existsByUsername=false、
            //        usernameAvailabilityService.isAvailable=true、
            //        permissionRepository.findByCodeIn(["CODE_A"]) が1件のPermissionを返す
            // Output: BaseResponse.success, message=MSG_VALIDATION_SUCCESS
        }

        @Test
        @DisplayName("UT-OUS-007: companyIdが存在しない場合、errorsにcompanyIdのみ含まれ他のチェックは実行されない")
        void validateCreateOperatorUser_nonExistingCompanyId_returnsCompanyIdErrorOnly() {
            when(operatorCompanyRepository.findByCompanyId("COMP-404")).thenReturn(Optional.empty());
            BaseResponse response = operatorUserService.validateCreateOperatorUser("COMP-404", "admin1", createRequest("x@example.com", "user", null));
            assertThat(response.getErrors()).containsKey("companyId");
            verifyNoInteractions(operatingUserRepository);
            // Input: companyId="COMP-404", request=任意
            //        operatorCompanyRepository.findByCompanyId(...) が Optional.empty
            // Output: errors={"companyId": MSG_NOT_FOUND}, errors.size=1
            //         operatingUserRepository.existsByEmail等が一度も呼ばれないこと（早期return）
        }

        @Test
        @DisplayName("UT-OUS-008: 企業のユーザー数が上限(50件)以上の場合、errorsにcompanyIdが含まれる")
        void validateCreateOperatorUser_limitExceeded_returnsCompanyIdError() {
            OperatorCompany company = company(1, "COMP-001");
            when(operatorCompanyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company));
            when(operatingUserRepository.countByCompany_Id(1)).thenReturn(50L);
            BaseResponse response = operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "user", null));
            assertThat(response.getErrors()).containsKey("companyId");
            // Input: companyId="COMP-001", request=有効な値
            //        company存在、countByCompany_Id=50
            // Output: errors["companyId"]=MSG_LIMIT_EXCEEDED
            //         email/username/permissionCodesのチェックも同時に実行されうる（早期returnなし）
        }

        @Test
        @DisplayName("UT-OUS-009: emailが空文字の場合、errorsにemailが含まれる")
        void validateCreateOperatorUser_blankEmail_returnsEmailError() {
            prepareCompanyValidation();
            BaseResponse response = operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("", "user", null));
            assertThat(response.getErrors()).containsKey("email");
            // Input: request.email=""
            //        company存在、countByCompany_Id=10
            // Output: errors["email"]=MSG_USED_EMAIL
        }

        @Test
        @DisplayName("UT-OUS-010: emailが既に使用されている場合、errorsにemailが含まれる")
        void validateCreateOperatorUser_duplicateEmail_returnsEmailError() {
            prepareCompanyValidation();
            when(operatingUserRepository.existsByEmail("dup@example.com")).thenReturn(true);
            assertThat(operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("dup@example.com", "user", null)).getErrors()).containsKey("email");
            // Input: request.email="dup@example.com"
            //        operatingUserRepository.existsByEmail("dup@example.com")=true
            // Output: errors["email"]=MSG_USED_EMAIL
        }

        @Test
        @DisplayName("UT-OUS-011: usernameが空文字の場合、errorsにusernameが含まれる")
        void validateCreateOperatorUser_blankUsername_returnsUsernameError() {
            prepareCompanyValidation();
            assertThat(operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "", null)).getErrors()).containsKey("username");
            // Input: request.username=""
            // Output: errors["username"]=MSG_USED_USERNAME
        }

        @Test
        @DisplayName("UT-OUS-012: usernameが既に使用されている場合、errorsにusernameが含まれる")
        void validateCreateOperatorUser_duplicateUsername_returnsUsernameError() {
            prepareCompanyValidation();
            when(operatingUserRepository.existsByUsername("dupuser")).thenReturn(true);
            assertThat(operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "dupuser", null)).getErrors()).containsKey("username");
            // Input: request.username="dupuser"
            //        operatingUserRepository.existsByUsername("dupuser")=true
            // Output: errors["username"]=MSG_USED_USERNAME
        }

        @Test
        @DisplayName("UT-OUS-013: usernameAvailabilityServiceがfalseを返す場合、errorsにusernameが含まれる")
        void validateCreateOperatorUser_usernameNotAvailable_returnsUsernameError() {
            prepareCompanyValidation();
            when(usernameAvailabilityService.isAvailable("reservedname")).thenReturn(false);
            assertThat(operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "reservedname", null)).getErrors()).containsKey("username");
            // Input: request.username="reservedname"
            //        existsByUsername=false, usernameAvailabilityService.isAvailable("reservedname")=false
            // Output: errors["username"]=MSG_USED_USERNAME
        }

        @Test
        @DisplayName("UT-OUS-014: permissionCodesに不正な値（PermissionCode未定義）が含まれる場合、errorsにpermissionCodesが含まれる")
        void validateCreateOperatorUser_invalidPermissionCode_returnsPermissionCodesError() {
            prepareCompanyValidation();
            assertThat(operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "user", List.of("INVALID_CODE"))).getErrors()).containsKey("permissionCodes");
            // Input: request.permissionCodes=["INVALID_CODE"]
            // Output: errors["permissionCodes"]="権限コードに不正な値が含まれています。"
            //         permissionRepository.findByCodeInが呼ばれないこと（lookupCodes.size!=requestedの時点で分岐）
        }

        @Test
        @DisplayName("UT-OUS-015: permissionCodesは有効形式だがDBに存在しないコードを含む場合、errorsにpermissionCodesが含まれる")
        void validateCreateOperatorUser_permissionCodeNotFoundInDb_returnsPermissionCodesError() {
            prepareCompanyValidation();
            when(permissionRepository.findByCodeIn(anySet())).thenReturn(List.of());
            assertThat(operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "user", List.of("PROJECT_CREATE", "PROJECT_READ"))).getErrors()).containsKey("permissionCodes");
            // Input: request.permissionCodes=["PROJECT_CREATE","PROJECT_READ"]
            //        permissionRepository.findByCodeIn([PROJECT_CREATE,PROJECT_READ]) が空リストを返す
            // Output: errors["permissionCodes"]="指定された権限コードの一部が見つかりませんでした。"
        }

        @Test
        @DisplayName("UT-OUS-016: permissionCodesがnullまたは空リストの場合、権限チェックはスキップされエラーなし")
        void validateCreateOperatorUser_nullOrEmptyPermissionCodes_skipsCheck() {
            prepareCompanyValidation();
            assertThat(operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "user", null)).getStatusCode()).isEqualTo(ResponseConstants.OK);
            verifyNoInteractions(permissionRepository);
            // Input: request.permissionCodes=null（およびケース: 空リスト[]）
            //        他の項目は全て有効
            // Output: BaseResponse.success
            //         permissionRepository.findByCodeInが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-017: email・username・permissionCodesが同時に不正な場合、全てのエラーがerrorsに含まれる")
        void validateCreateOperatorUser_multipleErrors_returnsAllErrorsTogether() {
            prepareCompanyValidation();
            when(operatingUserRepository.existsByEmail(anyString())).thenReturn(true);
            when(operatingUserRepository.existsByUsername(anyString())).thenReturn(true);
            BaseResponse response = operatorUserService.validateCreateOperatorUser("COMP-001", "admin1", createRequest("dup@example.com", "dupuser", List.of("INVALID_CODE")));
            assertThat(response.getErrors()).containsKeys("email", "username", "permissionCodes");
            // Input: request.email="dup@example.com"(重複), request.username="dupuser"(重複),
            //        request.permissionCodes=["INVALID_CODE"]
            //        company存在、countByCompany_Id=10
            // Output: errors.size=3, errors.keySet()=["email","username","permissionCodes"]
        }
    }

    // =========================================================
    // POST /admin/companies/operator-user
    //   - createOperatorUser
    // =========================================================
    @Nested
    @DisplayName("POST /admin/companies/operator-user のテスト（createOperatorUser）")
    class CreateOperatorUserTest {

        @Test
        @DisplayName("UT-OUS-018: 有効なリクエストの場合、ユーザーが作成されOKが返る")
        void createOperatorUser_validRequest_createsUserAndReturnsOk() {
            createUserSetup();
            CreateUserResponse response = operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("new@example.com", "newuser", null));
            assertThat(response).isNotNull();
            assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(operatingUserRepository).save(any(OperatingUser.class));
            // Input: companyId="COMP-001", username="admin1"（作成者）,
            //        request(email="new@example.com", username="newuser", password="rawPass",
            //                firstName="太郎", lastName="山田", permissionCodes=["CODE_A"])
            //        company存在(id=1), countByCompany_Id=10,
            //        existsByEmail=false, existsByEmailIgnoreCase=false,
            //        existsByUsername=false, usernameAvailabilityService.isAvailable=true,
            //        usernameAvailabilityService.lockAndRequireAvailable("newuser")="newuser",
            //        permissionRepository.findByCodeIn(["CODE_A"])=1件のPermission,
            //        passwordEncoder.encode("rawPass")="encodedPass",
            //        operatingUserRepository.save(any()) は引数をそのまま返す
            // Output: statusCode=OK, message="運用者ユーザーを作成しました。"
            //         user（レスポンス）のusername="newuser", role="operator"
            //         save時に渡されたOperatingUserのフィールド検証：
            //           role=RoleConstants.OPERATOR, isActive=true,
            //           password="encodedPass", createdBy=updatedBy="admin1",
            //           company=対象company, permissions=1件
        }

        @Test
        @DisplayName("UT-OUS-019: companyIdが存在しない場合、NOT_FOUNDを返し以降の処理は実行されない")
        void createOperatorUser_nonExistingCompanyId_returnsNotFound() {
            when(operatorCompanyRepository.findByCompanyId("COMP-404")).thenReturn(Optional.empty());
            assertThat(operatorUserService.createOperatorUser("COMP-404", "admin1", createRequest("x@example.com", "user", null)).getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            // Input: companyId="COMP-404"
            //        operatorCompanyRepository.findByCompanyId(...)がOptional.empty
            // Output: statusCode=NOT_FOUND, message=MSG_NOT_FOUND
            //         operatingUserRepository.save / passwordEncoder.encode が一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-020: 企業のユーザー数が上限(50件)以上の場合、LIMIT_EXCEEDEDを返し以降の処理は実行されない")
        void createOperatorUser_limitExceeded_returnsLimitExceeded() {
            createUserSetup();
            when(operatingUserRepository.countByCompany_Id(1)).thenReturn(50L);
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "user", null)).getStatusCode()).isEqualTo(ResponseConstants.LIMIT_EXCEEDED);
            // Input: companyId="COMP-001"
            //        company存在, countByCompany_Id=50
            // Output: statusCode=LIMIT_EXCEEDED, message=MSG_LIMIT_EXCEEDED
            //         existsByEmail等が呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-021: emailが空の場合、USED_EMAILを返し以降の処理は実行されない")
        void createOperatorUser_blankEmail_returnsUsedEmail() {
            createUserSetup();
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("", "user", null)).getStatusCode()).isEqualTo(ResponseConstants.USED_EMAIL);
            // Input: request.email=""
            //        company存在, countByCompany_Id=10
            // Output: statusCode=USED_EMAIL, message=MSG_USED_EMAIL
            //         existsByUsername等が呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-022: emailが既存（大文字小文字区別なし含む）と重複する場合、USED_EMAILを返す")
        void createOperatorUser_duplicateEmailIgnoreCase_returnsUsedEmail() {
            createUserSetup();
            when(operatingUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("DUP@example.com", "user", null)).getStatusCode()).isEqualTo(ResponseConstants.USED_EMAIL);
            // Input: request.email="DUP@example.com"
            //        existsByEmail("DUP@example.com")=false, existsByEmailIgnoreCase("DUP@example.com")=true
            // Output: statusCode=USED_EMAIL, message=MSG_USED_EMAIL
        }

        @Test
        @DisplayName("UT-OUS-023: usernameが空の場合、USED_USERNAMEを返し以降の処理は実行されない")
        void createOperatorUser_blankUsername_returnsUsedUsername() {
            createUserSetup();
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "", null)).getStatusCode()).isEqualTo(ResponseConstants.USED_USERNAME);
            // Input: request.username=""
            //        email側は有効
            // Output: statusCode=USED_USERNAME, message=MSG_USED_USERNAME
            //         permissionRepository.findByCodeIn等が呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-024: usernameが既に使用されている場合、USED_USERNAMEを返す")
        void createOperatorUser_duplicateUsername_returnsUsedUsername() {
            createUserSetup();
            when(operatingUserRepository.existsByUsername(anyString())).thenReturn(true);
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "dupuser", null)).getStatusCode()).isEqualTo(ResponseConstants.USED_USERNAME);
            // Input: request.username="dupuser"
            //        existsByUsername("dupuser")=true
            // Output: statusCode=USED_USERNAME, message=MSG_USED_USERNAME
        }

        @Test
        @DisplayName("UT-OUS-025: usernameAvailabilityServiceがfalseを返す場合、USED_USERNAMEを返す")
        void createOperatorUser_usernameNotAvailable_returnsUsedUsername() {
            createUserSetup();
            when(usernameAvailabilityService.isAvailable(anyString())).thenReturn(false);
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "reservedname", null)).getStatusCode()).isEqualTo(ResponseConstants.USED_USERNAME);
            // Input: request.username="reservedname"
            //        existsByUsername=false, usernameAvailabilityService.isAvailable("reservedname")=false
            // Output: statusCode=USED_USERNAME, message=MSG_USED_USERNAME
        }

        @Test
        @DisplayName("UT-OUS-026: permissionCodesに不正な値が含まれる場合、BAD_REQUESTを返しsaveは実行されない")
        void createOperatorUser_invalidPermissionCode_returnsBadRequest() {
            createUserSetup();
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "user", List.of("INVALID_CODE"))).getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            // Input: request.permissionCodes=["INVALID_CODE"]
            //        email/username側は有効
            // Output: statusCode=BAD_REQUEST, message="指定された権限コードに不正な値が含まれています。"
            //         operatingUserRepository.saveが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-027: permissionCodesにDB未存在のコードが含まれる場合、BAD_REQUESTを返しsaveは実行されない")
        void createOperatorUser_permissionCodeNotFoundInDb_returnsBadRequest() {
            createUserSetup();
            when(permissionRepository.findByCodeIn(anySet())).thenReturn(List.of());
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "user", List.of("CODE_A"))).getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            // Input: request.permissionCodes=["CODE_A","CODE_B"]
            //        permissionRepository.findByCodeIn(...)が1件のみ返す
            // Output: statusCode=BAD_REQUEST, message="指定された権限コードに不正な値が含まれています。"
            //         saveが呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-028: permissionCodesがnullの場合、権限なしでユーザーが作成される")
        void createOperatorUser_nullPermissionCodes_createsUserWithEmptyPermissions() {
            createUserSetup();
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(operatingUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "user", null)).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: request.permissionCodes=null
            //        email/username側は有効
            // Output: statusCode=OK
            //         save時に渡されたOperatingUserのpermissionsが空Setであること
        }

        @Test
        @DisplayName("UT-OUS-029: usernameAvailabilityService.lockAndRequireAvailableの戻り値が保存時のusernameに使用される")
        void createOperatorUser_usesLockAndRequireAvailableReturnValueAsUsername() {
            createUserSetup();
            when(usernameAvailabilityService.lockAndRequireAvailable(anyString())).thenReturn("newuser");
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(operatingUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "newuser", null))).isNotNull();
            // Input: request.username=" newuser "（前後空白あり想定）
            //        usernameAvailabilityService.lockAndRequireAvailable(" newuser ") が "newuser" を返す
            // Output: save時に渡されたOperatingUser.username="newuser"（lockAndRequireAvailableの戻り値そのまま）
        }

        @Test
        @DisplayName("UT-OUS-030: 作成成功時、createdBy/updatedBy/createdAt/updatedAtが呼び出し元usernameと現在時刻で設定される")
        void createOperatorUser_success_setsAuditFields() {
            createUserSetup();
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(operatingUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            assertThat(operatorUserService.createOperatorUser("COMP-001", "admin1", createRequest("x@example.com", "newuser", null)).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: username="admin1"（作成者）
            //        他は有効な値
            // Output: save時に渡されたOperatingUserの
            //           createdBy="admin1", updatedBy="admin1",
            //           createdAt=updatedAt（両方非null、同時刻付近）
        }
    }

    // =========================================================
    // PATCH /admin/companies/operator-user/{userId}
    //   - validateUpdateOperatorUser
    // =========================================================
    @Nested
    @DisplayName("PATCH /admin/companies/operator-user/{userId} のバリデーションテスト（validateUpdateOperatorUser）")
    class ValidateUpdateOperatorUserTest {

        @Test
        @DisplayName("UT-OUS-031: 全項目が有効な場合、バリデーション成功が返る")
        void validateUpdateOperatorUser_allValid_returnsSuccess() {
            UpdateUserRequest request = updateRequest(10, null);
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.validateUpdateOperatorUser("COMP-001", "admin1", request)).isNotNull();
            // Input: companyId="COMP-001", username="admin1",
            //        request(userId=10, permissionCodes=["CODE_A"])
            //        operatingUserRepository.findById(10) が company.companyId="COMP-001" のユーザーを返す
            //        permissionRepository.findByCodeIn(["CODE_A"])=1件
            // Output: BaseResponse.success, message=MSG_VALIDATION_SUCCESS
        }

        @Test
        @DisplayName("UT-OUS-032: userIdがnullの場合、errorsにuserIdのみ含まれ以降の処理は実行されない")
        void validateUpdateOperatorUser_nullUserId_returnsUserIdErrorOnly() {
            assertThat(operatorUserService.validateUpdateOperatorUser("COMP-001", "admin1", updateRequest(null, null)).getErrors()).containsKey("userId");
            // Input: request.userId=null
            // Output: errors={"userId": MSG_BAD_REQUEST}, errors.size=1
            //         operatingUserRepository.findByIdが一度も呼ばれないこと（早期return）
        }

        @Test
        @DisplayName("UT-OUS-033: userIdが存在しない場合、errorsにuserIdが含まれ以降の処理は実行されない")
        void validateUpdateOperatorUser_nonExistingUserId_returnsUserIdError() {
            when(operatingUserRepository.findById(999)).thenReturn(Optional.empty());
            assertThat(operatorUserService.validateUpdateOperatorUser("COMP-001", "admin1", updateRequest(999, null)).getErrors()).containsKey("userId");
            // Input: request.userId=999
            //        operatingUserRepository.findById(999)がOptional.empty
            // Output: errors={"userId": MSG_FORBIDDEN}, errors.size=1（早期return）
        }

        @Test
        @DisplayName("UT-OUS-034: 対象ユーザーが別会社に属する場合、errorsにuserIdが含まれ以降の処理は実行されない")
        void validateUpdateOperatorUser_userBelongsToDifferentCompany_returnsUserIdError() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(2, "COMP-999"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.validateUpdateOperatorUser("COMP-001", "admin1", updateRequest(10, null)).getErrors()).containsKey("userId");
            // Input: companyId="COMP-001", request.userId=10
            //        targetUser.company.companyId="COMP-999"
            // Output: errors={"userId": MSG_FORBIDDEN}, errors.size=1（早期return）
            //         permissionRepository.findByCodeInが呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-035: permissionCodesに不正な値が含まれる場合、errorsにpermissionCodesが含まれる")
        void validateUpdateOperatorUser_invalidPermissionCode_returnsPermissionCodesError() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.validateUpdateOperatorUser("COMP-001", "admin1", updateRequest(10, List.of("INVALID_CODE"))).getErrors()).containsKey("permissionCodes");
            // Input: request.userId=10（有効）, request.permissionCodes=["INVALID_CODE"]
            // Output: errors["permissionCodes"]="権限コードに不正な値が含まれています。"
        }

        @Test
        @DisplayName("UT-OUS-036: permissionCodesにDB未存在のコードが含まれる場合、errorsにpermissionCodesが含まれる")
        void validateUpdateOperatorUser_permissionCodeNotFoundInDb_returnsPermissionCodesError() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            when(permissionRepository.findByCodeIn(anySet())).thenReturn(List.of());
            assertThat(operatorUserService.validateUpdateOperatorUser("COMP-001", "admin1", updateRequest(10, List.of("CODE_A"))).getErrors()).containsKey("permissionCodes");
            // Input: request.userId=10（有効）, request.permissionCodes=["CODE_A","CODE_B"]
            //        permissionRepository.findByCodeIn(...)が1件のみ返す
            // Output: errors["permissionCodes"]="指定された権限コードの一部が見つかりませんでした。"
        }

        @Test
        @DisplayName("UT-OUS-037: permissionCodesがnullまたは空リストの場合、権限チェックはスキップされエラーなし")
        void validateUpdateOperatorUser_nullOrEmptyPermissionCodes_skipsCheck() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.validateUpdateOperatorUser("COMP-001", "admin1", updateRequest(10, null)).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: request.userId=10（有効）, request.permissionCodes=null
            // Output: BaseResponse.success
            //         permissionRepository.findByCodeInが呼ばれないこと
        }
    }

    // =========================================================
    // PATCH /admin/companies/operator-user/{userId}
    //   - updateUser
    // =========================================================
    @Nested
    @DisplayName("PATCH /admin/companies/operator-user/{userId} のテスト（updateUser）")
    class UpdateUserTest {

        @Test
        @DisplayName("UT-OUS-038: userIdがnullの場合、BAD_REQUESTを返し以降の処理は実行されない")
        void updateUser_nullUserId_returnsBadRequest() {
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", updateRequest(null, null), RoleConstants.OPERATOR, 1).getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            // Input: request.userId=null
            // Output: statusCode=BAD_REQUEST, message=MSG_BAD_REQUEST
            //         operatingUserRepository.findByIdが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-039: 対象ユーザーが存在しない場合、FORBIDDENを返し以降の処理は実行されない")
        void updateUser_nonExistingUser_returnsForbidden() {
            when(operatingUserRepository.findById(999)).thenReturn(Optional.empty());
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", updateRequest(999, null), RoleConstants.OPERATOR, 1).getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            // Input: request.userId=999
            //        operatingUserRepository.findById(999)がOptional.empty
            // Output: statusCode=FORBIDDEN, message=MSG_FORBIDDEN
            //         operatingUserRepository.saveが呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-040: 対象ユーザーが別会社に属する場合、FORBIDDENを返し以降の処理は実行されない")
        void updateUser_userBelongsToDifferentCompany_returnsForbidden() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(2, "COMP-999"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", updateRequest(10, null), RoleConstants.OPERATOR, 1).getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            // Input: companyId="COMP-001", request.userId=10
            //        targetUser.company.companyId="COMP-999"
            // Output: statusCode=FORBIDDEN, message=MSG_FORBIDDEN
            //         saveが呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-041: roleがoperatorの場合、permissionCodesが指定されても権限は変更されない")
        void updateUser_operatorRole_ignoresPermissionCodes() {
            OperatingUser user = user(10, company(1, "COMP-001"), RoleConstants.OPERATOR);
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user));
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", updateRequest(10, List.of("CODE_A")), RoleConstants.OPERATOR, 1).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: role="operator", request(userId=10, permissionCodes=["CODE_A"], 他フィールド未指定)
            //        targetUser=対象ユーザー（同一company）
            // Output: statusCode=OK, message="更新内容がありませんでした。"
            //         targetUser.replacePermissionsが一度も呼ばれないこと(verify never)
            //         permissionRepository.findByCodeInが呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-042: roleがadminかつpermissionCodesが有効な場合、権限が置き換えられOKが返る")
        void updateUser_adminRoleWithValidPermissionCodes_replacesPermissionsAndReturnsOk() {
            OperatingUser user = user(10, company(1, "COMP-001"), RoleConstants.ADMIN);
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user));
            when(permissionRepository.findByCodeIn(anySet())).thenReturn(List.of(mock(Permission.class), mock(Permission.class)));
            when(operatingUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", updateRequest(10, List.of("PROJECT_CREATE", "PROJECT_READ")), RoleConstants.ADMIN, 1).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: role="admin", username="admin1",
            //        request(userId=10, permissionCodes=["PROJECT_CREATE","PROJECT_READ"])
            //        targetUser=対象ユーザー（同一company）
            //        permissionRepository.findByCodeIn(["PROJECT_CREATE","PROJECT_READ"]) が2件のPermissionを返す
            // Output: statusCode=OK, message="運用者ユーザーを更新しました。"
            //         targetUser.replacePermissions(2件のPermission)が呼ばれること
            //         operatingUserRepository.saveが呼ばれること
            //         targetUser.updatedBy="admin1", updatedAtが更新されること
        }

        @Test
        @DisplayName("UT-OUS-043: roleがadminかつpermissionCodesに不正な値が含まれる場合、BAD_REQUESTを返しsaveは実行されない")
        void updateUser_adminRoleWithInvalidPermissionCodes_returnsBadRequest() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.ADMIN)));
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", updateRequest(10, List.of("INVALID_CODE")), RoleConstants.ADMIN, 1).getStatusCode()).isEqualTo(ResponseConstants.BAD_REQUEST);
            // Input: role="admin", request(userId=10, permissionCodes=["INVALID_CODE"])
            //        targetUser=対象ユーザー（同一company）
            // Output: statusCode=BAD_REQUEST, message="指定された権限コードに不正な値が含まれています。"
            //         operatingUserRepository.saveが一度も呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-044: roleがadminかつpermissionCodesがnullの場合、権限は変更されない")
        void updateUser_adminRoleWithNullPermissionCodes_doesNotChangePermissions() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.ADMIN)));
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", updateRequest(10, null), RoleConstants.ADMIN, 1).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: role="admin", request(userId=10, permissionCodes=null, 他フィールド未指定)
            //        targetUser=対象ユーザー（同一company）
            // Output: statusCode=OK, message="更新内容がありませんでした。"
            //         targetUser.replacePermissionsが呼ばれないこと
        }

        @Test
        @DisplayName("UT-OUS-045: 基本フィールド（氏名・email等）が既存値と異なる場合、更新されOKが返る")
        void updateUser_changedBaseFields_updatesAndReturnsOk() {
            OperatingUser user = user(10, company(1, "COMP-001"), RoleConstants.OPERATOR);
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user));
            UpdateUserRequest request = updateRequest(10, null);
            request.setFirstName("次郎");
            when(operatingUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", request, RoleConstants.OPERATOR, 1).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: role="operator", username="admin1",
            //        request(userId=10, firstName="次郎", email="new@example.com")
            //        targetUser.firstName="太郎", targetUser.email="old@example.com"
            // Output: statusCode=OK, message="運用者ユーザーを更新しました。"
            //         targetUser.firstName="次郎", targetUser.email="new@example.com"
            //         targetUser.updatedBy="admin1"、updatedAtが更新されること
            //         operatingUserRepository.saveが呼ばれること
        }

        @Test
        @DisplayName("UT-OUS-046: 全フィールドが未指定または既存値と同一の場合、更新されずメッセージが「更新内容がありませんでした。」となる")
        void updateUser_noChanges_returnsNoChangeMessage() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", updateRequest(10, null), RoleConstants.OPERATOR, 1).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: role="operator", request(userId=10, 他フィールド全てnullまたは既存値と同一)
            //        targetUser=既存の値を保持
            // Output: statusCode=OK, message="更新内容がありませんでした。"
            //         operatingUserRepository.saveが一度も呼ばれないこと(verify never)
        }

        @Test
        @DisplayName("UT-OUS-047: フィールドが空文字の場合、そのフィールドは更新対象とならない")
        void updateUser_blankFields_areNotUpdated() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            UpdateUserRequest request = updateRequest(10, null);
            request.setFirstName("");
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", request, RoleConstants.OPERATOR, 1)).isNotNull();
            // Input: role="operator", request(userId=10, firstName="", email=null)
            //        targetUser.firstName="太郎"（既存値）
            // Output: statusCode=OK, message="更新内容がありませんでした。"
            //         targetUser.firstNameは"太郎"のまま変更されないこと
        }

        @Test
        @DisplayName("UT-OUS-048: 基本フィールドの更新とpermissionCodesの更新が同時に発生した場合、両方が反映されOKが返る")
        void updateUser_bothBaseFieldsAndPermissionsChanged_updatesBothAndReturnsOk() {
            OperatingUser user = user(10, company(1, "COMP-001"), RoleConstants.ADMIN);
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user));
            when(permissionRepository.findByCodeIn(anySet())).thenReturn(List.of(mock(Permission.class)));
            when(operatingUserRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            UpdateUserRequest request = updateRequest(10, List.of("PROJECT_CREATE"));
            request.setFirstName("次郎");
            assertThat(operatorUserService.updateUser("COMP-001", "admin1", request, RoleConstants.ADMIN, 1).getStatusCode()).isEqualTo(ResponseConstants.OK);
            verify(permissionRepository).findByCodeIn(anySet());
            verify(operatingUserRepository).save(user);
            // Input: role="admin", username="admin1",
            //        request(userId=10, firstName="次郎", permissionCodes=["CODE_A"])
            //        targetUser.firstName="太郎"
            //        permissionRepository.findByCodeIn(["CODE_A"])=1件
            // Output: statusCode=OK, message="運用者ユーザーを更新しました。"
            //         targetUser.firstName="次郎", replacePermissionsが呼ばれること
            //         saveが1回だけ呼ばれること
        }
    }

    // =========================================================
    // GET /admin/companies/operator-user/{userId}
    //   - getOperatorUserProfileById
    // =========================================================
    @Nested
    @DisplayName("GET /admin/companies/operator-user/{userId} のテスト（getOperatorUserProfileById）")
    class GetOperatorUserProfileByIdTest {

        @Test
        @DisplayName("UT-OUS-049: userIdが存在しない場合、NOT_FOUNDを返す")
        void getOperatorUserProfileById_nonExistingUserId_returnsNotFound() {
            when(operatingUserRepository.findById(999)).thenReturn(Optional.empty());
            assertThat(operatorUserService.getOperatorUserProfileById(999, 1, RoleConstants.ADMIN, null).getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
            // Input: userId=999, requestingId=1, requestingRole="admin", requestingCompanyId=null
            //        operatingUserRepository.findById(999)がOptional.empty
            // Output: statusCode=NOT_FOUND, message=MSG_USER_NOT_FOUND
        }

        @Test
        @DisplayName("UT-OUS-050: operatorが自分以外のユーザーを閲覧しようとした場合、FORBIDDENを返す")
        void getOperatorUserProfileById_operatorViewingOtherUser_returnsForbidden() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.getOperatorUserProfileById(10, 5, RoleConstants.OPERATOR, "COMP-001").getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            // Input: userId=10, requestingId=5, requestingRole="operator", requestingCompanyId="COMP-001"
            //        user(id=10, company.companyId="COMP-001")が存在
            // Output: statusCode=FORBIDDEN, message=MSG_FORBIDDEN
        }

        @Test
        @DisplayName("UT-OUS-051: operatorが自分自身のプロフィールを閲覧できる")
        void getOperatorUserProfileById_operatorViewingSelf_returnsOk() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.getOperatorUserProfileById(10, 10, RoleConstants.OPERATOR, "COMP-001").getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: userId=10, requestingId=10, requestingRole="operator", requestingCompanyId="COMP-001"
            //        user(id=10, company.companyId="COMP-001")が存在
            // Output: statusCode=OK, message="運用者ユーザー情報を取得しました。"
            //         profile.id="10"
        }

        @Test
        @DisplayName("UT-OUS-052: requestingCompanyIdが非nullで対象ユーザーの会社と不一致の場合、FORBIDDENを返す")
        void getOperatorUserProfileById_companyMismatch_returnsForbidden() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.getOperatorUserProfileById(10, 10, RoleConstants.OPERATOR, "COMP-999").getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
            // Input: userId=10, requestingId=10, requestingRole="operator", requestingCompanyId="COMP-999"
            //        user(id=10, company.companyId="COMP-001")が存在（role/id条件は通過する設定）
            // Output: statusCode=FORBIDDEN, message=MSG_FORBIDDEN
        }

        @Test
        @DisplayName("UT-OUS-053: requestingCompanyIdがnullの場合、会社チェックはスキップされOKが返る")
        void getOperatorUserProfileById_nullRequestingCompanyId_skipsCompanyCheckAndReturnsOk() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.getOperatorUserProfileById(10, 1, RoleConstants.ADMIN, null).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: userId=10, requestingId=1, requestingRole="admin", requestingCompanyId=null
            //        user(id=10, company.companyId="COMP-001")が存在
            // Output: statusCode=OK
            //         会社不一致であっても許可されること（requestingCompanyId=nullのため）
        }

        @Test
        @DisplayName("UT-OUS-054: adminが同一会社の他ユーザーのプロフィールを閲覧できる")
        void getOperatorUserProfileById_adminViewingOtherUserSameCompany_returnsOk() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.getOperatorUserProfileById(10, 1, RoleConstants.ADMIN, "COMP-001").getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: userId=10, requestingId=1, requestingRole="admin", requestingCompanyId="COMP-001"
            //        user(id=10, company.companyId="COMP-001")が存在
            // Output: statusCode=OK, profile.id="10"
        }

        @Test
        @DisplayName("UT-OUS-055: adminロールユーザーのプロフィールを取得した場合、permissionsに全PermissionCodeが返る")
        void getOperatorUserProfileById_targetUserIsAdmin_returnsAllPermissionCodes() {
            when(operatingUserRepository.findById(1)).thenReturn(Optional.of(user(1, company(1, "COMP-001"), RoleConstants.ADMIN)));
            assertThat(operatorUserService.getOperatorUserProfileById(1, 1, RoleConstants.ADMIN, null).getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: userId=1, requestingId=1, requestingRole="admin", requestingCompanyId=null
            //        user(id=1, role="admin", permissions=空Set)が存在
            // Output: statusCode=OK
            //         profile.permissionsがPermissionCode.values()全件と一致すること
        }

        @Test
        @DisplayName("UT-OUS-056: operatorロールユーザーのプロフィールを取得した場合、紐づくpermissionのみ昇順で返る")
        void getOperatorUserProfileById_targetUserIsOperator_returnsOwnPermissionsSorted() {
            when(operatingUserRepository.findById(10)).thenReturn(Optional.of(user(10, company(1, "COMP-001"), RoleConstants.OPERATOR)));
            assertThat(operatorUserService.getOperatorUserProfileById(10, 10, RoleConstants.OPERATOR, "COMP-001").getStatusCode()).isEqualTo(ResponseConstants.OK);
            // Input: userId=10, requestingId=10, requestingRole="operator", requestingCompanyId="COMP-001"
            //        user(id=10, role="operator", company.companyId="COMP-001", permissions={CODE_B, CODE_A})
            // Output: statusCode=OK
            //         profile.permissions=["CODE_A","CODE_B"]（昇順）
        }
    }

    private OperatorCompany company(Integer id, String companyId) {
        OperatorCompany company = new OperatorCompany();
        company.setId(id);
        company.setCompanyId(companyId);
        return company;
    }

    private OperatingUser user(Integer id, OperatorCompany company, String role) {
        OperatingUser user = new OperatingUser();
        user.setId(id);
        user.setCompany(company);
        user.setRole(role);
        user.setPermissions(new HashSet<>());
        return user;
    }

    private CreateOperatorUserRequest createRequest(String email, String username, List<String> permissions) {
        CreateOperatorUserRequest request = new CreateOperatorUserRequest();
        request.setEmail(email);
        request.setUsername(username);
        request.setPassword("Password1");
        request.setFirstName("太郎");
        request.setFirstNameKana("タロウ");
        request.setLastName("山田");
        request.setLastNameKana("ヤマダ");
        request.setPhoneNumber("09012345678");
        request.setPermissionCodes(permissions);
        return request;
    }

    private UpdateUserRequest updateRequest(Integer id, List<String> permissions) {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUserId(id);
        request.setPermissionCodes(permissions);
        return request;
    }

    private void prepareCompanyValidation() {
        lenient().when(operatorCompanyRepository.findByCompanyId("COMP-001")).thenReturn(Optional.of(company(1, "COMP-001")));
        lenient().when(operatingUserRepository.countByCompany_Id(1)).thenReturn(0L);
        lenient().when(usernameAvailabilityService.isAvailable(anyString())).thenReturn(true);
    }

    private void createUserSetup() {
        prepareCompanyValidation();
        lenient().when(operatingUserRepository.existsByEmail(anyString())).thenReturn(false);
        lenient().when(operatingUserRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        lenient().when(operatingUserRepository.existsByUsername(anyString())).thenReturn(false);
        lenient().when(usernameAvailabilityService.lockAndRequireAvailable(anyString())).thenReturn("user");
        lenient().when(operatingUserRepository.save(any(OperatingUser.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}