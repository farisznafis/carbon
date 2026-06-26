// package com.kct.jcredit.core_backend.service;

// import com.kct.jcredit.core_backend.constant.ResponseConstants;
// import com.kct.jcredit.core_backend.constant.RoleConstants;
// import com.kct.jcredit.core_backend.dto.request.CreateSiteRequest;
// import com.kct.jcredit.core_backend.dto.request.UpdateSiteRequest;
// import com.kct.jcredit.core_backend.dto.response.CreateSiteResponse;
// import com.kct.jcredit.core_backend.dto.response.EquipmentSolarRegisterResponse;
// import com.kct.jcredit.core_backend.dto.response.SiteInfoResponse;
// import com.kct.jcredit.core_backend.dto.response.SiteListResponse;
// import com.kct.jcredit.core_backend.dto.response.details.EquipmentItemSolarResponse;
// import com.kct.jcredit.core_backend.dto.response.details.SiteDataResponse;
// import com.kct.jcredit.core_backend.dto.response.details.SiteResponse;
// import com.kct.jcredit.core_backend.entity.ParticipatingCompany;
// import com.kct.jcredit.core_backend.entity.ParticipatingUser;
// import com.kct.jcredit.core_backend.entity.Project;
// import com.kct.jcredit.core_backend.entity.Site;
// import com.kct.jcredit.core_backend.entity.SiteProjectRelation;
// import com.kct.jcredit.core_backend.entity.SiteR002EquipmentFile;
// import com.kct.jcredit.core_backend.entity.SiteR002MemberData;
// import com.kct.jcredit.core_backend.repository.postgres.ParticipatingCompanyRepository;
// import com.kct.jcredit.core_backend.repository.postgres.ParticipatingUserRepository;
// import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
// import com.kct.jcredit.core_backend.repository.postgres.SiteProjectRelationRepository;
// import com.kct.jcredit.core_backend.repository.postgres.SiteR002EquipmentFileRepository;
// import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
// import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
// import com.kct.jcredit.core_backend.service.SiteService;
// import com.kct.jcredit.core_backend.service.impl.SiteServiceImpl;

// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.Captor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyInt;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// public class SiteServiceTest {
//     @Mock
//     private ParticipatingCompanyRepository participatingCompanyRepository;

//     @Mock
//     private ProjectRepository projectRepository;

//     @Mock
//     private SiteRepository siteRepository;

//     @Mock
//     private SiteProjectRelationRepository siteProjectRelationRepository;

//     @Mock
//     private SiteR002MemberDataRepository siteR002MemberDataRepository;

//     @Mock
//     private SiteR002EquipmentFileRepository siteR002EquipmentFileRepository;

//     @Mock
//     private ParticipatingUserRepository participatingUserRepository;

        
//     @Captor
//     private ArgumentCaptor<Site> siteCaptor;

//     @InjectMocks
//     private SiteServiceImpl siteService;

//     //========== Helper Methods ==========

//     private ParticipatingCompany buildCompany(String companyId) {
//         ParticipatingCompany company = new ParticipatingCompany();
//         company.setCompanyId(companyId);
//         company.setNameJa("テスト株式会社");
//         company.setPrefecture("東京都");
//         company.setMunicipality("渋谷区");
//         return company;
//     }

//     private Site buildSite(int id, ParticipatingCompany company, String siteName) {
//         Site site = new Site();
//         site.setId(id);
//         site.setSiteName(siteName);
//         site.setPostalCode("100-0001");
//         site.setPrefecture("東京都");
//         site.setMunicipality("千代田区");
//         site.setBlocknum("1-1");
//         site.setBuildingName("ビルA");
//         site.setPicFirstName("太郎");
//         site.setPicLastName("山田");
//         site.setCompany(company);
//         site.setSiteProjectRelations(new ArrayList<>());
//         return site;
//     }

//     private Project buildProject(String projectName) {
//         Project project = new Project();
//         project.setProjectName(projectName);
//         return project;
//     }

//     private SiteProjectRelation buildSiteProjectRelation(Site site, Project project) {
//         SiteProjectRelation relation = new SiteProjectRelation();
//         relation.setSite(site);
//         relation.setProject(project);
//         return relation;
//     }

//     private CreateSiteRequest buildCreateSiteRequest() {
//         CreateSiteRequest request = new CreateSiteRequest();

//         // 拠点基本情報
//         request.setSiteName("東京支社");
//         request.setSiteNameKana("トウキョウシシャ");
//         request.setPostalCode("100-0001");
//         request.setPrefecture("東京都");
//         request.setMunicipality("千代田区");
//         request.setBlocknum("1-1");
//         request.setBuildingName("オフィスビルA10階");
        
//         // 担当者情報（PIC: Person In Charge）
//         request.setPicFirstName("太郎");
//         request.setPicFirstNameKana("タロウ");
//         request.setPicLastName("山田");
//         request.setPicLastNameKana("ヤマダ");
//         request.setPicEmail("yamada.taro@example.com");
//         request.setPicPhoneNumber("03-1234-5678");
//         request.setPicPosition("営業部長");
        
//         return request;
//     }

//     private Site buildSiteWithCompany(int id, String companyId) {
//         ParticipatingCompany company = new ParticipatingCompany();
//         company.setCompanyId(companyId);
//         company.setNameJa("テスト株式会社");

//         Site site = new Site();
//         site.setId(id);
//         site.setSiteName("テスト拠点");
//         // site.setSiteNameKana("テストキョテン");
//         site.setPicFirstName("太郎");
//         site.setPicLastName("山田");
//         site.setCompany(company);
        
//         return site;
//     }

//     private UpdateSiteRequest buildUpdateSiteRequest(
//             String postalCode,
//             String Prefecture,
//             String Municipality,
//             String townName,
//             String blockName,
//             String address) {
        
//         UpdateSiteRequest request = new UpdateSiteRequest();
//         request.setPostalCode(postalCode);
//         request.setPrefecture(Prefecture);
//         request.setMunicipality(Municipality);
//         request.setBlocknum(blockName);
//         request.setBuildingName(address);
        
//         return request;
//     }

//     private SiteProjectRelation buildSiteProjectRelation(int siteId, int projectId) {
//         SiteProjectRelation relation = new SiteProjectRelation();
//         relation.setSite(new Site());
//         relation.getSite().setId(siteId);
//         relation.setProject(new Project());
//         relation.getProject().setId(projectId);
//         return relation;
//     }

//     private SiteR002MemberData buildMemberData(Integer id) {
//         SiteR002MemberData memberData = new SiteR002MemberData();
//         memberData.setId(id);
//         memberData.setPcsManufacturer("パナソニック");
//         memberData.setPcsModel("VBPC255A");
//         memberData.setPowerCondUnitCount(5);
//         memberData.setDeviceNo("DEV-001");
//         return memberData;
//     }

//     private ParticipatingUser buildParticipatingUser(String username, String companyId, int siteId) {
//         Site userSite = new Site();
//         userSite.setId(siteId);

//         ParticipatingUser user = new ParticipatingUser();
//         user.setUsername(username);
//         user.setId(123);
//         user.setSite(userSite);
//         return user;
//     }

//     private SiteR002EquipmentFile buildEquipmentFile(Integer memberDataId, String s3Key) {
//         SiteR002MemberData memberData = new SiteR002MemberData();
//         memberData.setId(memberDataId);

//         SiteR002EquipmentFile file = new SiteR002EquipmentFile();
//         file.setMemberData(memberData);
//         file.setS3Key(s3Key);
//         return file;
//     }

//     private Project buildProject(int projectId) {
//         Project project = new Project();
//         project.setId(projectId);
//         project.setProjectName("太陽光発電プロジェクト");
//         return project;
//     }


//     //METHOD 1 GET/SITE
//     /**
//      * 正常系テストグループ
//      */
//         /**
//          * S1-01: 有効な企業IDで複数の拠点とプロジェクトを取得
//          *
//          * 前提条件:
//          * - 企業が存在する
//          * - 2つの拠点が存在する
//          * - 各拠点に1つずつプロジェクトが紐づいている
//          *
//          * 期待結果:
//          * - statusCode = "OK"
//          * - siteListのサイズが2
//          * - 各拠点のprojectsフィールドにプロジェクト名が含まれる
//          * - dataフィールドはnull
//          * - repositoryのメソッドが正しく呼ばれる
//          */
//     @Test
//     @DisplayName("UT-SS-101 有効な企業IDで複数の拠点が存在する場合、拠点一覧とプロジェクト名を返す")
//     void getSitesByCompanyId_validCompanyWithMultipleSites_returnsSiteList() {
//         // Arrange
//         String companyId = "COMP001";
//         ParticipatingCompany company = buildCompany(companyId);

//         // Site 1 with Project1
//         Site site1 = buildSite(1, company, "東京本社");
//         Project project1 = buildProject("太陽光発電プロジェクトA");
//         SiteProjectRelation relation1 = buildSiteProjectRelation(site1, project1);
//         site1.setSiteProjectRelations(List.of(relation1));

//         // Site 2 with Project 2
//         Site site2 = buildSite(2, company, "大阪支社");
//         Project project2 = buildProject("太陽光発電プロジェクトB");
//         SiteProjectRelation relation2 = buildSiteProjectRelation(site2, project2);
//         site2.setSiteProjectRelations(List.of(relation2));

//         List<Site> sites = List.of(site1, site2);

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.of(company));
//         when(siteRepository.findByCompany(company)).thenReturn(sites);

//         // Act
//         SiteListResponse response = siteService.getSitesByCompanyId(companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo("OK");
//         assertThat(response.getMessage()).isNull();
//         assertThat(response.getSiteList()).isNotNull();
//         assertThat(response.getSiteList()).hasSize(2);

//         // Site1 の検証
//         SiteResponse siteResponse1 = response.getSiteList().get(0);
//         assertThat(siteResponse1.getId()).isEqualTo("1");
//         assertThat(siteResponse1.getSiteName()).isEqualTo("東京本社");
//         assertThat(siteResponse1.getPostalCode()).isEqualTo("100-0001");
//         assertThat(siteResponse1.getPrefecture()).isEqualTo("東京都");
//         assertThat(siteResponse1.getMunicipality()).isEqualTo("千代田区");
//         assertThat(siteResponse1.getBlocknum()).isEqualTo("1-1");
//         assertThat(siteResponse1.getBuildingName()).isEqualTo("ビルA");
//         assertThat(siteResponse1.getPicFirstName()).isEqualTo("太郎");
//         assertThat(siteResponse1.getPicLastName()).isEqualTo("山田");
//         assertThat(siteResponse1.getProjects()).isNotNull();
//         assertThat(siteResponse1.getProjects()).hasSize(1);
//         assertThat(siteResponse1.getProjects()).containsExactly("太陽光発電プロジェクトA");
//         assertThat(siteResponse1.getData()).isNull();

