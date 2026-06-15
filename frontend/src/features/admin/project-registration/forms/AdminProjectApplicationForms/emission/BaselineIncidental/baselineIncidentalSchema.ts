// concept
// emission_act
// greenhouse_gas_type
// emissions
// monitor_calc_being_done
// emissions_total_value
import { MONITOR_CALC_BASELINE_OPTIONS, MONITOR_CALC_BASELINE_VALIDATION } from '@/lib/constants/select_types';
import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const baselineIncidentalSchema = z.object({
  concept: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  emissionAct: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  greenhouseGasType: z
    .string()
    .min(1, '必須項目です')
    .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20)),

  emissions: z
    .string()
    .min(1, '必須項目です')
    .regex(REGEX_PATTERNS.NUMERIC_12_1, REGEX_MESSAGES.NUMERIC_12_1),

  monitorCalcBeingDone: z.enum(MONITOR_CALC_BASELINE_OPTIONS, MONITOR_CALC_BASELINE_VALIDATION),

  emissionsTotalValue: z
    .string()
    .regex(REGEX_PATTERNS.NUMERIC_12_1, REGEX_MESSAGES.NUMERIC_12_1)
    .optional(),
});
