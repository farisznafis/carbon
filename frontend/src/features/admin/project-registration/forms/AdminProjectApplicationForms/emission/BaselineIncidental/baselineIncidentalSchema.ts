import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';
import { baselineIncidentalActivityRowSchema } from '../sectionTenRows';

export const baselineIncidentalSchema = z.object({
  concept: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  activities: z.array(baselineIncidentalActivityRowSchema),

  // Derived server-side from the rows; never typed.
  emissionsTotalValue: z.number().optional(),
});