//         // Site 2 の検証
//         SiteResponse siteResponse2 = response.getSiteList().get(1);
//         assertThat(siteResponse2.getId()).isEqualTo("2");
//         assertThat(siteResponse2.getSiteName()).isEqualTo("大阪支社");
//         assertThat(siteResponse2.getProjects()).isNotNull();
//         assertThat(siteResponse2.getProjects()).hasSize(1);
//         assertThat(siteResponse2.getProjects()).containsExactly("太陽光発電プロジェクトB");
//         assertThat(siteResponse2.getData()).isNull();

//         // Verify interactions
//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//         verify(siteRepository, times(1)).findByCompany(company);
//     }

//     /**
//      * S1-02: 拠点のsiteProjectRelationsがnullの場合
//      *
//      * 前提条件:
//      * - 企業が存在する
//      * - 拠点が存在するが、siteProjectRelationsフィールドがnull
//      *
//      * 期待結果:
//      * - statusCode = "OK"
//      * - siteList[0].projects =空のリスト
//      * - dataフィールドはnull
//      */
//     @Test
//     @DisplayName("UT-SS-102 拠点のプロジェクトリレーションがnullの場合、空のプロジェクトリストを返す")
//     void getSitesByCompanyId_siteWithNullProjectRelations_returnsEmptyProjects() {
//         // Arrange
//         String companyId = "COMP002";
//         ParticipatingCompany company = buildCompany(companyId);

//         Site site = buildSite(10, company, "名古屋営業所");
//         site.setSiteProjectRelations(null); // null設定

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.of(company));
//         when(siteRepository.findByCompany(company)).thenReturn(List.of(site));

//         // Act
//         SiteListResponse response = siteService.getSitesByCompanyId(companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo("OK");
//         assertThat(response.getSiteList()).isNotNull();
//         assertThat(response.getSiteList()).hasSize(1);

//         SiteResponse siteResponse = response.getSiteList().get(0);
//         assertThat(siteResponse.getId()).isEqualTo("10");
//         assertThat(siteResponse.getSiteName()).isEqualTo("名古屋営業所");
//         assertThat(siteResponse.getProjects()).isNotNull();
//         assertThat(siteResponse.getProjects()).isEmpty();
//         assertThat(siteResponse.getData()).isNull();

//         // Verify interactions
//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//         verify(siteRepository, times(1)).findByCompany(company);
//     }

//     /**
//      * S1-03: 拠点のsiteProjectRelationsが空のリストの場合
//      *
//      * 前提条件:
//      * - 企業が存在する
//      * - 拠点が存在するが、siteProjectRelationsフィールドが空のリスト
//      *
//      * 期待結果:
//      * - statusCode = "OK"
//      * - siteList[0].projects = 空のリスト
//      * - dataフィールドはnull
//      */
//     @Test
//     @DisplayName("UT-SS-103 拠点のプロジェクトリレーションが空のリストの場合、空のプロジェクトリストを返す")
//     void getSitesByCompanyId_siteWithEmptyProjectRelations_returnsEmptyProjects() {
//         // Arrange
//         String companyId = "COMP003";
//         ParticipatingCompany company = buildCompany(companyId);

//         Site site = buildSite(20, company, "福岡事務所");
//         site.setSiteProjectRelations(List.of()); // 空のリスト

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.of(company));
//         when(siteRepository.findByCompany(company)).thenReturn(List.of(site));

//         // Act
//         SiteListResponse response = siteService.getSitesByCompanyId(companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo("OK");
//         assertThat(response.getSiteList()).isNotNull();
//         assertThat(response.getSiteList()).hasSize(1);

//         SiteResponse siteResponse = response.getSiteList().get(0);
//         assertThat(siteResponse.getId()).isEqualTo("20");
//         assertThat(siteResponse.getSiteName()).isEqualTo("福岡事務所");
//         assertThat(siteResponse.getProjects()).isNotNull();
//         assertThat(siteResponse.getProjects()).isEmpty();
//         assertThat(siteResponse.getData()).isNull();

//         // Verify interactions
//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//         verify(siteRepository, times(1)).findByCompany(company);
//     }

//     /**
//      * S1-04: 企業が見つからない場合
//      *
//      * 前提条件:
//      * - findByCompanyId() が Optional.empty() を返す
//      *
//      * 期待結果:
//      * - statusCode = ResponseConstants.NOT_FOUND
//      * - message = ResponseConstants.MSG_SITE_NOT_FOUND
//      * - siteList = null
//      * - siteRepository.findByCompany() は呼ばれない
//      */
//     @Test
//     @DisplayName("UT-SS-104 企業が見つからない場合、NOT_FOUNDステータスを返す")
//     void getSitesByCompanyId_companyNotFound_returnsNotFound() {
//         // Arrange
//         String companyId = "COMP999";

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.empty());

//         // Act
//         SiteListResponse response = siteService.getSitesByCompanyId(companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
//         assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
//         assertThat(response.getSiteList()).isNull();

//         // Verify interactions
//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//         verify(siteRepository, never()).findByCompany(any(ParticipatingCompany.class));
//     }

//     /**
//      * S1-05: 企業は存在するが拠点が見つからない場合
//      *
//      * 前提条件:
//      * - 企業が存在する
//      * - findByCompany() が空のリストを返す
//      *
//      * 期待結果:
//      * - statusCode = ResponseConstants.NOT_FOUND
//      * - message = ResponseConstants.MSG_SITE_NOT_FOUND
//      * - siteList = null
//      */
//     @Test
//     @DisplayName("UT-SS-105 企業は存在するが拠点が見つからない場合、NOT_FOUNDステータスを返す")
//     void getSitesByCompanyId_companyExistsButNoSites_returnsNotFound() {
//         // Arrange
//         String companyId = "COMP004";
//         ParticipatingCompany company = buildCompany(companyId);

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.of(company));
//         when(siteRepository.findByCompany(company)).thenReturn(List.of());

//         // Act
//         SiteListResponse response = siteService.getSitesByCompanyId(companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
//         assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
//         assertThat(response.getSiteList()).isNull();

//         // Verify interactions
//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//         verify(siteRepository, times(1)).findByCompany(company);
//     }

//     //METHOD 2 GET/SITE{SITEID}
//     @Test
//         @DisplayName("UT-SS-201 有効なIDで拠点が存在する場合、すべてのフィールドを含む拠点情報を返す")
//         void getSiteById_validId_returnsSiteInfoWithAllFields() {
//             // Arrange
//             int siteId = 1;
//             ParticipatingCompany company = buildCompany("COMP005");
//             Site site = buildSite(siteId, company, "東京本社");
//             site.setBuildingName("オフィスビルA 10階");

//             // プロジェクトリレーションを設定
//             Project project1 = buildProject("太陽光発電プロジェクトA");
//             Project project2 = buildProject("太陽光発電プロジェクトB");
//             SiteProjectRelation relation1 = buildSiteProjectRelation(site, project1);
//             SiteProjectRelation relation2 = buildSiteProjectRelation(site, project2);
//             site.setSiteProjectRelations(List.of(relation1, relation2));

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));

//             // Act
//             SiteInfoResponse response = siteService.getSiteById(siteId);

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
//             assertThat(response.getMessage()).isNull();
//             assertThat(response.getSite()).isNotNull();

//             SiteResponse siteResponse = response.getSite();
//             assertThat(siteResponse.getId()).isEqualTo("1");
//             assertThat(siteResponse.getSiteName()).isEqualTo("東京本社");
//             assertThat(siteResponse.getPostalCode()).isEqualTo("100-0001");
//             assertThat(siteResponse.getPrefecture()).isEqualTo("東京都");
//             assertThat(siteResponse.getMunicipality()).isEqualTo("千代田区");
//             assertThat(siteResponse.getBlocknum()).isEqualTo("1-1");
//             assertThat(siteResponse.getBuildingName()).isEqualTo("オフィスビルA 10階");
//             assertThat(siteResponse.getPicFirstName()).isEqualTo("太郎");
//             assertThat(siteResponse.getPicLastName()).isEqualTo("山田");

//             // プロジェクトの検証
//             assertThat(siteResponse.getProjects()).isNotNull();
//             assertThat(siteResponse.getProjects()).hasSize(2);
//             assertThat(siteResponse.getProjects()).containsExactly("太陽光発電プロジェクトA", "太陽光発電プロジェクトB");

//             // データの検証
//             assertThat(siteResponse.getData()).isNotNull();
//             SiteDataResponse data = siteResponse.getData();
//             assertThat(data.getJCredit()).isEqualTo("1");
//             assertThat(data.getSumEmissionReduction()).isEqualTo("0.5");
//             assertThat(data.getSumEmission()).isEqualTo("1.5");

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);}

//         /**
//          * S2-02: プロジェクトリレーションがnullの場合
//          *
//          * 前提条件:
//          * - 拠点が存在する
//          * - siteProjectRelations = null
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.OK
//          * - projects = 空のリスト
//          * - その他のフィールドは正常に設定される
//          */
//     @Test
//     @DisplayName("UT-SS-202 拠点のプロジェクトリレーションがnullの場合、空のプロジェクトリストを返す")
//     void getSiteById_siteWithNullProjectRelations_returnsEmptyProjects() {
//         // Arrange
//         int siteId = 5;
//         Site site = buildSite(siteId, null, null);
//         site.setSiteProjectRelations(null); // null設定

//         when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));

//         // Act
//         SiteInfoResponse response = siteService.getSiteById(siteId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
//         assertThat(response.getMessage()).isNull();
//         assertThat(response.getSite()).isNotNull();

//         SiteResponse siteResponse = response.getSite();
//         assertThat(siteResponse.getId()).isEqualTo("5");
//         assertThat(siteResponse.getSiteName()).isEqualTo("東京本社");

//         // プロジェクトリストが空であることを確認
//         assertThat(siteResponse.getProjects()).isNotNull();
//         assertThat(siteResponse.getProjects()).isEmpty();

//         // データは存在することを確認
//         assertThat(siteResponse.getData()).isNotNull();

//         // Verify interactions
//         verify(siteRepository, times(1)).findById(siteId);
//     }
//     @Test
//         @DisplayName("UT-SS-203 拠点が見つからない場合、NOT_FOUNDステータスを返す")
//         void getSiteById_siteNotFound_returnsNotFound() {
//             // Arrange
//             int siteId = 999;

//             when(siteRepository.findById(siteId)).thenReturn(Optional.empty());

//             // Act
//             SiteInfoResponse response = siteService.getSiteById(siteId);

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
//             assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
//             assertThat(response.getSite()).isNull();

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//         }

