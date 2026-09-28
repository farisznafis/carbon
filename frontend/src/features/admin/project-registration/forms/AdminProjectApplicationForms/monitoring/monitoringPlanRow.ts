import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';

/**
 * One row of 11.モニタリング計画 — shared by （１）活動量 and （２）係数.
 *
 * The sheet is one table with grouped headers:
 *
 *   モニタリング項目   | モニタリング方法    | プロジェクト計画での想定 | 備考
 *   記号 / 定義 / 単位 | 分類 / 概要 / 頻度  | 想定値 / 根拠            |
 *
 * We used to store those three groups as three separate pages, one row each and
 * no row identifier, so nothing said which 定義 belonged to which 分類.
 *
 * A single モニタリング項目 may take several rows — the sheet's note says to
 * list every method it uses — and a continuation row repeats 記号 while leaving
 * 定義 / 単位 / 根拠 / 備考 blank. That is why only five of the nine are
 * required.
 */
const monitoringPlanRowBase = {
  symbol: z
    .string()
    .min(1, '必須項目です')
    .max(200, VALIDATION_MESSAGES.MAX_CHARACTERS(200)),

  definition: z.string().max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(500)).optional(),

  unit: z.string().max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)).optional(),

  overview: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  frequency: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),

  expectedValue: z.number(),

  basis: z.string().max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)).optional(),

  remarks: z.string().max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)).optional(),
};

/**
 * 分類 is the one column the two tables do not share.
 *
 * （１）活動量 uses Ａ・Ｂ・Ｃ and （２）係数 uses Ⅰ・Ⅱ・Ⅲ — the sheet carries a
 * separate ※1 note under each. The values stored are what the official example
 * stores: half-width A/B/C, and the Roman numerals Ⅰ/Ⅱ/Ⅲ.
 */
export const ACTIVITY_CLASSIFICATIONS = [
  { label: 'Ａ', value: 'A' },
  { label: 'Ｂ（計量器）', value: 'B' },
  { label: 'Ｃ（概算等）', value: 'C' },
];

export const COEFFICIENT_CLASSIFICATIONS = [
  { label: 'Ⅰ（実測）', value: 'Ⅰ' },
  { label: 'Ⅱ（第三者提供値）', value: 'Ⅱ' },
  { label: 'Ⅲ', value: 'Ⅲ' },
];

export const monitoringActivityPlanRowSchema = z.object({
  ...monitoringPlanRowBase,
  classification: z.enum(['A', 'B', 'C']),
});

export const monitoringCoefficientPlanRowSchema = z.object({
  ...monitoringPlanRowBase,
  classification: z.enum(['Ⅰ', 'Ⅱ', 'Ⅲ']),
});

/** Row-relative column names; `PlanTableField` applies the row index. */
const sharedColumnsBefore: FieldConfig['columns'] = [
  { name: 'symbol', label: '記号', type: 'text', width: '9rem' },
  {
    name: 'definition',
    label: '定義',
    type: 'textarea',
    required: false,
    width: '16rem',
  },
  { name: 'unit', label: '単位', type: 'text', required: false, width: '7rem' },
];

const sharedColumnsAfter: FieldConfig['columns'] = [
  { name: 'overview', label: '概要', type: 'textarea', width: '18rem' },
  { name: 'frequency', label: '頻度', type: 'text', width: '8rem' },
  { name: 'expectedValue', label: '想定値', type: 'number', width: '8rem' },
  { name: 'basis', label: '根拠', type: 'textarea', required: false, width: '18rem' },
  { name: 'remarks', label: '備考', type: 'textarea', required: false, width: '18rem' },
];

const classificationColumn = (
  options: FieldConfig['options'],
): NonNullable<FieldConfig['columns']>[number] => ({
  name: 'classification',
  label: '分類',
  type: 'select',
  options,
  width: '11rem',
});

export const monitoringActivityPlanColumns: FieldConfig['columns'] = [
  ...(sharedColumnsBefore ?? []),
  classificationColumn(ACTIVITY_CLASSIFICATIONS),
  ...(sharedColumnsAfter ?? []),
];

export const monitoringCoefficientPlanColumns: FieldConfig['columns'] = [
  ...(sharedColumnsBefore ?? []),
  classificationColumn(COEFFICIENT_CLASSIFICATIONS),
  ...(sharedColumnsAfter ?? []),
];
