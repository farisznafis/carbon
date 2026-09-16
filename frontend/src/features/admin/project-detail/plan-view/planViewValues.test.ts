import { describe, expect, it } from 'vitest';
import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import {
  appliesUnderRules,
  cellAppliesUnderRules,
  fileNameFromKey,
  formatPlanValue,
  isShownFor,
  readPath,
} from './planViewValues';
import { rulesInFormNames } from '@/features/admin/project-registration/forms/projectPlanPages';

const text = (name: string): FieldConfig => ({ name, label: name, type: 'text' });

describe('formatPlanValue', () => {
  it('shows a select value by its label, including boolean options', () => {
    const field: FieldConfig = {
      ...text('hasReport'),
      type: 'select',
      options: [
        { label: '有', value: true },
        { label: '無', value: false },
      ],
    };

    expect(formatPlanValue(field, false)).toBe('無');
    expect(formatPlanValue(field, true)).toBe('有');
  });

  it('shows an empty value as a dash rather than a blank', () => {
    expect(formatPlanValue(text('x'), '')).toBe('—');
    expect(formatPlanValue(text('x'), null)).toBe('—');
  });

  it('shows a checkbox boolean in words', () => {
    expect(formatPlanValue({ ...text('x'), type: 'checkbox' }, true)).toBe('はい');
  });
});

describe('isShownFor', () => {
  const values = { page: { isMethodologyValid: true } };
  const read = (path: string) => readPath(values, path);

  it('hides a field the wizard would hide for these answers', () => {
    const field: FieldConfig = {
      ...text('page.certTargetPeriodStart'),
      hideWhen: { field: 'page.isMethodologyValid', value: true },
    };
    expect(isShownFor(field, read)).toBe(false);
  });

  it('shows a field whose showWhen holds', () => {
    const field: FieldConfig = {
      ...text('page.detail'),
      showWhen: { field: 'page.isMethodologyValid', value: true },
    };
    expect(isShownFor(field, read)).toBe(true);
  });
});

describe('有/無 rules', () => {
  it('hides a dependent field once its toggle is answered the other way', () => {
    const rules = [{ field: 'energyActOpeNumber', when: 'hasReport', is: true }];
    const read = (path: string) => readPath({ page: { hasReport: false } }, path);

    expect(appliesUnderRules('page.energyActOpeNumber', rules, read)).toBe(false);
  });

  it('leaves the field visible while the toggle is unanswered', () => {
    const rules = [{ field: 'energyActOpeNumber', when: 'hasReport', is: true }];
    const read = (path: string) => readPath({ page: {} }, path);

    expect(appliesUnderRules('page.energyActOpeNumber', rules, read)).toBe(true);
  });

  it('finds a renamed toggle once the rule is restated in form names', () => {
    const serverRules = [
      { field: 'energyActOpeNumber', when: 'hasEnergyActPeriodReport', is: true },
    ];
    const renames = { hasEnergyActReport: 'hasEnergyActPeriodReport' };
    const read = (path: string) =>
      readPath({ page: { hasEnergyActReport: false } }, path);

    expect(appliesUnderRules('page.energyActOpeNumber', serverRules, read)).toBe(true);
    expect(
      appliesUnderRules('page.energyActOpeNumber', rulesInFormNames(serverRules, renames), read),
    ).toBe(false);
  });

  it('decides a row rule per row', () => {
    const rules = [{ field: 'rows[].detail', when: 'rows[].hasDetail', is: true }];

    expect(cellAppliesUnderRules('page.rows', 'detail', { hasDetail: false }, rules)).toBe(false);
    expect(cellAppliesUnderRules('page.rows', 'detail', { hasDetail: true }, rules)).toBe(true);
  });
});

describe('fileNameFromKey', () => {
  it('drops the storage path and the field prefix', () => {
    expect(
      fileNameFromKey('projects/PROJ1/plan/monitoring-plan/methodCollDescriptionImg_flow.png'),
    ).toBe('flow.png');
  });
});