//         /**
//          * S2-04: リポジトリが例外をスローした場合
//          *
//          * 前提条件:
//          * - findById() が RuntimeException をスローする
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.INTERNAL_SERVER_ERROR
//          * - message = ResponseConstants.MSG_INTERNAL_ERROR
//          * - site = null
//          * - 例外がキャッチされ、適切なエラーレスポンスが返される
//          */
//     @Test
//     @DisplayName("UT-SS-204 リポジトリが例外をスローした場合、INTERNAL_SERVER_ERRORステータスを返す")
//     void getSiteById_repositoryThrowsException_returnsInternalServerError() {
//         // Arrange
//         int siteId = 1;

//         when(siteRepository.findById(anyInt()))
//                 .thenThrow(new RuntimeException("DB error"));

//         // Act
//         SiteInfoResponse response = siteService.getSiteById(siteId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
//         assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
//         assertThat(response.getSite()).isNull();

//         // Verify interactions
//         verify(siteRepository, times(1)).findById(siteId);
//     }

// /**
//  * バグテストグループ
//  * 
//  * このグループは既知のバグや改善が必要な箇所を文書化するためのテストです。
//  * テストは意図的に失敗し、開発者が修正するまでそのままにしておきます。
//  */

//     /**
//      * S2-BUG-01: 統計データがハードコードされている問題
//      *
//      * 現在の実装:
//      * - jCredit = "1" (ハードコード)
//      * - sumEmissionReduction = "0.5" (ハードコード)
//      * - sumEmission = "1.5" (ハードコード)
//      *
//      * 期待される実装:
//      * - これらの値はデータベースから取得すべき
//      * - 統計データ専用のリポジトリまたはサービスから取得
//      *
//      * このテストの目的:
//      * - バグを文書化する
//      * - 開発者がこの問題を認識する
//      * - 修正が完了したらテストがパスする
//      *
//      * 修正方法:
//      * 1. SiteStatisticsRepository を作成
//      * 2. getSiteStatistics(siteId) メソッドを実装
//      * 3. ハードコードされた値を削除し、リポジトリから取得
//      *
//      * 注意: このテストは @Disabled を使用せず、意図的に失敗させます
//      */
//     @Test
//     @DisplayName("UT-SS-205【バグ】統計データがハードコードされている - データベースから取得すべき")
//     void getSiteById_statisticData_shouldNotBeHardcoded() {
//         // TODO: 統計データはデータベースから取得する必要があります
//         // 現在の実装では以下の値がハードコードされています:
//         // - data.setJCredit("1")
//         // - data.setSumEmissionReduction("0.5")
//         // - data.setSumEmission("1.5")
//         //
//         // 修正後は SiteStatisticsRepository から動的に取得すること

//         // Arrange
//         int siteId = 1;
//         Site site = buildSite(siteId, null, null);
//         site.setSiteProjectRelations(null);

//         when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));

//         // 期待される動作: データベースから実際の統計データを取得
//         // 例: SiteStatistics stats = siteStatisticsRepository.findBySiteId(siteId);
//         // 期待値（モックデータ）
//         String expectedJCredit = "10.5";           // データベースから取得されるべき値
//         String expectedSumEmissionReduction = "25.3"; // データベースから取得されるべき値
//         String expectedSumEmission = "50.8";       // データベースから取得されるべき値

//         // Act
//         SiteInfoResponse response = siteService.getSiteById(siteId);

//         // Assert - このテストは意図的に失敗します
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
//         assertThat(response.getSite()).isNotNull();
//         assertThat(response.getSite().getData()).isNotNull();

//         SiteDataResponse data = response.getSite().getData();

//         // 以下のアサーションは現在の実装では失敗します（これは意図的です）
//         //ハードコードされた値ではなく、データベースから取得した値であることを期待
//         assertThat(data.getJCredit())
//                 .as("JCreditはデータベースから取得されるべきです（現在はハードコード: '1'）")
//                 .isNotEqualTo("1")// ハードコードされた値ではないことを確認
//                 .isEqualTo(expectedJCredit);// データベースからの期待値

//         assertThat(data.getSumEmissionReduction())
//                 .as("排出削減量はデータベースから取得されるべきです（現在はハードコード: '0.5'）")
//                 .isNotEqualTo("0.5")  // ハードコードされた値ではないことを確認
//                 .isEqualTo(expectedSumEmissionReduction);  // データベースからの期待値

//         assertThat(data.getSumEmission())
//                 .as("排出量合計はデータベースから取得されるべきです（現在はハードコード: '1.5'）")
//                 .isNotEqualTo("1.5")  // ハードコードされた値ではないことを確認
//                 .isEqualTo(expectedSumEmission);  // データベースからの期待値

//         // Note: このテストが失敗している間、以下の作業が必要です:
//         // 1. SiteStatisticsRepository インターフェースを作成
//         // 2. findBySiteId(int siteId) メソッドを実装
//         // 3. SiteServiceImpl に SiteStatisticsRepository を注入
//         // 4. ハードコードされた値を削除し、リポジトリから取得するように変更
//         // 5. このテストが成功することを確認

//         verify(siteRepository, times(1)).findById(siteId);
//     }
// //METHOD 3 CREATE SITE {POST}
// /**
//      * S3-01: 有効なリクエストで拠点を正常に作成
//      *
//      * 前提条件:
//      * - 有効な CreateSiteRequest
//      * - 企業が存在する
//      * - リポジトリの保存が成功する
//      *
//      * 期待結果:
//      * - statusCode = ResponseConstants.OK
//      * - response.id が保存された Site の ID と一致
//      * - message が null
//      * - リポジトリメソッドが正しく呼ばれる
//      */
//     @Test
//     @DisplayName("UT-SS-301 有効なリクエストで拠点作成が成功し、IDとOKステータスを返す")
//     void createSite_validRequest_returnsSiteIdWithOkStatus() {
//         // Arrange
//         String companyId = "COMP001";
//         CreateSiteRequest request = buildCreateSiteRequest();
//         ParticipatingCompany company = buildCompany(companyId);

//         // モック Site を作成（保存後の状態をシミュレート）
//         Site savedSite = Site.builder()
//                 .siteName(request.getSiteName())
//                 .postalCode(request.getPostalCode())
//                 .prefecture(company.getPrefecture())
//                 .municipality(company.getMunicipality())
//                 .company(company)
//                 .blocknum(request.getBlocknum())
//                 .buildingName(request.getBuildingName())
//                 .picFirstName(request.getPicFirstName())
//                 .picFirstNameKana(request.getPicFirstNameKana())
//                 .picLastName(request.getPicLastName())
//                 .picLastNameKana(request.getPicLastNameKana())
//                 .picEmail(request.getPicEmail())
//                 .picPhoneNumber(request.getPicPhoneNumber())
//                 .picPosition(request.getPicPosition())
//                 .build();
//         savedSite.setId(100); // 保存後に付与されるID

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.of(company));
//         when(siteRepository.save(any(Site.class))).thenReturn(savedSite);

//         // Act
//         CreateSiteResponse response = siteService.createSite(request, companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
//         assertThat(response.getMessage()).isNull();
//         assertThat(response.getId()).isEqualTo(100);

//         // Verify interactions
//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//         verify(siteRepository, times(1)).save(any(Site.class));
//     }

//     /**
//      * S3-02: すべてのフィールドが正しくマッピングされることを検証
//      *
//      * 前提条件:
//      * - 有効な CreateSiteRequest（すべてのフィールド設定済み）
//      * - 企業が存在する
//      *
//      * 期待結果:
//      * - ArgumentCaptor で Site エンティティをキャプチャ
//      * - リクエストの各フィールドが Site エンティティに正しくマッピングされている
//      * - company が正しく設定されている
//      * - createdBy が設定されている
//      */
//     @Test
//     @DisplayName("UT-SS-302 有効なリクエストですべてのフィールドが正しくマッピングされることを検証")
//     void createSite_validRequest_verifiesAllFieldsMappedCorrectly() {
//         // Arrange
//         String companyId = "COMP002";
//         CreateSiteRequest request = buildCreateSiteRequest();
//         ParticipatingCompany company = buildCompany(companyId);

//         Site savedSite = Site.builder().build();
//         savedSite.setId(200);

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.of(company));
//         when(siteRepository.save(any(Site.class))).thenReturn(savedSite);

//         // Act
//         CreateSiteResponse response = siteService.createSite(request, companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

//         // ArgumentCaptor で save() に渡された Site をキャプチャ
//         verify(siteRepository).save(siteCaptor.capture());
//         Site capturedSite = siteCaptor.getValue();

//         // リクエストから直接マッピングされるべきフィールドの検証
//         assertThat(capturedSite.getSiteName()).isEqualTo(request.getSiteName());
//         assertThat(capturedSite.getPostalCode()).isEqualTo(request.getPostalCode());
//         assertThat(capturedSite.getBlocknum()).isEqualTo(request.getBlocknum());
//         assertThat(capturedSite.getBuildingName()).isEqualTo(request.getBuildingName());

//         // PIC（担当者）情報の検証
//         assertThat(capturedSite.getPicFirstName()).isEqualTo(request.getPicFirstName());
//         assertThat(capturedSite.getPicFirstNameKana()).isEqualTo(request.getPicFirstNameKana());
//         assertThat(capturedSite.getPicLastName()).isEqualTo(request.getPicLastName());
//         assertThat(capturedSite.getPicLastNameKana()).isEqualTo(request.getPicLastNameKana());
//         assertThat(capturedSite.getPicEmail()).isEqualTo(request.getPicEmail());
//         assertThat(capturedSite.getPicPhoneNumber()).isEqualTo(request.getPicPhoneNumber());
//         assertThat(capturedSite.getPicPosition()).isEqualTo(request.getPicPosition());

//         // Company の関連付けの検証
//         assertThat(capturedSite.getCompany()).isEqualTo(company);

//         // createdBy が設定されていることの検証（値が company.getNameJa() であることは BUG テストで検証）
//         assertThat(capturedSite.getCreatedBy()).isNotNull();

//         // 注意: prefecture と city のマッピングは BUG テストで検証
//         // 現在の実装では company から取得されているため、ここでは検証しない

//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//     }
// /**
//      * S3-03: 企業が見つからない場合
//      *
//      * 前提条件:
//      * - findByCompanyId() が Optional.empty() を返す
//      *
//      * 期待結果:
//      * - statusCode = ResponseConstants.NOT_FOUND
//      * - message = ResponseConstants.MSG_SITE_NOT_FOUND
//      * - id = 0 (デフォルト値)
//      * - siteRepository.save() は呼ばれない
//      */
//     @Test
//     @DisplayName("UT-SS-303 企業が見つからない場合、NOT_FOUNDステータスを返し保存処理を実行しない")
//     void createSite_companyNotFound_returnsNotFound() {
//         // Arrange
//         String companyId = "COMP999";
//         CreateSiteRequest request = buildCreateSiteRequest();

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.empty());

