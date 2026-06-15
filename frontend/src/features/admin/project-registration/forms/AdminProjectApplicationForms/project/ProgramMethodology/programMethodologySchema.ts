import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const programMethodologySchema = z.object({
  opeManageStructure: z
    .string()
    .min(1, '必須項目です')
    .max(3000, VALIDATION_MESSAGES.MAX_CHARACTERS(3000)),

  structureDiagram: z.instanceof(File).optional(),

  methodContentOfAggreement: z
    .string()
    .min(1, '必須項目です')
    .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000)),

  infoCollManageMethod: z
    .string()
    .min(1, '必須項目です')
    .max(2000, VALIDATION_MESSAGES.MAX_CHARACTERS(2000)),

  domesticImplementation: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  registrationInput: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  certExtensionInput: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  mustImplAfterTwoYears: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  additionality: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  implByApprovedMethod: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  otherProjectJcredit: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  otherSimilarSystem: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  methodIsCommonInfo: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  moniItemIsCommonInfo: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),
});
