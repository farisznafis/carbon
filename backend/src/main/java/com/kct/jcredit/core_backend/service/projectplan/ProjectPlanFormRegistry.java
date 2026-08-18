package com.kct.jcredit.core_backend.service.projectplan;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kct.jcredit.core_backend.dto.projectplan.ApplicableConditionsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.OtherOperatorProjectsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectApplicationRequest;
import com.kct.jcredit.core_backend.dto.projectplan.AdditionalAssessmentMethodRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ApplicableMethodologyRequest;
import com.kct.jcredit.core_backend.dto.projectplan.BaselineEmissionRequest;
import com.kct.jcredit.core_backend.dto.projectplan.BaselineIncidentalEmissionActivityRequest;
import com.kct.jcredit.core_backend.dto.projectplan.CoefficientMeasureExplanationRequest;
import com.kct.jcredit.core_backend.dto.projectplan.DataManagementRequest;
import com.kct.jcredit.core_backend.dto.projectplan.EmissionReductionCalculationMethodRequest;
import com.kct.jcredit.core_backend.dto.projectplan.EmissionReductionPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.EquipmentVerificationAfterRequest;
import com.kct.jcredit.core_backend.dto.projectplan.EquipmentVerificationBeforeNewRequest;
import com.kct.jcredit.core_backend.dto.projectplan.EquipmentVerificationBeforeUpdateRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ExpectedProjectPlanCoefficientRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ExpectedProjectPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.IncidentalActivityProjectEmissionRequest;
import com.kct.jcredit.core_backend.dto.projectplan.IncidentalBaselineEmissionRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MainActivityProjectEmissionRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MainBaselineEmissionRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MeasureOtherInstrumentsRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MeasureSpecifiedInstrumentRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringCalculationRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringItemPlanCoefficientRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringItemPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringMethodExplanationRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringMethodPlanCoefficientRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringMethodPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.MonitoringPlanRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ObtainingAdditionalAssessmentRequest;
import com.kct.jcredit.core_backend.dto.projectplan.PostImplementIncidentalActivityRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProgramTypeMethodologyRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectChangeInformationRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectOverviewRequest;
import com.kct.jcredit.core_backend.dto.projectplan.ProjectPlanCoverRequest;
import com.kct.jcredit.core_backend.util.AddressFormatter;

import org.springframework.stereotype.Component;

/**
 * The single place that knows which pages the project plan has.
 *
 * <p>Ordering mirrors the wizard steps in the frontend
 * ({@code frontend/src/features/admin/project-registration/forms/projectSteps.ts});
 * step 1 (プロジェクト作成) is the SQL-backed project record and is not a page here.
 *
 * <p>Duplicate slugs or sort keys fail fast at startup.
 */
@Component
public class ProjectPlanFormRegistry {

    private final Map<String, FormDefinition<?>> bySlug = new LinkedHashMap<>();
    private final Map<String, FormDefinition<?>> bySkPrefix = new LinkedHashMap<>();

