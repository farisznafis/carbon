// formTypes.ts
// Define the form values and validation schema for the multi-step form

import { z } from "zod";


import { projectBaseSchema } from "./projectBaseSchema";

import { 
  // project
  projectApplicationSchema,
  otherProjectOperatorSchema, //other_project_operator
  applicableConditionsSchema, // applicable_conditions
  projChangeInfoSchema, // project_change_information
  projPlanCoverSchema, // project_plan_cover
  projectOverviewSchema, // project_overview
  applicableMethodologySchema, // applicable_methodology
  programMethodologySchema, // program_type_methodology

  // emission
  emissionReductionSchema, // emission_reduction_plan
  emissionCalcSchema, // emission_reduction_calculation
  baselineIncidentalSchema, // baseline_incidental_activity
  postImplementalSchema, // post_impliment_incedental_activity
  mainActivitySchema, // main_activity_project_emission
  incidentalActivitySchema, // incidental_activity_project_emission
  // baselineEmissionSchema,
  // mainBaselineSchema, 
  // incidentalEmissionSchema,

  // monitoring
  monitoringCalcSchema, // monitoring_calculation
  dataManagementSchema, // data_management
  equipmentInfoSchema, // equipment_information_verification_method
  monitoringItemSchema, // monitoring_item_plan
  monitoringMethodSchema, // monitoring_method_plan
  expectedProjectSchema, // expected_project_plan
  monitoringItemPlanCoefficientSchema, // monitoring_item_plan_coefficient
  monitoringMethodPlanCoefficientSchema, // monitoring_method_plan_coefficient
  expectedProjectPlanCoefficientSchema, // expected_project_plan_coefficient
  monitoringPlanSchema, // monitoring_plan
  measureSpecifiedSchema, // measure_specified_instrument
  measureOtherSchema, // measure_other_instruments_overview
  monitoringExplanationSchema, // monitoring_method_explaination
  coefficientSchema, // coefficient_measure_explanation

  // assessment
  additionalAssessmentSchema, // additional_assesment_method
  obtainingAdditionalSchema, // obtaining_additional_method
} from "./AdminProjectApplicationForms";

export const formSchema = z.object({ 
  projectBase: projectBaseSchema,
  projectApplication: projectApplicationSchema,
  otherProjectOperator: otherProjectOperatorSchema,
  appConditions: applicableConditionsSchema,
  projChangeInfo: projChangeInfoSchema,
  projPlanCover: projPlanCoverSchema,
  projectOverview: projectOverviewSchema,
  applicableMethodology: applicableMethodologySchema,
  programMethodology: programMethodologySchema,
  emissionReduction: emissionReductionSchema,
  monitoringCalc: monitoringCalcSchema,
  dataManagement: dataManagementSchema, 
  equipmentInfo: equipmentInfoSchema,
  additionalAssessment: additionalAssessmentSchema, 
  obtainingAdditional: obtainingAdditionalSchema,
  emissionCalc: emissionCalcSchema,
  baselineIncidental: baselineIncidentalSchema,
  postImplemental: postImplementalSchema,
  mainActivity: mainActivitySchema,
  incidentalActivity: incidentalActivitySchema,
  baselineEmission: mainActivitySchema,
  mainBaseline: incidentalActivitySchema,
  incidentalEmission: incidentalActivitySchema,
  monitoringItem: monitoringItemSchema, 
  monitoringMethod: monitoringMethodSchema,
  expectedProject: expectedProjectSchema,
  monitoringItemPlanCoefficient: monitoringItemSchema,
  monitoringMethodPlanCoefficient: monitoringMethodSchema,
  expectedProjectPlanCoefficient: expectedProjectSchema,
  monitoringPlan: monitoringPlanSchema,
  measureSpecified: measureSpecifiedSchema,
  measureOther: measureOtherSchema,
  monitoringExplanation: monitoringExplanationSchema,
  coefficient: coefficientSchema
});

export type FormValues = z.output<typeof formSchema>;

export const adminProjectRegisterSchema = z.object({
  projectBase: projectBaseSchema,
  projectApplication: projectApplicationSchema,
});

export type AdminProjectRegisterFormInputValues = z.input<typeof adminProjectRegisterSchema>;
export type AdminProjectRegisterFormValues = z.output<typeof adminProjectRegisterSchema>;

export const step1Schema = z.object({
  projectBase: projectBaseSchema,
});

export type Step1Values = z.output<typeof step1Schema>;