//         // Act
//         CreateSiteResponse response = siteService.createSite(request, companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
//         assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
//         assertThat(response.getId()).isEqualTo(0); // デフォルト値

//         // Verify interactions
//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//         verify(siteRepository, never()).save(any(Site.class));
//     }

//     /**
//      * S3-04: リポジトリが例外をスローした場合
//      *
//      * 前提条件:
//      * - 企業が存在する
//      * - siteRepository.save() が RuntimeException をスローする
//      *
//      * 期待結果:
//      * - statusCode = ResponseConstants.INTERNAL_SERVER_ERROR
//      * - message = ResponseConstants.MSG_INTERNAL_ERROR
//      * - id = 0 (例外発生のため設定されない)
//      * - 例外がキャッチされ、適切なエラーレスポンスが返される
//      */
//     @Test
//     @DisplayName("UT-SS-304 リポジトリが例外をスローした場合、INTERNAL_SERVER_ERRORステータスを返す")
//     void createSite_repositoryThrowsException_returnsInternalServerError() {
//         // Arrange
//         String companyId = "COMP003";
//         CreateSiteRequest request = buildCreateSiteRequest();
//         ParticipatingCompany company = buildCompany(companyId);

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.of(company));
//         when(siteRepository.save(any(Site.class)))
//                 .thenThrow(new RuntimeException("DB connection error"));

//         // Act
//         CreateSiteResponse response = siteService.createSite(request, companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
//         assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
//         assertThat(response.getId()).isEqualTo(0); // 例外のため ID は設定されない

//         // Verify interactions
//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//         verify(siteRepository, times(1)).save(any(Site.class));
//     }

// /**
//  * バグテストグループ
//  * 
//  * このグループは既知のバグを文書化するためのテストです。
//  * テストは意図的に失敗し、開発者が修正するまでそのままにしておきます。
//  */

//     /**
//      * S3-BUG-01: 都道府県と市区町村が company から取得されている問題
//      *
//      * 現在の実装:
//      * - site.prefecture = company.getPrefecture()← BUG
//      * - site.city = company.getMunicipality()              ← BUG
//      *
//      * 期待される実装:
//      * - site.prefecture = request.getPrefecture()
//      * - site.city = request.getMunicipalityName()
//      *
//      * 問題点:
//      * - リクエストで指定された都道府県・市区町村が無視される
//      * - 企業の本社所在地が常に使用されてしまう
//      * - 企業が複数の都道府県に拠点を持つ場合に正しく動作しない
//      *
//      * 修正方法:
//      * Site.builder() で以下のように変更:
//      * .prefecture(request.getPrefecture())
//      * .city(request.getMunicipalityName())
//      *
//      * このテストの目的:
//      * - バグを文書化する
//      * - 開発者がこの問題を認識する
//      * - 修正が完了したらテストがパスする
//      */
//     @Test
//     @DisplayName("UT-SS-305【バグ】都道府県と市区町村はリクエストから取得すべき（現在は company から取得している）")
//     void createSite_prefectureAndCity_shouldComeFromRequest_notFromCompany() {
//         // TODO [BUG]: 都道府県と市区町村はリクエストから取得する必要があります
//         // 現在の実装:
//         //.prefecture(company.getPrefecture())
//         //   .city(company.getMunicipality())
//         // 
//         // 正しい実装:
//         //   .prefecture(request.getPrefecture())
//         //   .city(request.getMunicipalityName())
//         //
//         // この問題により、企業の本社所在地が常に使用され、
//         // リクエストで指定された拠点の実際の所在地が無視されます。

//         // Arrange
//         String companyId = "COMP004";
//         CreateSiteRequest request = buildCreateSiteRequest();
        
//         // リクエストには大阪の住所を設定
//         request.setPrefecture("大阪府");
//         request.setMunicipality("大阪市");
        
//         // 企業の本社は東京
//         ParticipatingCompany company = buildCompany(companyId);
//         company.setPrefecture("東京都");
//         company.setMunicipality("千代田区");

//         Site savedSite = Site.builder().build();
//         savedSite.setId(300);

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.of(company));
//         when(siteRepository.save(any(Site.class))).thenReturn(savedSite);

//         // Act
//         CreateSiteResponse response = siteService.createSite(request, companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

//         // ArgumentCaptor で save() に渡された Site をキャプチャ
//         verify(siteRepository).save(siteCaptor.capture());
//         Site capturedSite = siteCaptor.getValue();

//         // 以下のアサーションは現在の実装では失敗します（これは意図的です）
//         // prefecture と city はリクエストから取得されるべき
//         assertThat(capturedSite.getPrefecture())
//                 .as("都道府県はリクエスト（大阪府）から取得されるべきですが、" +
//                     "現在は company の都道府県（東京都）が使用されています")
//                 .isNotEqualTo(company.getPrefecture())// company の値ではないことを確認
//                 .isEqualTo(request.getPrefecture());       // request の値であることを確認

//         assertThat(capturedSite.getMunicipality())
//                 .as("市区町村はリクエスト（大阪市）から取得されるべきですが、" +
//                     "現在は company の市区町村（千代田区）が使用されています")
//                 .isNotEqualTo(company.getMunicipality())        // company の値ではないことを確認
//                 .isEqualTo(request.getMunicipality());       // request の値であることを確認

//         // Note: このテストが失敗している間、以下の修正が必要です:
//         // Site.builder() の中で:
//         //   .prefecture(company.getPrefecture()) → .prefecture(request.getPrefecture())
//         //   .city(company.getMunicipality()) → .city(request.getMunicipalityName())

//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//     }

//     /**
//      * S3-BUG-02: createdBy がログインユーザーではなく company名から設定されている問題
//      *
//      * 現在の実装:
//      * - site.setCreatedBy(company.getNameJa())← BUG
//      *
//      * 期待される実装:
//      * - site.setCreatedBy(loggedInUsername) または
//      * - site.setCreatedBy(SecurityContextHolder.getContext().getAuthentication().getName())
//      *
//      * 問題点:
//      * - 監査ログとして不正確
//      * - 誰が拠点を作成したかが追跡できない
//      * - 企業名が表示されるだけで、実際の操作者が不明
//      *
//      * 修正方法:
//      *1. メソッドシグネチャに String username パラメータを追加
//         *    createSite(CreateSiteRequest request, String companyId, String username)
//         * 2. controller で SecurityContext から username を取得して渡す
//         * 3. site.setCreatedBy(username) に変更
//         *
//         * このテストの目的:
//         * - バグを文書化する
//         * - セキュリティとコンプライアンスの観点から重要な問題であることを示す
//         * - 修正が完了したらテストがパスする
//         */
//     @Test
//     @DisplayName("UT-SS-306【バグ】createdBy はログインユーザーから設定すべき（現在は company 名が使用されている）")
//     void createSite_createdBy_shouldComeFromLoggedInUser_notFromCompanyName() {
//         // TODO [BUG]: createdByはログインユーザーから設定する必要があります
//         // 現在の実装:
//         //   site.setCreatedBy(company.getNameJa())
//         // 
//         // 正しい実装:
//         //   site.setCreatedBy(loggedInUsername)
//         //   または
//         //   site.setCreatedBy(SecurityContextHolder.getContext()
//         //                     .getAuthentication().getName())
//         //
//         // この問題により、監査ログが不正確になり、
//         // 誰が拠点を作成したかを追跡できません。

//         // Arrange
//         String companyId = "COMP005";
//         CreateSiteRequest request = buildCreateSiteRequest();
//         ParticipatingCompany company = buildCompany(companyId);
//         company.setNameJa("テスト株式会社");

//         // 期待される動作:ログインユーザー名が設定される
//         String expectedLoggedInUsername = "yamada.taro@example.com";

//         Site savedSite = Site.builder().build();
//         savedSite.setId(400);

//         when(participatingCompanyRepository.findByCompanyId(companyId))
//                 .thenReturn(Optional.of(company));
//         when(siteRepository.save(any(Site.class))).thenReturn(savedSite);

//         // Act
//         CreateSiteResponse response = siteService.createSite(request, companyId);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

//         // ArgumentCaptor で save() に渡された Site をキャプチャ
//         verify(siteRepository).save(siteCaptor.capture());
//         Site capturedSite = siteCaptor.getValue();

//         // 以下のアサーションは現在の実装では失敗します（これは意図的です）
//         // createdBy はログインユーザー名であるべき
//         assertThat(capturedSite.getCreatedBy())
//                 .as("createdBy はログインユーザー名（yamada.taro@example.com）であるべきですが、" +
//                     "現在は company名（テスト株式会社）が使用されています。" +
//                     "これは監査ログとして不正確であり、誰が拠点を作成したかを追跡できません。")
//                 .isNotEqualTo(company.getNameJa())// company 名ではないことを確認
//                 .isEqualTo(expectedLoggedInUsername);        // ログインユーザー名であることを確認

//         // Note: このテストが失敗している間、以下の修正が必要です:
//         //1. メソッドシグネチャを変更:
//         //    createSite(CreateSiteRequest request, String companyId, String username)
//         // 2. Controller で SecurityContext から username を取得
//         // 3. site.setCreatedBy(company.getNameJa()) → site.setCreatedBy(username)
//         //
//         // または、Service内で直接 SecurityContext から取得:
//         // String username = SecurityContextHolder.getContext()
//         //.getAuthentication().getName();
//         // site.setCreatedBy(username);

//         verify(participatingCompanyRepository, times(1)).findByCompanyId(companyId);
//     }

// // METHOD 4 UPDATE SITE {PATCH}
//     @Test
//     @DisplayName("UT-SS-401 すべての住所フィールドが提供された場合、全てのフィールドを更新する")
//     void updateSite_allFieldsProvided_updatesAllAddressFields() {
//         // Arrange
//         String siteId = "1";
//         String companyId = "COMP001";
//         Site existingSite = buildSiteWithCompany(1, companyId);
        
//         //初期値の設定
//         existingSite.setPostalCode("100-0001");
//         existingSite.setPrefecture("東京都");
//         existingSite.setMunicipality("千代田区");
//         existingSite.setBlocknum("1-1");
//         existingSite.setBuildingName("旧ビルA");

//         UpdateSiteRequest request = buildUpdateSiteRequest(
//                 "123-4567",    // postalCode
//                 "大阪府",       // Prefecture
//                 "大阪市",       // Municipality
//                 "北区",         // townName
//                 "中之島3-3-3", // blockName
//                 "新ビルB5階"  // address
//         );

//         Site updatedSite = buildSiteWithCompany(1, companyId);
//         updatedSite.setPostalCode(request.getPostalCode());
//         updatedSite.setPrefecture(request.getPrefecture());
//         updatedSite.setMunicipality(request.getMunicipality());
//         updatedSite.setBlocknum(request.getBlocknum());
//         updatedSite.setBuildingName(request.getBuildingName());
//         updatedSite.setUpdatedAt(LocalDateTime.now());

