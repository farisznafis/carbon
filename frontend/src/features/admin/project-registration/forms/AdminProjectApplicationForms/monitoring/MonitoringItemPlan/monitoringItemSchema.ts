import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const monitoringItemSchema = z.object({
  symbol: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
    .regex(REGEX_PATTERNS.HALFWIDTH_ORI, REGEX_MESSAGES.HALFWIDTH_ORI),

  definition: z
    .string()
    .min(1, '必須項目です')
    .max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(500))
    .regex(REGEX_PATTERNS.JAPANESE_ONLY, REGEX_MESSAGES.JAPANESE_ONLY),

  unit: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),
});
