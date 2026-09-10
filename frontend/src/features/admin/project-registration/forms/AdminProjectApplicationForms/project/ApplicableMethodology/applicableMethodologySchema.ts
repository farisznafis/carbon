import { PROJECT_TYPE_OPTIONS, PROJECT_TYPE_VALIDATION } from '@/lib/constants/project_types';
import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const applicableMethodologySchema = z.object({
  methodologyCategory: z
    .string()
    .max(5, VALIDATION_MESSAGES.MAX_CHARACTERS(5))
    // .regex(REGEX_PATTERNS.METHOD_CATEGORY, REGEX_MESSAGES.METHOD_CATEGORY)
    .optional(),

  methodologyNumber: z
    .string()
    .min(1, '必須項目です')
    .max(3, VALIDATION_MESSAGES.MAX_CHARACTERS(3))
    .regex(REGEX_PATTERNS.NUMERIC_3_DIGITS, REGEX_MESSAGES.NUMERIC_3_DIGITS),

  methodologyVersion: z
    .string()
    .min(1, '必須項目です')
    .max(10, VALIDATION_MESSAGES.MAX_CHARACTERS(10))
    // .regex(REGEX_PATTERNS.METHOD_VER, REGEX_MESSAGES.METHOD_VER)
    ,

  projectType: z.enum(PROJECT_TYPE_OPTIONS, PROJECT_TYPE_VALIDATION),

  approachDetails: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000))
    // .regex(REGEX_PATTERNS.JAPANESE_ONLY, REGEX_MESSAGES.JAPANESE_ONLY)
    ,

  reductionMethodSummary: z
    .string()
    .min(1, '必須項目です')
    .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000))
    // .regex(REGEX_PATTERNS.JAPANESE_ONLY, REGEX_MESSAGES.JAPANESE_ONLY)
    ,

  fuelTypeBeforeImpl: z
    .string()
    .min(1, '必須項目です')
    .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),

  targetEquipmentBeforeImpl: z
    .string()
    .min(1, '必須項目です')
    .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),

  overviewDiagramBeforeAct: z.string().min(1, '必須項目です'),

  fuelTypeAfterImpl: z
    .string()
    .min(1, '必須項目です')
    .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),

  targetEquipmentAfterImpl: z
    .string()
    .min(1, '必須項目です')
    .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),

  overviewDiagramAfterAct: z.string().min(1, '必須項目です'),

  reductionActivityMethodCondition: z
    .string()
    .min(1, '必須項目です')
    .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000))
    // .regex(REGEX_PATTERNS.JAPANESE_ONLY, REGEX_MESSAGES.JAPANESE_ONLY)
    ,
});
