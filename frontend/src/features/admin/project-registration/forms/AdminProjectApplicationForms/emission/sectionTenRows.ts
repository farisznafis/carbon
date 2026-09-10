import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';

/**
 * The repeating tables of 10.排出削減量の算定方法.
 *
 * Two shapes cover the whole section:
 *
 * - every 式 is followed by 記号 / 定義 / 単位 / 想定値, one row per symbol —
 *   10.3(1), 10.3(2), 10.4(2), 10.5(1) and 10.5(2);
 * - 10.2(1) and (2) list 排出活動 rows with a 合計 beneath them.
 *
 * Each page held a single row of its table before, so a formula with more than
 * one symbol could not be entered — and every formula in the official sheets
 * has at least two.
 */

// --- 記号 / 定義 / 単位 / 想定値 ---

export const formulaSymbolRowSchema = z.object({
  symbol: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),

  definition: z
    .string()
    .min(1, '必須項目です')
    .max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(500)),

  unit: z.string().max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)).optional(),

  expectedValue: z.number(),
});

export const formulaSymbolColumns: FieldConfig['columns'] = [
  { name: 'symbol', label: '記号', type: 'text', width: '11rem' },
  { name: 'definition', label: '定義', type: 'textarea', width: '22rem' },
  { name: 'unit', label: '単位', type: 'text', required: false, width: '9rem' },
  { name: 'expectedValue', label: '想定値', type: 'number', width: '9rem' },
];

// --- 10.2 の排出活動 ---

/**
 * モニタリング・算定方法 on 10.2(1) — two choices, so the value is a boolean.
 * 10.2(2) offers three; the sheet prints its own list under each table.
 */
export const BASELINE_MONITORING_CHOICES = [
  { label: '排出量の算定を行う。', value: true },
  { label: '排出量の算定を行わない。', value: false },
];

export const POST_IMPLEMENT_MONITORING_CHOICES = [
  { label: '排出量の算定を行う。', value: '排出量の算定を行う。' },
  {
    label:
      '排出量の算定を行う。ただし、排出量のモニタリングを省略し、影響度により排出量を評価する。',
    value:
      '排出量の算定を行う。ただし、排出量のモニタリングを省略し、影響度により排出量を評価する。',
  },
  { label: '排出量の算定を省略する。', value: '排出量の算定を省略する。' },
];

const emissionActivityBase = {
  emissionAct: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  greenhouseGasType: z
    .string()
    .min(1, '必須項目です')
    .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20)),

  emissions: z.number(),
};

export const baselineIncidentalActivityRowSchema = z.object({
  ...emissionActivityBase,
  monitorCalcBeingDone: z.boolean(),
});

export const postImplementActivityRowSchema = z.object({
  ...emissionActivityBase,
  // A share of the 排出削減量 from 10.1, so it cannot exceed 100.
  impact: z.number().min(0, '0以上で入力してください').max(100, '100以下で入力してください'),
  monitorCalcBeingDone: z.string().min(1, '必須項目です'),
});

const emissionActivityColumns: FieldConfig['columns'] = [
  { name: 'emissionAct', label: '排出活動', type: 'textarea', width: '20rem' },
  { name: 'greenhouseGasType', label: '温室効果ガスの種類', type: 'text', width: '9rem' },
  { name: 'emissions', label: '排出量 (t-CO2/年)', type: 'number', width: '9rem' },
];

export const baselineIncidentalActivityColumns: FieldConfig['columns'] = [
  ...emissionActivityColumns,
  {
    name: 'monitorCalcBeingDone',
    label: 'モニタリング・算定方法',
    type: 'select',
    options: BASELINE_MONITORING_CHOICES,
    width: '14rem',
  },
];

export const postImplementActivityColumns: FieldConfig['columns'] = [
  ...emissionActivityColumns,
  { name: 'impact', label: '影響度 (％)', type: 'number', width: '8rem' },
  {
    name: 'monitorCalcBeingDone',
    label: 'モニタリング・算定方法',
    type: 'select',
    options: POST_IMPLEMENT_MONITORING_CHOICES,
    width: '18rem',
  },
];
