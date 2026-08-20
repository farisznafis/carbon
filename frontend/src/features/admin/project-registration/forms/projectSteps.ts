// src/components/forms/Admin/projectSteps.ts
// projectSteps.ts
// Define the steps and their corresponding fields for the multi-step form
// possible form types: | "text" | "textarea" | "checkbox" | "number" | "email" | "select" | "date" | "file";

import { allProjectFields } from "./ProjectBase";

import {
  // project
  projectApplication, // project_application
  otherProjectOperator, //other_project_operator
  applicableConditions, // applicable_conditions
  projChangeInfo, // project_change_information
  projPlanCover, // project_plan_cover
  projectOverviewFields, // project_overview
  applicableMethodology, // applicable_methodology
  programMethodology, // program_type_methodology

  // emission
  emissionReduction, // emission_reduction_plan
  emissionCalc, // emission_reduction_calculation
  baselineIncidental, // baseline_incidental_activity
  postImplemental, // post_impliment_incedental_activity
  mainActivity, // main_activity_project_emission
  incidentalActivity, // incidental_activity_project_emission
  baselineEmission, // baseline_emission
  mainBaseline, // main_baseline_emission
  incidentalEmission, // incidental_emission_activity

  // monitoring
  monitoringCalc, // monitoring_calculation
  dataManagement, // data_management
  equipmentInfo, // equipment_information_verification_method
  equipmentInfoBeforeNew, // equipment_verification_before_new
  equipmentInfoAfter, // equipment_verification_after
  monitoringActivityPlan, // 11.モニタリング計画（１）活動量
  monitoringCoefficientPlan, // 11.モニタリング計画（２）係数
  monitoringPlan, // monitoring_plan
  measureSpecified, // measure_specified_instrument
  measureOther, // measure_other_instruments_overview
  monitoringExplanation, // monitoring_method_explaination
  coefficient, // coefficient_measure_explanation

  // assessment
  additionalAssessment, // additional_assesment_method
  obtainingAdditional, // obtaining_additional_method

} from "./AdminProjectApplicationForms";

// import { projectApplication } from "./AdminProjectApplicationForms/ProjectApplication/ProjectApplication";
// import { otherProjectOperator } from "./AdminProjectApplicationForms/project/OtherProjectOperator/OtherProjectOperator";

// import { applicableConditions } from "./AdminProjectApplicationForms/ApplicableConditions/ApplicableConditions";

// import { projChangeInfo } from "./AdminProjectApplicationForms/ProjChangeInfo/ProjChangeInfo";

// import { projPlanCover } from "./AdminProjectApplicationForms/ProjPlanCover/ProjPlanCover";

// import { projectOverviewFields } from "./AdminProjectApplicationForms/ProjectOverview/ProjectOverview";

// import { applicableMethodology } from "./AdminProjectApplicationForms/ApplicableMethodology/ApplicableMethodology";

// import { programMethodology } from "./AdminProjectApplicationForms/ProgramMethodology/ProgramMethodolgy";

// import { emissionReduction } from "./AdminProjectApplicationForms/EmmissionReduction/EmissionReduction";

// import { monitoringCalc } from "./AdminProjectApplicationForms/MonitoringCalc/MonitoringCalc";

// import { dataManagement } from "./AdminProjectApplicationForms/DataManagement/DataManagement";

// import { equipmentInfo } from "./AdminProjectApplicationForms/EquipmentInfo/EquipmentInfo";

// // import { additionalAssessment, obtainingAdditional } from "./AdminProjectApplicationForms/assessment/AdditionalAssessment/AdditionalAssessment";


// import { emissionCalc } from "./AdminProjectApplicationForms/emission/EmissionCalc";

// import { baselineIncidental } from "./AdminProjectApplicationForms/EmissionCalc/BaselineIncidental";

// import { postImplemental } from "./AdminProjectApplicationForms/EmissionCalc/PostImplemental";

// import { mainActivity } from "./AdminProjectApplicationForms/EmissionCalc/MainActivity";

// import { incidentalActivity } from "./AdminProjectApplicationForms/EmissionCalc/IncidentalActivity";

// import { baselineEmission } from "./AdminProjectApplicationForms/EmissionCalc/BaselineEmission";

