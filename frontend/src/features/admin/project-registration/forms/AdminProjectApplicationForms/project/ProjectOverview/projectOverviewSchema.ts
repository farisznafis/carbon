import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import {
  PREFECTURE_OPTIONS,
  PREFECTURE_VALIDATION,
} from '@/lib/constants/prefectures';
import { z } from 'zod';
import {
  REDUCTION_ACTIVITY_INCLUDED_OPTIONS,
  REDUCTION_ACTIVITY_INCLUDED_VALIDATION,
} from '@/lib/constants/select_types';

export const projectOverviewSchema = z
  .object({
    projectOperatorNameKana: z
      .string()
      // .min(1, "必須項目です")
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(REGEX_PATTERNS.KATAKANA_MINUS, REGEX_MESSAGES.KATAKANA_MINUS),

    projectOperatorNameJa: z
      .string()
      // .min(1, "必須項目です")
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS)
      .optional(),

    corpNumber: z
      .string()
      .min(1, '必須項目です')
      .max(13, VALIDATION_MESSAGES.MAX_NUMBERS(13))
      .regex(
        REGEX_PATTERNS.NUMERIC_13_DIGITS,
        REGEX_MESSAGES.NUMERIC_13_DIGITS,
      ),
    // .optional()

    prefecture: z.enum(PREFECTURE_OPTIONS, PREFECTURE_VALIDATION),

    city: z
      .string()
      .min(1, '市区町村は必須です')
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),
    // .regex(REGEX_PATTERNS.JAPANESE_ENGLISH, REGEX_MESSAGES.JAPANESE_ENGLISH)
    address: z
      .string()
      .max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(500))
      // .regex(REGEX_PATTERNS.JAPANESE_ENGLISH, REGEX_MESSAGES.JAPANESE_ENGLISH)

      .optional(),

    overview: z
      .string()
      .min(1, '必須項目です')
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),
    // .regex(REGEX_PATTERNS.JAPANESE_ENGLISH, REGEX_MESSAGES.JAPANESE_ENGLISH)
    companyName: z
      .string()
      // .min(1, '必須項目です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(REGEX_PATTERNS.FULLWIDTH_ORI, REGEX_MESSAGES.FULLWIDTH_ORI)
      .optional(),

    reductionActivityNameKana: z
      .string()
      // .min(1, '必須項目です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(REGEX_PATTERNS.KATAKANA_MINUS, REGEX_MESSAGES.KATAKANA_MINUS)
      .optional(),

    reductionActivityName: z
      .string()
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(REGEX_PATTERNS.FULLWIDTH_ORI, REGEX_MESSAGES.FULLWIDTH_ORI)
      .optional(),

    reductionActivityIncluded: z.enum(
      REDUCTION_ACTIVITY_INCLUDED_OPTIONS,
      REDUCTION_ACTIVITY_INCLUDED_VALIDATION,
    ),

    cnActParticipationDetails: z
      .string()
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000))
      .optional(),

    holderAccountNameKana: z
      .string()
      .min(1, '必須項目です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(REGEX_PATTERNS.KATAKANA_MINUS, REGEX_MESSAGES.KATAKANA_MINUS),

    holderAccountName: z
      .string()
      .min(1, '必須項目です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(REGEX_PATTERNS.FULLWIDTH_ORI, REGEX_MESSAGES.FULLWIDTH_ORI),

    holderCorpNumber: z
      .string()
      .max(13, VALIDATION_MESSAGES.MAX_NUMBERS(13))
      .regex(/^\d{13}$/, '13桁の数字で入力してください')
      .optional(),

    holderPrefecture: z.enum(PREFECTURE_OPTIONS, PREFECTURE_VALIDATION),

    holderCity: z
      .string()
      .min(1, '必須項目です')
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
      .regex(REGEX_PATTERNS.FULLWIDTH_ORI, REGEX_MESSAGES.FULLWIDTH_ORI),

    holderAddress: z
      .string()
      .min(1, '必須項目です')
      .max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(500)),
    // .regex(REGEX_PATTERNS.JAPANESE_ENGLISH, REGEX_MESSAGES.JAPANESE_ENGLISH)
    isHolderUndecided: z.boolean(),

    location: z
      .string()
      .max(10, VALIDATION_MESSAGES.MAX_CHARACTERS(10))
      .regex(REGEX_PATTERNS.FULLWIDTH_ORI, REGEX_MESSAGES.FULLWIDTH_ORI)
      .optional(),

    projectLocationClassificationEn: z
      .string()
      .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20))
      .regex(REGEX_PATTERNS.HALFWIDTH_ORI, REGEX_MESSAGES.HALFWIDTH_ORI)
      .optional(),

    regionalCategory: z
      .string()
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
      .regex(REGEX_PATTERNS.FULLWIDTH_ORI, REGEX_MESSAGES.FULLWIDTH_ORI)
      .optional(),

    targetArea: z
      .string()
      .min(1, '必須項目です')
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100))
      .regex(REGEX_PATTERNS.FULLWIDTH_ORI, REGEX_MESSAGES.FULLWIDTH_ORI),

    creditUtilizationPlan: z
      .string()
      .min(1, '必須項目です')
      .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),
    // .regex(REGEX_PATTERNS.JAPANESE_ENGLISH, REGEX_MESSAGES.JAPANESE_ENGLISH)
    projectConceptDiagram: z.instanceof(File),

    isMethodologyValid: z.boolean(),

    certTargetPeriodStart: z.coerce.date({
      required_error: '必須項目です',
      invalid_type_error: '正しい日付を入力してください',
    }),

    certTargetPeriodEnd: z.coerce.date({
      required_error: '必須項目です',
      invalid_type_error: '正しい日付を入力してください',
    }),

    // auto calculation
    certTargetPeriodYears: z
      .string()
      .regex(/^\d+$/, '有効な数値を入力してください')
      .optional(),
  })
  .refine(
    (data) => {
      // if reductionActivityIncluded is カーボンニュート..., cnActParticipationDetails is required
      if (
        data.reductionActivityIncluded ===
        'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含まない'
      ) {
        return (
          data.cnActParticipationDetails &&
          data.cnActParticipationDetails.trim().length > 0
        );
      }
      return true;
    },
    {
      message:
        'カーボンニュートラル活動に参加している場合、参加の詳細を入力してください',
      path: ['cnActParticipationDetails'],
    },
  )
  .refine(
    (data) => {
      // if reductionActivityIncluded is カーボンニュート..., cnActParticipationDetails is required
      if (
        data.reductionActivityIncluded ===
        'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含まない'
      ) {
        return !data.cnActParticipationDetails;
      }
      // if isMethodologyValid is false, certTargetPeriodStart, certTargetPeriodEnd and certTargetPeriodYears are required
      if (data.isMethodologyValid === false) {
        return (
          data.certTargetPeriodStart &&
          data.certTargetPeriodEnd &&
          data.certTargetPeriodYears
        );
      }
      return true;
    },
    {
      message:
        '手法が有効でない場合、認証対象期間と認証対象期間の年数を入力してください',
      path: [
        'certTargetPeriodStart',
        'certTargetPeriodEnd',
        'certTargetPeriodYears',
      ],
    },
  );
