import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const additionalAssessmentSchema = z.object({
  assessingAdditionality: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  assessingAdditionalityImg: z.string().min(1, '必須項目です'),
});