//         when(siteRepository.findById(1)).thenReturn(Optional.of(existingSite));
//         when(siteRepository.save(any(Site.class))).thenReturn(updatedSite);

//         // Act
//         SiteInfoResponse response = siteService.updateSite(siteId, companyId, request);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
//         assertThat(response.getMessage()).isNull();
//         assertThat(response.getSite()).isNotNull();

//         SiteResponse siteResponse = response.getSite();
//         assertThat(siteResponse.getPostalCode()).isEqualTo("123-4567");
//         assertThat(siteResponse.getPrefecture()).isEqualTo("大阪府");
//         assertThat(siteResponse.getMunicipality()).isEqualTo("大阪市");
//         assertThat(siteResponse.getBlocknum()).isEqualTo("中之島3-3-3");
//         assertThat(siteResponse.getBuildingName()).isEqualTo("新ビルB5階");

//         // save() に渡された Site をキャプチャして updatedAt を検証
//         verify(siteRepository).save(siteCaptor.capture());
//         Site capturedSite = siteCaptor.getValue();
//         assertThat(capturedSite.getUpdatedAt()).isNotNull();

//         verify(siteRepository, times(1)).findById(1);
//         verify(siteRepository, times(1)).save(any(Site.class));
//     }

//     /**
//      * S4-02: postalCode のみ提供された場合、そのフィールドだけ更新
//      *
//      * 前提条件:
//      * - 拠点が存在する
//      * - companyId が一致する
//      * - リクエストに postalCode のみ設定（他はblank/null）
//      *
//      * 期待結果:
//      * - statusCode = ResponseConstants.OK
//      * - postalCode のみ更新される
//      * - 他の5フィールド（prefecture, city, town, blocknum, address）は
//      *   元の値のまま変更されない
//      */
//     @Test
//     @DisplayName("UT-SS-402 郵便番号のみ提供された場合、郵便番号だけを更新する")
//     void updateSite_onlyPostalCodeProvided_updatesOnlyPostalCode() {
//         // Arrange
//         String siteId = "2";
//         String companyId = "COMP002";
//         Site existingSite = buildSiteWithCompany(2, companyId);
        
//         // 初期値の設定
//         existingSite.setPostalCode("100-0001");
//         existingSite.setPrefecture("東京都");
//         existingSite.setMunicipality("千代田区");
//         existingSite.setBlocknum("1-1");
//         existingSite.setBuildingName("オリジナルビル");

//         // postalCode のみ設定、他は blank/null
//         UpdateSiteRequest request = buildUpdateSiteRequest(
//                 "123-4567",  // postalCode
//                 null,        // Prefecture
//                 "",          // Municipality (blank)
//                 null,        // townName
//                 "",          // blockName (blank)
//                 null         // address
//         );

//         Site updatedSite = buildSiteWithCompany(2, companyId);
//         updatedSite.setPostalCode("123-4567");
//         updatedSite.setPrefecture("東京都");      // 変更なし
//         updatedSite.setMunicipality("千代田区");          // 変更なし         // 変更なし
//         updatedSite.setBlocknum("1-1");          // 変更なし
//         updatedSite.setBuildingName("オリジナルビル"); // 変更なし
//         updatedSite.setUpdatedAt(LocalDateTime.now());

//         when(siteRepository.findById(2)).thenReturn(Optional.of(existingSite));
//         when(siteRepository.save(any(Site.class))).thenReturn(updatedSite);

//         // Act
//         SiteInfoResponse response = siteService.updateSite(siteId, companyId, request);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

//         // save() に渡された Site をキャプチャ
//         verify(siteRepository).save(siteCaptor.capture());
//         Site capturedSite = siteCaptor.getValue();

//         // postalCode が更新されていることを確認
//         assertThat(capturedSite.getPostalCode()).isEqualTo("123-4567");

//         // 他のフィールドは元の値のまま
//         assertThat(capturedSite.getPrefecture()).isEqualTo("東京都");
//         assertThat(capturedSite.getMunicipality()).isEqualTo("千代田区");
//         assertThat(capturedSite.getBlocknum()).isEqualTo("1-1");
//         assertThat(capturedSite.getBuildingName()).isEqualTo("オリジナルビル");

//         assertThat(capturedSite.getUpdatedAt()).isNotNull();

//         verify(siteRepository, times(1)).findById(2);
//         verify(siteRepository, times(1)).save(any(Site.class));
//     }

//     /**
//      * S4-03: 一部のフィールドのみ提供された場合、該当フィールドだけ更新
//      *
//      * 前提条件:
//      * - 拠点が存在する
//      * - companyId が一致する
//      * - リクエストに postalCode と Municipality のみ設定
//      *
//      * 期待結果:
//      * - statusCode = ResponseConstants.OK
//      * - postalCode と city のみ更新される
//      * - 他の4フィールドは元の値のまま
//      */
//     @Test
//     @DisplayName("UT-SS-403 一部のフィールドのみ提供された場合、該当フィールドだけを更新する")
//     void updateSite_partialFields_updatesOnlyNonBlankFields() {
//         // Arrange
//         String siteId = "3";
//         String companyId = "COMP003";
//         Site existingSite = buildSiteWithCompany(3, companyId);
        
//         // 初期値の設定
//         existingSite.setPostalCode("100-0001");
//         existingSite.setPrefecture("東京都");
//         existingSite.setMunicipality("渋谷区");
//         existingSite.setBlocknum("2-2-2");
//         existingSite.setBuildingName("元のアドレス");

//         // postalCode と Municipality のみ設定
//         UpdateSiteRequest request = buildUpdateSiteRequest(
//                 "999-8888",  // postalCode
//                 null,        // Prefecture
//                 "新宿区",     // Municipality
//                 "",          // townName (blank)
//                 null,        // blockName
//                 ""           // address (blank)
//         );

//         Site updatedSite = buildSiteWithCompany(3, companyId);
//         updatedSite.setPostalCode("999-8888");
//         updatedSite.setPrefecture("東京都");    // 変更なし
//         updatedSite.setMunicipality("新宿区");          // 更新される
//         updatedSite.setBlocknum("2-2-2");      // 変更なし
//         updatedSite.setBuildingName("元のアドレス"); // 変更なし
//         updatedSite.setUpdatedAt(LocalDateTime.now());

//         when(siteRepository.findById(3)).thenReturn(Optional.of(existingSite));
//         when(siteRepository.save(any(Site.class))).thenReturn(updatedSite);

//         // Act
//         SiteInfoResponse response = siteService.updateSite(siteId, companyId, request);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

//         // save() に渡された Site をキャプチャ
//         verify(siteRepository).save(siteCaptor.capture());
//         Site capturedSite = siteCaptor.getValue();

//         // 更新されたフィールド
//         assertThat(capturedSite.getPostalCode()).isEqualTo("999-8888");
//         assertThat(capturedSite.getMunicipality()).isEqualTo("新宿区");

//         // 変更されていないフィールド
//         assertThat(capturedSite.getPrefecture()).isEqualTo("東京都");
//         assertThat(capturedSite.getBlocknum()).isEqualTo("2-2-2");
//         assertThat(capturedSite.getBuildingName()).isEqualTo("元のアドレス");

//         assertThat(capturedSite.getUpdatedAt()).isNotNull();

//         verify(siteRepository, times(1)).findById(3);
//         verify(siteRepository, times(1)).save(any(Site.class));
//     }

//     /**
//      * S4-04: 拠点が見つからない場合
//      *
//      * 前提条件:
//      * - findById() が Optional.empty() を返す
//      *
//      * 期待結果:
//      * - statusCode = ResponseConstants.NOT_FOUND
//      * - message = ResponseConstants.MSG_NOT_FOUND
//      * - site = null
//      * - siteRepository.save() は呼ばれない
//      */
//     @Test
//     @DisplayName("UT-SS-404 拠点が見つからない場合、NOT_FOUNDステータスを返す")
//     void updateSite_siteNotFound_returnsNotFound() {
//         // Arrange
//         String siteId = "999";
//         String companyId = "COMP999";
//         UpdateSiteRequest request = buildUpdateSiteRequest(
//                 "123-4567", "大阪府", "大阪市", "北区", "1-1-1", "ビルA"
//         );

//         when(siteRepository.findById(999)).thenReturn(Optional.empty());

//         // Act
//         SiteInfoResponse response = siteService.updateSite(siteId, companyId, request);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
//         assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_NOT_FOUND);
//         assertThat(response.getSite()).isNull();

//         // Verify interactions
//         verify(siteRepository, times(1)).findById(999);
//         verify(siteRepository, never()).save(any(Site.class));
//     }

//     /**
//      * S4-05: 拠点が異なる企業に属している場合
//      *
//      * 前提条件:
//      * - 拠点が存在する
//      * - site.company.companyId != companyId（権限なし）
//      *
//      * 期待結果:
//      * - statusCode = ResponseConstants.FORBIDDEN
//      * - message = ResponseConstants.MSG_FORBIDDEN
//      * - site = null
//      * - siteRepository.save() は呼ばれない
//      */
//     @Test
//     @DisplayName("UT-SS-405 拠点が異なる企業に属している場合、FORBIDDENステータスを返す")
//     void updateSite_siteFromDifferentCompany_returnsForbidden() {
//         // Arrange
//         String siteId = "10";
//         String requestCompanyId = "COMP001";
//         String actualCompanyId = "COMP002"; // 異なる企業
//         Site existingSite = buildSiteWithCompany(10, actualCompanyId);
//         UpdateSiteRequest request = buildUpdateSiteRequest(
//                 "123-4567", "大阪府", "大阪市", "北区", "1-1-1", "ビルA"
//         );

//         when(siteRepository.findById(10)).thenReturn(Optional.of(existingSite));

//         // Act
//         SiteInfoResponse response = siteService.updateSite(siteId, requestCompanyId, request);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
//         assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
//         assertThat(response.getSite()).isNull();

//         // Verify interactions
//         verify(siteRepository, times(1)).findById(10);
//         verify(siteRepository, never()).save(any(Site.class));
//     }

//     /**
//      * S4-06: 数値でない siteId が渡された場合
//      *
//      * 前提条件:
//      * - siteId = "invalid-id"（数値に変換できない文字列）
//      *
//      * 期待結果:
//      * - NumberFormatException がスローされる
//      *
//      * 注意:
//      * この例外は現在ハンドリングされていません。
//      * 本来は Controller または Service レイヤーで catch して
//      * BAD_REQUEST レスポンスを返すべきです。
//      * このテストは現在の動作を文書化するためのものです。
//      */
//     @Test
//     @DisplayName("UT-SS-406 数値でないsiteIDが渡された場合、NumberFormatExceptionをスローする")
//     void updateSite_nonNumericSiteId_throwsNumberFormatException() {
//         // Arrange
//         String invalidSiteId = "invalid-id";
//         String companyId = "COMP001";
//         UpdateSiteRequest request = buildUpdateSiteRequest(
//                 "123-4567", "大阪府", "大阪市", "北区", "1-1-1", "ビルA"
//         );

