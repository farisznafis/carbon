import { describe, expect, it } from 'vitest';
import {
  adjacentStepId,
  clampStepId,
  stepsForApplicationCategory,
} from './applicationCategory';
import { steps } from './projectSteps';

describe('stepsForApplicationCategory', () => {
  const registration = stepsForApplicationCategory(steps);

  it('leaves out the pages only a change files', () => {
    const ids = registration.map((step) => step.id);

    expect(ids).not.toContain(5);
    expect(ids).not.toContain(7);
    expect(ids).toContain(4);
    expect(ids).toContain(6);
  });

  it('drops the 変更有無 flags and 計画変更日 from the pages it keeps', () => {
    const names = registration.flatMap((step) => step.fields.map((field) => String(field.name)));

    expect(names.some((name) => name.split('.').pop()?.startsWith('isChange'))).toBe(false);
    expect(names).not.toContain('projectApplication.applicationChangeDate');
    expect(names).toContain('projectApplication.applicationCategory');
    expect(names).toContain('projectApplication.registrationDate');
  });

  it('keeps everything for a change', () => {
    expect(stepsForApplicationCategory(steps, '計画変更')).toHaveLength(steps.length);
  });
});

describe('step navigation across gaps', () => {
  const list = [{ id: 1 }, { id: 4 }, { id: 6 }, { id: 8 }];

  it('moves to the neighbouring step, skipping the ids that are not there', () => {
    expect(adjacentStepId(list, 4, 1)).toBe(6);
    expect(adjacentStepId(list, 6, -1)).toBe(4);
    expect(adjacentStepId(list, 8, 1)).toBe(8);
    expect(adjacentStepId(list, 1, -1)).toBe(1);
  });

  it('lands a hidden step on the nearest step below it', () => {
    expect(clampStepId(list, 5)).toBe(4);
    expect(clampStepId(list, 7)).toBe(6);
    expect(clampStepId(list, 6)).toBe(6);
    expect(clampStepId(list, 0)).toBe(1);
  });
});
