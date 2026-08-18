/**
 * Which wizard steps are backed by the project-plan API, and how each one's
 * form values map onto its backend page.
 *
 * Mirrors `ProjectPlanFormRegistry` on the backend: adding a page is one entry
 * here plus its registry entry there, not a new mapper function.
 *
 * Step 1 (プロジェクト作成) is the SQL-backed project record and is saved through
 * the project endpoints, so it has no entry.
 *
 * A step only belongs here once its form field names line up with the backend
 * DTO, which is generated from db/projectplan.xlsx. Listing a step whose names
 * disagree would let the operator fill a page that can never save, so the
 * remaining steps are deliberately left out until their names are aligned.
 */
export type ProjectPlanPage = {
  /** Wizard step id, matching `projectSteps.ts`. */
  step: number;
  /** URL segment on the backend. */
  slug: string;
  /** Key holding this page's values in the form. */
  formKey: string;
  /**
   * Form field name -> backend field name, for the few pages where they differ.
   * Anything not listed is passed through as-is.
   */
  renames?: Readonly<Record<string, string>>;
};

export const PROJECT_PLAN_PAGES: ReadonlyArray<ProjectPlanPage> = [
  {
    step: 2,
    slug: 'application',
    formKey: 'projectApplication',
  },
  {
    step: 3,
    slug: 'other-operator-projects',
    formKey: 'otherProjectOperator',
    renames: {
      projectOperatorNameKana: 'projectOperatorKana',
      hasEnergyActReport: 'hasEnergyActPeriodReport',
      energyManageFactoryNumber: 'energyManagFactoryNumber',
    },
  },
  { step: 4, slug: 'applicable-conditions', formKey: 'appConditions' },
  { step: 5, slug: 'project-change-information', formKey: 'projChangeInfo' },
  { step: 6, slug: 'project-plan-cover', formKey: 'projPlanCover' },
  {
    step: 7,
    slug: 'project-overview',
    formKey: 'projectOverview',
    // The spec abbreviates these; the form spells them out.
    renames: {
      projectOperatorNameKana: 'projectOperatorKana',
      projectOperatorNameJa: 'projectOperatorName',
    },
  },
  { step: 8, slug: 'applicable-methodology', formKey: 'applicableMethodology' },
  {
    step: 9,
    slug: 'program-type-methodology',
    formKey: 'programMethodology',
    // The spec truncates "Manage" to "Manag" in both column names.
    renames: {
      opeManageStructure: 'opeManagStructure',
      infoCollManageMethod: 'infoCollManagMethod',
    },
  },
  { step: 10, slug: 'emission-reduction-plan', formKey: 'emissionReduction' },
  {
    step: 13,
    slug: 'equipment-verification-before-update',
    formKey: 'equipmentInfo',
  },
  {
    step: 15,
    slug: 'obtaining-additional-assessment',
    formKey: 'obtainingAdditional',
  },
  {
    step: 16,
    slug: 'emission-reduction-calculation-method',
    formKey: 'emissionCalc',
  },
  {
    step: 17,
    slug: 'baseline-incidental-emission-activity',
    formKey: 'baselineIncidental',
  },
  {
    step: 19,
    slug: 'main-activity-project-emission',
    formKey: 'mainActivity',
  },
  {
    step: 11,
    slug: 'monitoring-calculation',
    formKey: 'monitoringCalc',
    renames: {
      isStandardMonitor: 'isStandardMonitorCalcEmissionAct',
      isUseSampling: 'isUsingSampling',
      collectDataAndMeasurement: 'collectDataAndMeasurementTarget',
      populationComposition: 'populationCompositionCharacter',
      dataAnalysis: 'dataAnalysisNonsamplErrHandling',
      implMethodSched: 'implMethodScheduleAssignee',
      implMethodSchedImg: 'implMethodScheduleAssigneeImg',
    },
  },
  {
    step: 12,
    slug: 'data-management',
    formKey: 'dataManagement',
    renames: {
      moniDataAcquiring: 'moniDataAcquiringMethod',
      moniDataCollection: 'moniDataCollectionMethod',
      moniDataRec: 'moniDataRecStoreMethod',
      dataStoring: 'dataStoringPeriod',
      isRisksForEmission: 'isRisksForEmissionReduction',
      isSimilar: 'isSimilarProjectRegistered',
      isIndependentofLegal: 'isIndependentOfLegalCompli',
    },
  },
  {
    step: 14,
    slug: 'additional-assessment-method',
    formKey: 'additionalAssessment',
    renames: {
      assessingAdditionality: 'assessingAdditionalityMethod',
      assessingAdditionalityImg: 'assessingAdditionalityMethodImg',
    },
  },
  {
    step: 18,
    slug: 'post-implement-incidental-activity',
    formKey: 'postImplemental',
    renames: {
      monitorCalc: 'monitorCalcBeingDone',
      emissionsTotal: 'emissionsTotalValue',
      impactTotal: 'impactTotalValue',
    },
  },
  // 20/22/23 share incidentalActivitySchema, which is mainActivitySchema minus
  // `concept` — the same six fields their DTOs declare.
  {
    step: 20,
    slug: 'incidental-activity-project-emission',
    formKey: 'incidentalActivity',
  },
  { step: 21, slug: 'baseline-emission', formKey: 'baselineEmission' },
  { step: 22, slug: 'main-baseline-emission', formKey: 'mainBaseline' },
  {
    step: 23,
    slug: 'incidental-baseline-emission',
    formKey: 'incidentalEmission',
  },
  { step: 24, slug: 'monitoring-item-plan', formKey: 'monitoringItem' },
  { step: 25, slug: 'monitoring-method-plan', formKey: 'monitoringMethod' },
  { step: 26, slug: 'expected-project-plan', formKey: 'expectedProject' },
  {
    step: 27,
    slug: 'monitoring-item-plan-coefficient',
    formKey: 'monitoringItemPlanCoefficient',
  },
  {
    step: 28,
    slug: 'monitoring-method-plan-coefficient',
    formKey: 'monitoringMethodPlanCoefficient',
  },
  {
    step: 29,
    slug: 'expected-project-plan-coefficient',
    formKey: 'expectedProjectPlanCoefficient',
  },
  {
    step: 30,
    slug: 'monitoring-plan',
    formKey: 'monitoringPlan',
    renames: {
      itemList: 'monitoringItemList',
      methodCollDesc: 'methodCollDescription',
      methodCollDescImg: 'methodCollDescriptionImg',
    },
  },
  {
    step: 31,
    slug: 'measure-specified-instrument',
    formKey: 'measureSpecified',
    renames: {
      validityPeriod: 'validityPeriodOfCert',
      methodCollImg: 'methodCollDescriptionImg',
    },
  },
  {
    step: 32,
    slug: 'measure-other-instruments',
    formKey: 'measureOther',
    renames: { instrumentCalibration: 'instrumentCalibrationMethodDescription' },
  },
  {
    step: 33,
    slug: 'monitoring-method-explanation',
    formKey: 'monitoringExplanation',
    renames: { monitoringDesc: 'monitoringItemDescription' },
  },
  {
    step: 34,
    slug: 'coefficient-measure-explanation',
    formKey: 'coefficient',
    // coefficientSchema is monitoringExplanationSchema without the diagram, so
    // it carries the same 説明 field under the form's shorter name.
    renames: { monitoringDesc: 'monitoringItemDescription' },
  },
];

const BY_STEP: ReadonlyMap<number, ProjectPlanPage> = new Map(
  PROJECT_PLAN_PAGES.map((page) => [page.step, page]),
);

const BY_SLUG: ReadonlyMap<string, ProjectPlanPage> = new Map(
  PROJECT_PLAN_PAGES.map((page) => [page.slug, page]),
);

export const getPlanPageForStep = (step: number): ProjectPlanPage | undefined =>
  BY_STEP.get(step);

export const getPlanPageForSlug = (slug: string): ProjectPlanPage | undefined =>
  BY_SLUG.get(slug);

/** Lowest wizard step the plan API serves; step 1 is the SQL project record. */
export const FIRST_PLAN_STEP = PROJECT_PLAN_PAGES.reduce(
  (min, page) => Math.min(min, page.step),
  Number.MAX_SAFE_INTEGER,
);

/** Highest wizard step the plan API currently serves. */
export const LAST_IMPLEMENTED_STEP = PROJECT_PLAN_PAGES.reduce(
  (max, page) => Math.max(max, page.step),
  1,
);