//         // Act & Assert
//         assertThatThrownBy(() -> siteService.updateSite(invalidSiteId, companyId, request))
//                 .isInstanceOf(NumberFormatException.class)
//                 .hasMessageContaining("invalid-id");

//         // Note: この例外は現在ハンドリングされていません
//         // 期待される動作:
//         // 1. Controller または Service で catch する
//         // 2. BAD_REQUEST ステータスを返す
//         // 3. 適切なエラーメッセージを設定する
//         //
//         // 修正例:
//         // try {
//         //     int id = Integer.parseInt(siteId);
//         //     ...
//         // } catch (NumberFormatException e) {
//         //     response.setStatusCode(ResponseConstants.BAD_REQUEST);
//         //     response.setMessage("Invalid site ID format");
//         //     return response;
//         // }

//         // repository は呼ばれないことを確認
//         verify(siteRepository, never()).findById(anyInt());
//         verify(siteRepository, never()).save(any(Site.class));
//     }

// /**
//  * バグテストグループ
//  * 
//  * このグループは既知のバグを文書化するためのテストです。
//  * テストは意図的に失敗し、開発者が修正するまでそのままにしておきます。
//  */
// /**
//      * S4-BUG-01: サイト名とサイト名カナが更新できない問題
//      *
//      * 現在の実装:
//      * - private updateSite() メソッドに siteName と siteNameKana の
//      *   更新処理が実装されていない
//      *
//      * 問題点:
//      * - CreateSiteRequest には siteName と siteNameKana が含まれている
//      * - UpdateSiteRequest にも同じフィールドが定義されている
//      * - しかし、updateSite() では住所フィールドのみ更新可能
//      * - ユーザーは拠点名を変更できない
//      *
//      * 期待される実装:
//      * private updateSite() に以下を追加:
//      * if (StringUtils.isNotBlank(request.getSiteName())) {
//      *     site.setSiteName(request.getSiteName());
//      * }
//      * if (StringUtils.isNotBlank(request.getSiteNameKana())) {
//      *     site.setSiteNameKana(request.getSiteNameKana());
//      * }
//      *
//      * このテストの目的:
//      * - バグを文書化する
//      * - 開発者がこの問題を認識する
//      * - 修正が完了したらテストがパスする
//      */
//     @Test
//     @DisplayName("UT-SS-407【バグ】サイト名とサイト名カナの更新が実装されていない")
//     void updateSite_siteNameAndSiteNameKana_shouldBeUpdatable() {
//         // TODO [BUG]: サイト名とサイト名カナの更新が実装されていません
//         // 
//         // 現在の private updateSite() メソッドには以下の処理がありません:
//         // - site.setSiteName(request.getSiteName())
//         // - site.setSiteNameKana(request.getSiteNameKana())
//         //
//         // この問題により、ユーザーは拠点の名前を変更できません。
//         // CreateSiteRequest では設定できるのに、UpdateSiteRequest では
//         // 更新できないという一貫性のない動作になっています。

//         // Arrange
//         String siteId = "5";
//         String companyId = "COMP005";
//         Site existingSite = buildSiteWithCompany(5, companyId);
        
//         // 初期値の設定
//         existingSite.setSiteName("旧サイト名");
//         //existingSite.setSiteNameKana("キュウサイトメイ");
//         existingSite.setPostalCode("100-0001");
//         existingSite.setPrefecture("東京都");
//         existingSite.setMunicipality("千代田区");
//         existingSite.setBlocknum("1-1");
//         existingSite.setBuildingName("旧ビル");

//         UpdateSiteRequest request = buildUpdateSiteRequest(
//                 "123-4567",
//                 "大阪府",
//                 "大阪市",
//                 "北区",
//                 "2-2-2",
//                 "新ビル"
//         );
//         // サイト名の更新をリクエスト
//         // request.setSiteName("新サイト名");
//         // request.setSiteNameKana("シンサイトメイ");

//         Site updatedSite = buildSiteWithCompany(5, companyId);
//         // 現在の実装では siteName と siteNameKana は更新されない
//         updatedSite.setSiteName("旧サイト名");        // 更新されない（バグ）
//         // updatedSite.setSiteNameKana("キュウサイトメイ"); // 更新されない（バグ）
//         updatedSite.setPostalCode("123-4567");
//         updatedSite.setPrefecture("大阪府");
//         updatedSite.setMunicipality("大阪市");
//         updatedSite.setBlocknum("2-2-2");
//         updatedSite.setBuildingName("新ビル");
//         updatedSite.setUpdatedAt(LocalDateTime.now());

//         when(siteRepository.findById(5)).thenReturn(Optional.of(existingSite));
//         when(siteRepository.save(any(Site.class))).thenReturn(updatedSite);

//         // Act
//         SiteInfoResponse response = siteService.updateSite(siteId, companyId, request);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

//         // save() に渡された Site をキャプチャ
//         verify(siteRepository).save(siteCaptor.capture());
//         Site capturedSite = siteCaptor.getValue();

//         // 以下のアサーションは現在の実装では失敗します（これは意図的です）
//         // siteName が更新されるべき
//         assertThat(capturedSite.getSiteName()).as("サイト名はリクエスト（新サイト名）に更新されるべきですが、" +
//                     "現在は更新されず旧サイト名のままです。" +
//                     "private updateSite() に setSiteName() の処理が実装されていません。")
//                 .isNotEqualTo("旧サイト名")// 旧値ではないことを確認
//                 .isEqualTo("新サイト名");      // 新値であることを確認

//         // // siteNameKana が更新されるべき
//         // assertThat(capturedSite.getSiteNameKana())
//         //         .as("サイト名カナはリクエスト（シンサイトメイ）に更新されるべきですが、" +
//         //             "現在は更新されずキュウサイトメイのままです。" +
//         //             "private updateSite() に setSiteNameKana() の処理が実装されていません。")
//         //         .isNotEqualTo("キュウサイトメイ")  // 旧値ではないことを確認
//         //         .isEqualTo("シンサイトメイ");      // 新値であることを確認

//         // Note: このテストが失敗している間、以下の修正が必要です:// private updateSite(Site site, UpdateSiteRequest request) に追加:
//         //
//         // if (StringUtils.isNotBlank(request.getSiteName())) {
//         //     site.setSiteName(request.getSiteName());
//         // }
//         // if (StringUtils.isNotBlank(request.getSiteNameKana())) {
//         //     site.setSiteNameKana(request.getSiteNameKana());
//         // }
//         //
//         // この修正により、CreateSiteRequest と UpdateSiteRequest の
//         // 一貫性が保たれ、ユーザーは拠点名を変更できるようになります。

//         verify(siteRepository, times(1)).findById(5);
//         verify(siteRepository, times(1)).save(any(Site.class));
//     }
// //METHOD 5 GET SITE PROJECT EQUIPMENT
// /**
//          * S5-01: MASTER ロールで設備データを取得
//          *
//          * 前提条件:
//          * - role = MASTER
//          * - すべての必要なエンティティが存在する
//          * - companyId が一致する
//          * - memberData と equipmentFiles が存在する
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.OK
//          * - equipments のサイズが 1
//          * - equipmentId が memberData の ID と一致
//          * - 設備情報がすべて設定されている
//          */
//         @Test
//         @DisplayName("UT-SS-501 MASTERロールで全データが存在する場合、設備情報を返す")
//         void getSiteProjectEquipment_masterRole_returnsEquipmentData() {
//             // Arrange
//             Integer siteId = 1;
//             Integer projectId = 10;
//             String companyId = "COMP001";
//             String role = RoleConstants.MASTER;
//             String username = "master@example.com";

//             // モックの設定
//             Site site = buildSiteWithCompany(siteId, companyId);
//             Project project = buildProject(projectId);
//             SiteProjectRelation relation = buildSiteProjectRelation(siteId, projectId);
//             SiteR002MemberData memberData = buildMemberData(100);
//             List<SiteR002EquipmentFile> equipmentFiles = List.of(buildEquipmentFile(100, "s3://bucket/file1.pdf"),
//                     buildEquipmentFile(100, "s3://bucket/file2.pdf")
//             );

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
//             when(siteProjectRelationRepository.findByProjectIdAndSiteId(projectId, siteId))
//                     .thenReturn(relation);
//             when(siteR002MemberDataRepository.findByProject_IdAndSite_Id(projectId, siteId))
//                     .thenReturn(Optional.of(memberData));
//             when(siteR002EquipmentFileRepository.findByMemberData_Id(memberData.getId()))
//                     .thenReturn(equipmentFiles);

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
//             assertThat(response.getMessage()).isNull();
//             assertThat(response.getEquipments()).isNotNull();
//             assertThat(response.getEquipments()).hasSize(1);EquipmentItemSolarResponse equipment = response.getEquipments().get(0);
//             assertThat(equipment.getEquipmentId()).isEqualTo(100);
//             assertThat(equipment.getPcsManufacturer()).isEqualTo("パナソニック");
//             assertThat(equipment.getPcsModel()).isEqualTo("VBPC255A");
//             assertThat(equipment.getPowerCondUnitCount()).isEqualTo(5);
//             assertThat(equipment.getDeviceNo()).isEqualTo("DEV-001");

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(siteProjectRelationRepository, times(1)).findByProjectIdAndSiteId(projectId, siteId);
//             verify(siteR002MemberDataRepository, times(1)).findByProject_IdAndSite_Id(projectId, siteId);
//             verify(siteR002EquipmentFileRepository, times(1)).findByMemberData_Id(memberData.getId());
            
//             // MASTER ロールなので user リポジトリは呼ばれない
//             verify(participatingUserRepository, never()).findByUsernameAndCompany_Id(username, projectId);
//         }

//         /**
//          * S5-02: USER ロールで割り当てられた拠点の設備データを取得
//          *
//          * 前提条件:
//          * - role = USER
//          * - ユーザーがデータベースに存在する
//          * - user.site.id が siteId と一致する
//          * - すべての必要なエンティティが存在する
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.OK
//          * -設備データが返される
//          * - hasAccessToSite() が true を返す
//          */
//         @Test
//         @DisplayName("UT-SS-502 USERロールで割り当てられた拠点の場合、設備情報を返す")
//         void getSiteProjectEquipment_userRoleWithAssignedSite_returnsEquipmentData() {
//             // Arrange
//             Integer siteId = 2;
//             Integer projectId = 20;
//             String companyId = "COMP002";
//             String role = RoleConstants.USER;
//             String username = "user@example.com";

