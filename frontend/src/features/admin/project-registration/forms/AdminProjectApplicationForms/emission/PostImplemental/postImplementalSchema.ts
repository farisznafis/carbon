import { MONITOR_CALC_POST_VALIDATION, MONITOR_CALC_POST_OPTIONS } from '@/lib/constants/select_types';
import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const postImplementalSchema = z.object({
  concept: z
    .string()
    .min(1, '必須項目です')
    .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000)),

  emissionAct: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  greenhouseGasType: z
    .string()
    .min(1, '必須項目です')
    .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20))
    .regex(
      REGEX_PATTERNS.HALFWIDTH_ALPHA_ONLY,
      REGEX_MESSAGES.HALFWIDTH_ALPHA_ONLY,
    ),

  emissions: z
    .string()
    .min(1, '必須項目です')
    .regex(REGEX_PATTERNS.NUMERIC_12_1, REGEX_MESSAGES.NUMERIC_12_1),

  impact: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  monitorCalc: z.enum(MONITOR_CALC_POST_OPTIONS, MONITOR_CALC_POST_VALIDATION),

  // auto calculation
  emissionsTotal: z
    .string()
    .regex(REGEX_PATTERNS.NUMERIC_12_1, REGEX_MESSAGES.NUMERIC_12_1)
    .optional(),

  impactTotal: z
    .string()
    .optional(),
});
