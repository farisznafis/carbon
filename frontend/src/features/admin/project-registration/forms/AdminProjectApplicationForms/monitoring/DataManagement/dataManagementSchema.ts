import {
  IS_SIMILAR_OPTIONS,
  IS_SIMILAR_VALIDATION,
  SELECT_TYPE_REQUIRED_VALIDATION,
} from '@/lib/constants/select_types';
import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const dataManagementSchema = z
  .object({
    dataManagementPic: z
      .string()
      .min(1, '必須項目です')
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),
    // .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS),

    monitoringPic: z
      .string()
      .min(1, '必須項目です')
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),
    // .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS),

    moniDataAcquiring: z
      .string()
      .min(1, '必須項目です')
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

    moniDataCollection: z
      .string()
      .min(1, '必須項目です')
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

    moniDataRec: z
      .string()
      .min(1, '必須項目です')
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

    dataStoring: z.number(),

    isRisksForEmission: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION),

    riskFactors: z
      .string()
      .min(1, '必須項目です')
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000))
      // .regex(REGEX_PATTERNS.JAPANESE_ONLY, REGEX_MESSAGES.JAPANESE_ONLY)
      .optional(),

    isSimilar: z.boolean({ message: '必須項目です' }),

    similarSystemName: z
      .string()
      .min(1, '必須項目です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .optional(),
    // .regex(REGEX_PATTERNS.HALFWIDTH_ORI, REGEX_MESSAGES.HALFWIDTH_ORI),

    similarSystemPeriodStart: z.coerce.date().optional(),

    similarSystemPeriodEnd: z.coerce.date().optional(),

    isIndependentofLegal: z.boolean({ message: '必須項目です' }),
  })
  .refine(
    // 類似制度に登録している場合のみ、制度名と期間が要る
    (data) => {
      if (data.isSimilar === true) {
        return (
          data.similarSystemName &&
          data.similarSystemPeriodStart &&
          data.similarSystemPeriodEnd
        );
      }
      return true;
    },
    {
      message:
        '類似制度に登録している場合、類似制度名と認証予定期間を入力してください',
    },
  );
