import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';

export const authorityHolderChangeSchema = z.object({
  submissionDate: z.coerce.date(),

  projectNumber: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),

  projectOperatorName: z
    .string()
    .min(1, '必須項目です')
    .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255)),

  newAccountNumber: z
    .string()
    .min(1, '必須項目です')
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),

  newCorpName: z
    .string()
    .min(1, '必須項目です')
    .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255)),

  newPicPositionAndName: z
    .string()
    .min(1, '必須項目です')
    .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255)),

  newPicEmail: z
    .string()
    .min(1, '必須項目です')
    .email('メールアドレスの形式で入力してください')
    .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255)),
});