    public ProjectPlanFormRegistry() {
        register(new FormDefinition<>(
                "application",
                "PROJECT_APPLICATION",
                "プロジェクト申請",
                2,
                false,
                ProjectApplicationRequest.class,
                List.of(
                        sql("projectOperatorKana",
                                (d, c) -> d.setProjectOperatorKana(c.company().getNameFurigana())),
                        sql("projectOperatorNameJa",
                                (d, c) -> d.setProjectOperatorNameJa(c.company().getNameJa())),
                        sql("projectOperatorNameEn",
                                (d, c) -> d.setProjectOperatorNameEn(c.company().getNameEn())),
                        sql("corpNumber", (d, c) -> d.setCorpNumber(c.company().getCorpNumber())),
                        sql("corpPosition", (d, c) -> d.setCorpPosition(c.company().getRepPosition())),
                        sql("corpName", (d, c) -> d.setCorpName(c.company().getNameJa())),
                        sql("prefecture", (d, c) -> d.setPrefecture(c.company().getPrefecture())),
                        sql("city", (d, c) -> d.setCity(c.company().getMunicipality())),
                        // 市区町村以降の住所: the company record splits this across two
                        // columns, the form keeps it as one line.
                        sql("address", (d, c) -> d.setAddress(
                                AddressFormatter.joinAddressAfterCity(c.company().getBlocknum(), c.company().getBuildingName()))),
                        sql("representativePosition",
                                (d, c) -> d.setRepresentativePosition(c.company().getRepPosition())),
                        sql("representativeName",
                                (d, c) -> d.setRepresentativeName(c.repFullName())),
                        sql("picPosition", (d, c) -> d.setPicPosition(c.company().getRepPosition())),
                        // Preserve stored values when the company has no master user,
                        // matching the behaviour of the service this replaces.
                        sql("picName", (d, c) -> {
                            if (c.masterUser() != null) {
                                d.setPicName(c.masterFullName());
                            }
                        }),
                        sql("picEmail", (d, c) -> {
                            if (c.masterUser() != null) {
                                d.setPicEmail(c.masterEmail());
                            }
                        }),
                        sql("picPhoneNumber", (d, c) -> {
                            if (c.masterUser() != null) {
                                d.setPicPhoneNumber(c.masterPhoneNumber());
                            }
                        }),
                        sql("projectNumber", (d, c) -> d.setProjectNumber(c.project().getProjectNumber())),
                        sql("projectName", (d, c) -> d.setProjectName(c.project().getProjectName())),
                        sql("methodologyCategory",
                                (d, c) -> d.setMethodologyCategory(c.project().getMethodCategory())),
                        sql("methodologyNumber",
                                (d, c) -> d.setMethodologyNumber(c.project().getMethodNumber())),
                        sql("methodology", (d, c) -> d.setMethodology(c.project().getMethodName())),
                        sql("regionalCategory",
                                (d, c) -> d.setRegionalCategory(c.project().getRegionalCategory())))));

        register(new FormDefinition<>(
                "other-operator-projects",
                "OTHER_OPERATOR_PROJECTS",
                "代表以外のプログラム型運営・管理者",
                3,
                false,
                OtherOperatorProjectsRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "applicable-conditions",
                "APPLICABLE_CONDITIONS",
                "(参考様式)適用条件",
                4,
                false,
                ApplicableConditionsRequest.class,
                List.of(
                        sql("methodologyCode", (d, c) -> d.setMethodologyCode(c.methodologyCode())),
                        sql("methodologyVersion",
                                (d, c) -> d.setMethodologyVersion(c.project().getMethodologyVersion())),
                        sql("businessName", (d, c) -> d.setBusinessName(c.company().getNameJa())),
                        sql("representativePosition",
                                (d, c) -> d.setRepresentativePosition(c.company().getRepPosition())),
                        sql("representativeName",
                                (d, c) -> d.setRepresentativeName(c.repFullName())))));

        register(new FormDefinition<>(
                "project-change-information",
                "PROJECT_CHANGE_INFORMATION",
                "プロジェクト計画変更情報",
                4,
                false,
                ProjectChangeInformationRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "project-plan-cover",
                "PROJECT_PLAN_COVER",
                "【HP公開】プロジェクト計画書表紙",
                5,
                false,
                ProjectPlanCoverRequest.class,
                List.of(
                        sql("projectName", (d, c) -> d.setProjectName(c.project().getProjectName())),
                        sql("projectNumber", (d, c) -> d.setProjectNumber(c.project().getProjectNumber())),
                        sql("projectOperatorName", (d, c) -> d.setProjectOperatorName(c.company().getNameJa())))));

        register(new FormDefinition<>(
                "project-overview",
                "PROJECT_OVERVIEW",
                "【HP公開】1.プロジェクト概要",
                6,
                false,
                ProjectOverviewRequest.class,
                List.of(
                        sql("projectOperatorKana", (d, c) -> d.setProjectOperatorKana(c.company().getNameFurigana())),
                        sql("projectOperatorName", (d, c) -> d.setProjectOperatorName(c.company().getNameJa())),
                        sql("corpNumber", (d, c) -> d.setCorpNumber(c.company().getCorpNumber())),
                        sql("prefecture", (d, c) -> d.setPrefecture(c.company().getPrefecture())),
                        sql("city", (d, c) -> d.setCity(c.company().getMunicipality())),
                        sql("address", (d, c) -> d.setAddress(AddressFormatter.joinAddressAfterCity(
                                c.company().getBlocknum(), c.company().getBuildingName()))),
                        // Ｊ－クレジット保有者 — from the company's account. Left to the
                        // operator when the company has no single active account.
                        sql("holderAccountNameKana", (d, c) -> d.setHolderAccountNameKana(
                                c.holderAccount() == null
                                        ? null : c.holderAccount().getHolderAccountNameKana())),
                        sql("holderAccountName", (d, c) -> d.setHolderAccountName(
                                c.holderAccount() == null
                                        ? null : c.holderAccount().getHolderAccountName())),
                        sql("holderCorpNumber", (d, c) -> d.setHolderCorpNumber(
                                c.holderAccount() == null
                                        ? null : c.holderAccount().getHolderCorpNumber())),
                        sql("holderPrefecture", (d, c) -> d.setHolderPrefecture(
                                c.holderAccount() == null
                                        ? null : c.holderAccount().getHolderPrefecture())),
                        sql("holderCity", (d, c) -> d.setHolderCity(
                                c.holderAccount() == null
                                        ? null : c.holderAccount().getHolderCity())),
                        sql("holderAddress", (d, c) -> d.setHolderAddress(
                                c.holderAccount() == null
                                        ? null : c.holderAccount().getHolderAddress())),
                        sql("isHolderUndecided", (d, c) -> d.setIsHolderUndecided(
                                c.holderAccount() == null
                                        ? null : c.holderAccount().getIsHolderUndecided())))));

        register(new FormDefinition<>(
                "applicable-methodology",
                "APPLICABLE_METHODOLOGY",
                "適用する方法論",
                7,
                false,
                ApplicableMethodologyRequest.class,
                List.of(
                        sql("methodologyCategory", (d, c) -> d.setMethodologyCategory(c.project().getMethodCategory())),
                        sql("methodologyNumber", (d, c) -> d.setMethodologyNumber(c.project().getMethodNumber())),
                        sql("methodologyVersion", (d, c) -> d.setMethodologyVersion(c.project().getMethodologyVersion())))));

        register(new FormDefinition<>(
                "program-type-methodology",
                "PROGRAM_TYPE_METHODOLOGY",
                "プログラム型プロジェクトの運営・管理",
                8,
                false,
                ProgramTypeMethodologyRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "emission-reduction-plan",
                "EMISSION_REDUCTION_PLAN",
                "排出削減計画",
                9,
                false,
                EmissionReductionPlanRequest.class,
                List.of(
                        sql("newMembersTotal", (d, c) -> d.setNewMembersTotal(c.project().getNewMembersTotal())),
                        sql("cumulativeMembersTotal",
                                (d, c) -> d.setCumulativeMembersTotal(c.project().getCumulativeMembersTotal())),
                        sql("membersTotal", (d, c) -> d.setMembersTotal(c.project().getMembersTotal())),
                        sql("baselineEmissions", (d, c) -> d.setBaselineEmissions(
                                ProjectPlanSqlContext.decimal(c.project().getBaselineEmission()))),
                        sql("afterImplEmissions", (d, c) -> d.setAfterImplEmissions(
                                ProjectPlanSqlContext.decimal(c.project().getPostProjectEmission()))),
                        sql("emissionReductionTotal", (d, c) -> d.setEmissionReductionTotal(
                                ProjectPlanSqlContext.decimal(c.project().getEmissionReduction()))))));

        register(new FormDefinition<>(
                "monitoring-calculation",
                "MONITORING_CALCULATION",
                "モニタリング・算定方法",
                10,
                false,
                MonitoringCalculationRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "data-management",
                "DATA_MANAGEMENT",
                "データ管理・特記事項",
                11,
                false,
                DataManagementRequest.class,
                List.of(
                        sql("dataManagementPic", (d, c) -> d.setDataManagementPic(c.project().getDataManagementPic())))));

        register(new FormDefinition<>(
                "equipment-verification-before-update",
                "EQUIPMENT_VERIFICATION_BEFORE_UPDATE",
                "設備情報の収集・確認方法（実施前 - 更新）",
                12,
                false,
                EquipmentVerificationBeforeUpdateRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "equipment-verification-before-new",
                "EQUIPMENT_VERIFICATION_BEFORE_NEW",
                "設備情報の収集・確認方法（実施前 - 新設）",
                13,
                false,
                EquipmentVerificationBeforeNewRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "equipment-verification-after",
                "EQUIPMENT_VERIFICATION_AFTER",
                "設備情報の収集・確認方法（削減活動実施後）",
                14,
                false,
                EquipmentVerificationAfterRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "additional-assessment-method",
                "ADDITIONAL_ASSESSMENT_METHOD",
                "追加性の評価",
                15,
                false,
                AdditionalAssessmentMethodRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "obtaining-additional-assessment",
                "OBTAINING_ADDITIONAL_ASSESSMENT",
                "追加性評価に必要な情報の入手方法",
                16,
                false,
                ObtainingAdditionalAssessmentRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "emission-reduction-calculation-method",
                "EMISSION_REDUCTION_CALCULATION_METHOD",
                "排出削減量の算定方法",
                17,
                false,
                EmissionReductionCalculationMethodRequest.class,
                List.of(
                        sql("emissionReduction", (d, c) -> d.setEmissionReduction(
                                ProjectPlanSqlContext.decimal(c.project().getEmissionReduction()))),
                        sql("baselineEmission", (d, c) -> d.setBaselineEmission(
                                ProjectPlanSqlContext.decimal(c.project().getBaselineEmission()))),
                        sql("postProjectEmission", (d, c) -> d.setPostProjectEmission(
                                ProjectPlanSqlContext.decimal(c.project().getPostProjectEmission()))))));

        register(new FormDefinition<>(
                "baseline-incidental-emission-activity",
                "BASELINE_INCIDENTAL_EMISSION_ACTIVITY",
                "ベースラインの付随的な排出活動",
                18,
                false,
                BaselineIncidentalEmissionActivityRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "post-implement-incidental-activity",
                "POST_IMPLEMENT_INCIDENTAL_ACTIVITY",
                "プロジェクト実施後の付随的な排出活動",
                19,
                false,
                PostImplementIncidentalActivityRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "main-activity-project-emission",
                "MAIN_ACTIVITY_PROJECT_EMISSION",
                "プロジェクト排出量情報（主要排出活動）",
                20,
                false,
                MainActivityProjectEmissionRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "incidental-activity-project-emission",
                "INCIDENTAL_ACTIVITY_PROJECT_EMISSION",
                "プロジェクト排出量情報（付随的な排出活動）",
                21,
                false,
                IncidentalActivityProjectEmissionRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "baseline-emission",
                "BASELINE_EMISSION",
                "ベースライン排出量の考え方",
                22,
                false,
                BaselineEmissionRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "main-baseline-emission",
                "MAIN_BASELINE_EMISSION",
                "ベースライン排出量（主要排出活動）",
                23,
                false,
                MainBaselineEmissionRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "incidental-baseline-emission",
                "INCIDENTAL_BASELINE_EMISSION",
                "ベースライン排出量（付随的な排出活動）",
                24,
                false,
                IncidentalBaselineEmissionRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "monitoring-item-plan",
                "MONITORING_ITEM_PLAN",
                "モニタリング項目_計画",
                25,
                false,
                MonitoringItemPlanRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "monitoring-method-plan",
                "MONITORING_METHOD_PLAN",
                "モニタリング方法_計画",
                26,
                false,
                MonitoringMethodPlanRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "expected-project-plan",
                "EXPECTED_PROJECT_PLAN",
                "プロジェクト計画での想定",
                27,
                false,
                ExpectedProjectPlanRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "monitoring-item-plan-coefficient",
                "MONITORING_ITEM_PLAN_COEFFICIENT",
                "モニタリング項目_計画（係数）",
                28,
                false,
                MonitoringItemPlanCoefficientRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "monitoring-method-plan-coefficient",
                "MONITORING_METHOD_PLAN_COEFFICIENT",
                "モニタリング方法_計画（係数）",
                29,
                false,
                MonitoringMethodPlanCoefficientRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "expected-project-plan-coefficient",
                "EXPECTED_PROJECT_PLAN_COEFFICIENT",
                "プロジェクト計画での想定（係数）",
                30,
                false,
                ExpectedProjectPlanCoefficientRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "monitoring-plan",
                "MONITORING_PLAN",
                "モニタリング計画",
                31,
                false,
                MonitoringPlanRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "measure-specified-instrument",
                "MEASURE_SPECIFIED_INSTRUMENT",
                "計量器の概要（特定計量器の場合）",
                32,
                false,
                MeasureSpecifiedInstrumentRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "measure-other-instruments",
                "MEASURE_OTHER_INSTRUMENTS",
                "計量器の概要（特定計量器以外）",
                33,
                false,
                MeasureOtherInstrumentsRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "monitoring-method-explanation",
                "MONITORING_METHOD_EXPLANATION",
                "モニタリング方法等に関する説明",
                34,
                false,
                MonitoringMethodExplanationRequest.class,
                List.of()));

        register(new FormDefinition<>(
                "coefficient-measure-explanation",
                "COEFFICIENT_MEASURE_EXPLANATION",
                "係数の実測方法に関する説明",
                35,
                false,
                CoefficientMeasureExplanationRequest.class,
                List.of()));    }

