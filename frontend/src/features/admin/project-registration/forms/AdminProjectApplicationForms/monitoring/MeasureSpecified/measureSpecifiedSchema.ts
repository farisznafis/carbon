import {
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const measureSpecifiedSchema = z.object({
  monitoringItem: z
    .string()
    .min(1, '必須項目です')
    .max(200, VALIDATION_MESSAGES.MAX_CHARACTERS(200)),

  instrumentType: z
    .string()
    .min(1, '必須項目です')
    .max(200, VALIDATION_MESSAGES.MAX_CHARACTERS(200)),

  monitoringPoint: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),

  validityPeriod: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),

  methodCollImg: z.string().min(1, '必須項目です'),
});
