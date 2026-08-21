import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';
import { formulaSymbolRowSchema } from '../sectionTenRows';

/**
 * A 式 plus its 記号/定義/単位/想定値 table.
 *
 * Shared by 10.3（１）and 10.4 — both of which open with a 考え方 paragraph.
 * The pages without one use `incidentalActivitySchema`.
 */
export const mainActivitySchema = z.object({
  concept: z
    .string()
    .min(1, '必須項目です')
    .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000)),

  // No character-set rule: a formula is arithmetic, written with = and ×.
  evaluationFormula: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  formulaNumber: z.number(),

  symbols: z.array(formulaSymbolRowSchema),
});
