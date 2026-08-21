import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';
import { formulaSymbolRowSchema } from '../sectionTenRows';

/**
 * `mainActivitySchema` without the 考え方 — 10.3（２）, 10.5（１）and 10.5（２）
 * open straight on the 式, as their sheets do.
 */
export const incidentalActivitySchema = z.object({
  evaluationFormula: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  formulaNumber: z.number(),

  symbols: z.array(formulaSymbolRowSchema),
});
