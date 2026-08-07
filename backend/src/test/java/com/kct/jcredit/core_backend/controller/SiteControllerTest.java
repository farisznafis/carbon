package com.kct.jcredit.core_backend.controller;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.dto.request.CreateSiteRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentUpdateRequest;
import com.kct.jcredit.core_backend.dto.request.UpdateSiteRequest;
import com.kct.jcredit.core_backend.dto.response.CreateSiteResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectListInfoResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentRegisterResponse;
import com.kct.jcredit.core_backend.dto.response.EnR002EquipmentResponse;
import com.kct.jcredit.core_backend.dto.response.SiteInfoResponse;
import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
import com.kct.jcredit.core_backend.entity.EnR002Equipment;
import com.kct.jcredit.core_backend.entity.Site;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.service.EnR002EquipmentService;
import com.kct.jcredit.core_backend.service.ProjectService;
import com.kct.jcredit.core_backend.service.SiteService;
import com.kct.jcredit.core_backend.util.ResponseUtil;
import com.kct.jcredit.core_backend.util.JwtUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteController のテスト")
class SiteControllerTest {

    @Mock
    private SiteService siteService;
    @Mock
    private ProjectService projectService;
    @Mock
    private EnR002EquipmentService equipmentService; 
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private SiteController siteController;

    private static final String TOKEN = "dummy-token";

    // ---------- helper ----------

    private Site buildSite(int dbId, String publicId) {
        Site site = new Site();
        site.setId(dbId);
        site.setPublicId(publicId);
        return site;
    }

    private EnR002Equipment buildEquipment(int id, int siteId) {
        return EnR002Equipment.builder() 
                .equipmentOpDate(LocalDate.of(2026, 3, 20))
                .nominalMaxOutput(new BigDecimal("49.5"))
                .pcsManufacturer("株式会社オムロン")
                .pcsModel("KP49R")
                .pcsUnitCount(1)
                .deviceNo("SF-TKY-001")
                .pcsStandby(new BigDecimal("0.05"))
                .hasBattery(true)
                .batteryMaker("Tesla Japan")
                .batteryModel("Powerwall 3")
                .batteryCapacity(new BigDecimal("13.5"))
                .totalInvestment(7800000)
                .hasSubsidy(true)
                .subsidyName("東京都自家消費型太陽光発電導入補助金")
                .subsidyAmount(new BigDecimal("1500000"))
                .preProjectUnitPrice(new BigDecimal("26.50"))
                .build();
    }

    // ============================================================================
    // 1. getAllSitesByCompanyId — TIDAK ADA Authentication sama sekali
    // ============================================================================
    @Nested
    @DisplayName("getAllSitesByCompanyId のテスト")
    class GetAllSitesByCompanyIdTest {

        @Test
        @DisplayName("SC-1: 有効な companyId で拠点一覧を取得し、200OK を返す")
        void validCompanyId_returnsOkResponse() {
            SiteListResponse mockResponse = new SiteListResponse();
            mockResponse.setStatusCode("OK");

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(siteService.getSitesByCompanyId("COMP001")).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus("OK")).thenReturn(HttpStatus.OK);

