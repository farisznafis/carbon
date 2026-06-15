import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const obtainingAdditionalSchema = z.object({
  item: z
    .string()
    .min(1, '必須項目です')
    .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),

  acquisitionMethod: z
    .string()
    .min(1, '必須項目です')
    .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255)),
});
