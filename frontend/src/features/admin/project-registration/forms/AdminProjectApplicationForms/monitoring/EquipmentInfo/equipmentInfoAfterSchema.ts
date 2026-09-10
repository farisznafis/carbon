import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';

const text = z
  .string()
  .min(1, '必須項目です')
  .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000));

/**
 * 設備情報の収集・確認方法（削減活動実施後）.
 *
 * Mirrors equipmentInfoSchema; the DTO names メーカー and 型番 after the PCS on
 * this page, so the two differing keys are spelled out rather than shared.
 */
export const equipmentInfoAfterSchema = z.object({
  pcsManufacturer: text,
  pcsModel: text,
  equipmentSerialNumber: text,
  output: text,
  launchDate: z.coerce.date(),
  operationStartDate: z.coerce.date(),
  legalUsefulLife: text,
  efficiency: text,
  fuel: text,
  usage: text,
});
