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
    // The payload itself is built by mapAdminProjectPlanApplicationPayload;
    // these are what put a rejected field's error, a loaded value and a
    // read-only rule on the input the operator actually sees.
    renames: {
      isUsingCostSupport: 'isUsingExamCostSupport',
      projectOperatorJa: 'projectOperatorNameJa',
      projectOperatorEn: 'projectOperatorNameEn',
      energyActOpeNumber: 'energyActOperationNumber',
      picPhone: 'picPhoneNumber',
      SaaS: 'locationEn',
      regionCategory: 'regionalCategory',
    },
  },
  {
    step: 3,
    slug: 'other-operator-projects',
    formKey: 'otherProjectOperator',
    renames: {
      projectOperatorNameKana: 'projectOperatorKana',
      hasEnergyActReport: 'hasEnergyActPeriodReport',
    },
  },
  { step: 4, slug: 'applicable-conditions', formKey: 'appConditions' },
  // Filed only when the project's owner changes, so the backend registers it
  // optional: listed and reachable, but not counted towards completion and
  // never offered as 続きから.
  {
    step: 7,
    slug: 'authority-holder-change',
    formKey: 'authorityHolderChange',
  },
  { step: 5, slug: 'project-change-information', formKey: 'projChangeInfo' },
  { step: 6, slug: 'project-plan-cover', formKey: 'projPlanCover' },
  {
    step: 8,
    slug: 'project-overview',
    formKey: 'projectOverview',
    // The spec abbreviates these; the form spells them out.
    renames: {
      projectOperatorNameKana: 'projectOperatorKana',
      projectOperatorNameJa: 'projectOperatorName',
    },
  },
  { step: 9, slug: 'applicable-methodology', formKey: 'applicableMethodology' },
  { step: 10, slug: 'program-type-methodology', formKey: 'programMethodology' },
  { step: 11, slug: 'emission-reduction-plan', formKey: 'emissionReduction' },
  {
    step: 14,
    slug: 'equipment-verification-before-update',
    formKey: 'equipmentInfo',
  },
  {
    step: 15,
    slug: 'equipment-verification-before-new',
    formKey: 'equipmentInfoBeforeNew',
  },
  {
    step: 16,
    slug: 'equipment-verification-after',
    formKey: 'equipmentInfoAfter',
  },
  {
    step: 18,
    slug: 'obtaining-additional-assessment',
    formKey: 'obtainingAdditional',
  },
  {
    step: 19,
    slug: 'emission-reduction-calculation-method',
    formKey: 'emissionCalc',
  },
  {
    step: 20,
    slug: 'baseline-incidental-emission-activity',
    formKey: 'baselineIncidental',
  },
  {
    step: 22,
    slug: 'main-activity-project-emission',
    formKey: 'mainActivity',
  },
  {
    step: 12,
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
    step: 13,
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
    step: 17,
    slug: 'additional-assessment-method',
    formKey: 'additionalAssessment',
    renames: {
      assessingAdditionality: 'assessingAdditionalityMethod',
      assessingAdditionalityImg: 'assessingAdditionalityMethodImg',
    },
  },
  {
    step: 21,
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
    step: 23,
    slug: 'incidental-activity-project-emission',
    formKey: 'incidentalActivity',
  },
  { step: 24, slug: 'baseline-emission', formKey: 'baselineEmission' },
  { step: 25, slug: 'main-baseline-emission', formKey: 'mainBaseline' },
  {
    step: 26,
    slug: 'incidental-baseline-emission',
    formKey: 'incidentalEmission',
  },
  // 11.モニタリング計画 is three tables in the sheet. （１）and（２）used to be
  // three pages each, splitting one table by column group — the wrong axis,
  // since the form's unit is the row. Each is now one page of rows carrying all
  // nine columns.
  { step: 27, slug: 'monitoring-plan-activity', formKey: 'monitoringActivityPlan' },
  {
    step: 28,
    slug: 'monitoring-plan-coefficient',
    formKey: 'monitoringCoefficientPlan',
  },
  {
    step: 29,
    slug: 'monitoring-plan',
    formKey: 'monitoringPlan',
    renames: {
      itemList: 'monitoringItemList',
      methodCollDesc: 'methodCollDescription',
      methodCollDescImg: 'methodCollDescriptionImg',
    },
  },
  // 12.1（１）is two tables of 計量器. Column names match the DTO exactly:
  // mapPlanPagePayload renames only top-level keys, never inside a row.
  { step: 30, slug: 'measure-specified-instrument', formKey: 'measureSpecified' },
  { step: 31, slug: 'measure-other-instruments', formKey: 'measureOther' },
  {
    step: 32,
    slug: 'monitoring-method-explanation',
    formKey: 'monitoringExplanation',
    renames: { monitoringDesc: 'monitoringItemDescription' },
  },
  {
    step: 33,
    slug: 'coefficient-measure-explanation',
    formKey: 'coefficient',
    // coefficientSchema is monitoringExplanationSchema without the diagram, so
    // it carries the same 説明 field under the form's shorter name.
    renames: { monitoringDesc: 'monitoringItemDescription' },
  },
];

/** Separates a table's name from a column inside its rows: `years[].startDate`. */
const ROW_MARKER = '[]';

/**
 * Restates a backend field name in the form's name for that page.
 *
 * `renames` covers top-level keys only, so a row-relative name renames its
 * table and keeps its column.
 */
export function toFormFieldName(
  backendName: string,
  renames?: Readonly<Record<string, string>>,
): string {
  if (!renames) return backendName;

  const toForm = new Map(Object.entries(renames).map(([form, backend]) => [backend, form]));
  const [table, column] = backendName.split(`${ROW_MARKER}.`);
  const formTable = toForm.get(table) ?? table;
  return column === undefined ? formTable : `${formTable}${ROW_MARKER}.${column}`;
}

/**
 * A page's 有/無 rules, in form field names.
 *
 * The server names both the field and its toggle by its own names, while the
 * form holds values under its own. Untranslated, a rule on a renamed field is
 * never found and a renamed toggle reads as unanswered — either way the field
 * never hides (step 3's 省エネ法 numbers stayed visible with 定期報告 無).
 */
export function rulesInFormNames<T extends { field: string; when: string }>(
  rules: ReadonlyArray<T>,
  renames?: Readonly<Record<string, string>>,
): T[] {
  return rules.map((rule) => ({
    ...rule,
    field: toFormFieldName(rule.field, renames),
    when: toFormFieldName(rule.when, renames),
  }));
}

/**
 * The fields the server fills from SQL, in form field names. Untranslated, a
 * renamed one is left editable and the edit is overwritten on the next read.
 */
export function readOnlyFieldsInFormNames(
  fields: ReadonlyArray<string>,
  renames?: Readonly<Record<string, string>>,
): string[] {
  return fields.map((name) => toFormFieldName(name, renames));
}

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
