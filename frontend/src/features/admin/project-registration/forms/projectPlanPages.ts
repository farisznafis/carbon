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
  { step: 8, slug: 'applicable-methodology', formKey: 'applicableMethodology' },
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
  { step: 24, slug: 'monitoring-item-plan', formKey: 'monitoringItem' },
  { step: 25, slug: 'monitoring-method-plan', formKey: 'monitoringMethod' },
  { step: 26, slug: 'expected-project-plan', formKey: 'expectedProject' },
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

/** Highest wizard step the plan API currently serves. */
export const LAST_IMPLEMENTED_STEP = PROJECT_PLAN_PAGES.reduce(
  (max, page) => Math.max(max, page.step),
  1,
);
