//排出削減量の算定方法_方法論

import {
  EMISSION_UNIT_OPTIONS,
  EMISSION_UNIT_VALIDATION,
} from '@/lib/constants/emission_helpers';
import { REGEX_MESSAGES, REGEX_PATTERNS } from '@/lib/validations';
import { z } from 'zod';

export const emissionCalcSchema = z.object({
  emissionReduction: z
    .string()
    .min(1, '必須項目です')
    .regex(REGEX_PATTERNS.NUMERIC_12_1, REGEX_MESSAGES.NUMERIC_12_1),

  emissionReductionUnit: z.enum(
    EMISSION_UNIT_OPTIONS,
    EMISSION_UNIT_VALIDATION,
  ),

  baselineEmission: z
    .string()
    .min(1, '必須項目です')
    .regex(REGEX_PATTERNS.NUMERIC_12_1, REGEX_MESSAGES.NUMERIC_12_1),

  baselineEmissionUnit: z.enum(EMISSION_UNIT_OPTIONS, EMISSION_UNIT_VALIDATION),
  postProjectEmission: z
    .string()
    .min(1, '必須項目です')
    .regex(REGEX_PATTERNS.NUMERIC_12_1, REGEX_MESSAGES.NUMERIC_12_1),

  postProjectEmissionUnit: z.enum(
    EMISSION_UNIT_OPTIONS,
    EMISSION_UNIT_VALIDATION,
  ),
});