                ResponseEntity<?> response = siteController.getAllSitesByCompanyId("COMP001");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);
                verify(siteService, times(1)).getSitesByCompanyId("COMP001");
            }
        }

        @Test
        @DisplayName("EC-1: 拠点が見つからない場合、404 NOT_FOUND を返す")
        void companyNotFound_returnsNotFoundResponse() {
            SiteListResponse mockResponse = new SiteListResponse();
            mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(siteService.getSitesByCompanyId("COMP999")).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                        .thenReturn(HttpStatus.NOT_FOUND);

                ResponseEntity<?> response = siteController.getAllSitesByCompanyId("COMP999");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }

        /*
         * SEC-01 — TEMUAN KEAMANAN (BOLA/IDOR), bukan sekadar catatan gaya kode:
         * companyId diambil MENTAH dari @RequestParam, tidak pernah dicocokkan dengan
         * identitas pemanggil (tidak ada Authentication/JWT sama sekali di endpoint ini).
         * @PreAuthorize hanya memeriksa ROLE ("master"), bukan KEPEMILIKAN data.
         * Akibatnya: user role master dari perusahaan MANAPUN bisa melihat daftar site
         * perusahaan LAIN hanya dengan mengganti nilai companyId di query string.
         *
         * Test ini MEMBUKTIKAN celahnya: memanggil dengan companyId sembarang tetap
         * diteruskan apa adanya ke service tanpa penyaringan apapun di level controller.
         */
        @Test
        @DisplayName("SEC-01: companyId diteruskan tanpa verifikasi kepemilikan sama sekali（重大なセキュリティ欠陥）")
        void companyId_isPassedThroughWithoutAnyOwnershipCheck() {
            SiteListResponse mockResponse = new SiteListResponse();
            mockResponse.setStatusCode("OK");

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                // Simulasikan: penyerang (role master, perusahaan sendiri "COMP-ATTACKER")
                // mengganti companyId jadi milik korban "COMP-VICTIM".
                when(siteService.getSitesByCompanyId("COMP-VICTIM")).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus("OK")).thenReturn(HttpStatus.OK);

                siteController.getAllSitesByCompanyId("COMP-VICTIM");

                // Controller TIDAK PERNAH mengecek siapa pemanggilnya — request lolos begitu saja.
                verify(siteService, times(1)).getSitesByCompanyId("COMP-VICTIM");
                verifyNoInteractions(authentication, jwtUtil); // tidak ada satupun pengecekan identitas
            }
        }
    }

    // ============================================================================
    // 2. getSite — TIDAK ADA Authentication, dan service-nya pun tidak terima companyId
    // ============================================================================
    @Nested
    @DisplayName("getSite のテスト")
    class GetSiteTest {

        @Test
        @DisplayName("SC-1: 有効な siteId で拠点情報を取得し、200OK を返す")
        void validSiteId_returnsOkResponse() {
            SiteInfoResponse mockResponse = new SiteInfoResponse();
            mockResponse.setStatusCode(ResponseConstants.OK);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(siteService.getSiteByPublicId("PUB-SITE-001")).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

                ResponseEntity<SiteInfoResponse> response = siteController.getSite("PUB-SITE-001");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(mockResponse);
            }
        }

        @Test
        @DisplayName("EC-1: 拠点が見つからない場合、404 NOT_FOUND を返す")
        void siteNotFound_returnsNotFoundResponse() {
            SiteInfoResponse mockResponse = new SiteInfoResponse();
            mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(siteService.getSiteByPublicId("UNKNOWN")).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                        .thenReturn(HttpStatus.NOT_FOUND);

                ResponseEntity<SiteInfoResponse> response = siteController.getSite("UNKNOWN");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }

        /*
         * SEC-02 — TEMUAN KEAMANAN, lebih parah dari SEC-01 karena bahkan tidak ada
         * parameter companyId sama sekali di endpoint ini maupun di service yang dipanggil.
         * User dari perusahaan A yang tahu/menebak siteId milik perusahaan B akan tetap
         * mendapat detail LENGKAP site tersebut (nama PIC, email, telepon, alamat) —
         * kebocoran data lintas-tenant, bukan sekadar celah otorisasi teoritis.
         */
        @Test
        @DisplayName("SEC-02: siteId dari perusahaan manapun bisa diakses tanpa pengecekan companyId（重大なセキュリティ欠陥）")
        void anySiteIdAccessible_regardlessOfCallerCompany() {
            SiteInfoResponse mockResponse = new SiteInfoResponse();
            mockResponse.setStatusCode(ResponseConstants.OK);
            // Detail sensitif milik perusahaan LAIN — tetap terkirim tanpa syarat apapun
            SiteResponse victimSiteDetails = SiteResponse.builder()
                    .siteId("PUB-SITE-VICTIM")
                    .siteName("競合他社の拠点")
                    .picFirstName("極秘")
                    .picLastName("担当者")
                    .build();
            mockResponse.setSite(victimSiteDetails);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(siteService.getSiteByPublicId("PUB-SITE-VICTIM")).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

                ResponseEntity<SiteInfoResponse> response = siteController.getSite("PUB-SITE-VICTIM");

                // Data sensitif lintas-tenant tetap lolos ke response tanpa hambatan
                assertThat(response.getBody().getSite().getPicFirstName()).isEqualTo("極秘");
                verifyNoInteractions(authentication, jwtUtil);
            }
        }
    }

    // ============================================================================
    // 3. getProjectsBySiteId — memanggil projectService, BUKAN siteService
    // ============================================================================
    @Nested
    @DisplayName("getProjectsBySiteId のテスト")
    class GetProjectsBySiteIdTest {

        @Test
        @DisplayName("SC-1: 有効な siteId でプロジェクト一覧を取得し、200OK を返す")
        void validSiteId_returnsOkResponse() {
            ProjectListInfoResponse mockResponse = new ProjectListInfoResponse();
            mockResponse.setStatusCode(ResponseConstants.OK);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                when(projectService.getProjectsBySiteIdAndCompanyId("PUB-SITE-001", "COMP001"))
                        .thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

                ResponseEntity<ProjectListInfoResponse> response =
                        siteController.getProjectsBySiteId(authentication, "PUB-SITE-001");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(projectService, times(1))
                        .getProjectsBySiteIdAndCompanyId("PUB-SITE-001", "COMP001");
                // siteService TIDAK boleh disentuh — endpoint ini memakai projectService
                verifyNoInteractions(siteService);
            }
        }

        @Test
        @DisplayName("EC-1: トークンが無効な場合、projectService は呼ばれない")
        void invalidToken_skipsProjectService() {
            when(authentication.getCredentials()).thenReturn("");
            when(jwtUtil.extractCompanyId(""))
                    .thenThrow(new CustomException("E004", "トークンが無効です"));

            CustomException thrown = assertThrows(CustomException.class,
                    () -> siteController.getProjectsBySiteId(authentication, "PUB-SITE-001"));

            assertThat(thrown.getMessage()).isEqualTo("トークンが無効です");
            verifyNoInteractions(projectService);
        }

        @Test
        @DisplayName("EC-2: サービスが例外をスローする場合、例外が伝播される")
        void serviceThrows_propagatesException() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(projectService.getProjectsBySiteIdAndCompanyId("PUB-SITE-001", "COMP001"))
                    .thenThrow(new CustomException("E003", "プロジェクト一覧の取得に失敗しました"));

            assertThrows(CustomException.class,
                    () -> siteController.getProjectsBySiteId(authentication, "PUB-SITE-001"));
        }
    }

    // ============================================================================
    // 4. createSite
    // ============================================================================
    @Nested
    @DisplayName("createSite のテスト")
    class CreateSiteTest {

        @Test
        @DisplayName("SC-1: 有効なリクエストで拠点作成が成功し、200OK を返す")
        void validRequest_returnsOkResponse() {
            CreateSiteRequest request = new CreateSiteRequest();
            CreateSiteResponse mockResponse = new CreateSiteResponse();
            mockResponse.setStatusCode(ResponseConstants.OK);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                when(siteService.createSite(request, "COMP001")).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

                ResponseEntity<CreateSiteResponse> response =
                        siteController.createSite(authentication, request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(siteService, times(1)).createSite(request, "COMP001");
            }
        }

        @Test
        @DisplayName("EC-1: トークンが無効な場合、サービスは呼ばれない")
        void invalidToken_skipsService() {
            CreateSiteRequest request = new CreateSiteRequest();
            when(authentication.getCredentials()).thenReturn("");
            when(jwtUtil.extractCompanyId(""))
                    .thenThrow(new CustomException("E004", "トークンが無効です"));

            assertThrows(CustomException.class,
                    () -> siteController.createSite(authentication, request));

            verifyNoInteractions(siteService);
        }

        @Test
        @DisplayName("EC-2: 拠点数が上限に達している場合、LIMIT_EXCEEDED を返す")
        void siteLimitExceeded_returnsLimitExceededResponse() {
            CreateSiteRequest request = new CreateSiteRequest();
            CreateSiteResponse mockResponse = new CreateSiteResponse();
            mockResponse.setStatusCode(ResponseConstants.LIMIT_EXCEEDED);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                when(siteService.createSite(request, "COMP001")).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.LIMIT_EXCEEDED))
                        .thenReturn(HttpStatus.FORBIDDEN); // TODO: sesuaikan mapping HTTP status asli

                ResponseEntity<CreateSiteResponse> response =
                        siteController.createSite(authentication, request);

                assertThat(response.getBody().getStatusCode()).isEqualTo(ResponseConstants.LIMIT_EXCEEDED);
            }
        }
    }

    // ============================================================================
    // 5. validateCreateSite
    // ============================================================================
    @Nested
    @DisplayName("validateCreateSite のテスト")
    class ValidateCreateSiteTest {

        @Test
        @DisplayName("SC-1: 検証エラーがない場合、200OK を返す")
        void noValidationErrors_returnsOkResponse() {
            CreateSiteRequest request = new CreateSiteRequest();
            BaseResponse mockResponse = BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS);
            mockResponse.setStatusCode(ResponseConstants.OK);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                when(siteService.validateCreateSite(request, "COMP001")).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

                ResponseEntity<BaseResponse> response =
                        siteController.validateCreateSite(authentication, request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(siteService, times(1)).validateCreateSite(request, "COMP001");
            }
        }

        @Test
        @DisplayName("EC-1: トークンが無効な場合、サービスは呼ばれない")
        void invalidToken_skipsService() {
            CreateSiteRequest request = new CreateSiteRequest();
            when(authentication.getCredentials()).thenReturn("");
            when(jwtUtil.extractCompanyId(""))
                    .thenThrow(new CustomException("E004", "トークンが無効です"));

            assertThrows(CustomException.class,
                    () -> siteController.validateCreateSite(authentication, request));

            verifyNoInteractions(siteService);
        }
    }

    // ============================================================================
    // 6. updateSite
    // ============================================================================
    @Nested
    @DisplayName("updateSite のテスト")
    class UpdateSiteTest {

        @Test
        @DisplayName("SC-1: 有効なリクエストで拠点更新が成功し、200OK を返す")
        void validRequest_returnsOkResponse() {
            UpdateSiteRequest request = new UpdateSiteRequest();
            SiteInfoResponse mockResponse = new SiteInfoResponse();
            mockResponse.setStatusCode(ResponseConstants.OK);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                when(siteService.updateSite("PUB-SITE-001", "COMP001", request)).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

                ResponseEntity<SiteInfoResponse> response =
                        siteController.updateSite(authentication, "PUB-SITE-001", request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(siteService, times(1)).updateSite("PUB-SITE-001", "COMP001", request);
            }
        }

        @Test
        @DisplayName("EC-1: 拠点が見つからない場合、404 NOT_FOUND を返す")
        void siteNotFound_returnsNotFoundResponse() {
            UpdateSiteRequest request = new UpdateSiteRequest();
            SiteInfoResponse mockResponse = new SiteInfoResponse();
            mockResponse.setStatusCode(ResponseConstants.NOT_FOUND);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                when(siteService.updateSite("UNKNOWN", "COMP001", request)).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.NOT_FOUND))
                        .thenReturn(HttpStatus.NOT_FOUND);

                ResponseEntity<SiteInfoResponse> response =
                        siteController.updateSite(authentication, "UNKNOWN", request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }

        @Test
        @DisplayName("EC-2: トークンが無効な場合、サービスは呼ばれない")
        void invalidToken_skipsService() {
            UpdateSiteRequest request = new UpdateSiteRequest();
            when(authentication.getCredentials()).thenReturn("");
            when(jwtUtil.extractCompanyId(""))
                    .thenThrow(new CustomException("E004", "トークンが無効です"));

            assertThrows(CustomException.class,
                    () -> siteController.updateSite(authentication, "PUB-SITE-001", request));

            verifyNoInteractions(siteService);
        }
    }

    // ============================================================================
    // 7. validateUpdateSite
    // ============================================================================
    @Nested
    @DisplayName("validateUpdateSite のテスト")
    class ValidateUpdateSiteTest {

        @Test
        @DisplayName("SC-1: 検証エラーがない場合、200OK を返す")
        void noValidationErrors_returnsOkResponse() {
            UpdateSiteRequest request = new UpdateSiteRequest();
            BaseResponse mockResponse = BaseResponse.success(ResponseConstants.MSG_VALIDATION_SUCCESS);
            mockResponse.setStatusCode(ResponseConstants.OK);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                when(siteService.validateUpdateSite("PUB-SITE-001", "COMP001", request)).thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

                ResponseEntity<BaseResponse> response =
                        siteController.validateUpdateSite(authentication, "PUB-SITE-001", request);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                verify(siteService, times(1)).validateUpdateSite("PUB-SITE-001", "COMP001", request);
            }
        }

        @Test
        @DisplayName("EC-1: トークンが無効な場合、サービスは呼ばれない")
        void invalidToken_skipsService() {
            UpdateSiteRequest request = new UpdateSiteRequest();
            when(authentication.getCredentials()).thenReturn("");
            when(jwtUtil.extractCompanyId(""))
                    .thenThrow(new CustomException("E004", "トークンが無効です"));

            assertThrows(CustomException.class,
                    () -> siteController.validateUpdateSite(authentication, "PUB-SITE-001", request));

            verifyNoInteractions(siteService);
        }
    }

    // ============================================================================
    // 8. getSiteProjectEquipment — 1つのtokenから3つの値を抽出
    // ============================================================================
    @Nested
    @DisplayName("getSiteProjectEquipment のテスト")
    class GetSiteProjectEquipmentTest {

        @Test
        @DisplayName("SC-1: 有効なリクエストで設備情報を取得し、200OK を返す")
        void validRequest_returnsOkResponse() {
            EnR002EquipmentRegisterResponse mockResponse = new EnR002EquipmentRegisterResponse();
            mockResponse.setStatusCode(ResponseConstants.OK);

            try (MockedStatic<ResponseUtil> mockedUtil = mockStatic(ResponseUtil.class)) {
                when(authentication.getCredentials()).thenReturn(TOKEN);
                when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
                when(jwtUtil.extractRole(TOKEN)).thenReturn(RoleConstants.MASTER);
                when(jwtUtil.extractUsername(TOKEN)).thenReturn("master001");
                when(siteService.getSiteProjectEquipment(
                        "PUB-SITE-001", "PUB-PROJ-001", "COMP001", RoleConstants.MASTER, "master001"))
                        .thenReturn(mockResponse);
                mockedUtil.when(() -> ResponseUtil.getHttpStatus(ResponseConstants.OK)).thenReturn(HttpStatus.OK);

                ResponseEntity<EnR002EquipmentRegisterResponse> response =
                        siteController.getSiteProjectEquipment(authentication, "PUB-SITE-001", "PUB-PROJ-001");

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                // Token diambil SATU KALI (getCredentials), lalu dipakai untuk 3 ekstraksi berbeda
                verify(authentication, times(1)).getCredentials();
                verify(jwtUtil, times(1)).extractCompanyId(TOKEN);
                verify(jwtUtil, times(1)).extractRole(TOKEN);
                verify(jwtUtil, times(1)).extractUsername(TOKEN);
            }
        }

        @Test
        @DisplayName("EC-1: companyId 抽出が失敗した場合、role・username 抽出とサービス呼び出しは行われない")
        void extractCompanyIdThrows_skipsRestOfExtractionAndService() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN))
                    .thenThrow(new CustomException("E005", "会社情報を取得できません"));

            assertThrows(CustomException.class,
                    () -> siteController.getSiteProjectEquipment(authentication, "PUB-SITE-001", "PUB-PROJ-001"));

            // Kode memanggil extractCompanyId lebih dulu (sebagai argumen pertama) —
            // Java evaluasi argumen kiri-ke-kanan, jadi extractRole/extractUsername
            // tidak akan pernah dieksekusi kalau argumen sebelumnya melempar exception.
            verify(jwtUtil, never()).extractRole(any());
            verify(jwtUtil, never()).extractUsername(any());
            verifyNoInteractions(siteService);
        }

        @Test
        @DisplayName("EC-2: サービスが例外をスローする場合、例外が伝播される")
        void serviceThrows_propagatesException() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(jwtUtil.extractRole(TOKEN)).thenReturn(RoleConstants.USER);
            when(jwtUtil.extractUsername(TOKEN)).thenReturn("user001");
            when(siteService.getSiteProjectEquipment(any(), any(), any(), any(), any()))
                    .thenThrow(new CustomException("E003", "設備情報の取得に失敗しました"));

            assertThrows(CustomException.class,
                    () -> siteController.getSiteProjectEquipment(authentication, "PUB-SITE-001", "PUB-PROJ-001"));
        }
    }

    // ============================================================================
    // 9-12. Endpoint equipment (addEquipment / getEquipments / getEquipmentById / updateEquipment)
    // CATATAN PENTING: keempat endpoint ini SELALU ResponseEntity.ok(...) — TIDAK memakai
    // ResponseUtil.getHttpStatus() sama sekali. Ini beda pola dari 8 endpoint sebelumnya.
    // Konsekuensi: client TIDAK BISA mengandalkan HTTP status code untuk endpoint ini —
    // satu-satunya jalur error adalah exception yang dilempar equipmentService.
    // ============================================================================
    @Nested
    @DisplayName("addEquipment のテスト")
    class AddEquipmentTest {

        @Test
        @DisplayName("SC-1: 有効なリクエストで設備追加が成功し、200OK を返す（ResponseUtilを使わないパターン）")
        void validRequest_returnsOkResponse() {
            EnR002EquipmentRequest request = new EnR002EquipmentRequest();
            EnR002EquipmentResponse mockResponse = EnR002EquipmentResponse.builder().id(1).build();

            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(equipmentService.addEquipment("PUB-SITE-001", "COMP001", request)).thenReturn(mockResponse);

            ResponseEntity<EnR002EquipmentResponse> response =
                    siteController.addEquipment(authentication, "PUB-SITE-001", request);

            // Selalu 200 OK terlepas dari isi response — tidak ada ResponseUtil di endpoint ini
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(mockResponse);
            verify(equipmentService, times(1)).addEquipment("PUB-SITE-001", "COMP001", request);
        }

        @Test
        @DisplayName("EC-1: トークンが無効な場合、サービスは呼ばれない")
        void invalidToken_skipsService() {
            EnR002EquipmentRequest request = new EnR002EquipmentRequest();
            when(authentication.getCredentials()).thenReturn("");
            when(jwtUtil.extractCompanyId(""))
                    .thenThrow(new CustomException("E004", "トークンが無効です"));

            assertThrows(CustomException.class,
                    () -> siteController.addEquipment(authentication, "PUB-SITE-001", request));

            verifyNoInteractions(equipmentService);
        }

        @Test
        @DisplayName("EC-2: サービスが例外をスローする場合、例外が伝播される（このエンドポイント唯一のエラー経路）")
        void serviceThrows_propagatesException() {
            EnR002EquipmentRequest request = new EnR002EquipmentRequest();
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(equipmentService.addEquipment("PUB-SITE-001", "COMP001", request))
                    .thenThrow(new CustomException("E003", "設備の追加に失敗しました"));

            assertThrows(CustomException.class,
                    () -> siteController.addEquipment(authentication, "PUB-SITE-001", request));
        }
    }

    @Nested
    @DisplayName("getEquipments のテスト")
    class GetEquipmentsTest {

        @Test
        @DisplayName("SC-1: 設備が存在する場合、リストを返し 200OK を返す")
        void hasEquipments_returnsOkResponse() {
            List<EnR002EquipmentResponse> mockList = List.of(
                    EnR002EquipmentResponse.builder().id(1).build(),
                    EnR002EquipmentResponse.builder().id(2).build());

            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(equipmentService.getEquipmentsBySite("PUB-SITE-001", "COMP001")).thenReturn(mockList);

            ResponseEntity<List<EnR002EquipmentResponse>> response =
                    siteController.getEquipments(authentication, "PUB-SITE-001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("SC-2: 設備が1件もない場合、空リストで 200OK を返す")
        void noEquipments_returnsOkWithEmptyList() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(equipmentService.getEquipmentsBySite("PUB-SITE-001", "COMP001"))
                    .thenReturn(Collections.emptyList());

            ResponseEntity<List<EnR002EquipmentResponse>> response =
                    siteController.getEquipments(authentication, "PUB-SITE-001");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }

        @Test
        @DisplayName("EC-1: トークンが無効な場合、サービスは呼ばれない")
        void invalidToken_skipsService() {
            when(authentication.getCredentials()).thenReturn("");
            when(jwtUtil.extractCompanyId(""))
                    .thenThrow(new CustomException("E004", "トークンが無効です"));

            assertThrows(CustomException.class,
                    () -> siteController.getEquipments(authentication, "PUB-SITE-001"));

            verifyNoInteractions(equipmentService);
        }
    }

    @Nested
    @DisplayName("getEquipmentById のテスト")
    class GetEquipmentByIdTest {

        @Test
        @DisplayName("SC-1: 有効な equipmentId で設備情報を取得し、200OK を返す")
        void validEquipmentId_returnsOkResponse() {
            EnR002EquipmentResponse mockResponse = EnR002EquipmentResponse.builder().id(100).build();

            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(equipmentService.getEquipmentById("PUB-SITE-001", 100, "COMP001")).thenReturn(mockResponse);

            ResponseEntity<EnR002EquipmentResponse> response =
                    siteController.getEquipmentById(authentication, "PUB-SITE-001", 100);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getId()).isEqualTo(100);
        }

        @Test
        @DisplayName("EC-1: 設備が見つからない場合、サービスの例外がそのまま伝播される（ResponseUtil を使わないため）")
        void equipmentNotFound_propagatesServiceException() {
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(equipmentService.getEquipmentById("PUB-SITE-001", 999, "COMP001"))
                    .thenThrow(new CustomException("E404", "設備が見つかりません"));

            assertThrows(CustomException.class,
                    () -> siteController.getEquipmentById(authentication, "PUB-SITE-001", 999));
        }

        @Test
        @DisplayName("EC-2: トークンが無効な場合、サービスは呼ばれない")
        void invalidToken_skipsService() {
            when(authentication.getCredentials()).thenReturn("");
            when(jwtUtil.extractCompanyId(""))
                    .thenThrow(new CustomException("E004", "トークンが無効です"));

            assertThrows(CustomException.class,
                    () -> siteController.getEquipmentById(authentication, "PUB-SITE-001", 100));

            verifyNoInteractions(equipmentService);
        }
    }

    @Nested
    @DisplayName("updateEquipment のテスト")
    class UpdateEquipmentTest {

        @Test
        @DisplayName("SC-1: 有効なリクエストで設備更新が成功し、200OK を返す")
        void validRequest_returnsOkResponse() {
            EnR002EquipmentUpdateRequest request = new EnR002EquipmentUpdateRequest();
            EnR002EquipmentResponse mockResponse = EnR002EquipmentResponse.builder().id(100).build();

            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(equipmentService.updateEquipment("PUB-SITE-001", 100, "COMP001", request))
                    .thenReturn(mockResponse);

            ResponseEntity<EnR002EquipmentResponse> response =
                    siteController.updateEquipment(authentication, "PUB-SITE-001", 100, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(equipmentService, times(1))
                    .updateEquipment("PUB-SITE-001", 100, "COMP001", request);
        }

        @Test
        @DisplayName("EC-1: トークンが無効な場合、サービスは呼ばれない")
        void invalidToken_skipsService() {
            EnR002EquipmentUpdateRequest request = new EnR002EquipmentUpdateRequest();
            when(authentication.getCredentials()).thenReturn("");
            when(jwtUtil.extractCompanyId(""))
                    .thenThrow(new CustomException("E004", "トークンが無効です"));

            assertThrows(CustomException.class,
                    () -> siteController.updateEquipment(authentication, "PUB-SITE-001", 100, request));

            verifyNoInteractions(equipmentService);
        }

        @Test
        @DisplayName("EC-2: サービスが例外をスローする場合、例外が伝播される")
        void serviceThrows_propagatesException() {
            EnR002EquipmentUpdateRequest request = new EnR002EquipmentUpdateRequest();
            when(authentication.getCredentials()).thenReturn(TOKEN);
            when(jwtUtil.extractCompanyId(TOKEN)).thenReturn("COMP001");
            when(equipmentService.updateEquipment("PUB-SITE-001", 100, "COMP001", request))
                    .thenThrow(new CustomException("E003", "設備の更新に失敗しました"));

            assertThrows(CustomException.class,
                    () -> siteController.updateEquipment(authentication, "PUB-SITE-001", 100, request));
        }
    }
}