//             // モックの設定
//             Site site = buildSiteWithCompany(siteId, companyId);
//             Project project = buildProject(projectId);
//             SiteProjectRelation relation = buildSiteProjectRelation(siteId, projectId);
//             SiteR002MemberData memberData = buildMemberData(200);
//             List<SiteR002EquipmentFile> equipmentFiles = List.of(
//                     buildEquipmentFile(200, "s3://bucket/equipment.pdf")
//             );
//             // USER ロール用:ユーザーが siteId に割り当てられている
//             ParticipatingUser user = buildParticipatingUser(username, companyId, siteId);

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
//             when(participatingUserRepository.findByUsernameAndCompany_Id(username, projectId))
//                     .thenReturn(Optional.of(user));
//             when(siteProjectRelationRepository.findByProjectIdAndSiteId(projectId, siteId))
//                     .thenReturn(relation);
//             when(siteR002MemberDataRepository.findByProject_IdAndSite_Id(projectId, siteId))
//                     .thenReturn(Optional.of(memberData));
//             when(siteR002EquipmentFileRepository.findByMemberData_Id(memberData.getId()))
//                     .thenReturn(equipmentFiles);

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
//             assertThat(response.getMessage()).isNull();
//             assertThat(response.getEquipments()).isNotNull();
//             assertThat(response.getEquipments()).hasSize(1);

//             EquipmentItemSolarResponse equipment = response.getEquipments().get(0);
//             assertThat(equipment.getEquipmentId()).isEqualTo(200);
//             assertThat(equipment.getPcsManufacturer()).isEqualTo("パナソニック");
//             assertThat(equipment.getPcsModel()).isEqualTo("VBPC255A");
//             assertThat(equipment.getPowerCondUnitCount()).isEqualTo(5);
//             assertThat(equipment.getDeviceNo()).isEqualTo("DEV-001");

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(participatingUserRepository, times(1)).findByUsernameAndCompany_Id(username, projectId);
//             verify(siteProjectRelationRepository, times(1)).findByProjectIdAndSiteId(projectId, siteId);
//             verify(siteR002MemberDataRepository, times(1)).findByProject_IdAndSite_Id(projectId, siteId);
//             verify(siteR002EquipmentFileRepository, times(1)).findByMemberData_Id(memberData.getId());
//         }

//         /**
//          * S5-03: 設備ファイルが空の場合でも memberData から設備情報を返す
//          *
//          * 前提条件:
//          * - role = MASTER
//          * - memberData が存在する
//          * - equipmentFiles が空のリスト
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.OK
//          * - equipments のサイズが 1（memberData から生成）
//          * - ファイルがなくても設備情報は返される
//          */
//         @Test
//         @DisplayName("UT-SS-503 設備ファイルが空でも memberData から設備情報を返す")
//         void getSiteProjectEquipment_emptyEquipmentFiles_returnsEquipmentItemWithoutFiles() {
//             // Arrange
//             Integer siteId = 3;
//             Integer projectId = 30;
//             String companyId = "COMP003";
//             String role = RoleConstants.MASTER;
//             String username = "master@example.com";

//             // モックの設定
//             Site site = buildSiteWithCompany(siteId, companyId);
//             Project project = buildProject(projectId);
//             SiteProjectRelation relation = buildSiteProjectRelation(siteId, projectId);
//             SiteR002MemberData memberData = buildMemberData(300);
//             List<SiteR002EquipmentFile> equipmentFiles = List.of(); // 空のリスト

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
//             when(siteProjectRelationRepository.findByProjectIdAndSiteId(projectId, siteId))
//                     .thenReturn(relation);
//             when(siteR002MemberDataRepository.findByProject_IdAndSite_Id(projectId, siteId))
//                     .thenReturn(Optional.of(memberData));
//             when(siteR002EquipmentFileRepository.findByMemberData_Id(memberData.getId()))
//                     .thenReturn(equipmentFiles);

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);
//             assertThat(response.getMessage()).isNull();
//             assertThat(response.getEquipments()).isNotNull();
//             assertThat(response.getEquipments()).hasSize(1);

//             // equipmentFiles が空でも memberData から設備情報が構築される
//             EquipmentItemSolarResponse equipment = response.getEquipments().get(0);
//             assertThat(equipment.getEquipmentId()).isEqualTo(300);
//             assertThat(equipment.getPcsManufacturer()).isEqualTo("パナソニック");
//             assertThat(equipment.getPcsModel()).isEqualTo("VBPC255A");
//             assertThat(equipment.getPowerCondUnitCount()).isEqualTo(5);
//             assertThat(equipment.getDeviceNo()).isEqualTo("DEV-001");

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(siteProjectRelationRepository, times(1)).findByProjectIdAndSiteId(projectId, siteId);
//             verify(siteR002MemberDataRepository, times(1)).findByProject_IdAndSite_Id(projectId, siteId);
//             verify(siteR002EquipmentFileRepository, times(1)).findByMemberData_Id(memberData.getId());
//         }
// /**
//          * S5-04: 拠点が見つからない場合
//          *
//          * 前提条件:
//          * - siteRepository.findById() が Optional.empty() を返す
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.NOT_FOUND
//          * - message = ResponseConstants.MSG_SITE_NOT_FOUND
//          */
//         @Test
//         @DisplayName("UT-SS-504 拠点が見つからない場合、NOT_FOUNDステータスを返す")
//         void getSiteProjectEquipment_siteNotFound_returnsNotFound() {
//             // Arrange
//             Integer siteId = 999;
//             Integer projectId = 10;
//             String companyId = "COMP001";
//             String role = RoleConstants.MASTER;
//             String username = "master@example.com";

//             when(siteRepository.findById(siteId)).thenReturn(Optional.empty());

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
//             assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
//             assertThat(response.getEquipments()).isNull();

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);verify(projectRepository, never()).findById(anyInt());
//         }

//         /**
//          * S5-05: プロジェクトが見つからない場合
//          *
//          * 前提条件:
//          * - 拠点は存在する
//          * - projectRepository.findById() が Optional.empty() を返す
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.NOT_FOUND
//          * - message = ResponseConstants.MSG_PROJECT_NOT_FOUND
//          */
//         @Test
//         @DisplayName("UT-SS-505 プロジェクトが見つからない場合、NOT_FOUNDステータスを返す")
//         void getSiteProjectEquipment_projectNotFound_returnsNotFound() {
//             // Arrange
//             Integer siteId = 1;
//             Integer projectId = 999;
//             String companyId = "COMP001";
//             String role = RoleConstants.MASTER;
//             String username = "master@example.com";

//             Site site = buildSiteWithCompany(siteId, companyId);

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
//             assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_PROJECT_NOT_FOUND);
//             assertThat(response.getEquipments()).isNull();

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(siteProjectRelationRepository, never()).findByProjectIdAndSiteId(anyInt(), anyInt());
//         }

//         /**
//          * S5-06: 企業 ID が一致しない場合
//          *
//          * 前提条件:
//          * - 拠点が存在する
//          * - site.company.companyId != リクエストの companyId
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.FORBIDDEN
//          * - message = ResponseConstants.MSG_FORBIDDEN
//          */
//         @Test
//         @DisplayName("UT-SS-506 企業IDが一致しない場合、FORBIDDENステータスを返す")
//         void getSiteProjectEquipment_companyIdMismatch_returnsForbidden() {
//             // Arrange
//             Integer siteId = 1;
//             Integer projectId = 10;
//             String requestCompanyId = "COMP001";
//             String actualCompanyId = "COMP002"; // 異なる企業
//             String role = RoleConstants.MASTER;
//             String username = "master@example.com";

//             Site site = buildSiteWithCompany(siteId, actualCompanyId);
//             Project project = buildProject(projectId);

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, requestCompanyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
//             assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
//             assertThat(response.getEquipments()).isNull();

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(siteProjectRelationRepository, never()).findByProjectIdAndSiteId(anyInt(), anyInt());
//         }

//         /**
//          * S5-07: USER ロールで割り当てられていない拠点にアクセス
//          *
//          * 前提条件:
//          * - role = USER
//          * - ユーザーがデータベースに存在する
//          * - user.site.id != リクエストの siteId
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.FORBIDDEN
//          * - message = ResponseConstants.MSG_FORBIDDEN
//          */
//         @Test
//         @DisplayName("UT-SS-507 USERロールで割り当てられていない拠点の場合、FORBIDDENステータスを返す")
//         void getSiteProjectEquipment_userRoleNotAssignedToThisSite_returnsForbidden() {
//             // Arrange
//             Integer siteId = 1;
//             Integer projectId = 10;
//             String companyId = "COMP001";
//             String role = RoleConstants.USER;
//             String username = "user@example.com";

//             Site site = buildSiteWithCompany(siteId, companyId);
//             Project project = buildProject(projectId);
//             // ユーザーは別の拠点（siteId=999）に割り当てられている
//             ParticipatingUser user = buildParticipatingUser(username, companyId, 999);

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
//             when(participatingUserRepository.findByUsernameAndCompany_Id(username, projectId))
//                     .thenReturn(Optional.of(user));

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
//             assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
//             assertThat(response.getEquipments()).isNull();

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(participatingUserRepository, times(1)).findByUsernameAndCompany_Id(username, projectId);
//             verify(siteProjectRelationRepository, never()).findByProjectIdAndSiteId(anyInt(), anyInt());
//         }

//         /**
//          * S5-08: USER ロールだがユーザーがデータベースに存在しない
//          *
//          * 前提条件:
//          * - role = USER
//          * - findByUsernameAndCompany_Id() が Optional.empty() を返す
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.FORBIDDEN
//          * - message = ResponseConstants.MSG_FORBIDDEN
//          */
//         @Test
//         @DisplayName("UT-SS-508 USERロールだがユーザーがデータベースに存在しない場合、FORBIDDENステータスを返す")
//         void getSiteProjectEquipment_userRoleUserNotFoundInDb_returnsForbidden() {
//             // Arrange
//             Integer siteId = 1;
//             Integer projectId = 10;
//             String companyId = "COMP001";
//             String role = RoleConstants.USER;
//             String username = "nonexistent@example.com";

//             Site site = buildSiteWithCompany(siteId, companyId);
//             Project project = buildProject(projectId);

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
//             when(participatingUserRepository.findByUsernameAndCompany_Id(username, projectId))
//                     .thenReturn(Optional.empty());

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.FORBIDDEN);
//             assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_FORBIDDEN);
//             assertThat(response.getEquipments()).isNull();

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(participatingUserRepository, times(1)).findByUsernameAndCompany_Id(username, projectId);
//             verify(siteProjectRelationRepository, never()).findByProjectIdAndSiteId(anyInt(), anyInt());
//         }

