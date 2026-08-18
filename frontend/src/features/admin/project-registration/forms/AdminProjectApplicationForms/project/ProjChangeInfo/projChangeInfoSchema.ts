import {
  REVIEWER_NAME_OPTIONS,
  REVIEWER_NAME_VALIDATION,
} from '@/lib/constants/reviewer_names';
import { z } from 'zod';

export const projChangeInfoSchema = z.object({
  numberOfChanges: z.number().min(1, '必須項目です'),

  applicationChangeDate: z.coerce.date(),
  effectiveDate: z.coerce.date(),
  
  changeTypeOperatorName: z.boolean(),
  
  changeTypeCreditHolder: z.boolean(),
  
  changeTypeExtension: z.boolean(),
  
  changeTypeOther: z.boolean(),
  
  overview: z
    .string()
    .min(1, '必須項目です')
    .max(1000, '文字列は最大1000文字までです'),

  isNeedRevalidation: z.boolean({ message: '必須項目です' }),

  reviewerName: z.enum(REVIEWER_NAME_OPTIONS, REVIEWER_NAME_VALIDATION),

  historyNumberOfChanges: z.number().min(1, '必須項目です'),

  historyApplicationChangeDate: z.coerce.date(),

  historyChangeTypeOperatorName: z.boolean(),

  historyChangeTypeCreditHolder: z.boolean(),

  historyChangeTypeExtension: z.boolean(),

  historyChangeTypeOther: z.boolean(),

  historyOverview: z
    .string()
    .min(1, '必須項目です')
    .max(1000, '文字列は最大1000文字までです'),
});

export type projChangeInfoValue = z.infer<typeof projChangeInfoSchema>;
