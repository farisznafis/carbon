import { describe, expect, it } from 'vitest';
import type { DocumentDataSection } from '@/api/admin/projectDocumentData';
import { copyValueText, displayValue, filterSections } from './documentDataCopy';

const planSection: DocumentDataSection = {
  key: 'plan',
  title: '認証対象期間・会員数計画',
  items: [
    { key: 'members', label: '会員数', value: '500', unit: '社', type: 'NUMBER' },
    { key: 'note', label: '備考', value: null, unit: null, type: 'TEXT' },
  ],
  table: {
    columns: [
      { key: 'year', label: '年度', unit: null, type: 'TEXT' },
      { key: 'co2', label: '削減量', unit: 't-CO2', type: 'NUMBER' },
    ],
    rows: [
      ['2025年度', '120'],
      ['2026年度', '140'],
    ],
  },
};

describe('displayValue / copyValueText', () => {
  it('uses the value as sent, never the unit', () => {
    expect(displayValue('500')).toBe('500');
    expect(copyValueText('500')).toBe('500');
  });

  it('shows a dash but copies nothing for a blank value', () => {
    expect(displayValue(null)).toBe('—');
    expect(displayValue('')).toBe('—');
    expect(copyValueText(null)).toBe('');
  });
});

describe('filterSections', () => {
  it('keeps matching items and table rows only', () => {
    const [section] = filterSections([planSection], '2025');
    expect(section.items).toEqual([]);
    expect(section.table?.rows).toEqual([['2025年度', '120']]);
  });

  it('keeps a whole section when its title matches and drops sections with no match', () => {
    expect(filterSections([planSection], '会員数計画')).toEqual([planSection]);
    expect(filterSections([planSection], 'zzz')).toEqual([]);
  });
});
