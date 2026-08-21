import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';
import { postImplementActivityRowSchema } from '../sectionTenRows';

export const postImplementalSchema = z.object({
  concept: z
    .string()
    .min(1, '必須項目です')
    .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000)),

  activities: z.array(postImplementActivityRowSchema),

  // Both derived server-side from the rows; never typed.
  emissionsTotalValue: z.number().optional(),
  impactTotalValue: z.number().optional(),
});
