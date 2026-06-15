import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const monitoringMethodSchema = z.object({
  classification: z
    .string()
    .min(1, '必須項目です')
    .max(200, VALIDATION_MESSAGES.MAX_CHARACTERS(200))
    .regex(REGEX_PATTERNS.HALFWIDTH_ORI, REGEX_MESSAGES.HALFWIDTH_ORI),

  overview: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  frequency: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),
});