//         /**
//          * S5-09: 拠点とプロジェクトのリレーションが見つからない場合
//          *
//          * 前提条件:
//          * - 拠点とプロジェクトは存在する
//          * - アクセス権限はある
//          * - findByProjectIdAndSiteId() が null を返す
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.NOT_FOUND
//          * - message = ResponseConstants.MSG_SITE_NOT_FOUND
//          */
//         @Test
//         @DisplayName("UT-SS-509 拠点とプロジェクトのリレーションが見つからない場合、NOT_FOUNDステータスを返す")
//         void getSiteProjectEquipment_siteProjectRelationNotFound_returnsNotFound() {
//             // Arrange
//             Integer siteId = 1;
//             Integer projectId = 10;
//             String companyId = "COMP001";
//             String role = RoleConstants.MASTER;
//             String username = "master@example.com";

//             Site site = buildSiteWithCompany(siteId, companyId);
//             Project project = buildProject(projectId);

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
//             when(siteProjectRelationRepository.findByProjectIdAndSiteId(projectId, siteId))
//                     .thenReturn(null);

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
//             assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
//             assertThat(response.getEquipments()).isNull();

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(siteProjectRelationRepository, times(1)).findByProjectIdAndSiteId(projectId, siteId);
//             verify(siteR002MemberDataRepository, never()).findByProject_IdAndSite_Id(anyInt(), anyInt());
//         }

//         /**
//          * S5-10: memberData が見つからない場合
//          *
//          * 前提条件:
//          * - 拠点、プロジェクト、リレーションは存在する
//          * - アクセス権限はある
//          * - findByProject_IdAndSite_Id() が Optional.empty() を返す
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.NOT_FOUND
//          * - message = ResponseConstants.MSG_SITE_NOT_FOUND
//          */
//         @Test
//         @DisplayName("UT-SS-510 memberDataが見つからない場合、NOT_FOUNDステータスを返す")
//         void getSiteProjectEquipment_memberDataNotFound_returnsNotFound() {
//             // Arrange
//             Integer siteId = 1;
//             Integer projectId = 10;
//             String companyId = "COMP001";
//             String role = RoleConstants.MASTER;
//             String username = "master@example.com";

//             Site site = buildSiteWithCompany(siteId, companyId);
//             Project project = buildProject(projectId);
//             SiteProjectRelation relation = buildSiteProjectRelation(siteId, projectId);

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
//             when(siteProjectRelationRepository.findByProjectIdAndSiteId(projectId, siteId))
//                     .thenReturn(relation);
//             when(siteR002MemberDataRepository.findByProject_IdAndSite_Id(projectId, siteId))
//                     .thenReturn(Optional.empty());

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.NOT_FOUND);
//             assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_SITE_NOT_FOUND);
//             assertThat(response.getEquipments()).isNull();

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(siteProjectRelationRepository, times(1)).findByProjectIdAndSiteId(projectId, siteId);
//             verify(siteR002MemberDataRepository, times(1)).findByProject_IdAndSite_Id(projectId, siteId);
//             verify(siteR002EquipmentFileRepository, never()).findByMemberData_Id(anyInt());
//         }

//         /**
//          * S5-11: リポジトリが予期しない例外をスローした場合
//          *
//          * 前提条件:
//          * - 拠点、プロジェクト、リレーションは存在する
//          * - siteR002MemberDataRepository.findByProject_IdAndSite_Id() が
//          *   RuntimeException をスローする
//          *
//          * 期待結果:
//          * - statusCode = ResponseConstants.INTERNAL_SERVER_ERROR
//          * - message = ResponseConstants.MSG_INTERNAL_ERROR
//          * - 例外がキャッチされ、適切なエラーレスポンスが返される
//          */
//         @Test
//         @DisplayName("UT-SS-511 予期しない例外が発生した場合、INTERNAL_SERVER_ERRORステータスを返す")
//         void getSiteProjectEquipment_unexpectedException_returnsInternalServerError() {
//             // Arrange
//             Integer siteId = 1;
//             Integer projectId = 10;
//             String companyId = "COMP001";
//             String role = RoleConstants.MASTER;
//             String username = "master@example.com";

//             Site site = buildSiteWithCompany(siteId, companyId);
//             Project project = buildProject(projectId);
//             SiteProjectRelation relation = buildSiteProjectRelation(siteId, projectId);

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
//             when(siteProjectRelationRepository.findByProjectIdAndSiteId(projectId, siteId))
//                     .thenReturn(relation);
//             when(siteR002MemberDataRepository.findByProject_IdAndSite_Id(projectId, siteId))
//                     .thenThrow(new RuntimeException("Database connection error"));

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.INTERNAL_SERVER_ERROR);
//             assertThat(response.getMessage()).isEqualTo(ResponseConstants.MSG_INTERNAL_ERROR);
//             assertThat(response.getEquipments()).isNull();

//             // Verify interactions
//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(siteProjectRelationRepository, times(1)).findByProjectIdAndSiteId(projectId, siteId);
//             verify(siteR002MemberDataRepository, times(1)).findByProject_IdAndSite_Id(projectId, siteId);
//         }
// /**
//          * S5-BUG-01: フォールバックロジックで異なるテーブルの ID を混同している問題
//          *
//          * 現在の実装:
//          * if (equipmentFiles.isEmpty() && !memberData.getId().equals(siteId)) {
//          *     equipmentFiles = siteR002EquipmentFileRepository.findByMemberData_Id(siteId);
//          * }
//          *
//          * 問題点:
//          * - siteId は site テーブルの主キー
//          * - memberData.getId() は site_r002_member_data テーブルの主キー
//          * - これらは異なるテーブルの ID であり、比較しても意味がない
//          * - findByMemberData_Id(siteId) は memberDataId として siteId を渡しているが、
//          *   これは論理的に誤り
//          * - 結果として、存在しない memberData の ID でクエリが実行される
//          *
//          * 期待される動作:
//          * このフォールバックロジック自体が不要か、または:
//          * - 正しいビジネスロジックに基づいた条件判定が必要
//          * - 異なるテーブルの ID を混同してはいけない
//          *
//          * 修正例:
//          * 1. フォールバックロジックを完全に削除する
//          * 2. または、正しいビジネスルールに基づいた別の条件を実装する
//          *
//          * このテストの目的:
//          * - データモデルの誤用を文書化する
//          * - 開発者がこの重大な論理エラーを認識する
//          * - 修正が完了したらテストがパスする
//          */
//         @Test
//         @DisplayName("UT-SS-512【バグ】フォールバックロジックで siteId を memberDataId として使用している")
//         void getSiteProjectEquipment_fallbackLogic_shouldNotQueryWithSiteIdAsMemberDataId() {
//             // TODO [BUG]: site_idとmemberData_idは異なるテーブルのIDです。フォールバックロジックが誤っています
//             //
//             // 現在の実装の問題:
//             // if (equipmentFiles.isEmpty() && !memberData.getId().equals(siteId)) {
//             //     equipmentFiles = siteR002EquipmentFileRepository.findByMemberData_Id(siteId);
//             // }
//             //
//             // この条件は以下の点で誤っています:
//             // 1. siteId (site.id) と memberData.id は異なるテーブルの主キー
//             // 2. これらを比較しても意味がない
//             // 3. findByMemberData_Id(siteId) は memberDataId として siteId を渡している
//             // 4. データモデルの整合性を破壊する可能性がある
//             //
//             // 修正が必要な理由:
//             // - データベースの正規化を無視している
//             // - 偶然ID が一致した場合、間違ったデータが返される
//             // - ビジネスロジックとして意味不明
//             //
//             // 推奨される修正:
//             // このフォールバックロジック自体を削除するか、
//             // 正しいビジネスルールに基づいた条件に置き換える

//             // Arrange
//             Integer siteId = 1;
//             Integer projectId = 10;
//             String companyId = "COMP001";
//             String role = RoleConstants.MASTER;
//             String username = "master@example.com";

//             Site site = buildSiteWithCompany(siteId, companyId);
//             Project project = buildProject(projectId);
//             SiteProjectRelation relation = buildSiteProjectRelation(siteId, projectId);
//             // memberData の ID を siteId とは異なる値に設定
//             SiteR002MemberData memberData = buildMemberData(100); // siteId=1 とは異なる
            
//             // equipmentFiles を空に設定（フォールバックロジックをトリガー）
//             List<SiteR002EquipmentFile> emptyEquipmentFiles = List.of();

//             when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//             when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
//             when(siteProjectRelationRepository.findByProjectIdAndSiteId(projectId, siteId))
//                     .thenReturn(relation);
//             when(siteR002MemberDataRepository.findByProject_IdAndSite_Id(projectId, siteId))
//                     .thenReturn(Optional.of(memberData));
//             when(siteR002EquipmentFileRepository.findByMemberData_Id(memberData.getId()))
//                     .thenReturn(emptyEquipmentFiles);

//             // Act
//             EquipmentSolarRegisterResponse response = siteService.getSiteProjectEquipment(
//                     siteId, projectId, companyId, role, username
//             );

//             // Assert
//             assertThat(response).isNotNull();
//             assertThat(response.getStatusCode()).isEqualTo(ResponseConstants.OK);

//             // 以下のアサーションは現在の実装では失敗します（これは意図的です）
//             // フォールバックロジックで findByMemberData_Id(siteId) が呼ばれるべきではない
//             verify(siteR002EquipmentFileRepository, times(1))
//                     .findByMemberData_Id(memberData.getId()); // 正しい memberDataId でのクエリ

//             // バグ:以下のクエリは呼ばれるべきではない
//             // siteId (1) を memberDataId として使用するのは論理的に誤り
//             verify(siteR002EquipmentFileRepository, never())
//                     .findByMemberData_Id(siteId);

//             // Note: 現在の実装では以下の条件でフォールバックが実行される:
//             // - equipmentFiles.isEmpty() == true
//             // - !memberData.getId().equals(siteId) == true (100 != 1)
//             //
//             // その結果、findByMemberData_Id(1) が呼ばれてしまう
//             // これは memberDataId=1 でクエリすることを意味するが、
//             // siteId=1 を memberDataId として使用するのは誤り
//             //
//             // このテストが失敗している間、以下の修正が必要です:
//             // 1. フォールバックロジックを完全に削除する:
//             //    if (equipmentFiles.isEmpty() && ...) { ... }← この部分を削除
//             //
//             // 2. または、正しいビジネスロジックを実装する:
//             //    例: 親子関係のある memberData を検索する
//             //    例: 別のテーブルから関連データを取得する
//             //
//             // いずれにせよ、異なるテーブルの ID を混同してはいけません。

//             verify(siteRepository, times(1)).findById(siteId);
//             verify(projectRepository, times(1)).findById(projectId);
//             verify(siteProjectRelationRepository, times(1)).findByProjectIdAndSiteId(projectId, siteId);
//             verify(siteR002MemberDataRepository, times(1)).findByProject_IdAndSite_Id(projectId, siteId);
//         }

// }