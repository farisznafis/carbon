/**
 * Maps wizard steps onto the backend's project-plan form registry.
 *
 * Step 1 (プロジェクト作成) is the SQL-backed project record and is saved through
 * the project endpoints, so it has no slug here. Every other step is a page in
 * the plan and is staged individually: it is stored only once it fully
 * validates, which is what lets the progress endpoint say where to resume.
 *
 * Keep in sync with `ProjectPlanFormRegistry` on the backend.
 */
export const PROJECT_PLAN_FORM_SLUG_BY_STEP: Readonly<Record<number, string>> = {
  2: 'application',
  3: 'other-operator-projects',
  4: 'applicable-conditions',
};

/** Highest wizard step currently backed by the plan API. */
export const LAST_IMPLEMENTED_STEP = Math.max(
  ...Object.keys(PROJECT_PLAN_FORM_SLUG_BY_STEP).map(Number),
);

export const PROJECT_PLAN_STEP_BY_FORM_SLUG: Readonly<Record<string, number>> =
  Object.fromEntries(
    Object.entries(PROJECT_PLAN_FORM_SLUG_BY_STEP).map(([step, slug]) => [slug, Number(step)]),
  );

export const getFormSlugForStep = (step: number): string | undefined =>
  PROJECT_PLAN_FORM_SLUG_BY_STEP[step];

export const getStepForFormSlug = (slug: string): number | undefined =>
  PROJECT_PLAN_STEP_BY_FORM_SLUG[slug];