    /** Type-inference helper so the lambdas above stay readable. */
    private static <T> SqlField<T> sql(
            String jsonName, java.util.function.BiConsumer<T, ProjectPlanSqlContext> apply) {
        return new SqlField<>(jsonName, apply);
    }


    private void register(FormDefinition<?> definition) {
        rejectUnsupportedRepetition(definition);
        if (bySlug.putIfAbsent(definition.slug(), definition) != null) {
            throw new IllegalStateException("Duplicate project-plan form slug: " + definition.slug());
        }
        if (bySkPrefix.putIfAbsent(definition.skPrefix(), definition) != null) {
            throw new IllegalStateException(
                    "Duplicate project-plan form sort key: " + definition.skPrefix());
        }
    }

    /** Package-private so the guard itself can be tested, not just its effect. */
    static void rejectUnsupportedRepetition(FormDefinition<?> definition) {
        if (definition.perMethodology()) {
            // The framework can key a page by 方法論 (SK suffix #M2), but nothing
            // can store the methodologies themselves: project holds exactly one,
            // in method_category/method_number/method_name, all NOT NULL. A page
            // registered as repeating would therefore collect ②–⑤ data that the
            // project record, monitoring and credit calculation cannot represent.
            // Enabling this needs the schema first — see the project-plan notes.
            throw new IllegalStateException(
                    "Page " + definition.slug() + " is registered as perMethodology, but the"
                            + " schema stores only one 方法論 per project. Add storage for"
                            + " 方法論 ②–⑤ before enabling repetition.");
        }
    }

    public Optional<FormDefinition<?>> bySlug(String slug) {
        return Optional.ofNullable(slug == null ? null : bySlug.get(slug));
    }

    /** Resolves a stored sort key ({@code PREFIX} or {@code PREFIX#M2}) back to its page. */
    public Optional<FormDefinition<?>> bySortKey(String sortKey) {
        if (sortKey == null) {
            return Optional.empty();
        }
        int separator = sortKey.indexOf('#');
        String prefix = separator < 0 ? sortKey : sortKey.substring(0, separator);
        return Optional.ofNullable(bySkPrefix.get(prefix));
    }

    public List<FormDefinition<?>> all() {
        return bySlug.values().stream()
                .sorted(Comparator.comparingInt(FormDefinition::order))
                .toList();
    }
}