// import { mainBaseline } from "./AdminProjectApplicationForms/EmissionCalc/MainBaseline";

// import { incidentalEmission } from "./AdminProjectApplicationForms/EmissionCalc/IncidentalEmission";







// import { monitoringPlan } from "./AdminProjectApplicationForms/MonitoringPlan/MonitoringPlan";

// import { measureSpecified } from "./AdminProjectApplicationForms/monitoring/MeasureSpecified/MeasureSpecified";

// import { measureOther } from "./AdminProjectApplicationForms/monitoring/MeasureOther/MeasureOther";

// import { monitoringExplanation } from "./AdminProjectApplicationForms/monitoring/MonitoringExplanation/MonitoringExplanation";

// import { coefficient } from "./AdminProjectApplicationForms/Coefficient/Coefficient";

export const steps = [
  { id: 1, title: "プロジェクト作成", fields: allProjectFields },
  { id: 2, title: "プロジェクト申請", fields: projectApplication },
  {
    id: 3,
    title: "代表以外のプログラム型運営・管理者",
    fields: otherProjectOperator,
  },
  { id: 4, title: "(参考様式)適用条件", fields: applicableConditions },
  { id: 5, title: "プロジェクト計画変更情報", fields: projChangeInfo },
  { id: 6, title: "【HP公開】プロジェクト計画書表紙", fields: projPlanCover },
  {
    id: 7,
    title: "【HP公開】1.プロジェクト概要",
    fields: projectOverviewFields,
  },
  { id: 8, title: "適用方法論", fields: applicableMethodology },
  {
    id: 9,
    title: "プログラム型プロジェクトの運営・管理_方法論",
    fields: programMethodology,
  },
  { id: 10, title: "排出削減計画", fields: emissionReduction },
  { id: 11, title: "モニタリング・算定方法", fields: monitoringCalc },
  { id: 12, title: "データ管理・特記事項_方法論", fields: dataManagement },
  {
    id: 13,
    title: "設備情報の収集・確認方法_方法論（実施前 - 更新）",
    fields: equipmentInfo,
  },
  {
    id: 14,
    title: "設備情報の収集・確認方法_方法論（実施前 - 新設）",
    fields: equipmentInfoBeforeNew,
  },
  {
    id: 15,
    title: "設備情報の収集・確認方法_方法論（削減活動実施後）",
    fields: equipmentInfoAfter,
  },
  { id: 16, title: "追加性の評価_方法論", fields: additionalAssessment },
  {
    id: 17,
    title: "追加性評価に必要な情報の入手方法",
    fields: obtainingAdditional,
  },
  { id: 18, title: "排出削減量の算定方法_方法論", fields: emissionCalc },
  {
    id: 19,
    title: "ベースラインの付随的な排出活動",
    fields: baselineIncidental,
  },
  {
    id: 20,
    title: "プロジェクト実施後の付随的な排出活動",
    fields: postImplemental,
  },
  {
    id: 21,
    title: "プロジェクト排出量情報（主要排出活動）",
    fields: mainActivity,
  },
  {
    id: 22,
    title: "プロジェクト排出量情報（付随的な排出活動）",
    fields: incidentalActivity,
  },
  { id: 23, title: "ベースライン排出量の考え方", fields: baselineEmission },
  { id: 24, title: "ベースライン排出量（主要排出活動）", fields: mainBaseline },
  {
    id: 25,
    title: "ベースライン排出量（付随的な排出活動）",
    fields: incidentalEmission,
  },
  {
    id: 26,
    title: "モニタリング計画（１）活動量",
    fields: monitoringActivityPlan,
  },
  {
    id: 27,
    title: "モニタリング計画（２）係数",
    fields: monitoringCoefficientPlan,
  },
  { id: 28, title: "モニタリング計画（３）データの収集方法", fields: monitoringPlan },
  {
    id: 29,
    title: "計量器の概要（特定計量器の場合）",
    fields: measureSpecified,
  },
  {
    id: 30,
    title: "計量器の概要（特定計量器以外の計量器の場合）",
    fields: measureOther,
  },
  {
    id: 31,
    title: "モニタリング方法等に関する説明",
    fields: monitoringExplanation,
  },
  { id: 32, title: "係数の実測方法に関する説明", fields: coefficient },
] as const;
