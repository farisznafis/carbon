import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const mainActivitySchema = z.object({
  concept: z
    .string()
    .min(1, '必須項目です')
    .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000)),

  evaluationFormula: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  formulaNumber: z
    .string()
    .min(1, '必須項目です')
    .regex(REGEX_PATTERNS.HALFWIDTH_NUMERIC_ONLY, REGEX_MESSAGES.HALFWIDTH_NUMERIC_ONLY),

  symbol: z
    .string()
    .max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
    .regex(REGEX_PATTERNS.HALFWIDTH_ORI, REGEX_MESSAGES.HALFWIDTH_ORI)
    .optional(),

  definition: z
    .string()
    .max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(500))
    .regex(REGEX_PATTERNS.JAPANESE_ONLY, REGEX_MESSAGES.JAPANESE_ONLY)
    .optional(),

  unit: z
    .string()
    .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20))
    .optional(),

  expectedValue: z
    .string()
    .min(1, '必須項目です')
    .regex(REGEX_PATTERNS.NUMERIC_12_1, REGEX_MESSAGES.NUMERIC_12_1)
    .optional(),
});
