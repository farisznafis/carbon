import { describe, expect, it } from 'vitest';

import {
  REVIEWER_NAMES,
  REVIEWER_NAME_OPTIONS,
} from './reviewer_names';

/**
 * The list is fixed by J-Credit's 【削除不可】プルダウン用 sheet. It has already been
 * short by two entries once, so pin the count and the shape here.
 */
describe('REVIEWER_NAME_OPTIONS', () => {
  it('carries all 14 official 審査機関', () => {
    expect(REVIEWER_NAME_OPTIONS).toHaveLength(14);
  });

  it('includes the two bodies that were previously missing', () => {
    expect(REVIEWER_NAME_OPTIONS).toContain('日本検査キューエイ株式会社');
    expect(REVIEWER_NAME_OPTIONS).toContain('株式会社日本スマートエナジー認証機構');
  });

  it('keeps 日本海事検定キューエイ and 日本検査キューエイ as separate bodies', () => {
    // Near-identical names; neither is a typo of the other.
    expect(REVIEWER_NAME_OPTIONS).toContain('日本海事検定キューエイ株式会社');
    expect(REVIEWER_NAME_OPTIONS).toContain('日本検査キューエイ株式会社');
  });

  it('has no duplicates', () => {
    expect(new Set(REVIEWER_NAME_OPTIONS).size).toBe(REVIEWER_NAME_OPTIONS.length);
  });
});

describe('REVIEWER_NAMES', () => {
  it('is derived from the options, so the dropdown cannot drift from the schema', () => {
    expect(REVIEWER_NAMES.map((option) => option.value)).toEqual([
      ...REVIEWER_NAME_OPTIONS,
    ]);
    REVIEWER_NAMES.forEach((option) => {
      expect(option.label).toBe(option.value);
    });
  });
});
