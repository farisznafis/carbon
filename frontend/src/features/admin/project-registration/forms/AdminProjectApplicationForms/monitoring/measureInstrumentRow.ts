import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';

/**
 * 12.1（１）計量器の概要 — two tables sharing three columns.
 *
 * ①特定計量器の場合 ends in 検定の有効期限, ②特定計量器以外 ends in
 * 計量器の校正方法の説明. Both pages held a single 計量器 before, while the
 * official example lists two in each.
 */
const measureInstrumentBase = {
  monitoringItem: z
    .string()
    .min(1, '必須項目です')
    .max(200, VALIDATION_MESSAGES.MAX_CHARACTERS(200)),

  instrumentType: z
    .string()
    .min(1, '必須項目です')
    .max(200, VALIDATION_MESSAGES.MAX_CHARACTERS(200)),

  // ※1: 「モニタリングポイントは(2)と整合する番号を記載すること。」
  monitoringPoint: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),
};

export const specifiedInstrumentRowSchema = z.object({
  ...measureInstrumentBase,
  // A duration, not a date — the example writes 「10年間」.
  validityPeriodOfCert: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),
});

export const otherInstrumentRowSchema = z.object({
  ...measureInstrumentBase,
  // Optional: the example fills it on the first row and leaves it blank on the
  // second.
  instrumentCalibrationMethodDescription: z
    .string()
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000))
    .optional(),
});

const sharedColumns: FieldConfig['columns'] = [
  {
    name: 'monitoringItem',
    label: 'モニタリング項目',
    type: 'textarea',
    width: '20rem',
  },
  { name: 'instrumentType', label: '計量器の種類', type: 'text', width: '12rem' },
  {
    name: 'monitoringPoint',
    label: 'モニタリングポイント',
    type: 'text',
    width: '9rem',
    placeholder: '（２）と整合する番号',
  },
];

export const specifiedInstrumentColumns: FieldConfig['columns'] = [
  ...(sharedColumns ?? []),
  {
    name: 'validityPeriodOfCert',
    label: '検定の有効期限',
    type: 'text',
    width: '10rem',
    placeholder: '例: 10年間',
  },
];

export const otherInstrumentColumns: FieldConfig['columns'] = [
  ...(sharedColumns ?? []),
  {
    name: 'instrumentCalibrationMethodDescription',
    label: '計量器の校正方法の説明',
    type: 'textarea',
    required: false,
    width: '22rem',
  },
];
