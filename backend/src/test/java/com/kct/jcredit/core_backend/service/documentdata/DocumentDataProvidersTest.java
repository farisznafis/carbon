package com.kct.jcredit.core_backend.service.documentdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kct.jcredit.core_backend.constant.PermissionCode;
import com.kct.jcredit.core_backend.dto.response.EnR002IndividualActivityRowResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Item;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.Section;
import com.kct.jcredit.core_backend.dto.response.ProjectDocumentDataResponse.ValueType;
import com.kct.jcredit.core_backend.entity.EnR002ReductionResultTotal;
import com.kct.jcredit.core_backend.entity.OperatorCompany;
import com.kct.jcredit.core_backend.entity.Project;
import com.kct.jcredit.core_backend.enums.EligibilityStatus;
import com.kct.jcredit.core_backend.enums.ProjectFileType;
import com.kct.jcredit.core_backend.enums.ReportReadiness;
import com.kct.jcredit.core_backend.repository.postgres.EnR002ReductionResultTotalRepository;
import com.kct.jcredit.core_backend.service.EnR002IndividualActivityService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** What each document's data is made of, and how its values are written. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentDataProvidersTest {

    private static final String PROJECT_ID = "PROJ1";
    private static final String USERNAME = "operator01";

    @Mock private EnR002IndividualActivityService activityService;
    @Mock private EnR002ReductionResultTotalRepository totals;

    private Project project;

    @BeforeEach
    void setUp() {
        OperatorCompany company = OperatorCompany.builder()
                .companyId("COMP-001")
                .nameJa("株式会社サンプル")
                .nameFurigana("カブシキガイシャサンプル")
                .nameEn("Sample Inc.")
                .corpNumber("1234567890123")
                .postalCode("100-0001")
                .prefecture("東京都")
                .municipality("千代田区")
                .blocknum("千代田1-1")
                .buildingName(null)
                .phoneNumber("03-0000-0000")
                .email("info@example.com")
                .build();
        project = Project.builder()
                .publicId("PUBLIC-1")
                .projectName("太陽光発電プログラム")
                .projectNumber("P0001")
                .regionalCategory("全国")
                // 2025-04-01 00:00 in Japan is still 31 March in UTC.
                .startedAt(Instant.parse("2025-03-31T15:00:00Z"))
                .usePurpose("自家消費")
                .methodCategory("EN-R")
                .methodNumber("002")
                .methodName("太陽光発電設備の導入")
                .methodologyVersion("Ver.3.2")
                .methodMainEmission("系統電力")
                .methodDetails("  ")
                .emissionReduction(12345678.5)
                .baselineEmission(2000.0)
                .postProjectEmission(0.0)
                .repLastName("山田").repFirstName("太郎")
                .repLastNameKana("ヤマダ").repFirstNameKana("タロウ")
                .repPosition("代表取締役").repEmail("rep@example.com").repPhoneNumber("03-1111-1111")
                .picLastName("佐藤").picFirstName("花子")
                .picLastNameKana("サトウ").picFirstNameKana("ハナコ")
                .picPosition("部長").picEmail("pic@example.com").picPhoneNumber("03-2222-2222")
                .dataManagementPic("鈴木一郎")
                .newMembersTotal(120).withdrawnMembers(5).cumulativeMembersTotal(115).membersTotal(110)
                .operatorCompany(company)
                .build();
        project.setId(7);
        when(totals.findByProjectId(7)).thenReturn(Optional.empty());
    }

    // --- プロジェクト計画書 ---

    @Test
    void thePlanDocumentIsTheProjectRecord() {
        List<Section> sections = planSections();

        assertThat(sections).extracting(Section::key).containsExactly(
                "project", "methodology", "emissions", "operator", "representative", "personInCharge");
        assertThat(value(sections, "project", "projectName")).isEqualTo("太陽光発電プログラム");
        assertThat(value(sections, "project", "regionalCategory")).isEqualTo("全国");
        assertThat(value(sections, "methodology", "methodologyVersion")).isEqualTo("Ver.3.2");
        assertThat(value(sections, "operator", "operatorName")).isEqualTo("株式会社サンプル");
        assertThat(value(sections, "representative", "repName")).isEqualTo("山田 太郎");
        assertThat(value(sections, "personInCharge", "picNameKana")).isEqualTo("サトウ ハナコ");
        assertThat(new ProjectPlanDataProvider().additionalPermission()).isEmpty();
    }

    @Test
    void writesTheMethodologyNumberWithItsCategory() {
        assertThat(value(planSections(), "methodology", "methodNumber")).isEqualTo("EN-R-002");
    }

    @Test
    void writesTheStartDateAsTheJapaneseCalendarDay() {
        Item started = item(planSections(), "project", "projectStartedAt");

        assertThat(started.value()).isEqualTo("2025-04-01");
        assertThat(started.type()).isEqualTo(ValueType.DATE);
    }

    @Test
    void writesNumbersPlainSoExcelReadsThemAsNumbers() {
        List<Section> sections = planSections();

        Item reduction = item(sections, "emissions", "emissionReduction");
        assertThat(reduction.value()).isEqualTo("12345678.5");
        assertThat(reduction.unit()).isEqualTo("t-CO2");
        assertThat(reduction.type()).isEqualTo(ValueType.NUMBER);
        assertThat(value(sections, "emissions", "baselineEmission")).isEqualTo("2000");
        assertThat(value(sections, "emissions", "postProjectEmission")).isEqualTo("0");
    }

    @Test
    void leavesOutAddressPartsThatAreNotGivenAndBlanksBecomeNoValue() {
        List<Section> sections = planSections();

        assertThat(value(sections, "operator", "operatorAddress")).isEqualTo("東京都千代田区千代田1-1");
        assertThat(value(sections, "methodology", "methodDetails")).isNull();
    }

    @Test
    void noDocumentCarriesThePeriodOrMemberCountsWhichAreFilledInByHand() {
        // The project of this test has member counts on its record; none may surface.
        List<Section> all = new ArrayList<>(planSections());
        all.addAll(new MonitoringReportDataProvider(totals).sections(project, PROJECT_ID, USERNAME));

        assertThat(all).extracting(Section::key).doesNotContain("plan", "memberResults");
        assertThat(all).flatExtracting(Section::items).extracting(Item::key)
                .noneMatch(key -> key.toLowerCase().contains("member") || key.startsWith("certTargetPeriod"));
        assertThat(all).extracting(Section::table).containsOnlyNulls();
    }

    // --- モニタリング報告書 ---

    @Test
    void theMonitoringReportSaysWhoAndWhatTheProjectIs() {
        MonitoringReportDataProvider provider = new MonitoringReportDataProvider(totals);

        List<Section> sections = provider.sections(project, PROJECT_ID, USERNAME);

        assertThat(provider.fileType()).isEqualTo(ProjectFileType.MONITORING_REPORT);
        assertThat(provider.additionalPermission()).contains(PermissionCode.MONITORING_READ);
        assertThat(sections).extracting(Section::key).containsExactly(
                "project", "methodology", "batchTotals", "operator", "personInCharge");
        assertThat(value(sections, "project", "projectNumber")).isEqualTo("P0001");
    }

    @Test
    void theMonitoringReportCarriesTheProjectsCumulativeBatchResult() {
        when(totals.findByProjectId(7)).thenReturn(Optional.of(EnR002ReductionResultTotal.builder()
                .projectId(7)
                .processingDate(LocalDate.of(2026, 9, 30))
                .elpj(new BigDecimal("123456.700"))
                .elbl(new BigDecimal("130000.000"))
                .embl(new BigDecimal("56.940"))
                .empj(new BigDecimal("2.315"))
                .er(new BigDecimal("54.625"))
                .build()));

        List<Section> sections = new MonitoringReportDataProvider(totals).sections(project, PROJECT_ID, USERNAME);

        assertThat(value(sections, "batchTotals", "batchProcessingDate")).isEqualTo("2026-09-30");
        assertThat(value(sections, "batchTotals", "elpj")).isEqualTo("123456.7");
        assertThat(item(sections, "batchTotals", "elpj").unit()).isEqualTo("kWh");
        // Kept in kWh; converted for the forms that ask for MWh.
        assertThat(value(sections, "batchTotals", "elpjMwh")).isEqualTo("123.4567");
        assertThat(item(sections, "batchTotals", "elpjMwh").unit()).isEqualTo("MWh");
        assertThat(value(sections, "batchTotals", "elbl")).isEqualTo("130000");
        assertThat(value(sections, "batchTotals", "embl")).isEqualTo("56.94");
        assertThat(value(sections, "batchTotals", "empj")).isEqualTo("2.315");
        assertThat(value(sections, "batchTotals", "er")).isEqualTo("54.625");
        assertThat(item(sections, "batchTotals", "er").unit()).isEqualTo("t-CO2");
        // Looked up by the database id, not by the id the project was requested with.
        verify(totals).findByProjectId(7);
    }

    @Test
    void aProjectTheBatchHasNotRunForHasTheBatchSectionEmpty() {
        List<Section> sections = new MonitoringReportDataProvider(totals).sections(project, PROJECT_ID, USERNAME);

        assertThat(section(sections, "batchTotals").items()).extracting(Item::value).containsOnlyNulls();
    }

    // --- 個別活動実績報告リスト ---

    @Test
    void theActivityListIsOneRowPerEquipmentOfTheProject() {
        when(activityService.getRowsByProject("PUBLIC-1", null, USERNAME)).thenReturn(List.of(
                new EnR002IndividualActivityRowResponse(
                        11, "MEM-001", "会員A", "SITE-001", "東京都 千代田区 1-1", true,
                        EligibilityStatus.ELIGIBLE, ReportReadiness.READY, null, null),
                new EnR002IndividualActivityRowResponse(
                        12, "MEM-002", "会員B", "SITE-002", "大阪府 大阪市 2-2", false,
                        EligibilityStatus.PENDING, ReportReadiness.NO_BATCH_RESULT, null, null)));
        ActivityResultListDataProvider provider = new ActivityResultListDataProvider(activityService);

        List<Section> sections = provider.sections(project, PROJECT_ID, USERNAME);

        assertThat(provider.fileType()).isEqualTo(ProjectFileType.ACTIVITY_RESULT_LIST);
        assertThat(provider.additionalPermission()).contains(PermissionCode.MONITORING_READ);
        assertThat(value(sections, "activities", "activityCount")).isEqualTo("2");
        Section activities = section(sections, "activities");
        assertThat(activities.table().columns()).hasSize(7);
        assertThat(activities.table().rows()).containsExactly(
                List.of("MEM-001", "会員A", "SITE-001", "東京都 千代田区 1-1", "有", "適格", "報告可"),
                List.of("MEM-002", "会員B", "SITE-002", "大阪府 大阪市 2-2", "無", "確認中", "バッチ結果なし"));
        // The project is named by its public id, whatever id it was requested by.
        verify(activityService).getRowsByProject("PUBLIC-1", null, USERNAME);
    }

    @Test
    void aProjectWithNoEquipmentHasAnEmptyActivityTable() {
        when(activityService.getRowsByProject("PUBLIC-1", null, USERNAME)).thenReturn(List.of());

        List<Section> sections =
                new ActivityResultListDataProvider(activityService).sections(project, PROJECT_ID, USERNAME);

        assertThat(value(sections, "activities", "activityCount")).isEqualTo("0");
        assertThat(section(sections, "activities").table().rows()).isEmpty();
    }

    // --- contract ---

    @Test
    void everyKeyIsPresentInTheJsonEvenWhenThereIsNoValue() throws Exception {
        ProjectDocumentDataResponse response = new ProjectDocumentDataResponse();
        response.setProjectId(PROJECT_ID);
        response.setFileType(ProjectFileType.PROJECT_PLAN);
        response.setSections(planSections());

        JsonNode json = new ObjectMapper().findAndRegisterModules().valueToTree(response);

        assertThat(json.get("fileType").asText()).isEqualTo("PROJECT_PLAN");
        JsonNode methodology = json.get("sections").get(1);
        assertThat(methodology.get("key").asText()).isEqualTo("methodology");
        assertThat(methodology.has("table")).isTrue();
        assertThat(methodology.get("table").isNull()).isTrue();
        JsonNode details = methodology.get("items").get(5);
        assertThat(details.get("key").asText()).isEqualTo("methodDetails");
        assertThat(details.has("value")).isTrue();
        assertThat(details.get("value").isNull()).isTrue();
        assertThat(details.get("type").asText()).isEqualTo("TEXT");
        assertThat(json.get("sections").get(2).get("items").get(0).get("unit").asText()).isEqualTo("t-CO2");
    }

    private List<Section> planSections() {
        return new ProjectPlanDataProvider().sections(project, PROJECT_ID, USERNAME);
    }

    private static Section section(List<Section> sections, String key) {
        return sections.stream().filter(section -> section.key().equals(key)).findFirst().orElseThrow();
    }

    private static Item item(List<Section> sections, String sectionKey, String itemKey) {
        return section(sections, sectionKey).items().stream()
                .filter(item -> item.key().equals(itemKey))
                .findFirst()
                .orElseThrow();
    }

    private static String value(List<Section> sections, String sectionKey, String itemKey) {
        return item(sections, sectionKey, itemKey).value();
    }
}
