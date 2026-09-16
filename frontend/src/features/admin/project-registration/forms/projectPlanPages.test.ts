import { describe, expect, it } from 'vitest';
import {
  FIRST_PLAN_STEP,
  LAST_IMPLEMENTED_STEP,
  PROJECT_PLAN_PAGES,
  getPlanPageForSlug,
  getPlanPageForStep,
  readOnlyFieldsInFormNames,
  rulesInFormNames,
  toFormFieldName,
} from './projectPlanPages';
import { steps } from './projectSteps';

/**
 * The wizard and the backend registry have to agree on the set of pages. They
 * drifted once already — the backend served two equipment pages the wizard had
 * no step for — and the failure is silent: the operator simply cannot reach
 * those pages, and 計画書 progress can never read complete.
 */
describe('project plan page wiring', () => {
  it('numbers the plan pages contiguously from the first one', () => {
    const numbers = PROJECT_PLAN_PAGES.map((page) => page.step).sort(
      (a, b) => a - b,
    );

    expect(numbers[0]).toBe(FIRST_PLAN_STEP);
    expect(numbers.at(-1)).toBe(LAST_IMPLEMENTED_STEP);
    expect(new Set(numbers).size).toBe(numbers.length);
    numbers.forEach((step, index) => {
      expect(step).toBe(FIRST_PLAN_STEP + index);
    });
  });

  it('gives every plan page a wizard step to render it', () => {
    // steps is const-asserted, so its ids are literal types; widen to compare.
    const stepIds = new Set<number>(steps.map((step) => step.id));

    PROJECT_PLAN_PAGES.forEach((page) => {
      expect(stepIds.has(page.step)).toBe(true);
    });
  });

  it('leaves no wizard step past step 1 without a backing plan page', () => {
    // Step 1 is the SQL project record, saved through the project endpoints.
    const unwired = steps
      .filter((step) => step.id > 1 && !getPlanPageForStep(step.id))
      .map((step) => `${step.id}: ${step.title}`);

    expect(unwired).toEqual([]);
  });

  it('keeps slugs unique and resolvable both ways', () => {
    const slugs = PROJECT_PLAN_PAGES.map((page) => page.slug);
    expect(new Set(slugs).size).toBe(slugs.length);

    PROJECT_PLAN_PAGES.forEach((page) => {
      expect(getPlanPageForSlug(page.slug)?.step).toBe(page.step);
      expect(getPlanPageForStep(page.step)?.slug).toBe(page.slug);
    });
  });

  it('names a distinct form key per page', () => {
    const keys = PROJECT_PLAN_PAGES.map((page) => page.formKey);
    expect(new Set(keys).size).toBe(keys.length);
  });
});

/**
 * The server describes its rules in its own field names; the wizard renders
 * form names. These pin the translation for the pages where the two differ.
 */
describe('server field names on renamed pages', () => {
  it('puts step 3 有/無 rules on the toggle the form actually holds', () => {
    const page = getPlanPageForSlug('other-operator-projects');
    const [rule] = rulesInFormNames(
      [{ field: 'energyActOpeNumber', when: 'hasEnergyActPeriodReport', is: true }],
      page?.renames,
    );

    expect(rule).toEqual({ field: 'energyActOpeNumber', when: 'hasEnergyActReport', is: true });
  });

  it('locks the autofilled operator names on step 8 under their form names', () => {
    const page = getPlanPageForSlug('project-overview');

    expect(
      readOnlyFieldsInFormNames(['projectOperatorKana', 'projectOperatorName', 'corpNumber'], page?.renames),
    ).toEqual(['projectOperatorNameKana', 'projectOperatorNameJa', 'corpNumber']);
  });

  it('renames only the table of a row-relative name', () => {
    expect(toFormFieldName('rows[].symbol', { formRows: 'rows' })).toBe('formRows[].symbol');
    expect(toFormFieldName('symbol', undefined)).toBe('symbol');
  });
});
