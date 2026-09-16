import { z } from 'zod';
import { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import {
  REGEX_PATTERNS,
  REGEX_MESSAGES,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import {
  PREFECTURE_OPTIONS,
  PREFECTURE_VALIDATION,
} from '@/lib/constants/prefectures';
import {
  IS_DESIGNATED_EMITTER_VALIDATION,
  SELECT_TYPE_REQUIRED_VALIDATION,
} from '@/lib/constants/select_types';

/**
 * J-クレジット制度 排出削減プロジェクト（プログラム型）登録申請
 * プログラム型運営・管理者関連情報（抜粋）
 */
export const otherProjectOperatorSchema = z
  .object({
    projectOperatorNameJa: z
      .string()
      .min(1, '必須項目です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(REGEX_PATTERNS.KANJI_SYMBOLS, REGEX_MESSAGES.KANJI_SYMBOLS),
    // .regex(/^[一-龯々〆ヵヶ&.,'\-]+$/, "漢字、「& . , ’ -」のみ使用できます"), // only kanji and symbol

    projectOperatorNameKana: z
      .string()
      .min(1, '必須項目です')
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
      .regex(REGEX_PATTERNS.KATAKANA_MINUS, REGEX_MESSAGES.KATAKANA_MINUS),
    // .regex(
    //   /^[ァ-ヶー\-]+$/,
    //   "全角カタカナ、長音記号(ー)、ハイフン(-)のみ使用できます",
    // ), // only katakana and - symbol

    /** 法人番号 */ //maybe import from other schema
    otherCorpNumber: z
      .string()
      // .regex(/^\d{13}$/, "13桁の数字で入力してください")
      // .regex(REGEX_PATTERNS.NUMERIC_13_DIGITS, REGEX_MESSAGES.NUMERIC_13_DIGITS)
      .optional(),

    /** 郵便番号 — fills the 本社所在地 fields; not on the official sheet */
    postalCode: z
      .string()
      .regex(/^\d{7}$/, '郵便番号はハイフンなしの7桁の数字で入力してください')
      .optional()
      .or(z.literal('')),

    /** 本社所在地（登記上の本店所在地） */ //maybe import from other schema
    prefecture: z.enum(PREFECTURE_OPTIONS, PREFECTURE_VALIDATION),

    city: z
      .string()
      .min(1, '必須項目です')
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),
    // .regex(/^[一-龯々〆ヵヶ\-]+$/, "漢字のみ使用できます"), // only kanji

    address: z
      .string()
      .min(1, '必須項目です')
      .max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(500)),
    // .regex(REGEX_PATTERNS.JAPANESE_ENGLISH, REGEX_MESSAGES.JAPANESE_ENGLISH)
    /** 代表者役職 */
    representativePosition: z
      .string()
      .min(1, '必須項目です')
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),

    isChangeRepresentativePosition: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    /** 代表者氏名 */
    representativeName: z
      .string()
      .min(1, '必須項目です')
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),

    isChangeRepresentativeName: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    picPosition: z
      .string()
      .min(1, '必須項目です')
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100)),

    isChangePicPosition: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    picName: z
      .string()
      .min(1, '必須項目です')
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50)),

    isChangePicName: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    picEmail: z
      .string()
      .email('メールアドレスの形式で入力してください')
      .min(1, '必須項目です')
      .max(254, VALIDATION_MESSAGES.MAX_CHARACTERS(254)),

    isChangePicEmail: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    picPhoneNumber: z
      .string()
      .min(1, '必須項目です')
      .max(20, VALIDATION_MESSAGES.MAX_NUMBERS(20))
      .regex(REGEX_PATTERNS.PHONE_NUMBER_DIGITS, REGEX_MESSAGES.PHONE_NUMBER_DIGITS),

    isChangePicPhoneNumber: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    /** 特定排出者であるか */
    isDesignatedEmitter: z.boolean(IS_DESIGNATED_EMITTER_VALIDATION),

    /** 特定排出者コード（特定排出者である場合） */ //get from other schema
    emitterCode: z
      .string()
      .min(1, '必須項目です')
      .max(9, VALIDATION_MESSAGES.MAX_NUMBERS(9))
      .regex(REGEX_PATTERNS.NUMERIC_9_DIGITS, REGEX_MESSAGES.NUMERIC_9_DIGITS)
      .optional(),

    /** プログラム型運営・管理者の地球温暖化対策の推進に関する法律に基づく算定・報告・公表制度の有無 */
    hasEnergyActReport: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION),
    energyActOpeNumber: z
      .string()
      .max(50, VALIDATION_MESSAGES.MAX_NUMBERS(50))
      // .regex(/^[A-Za-z0-9]+$/)
      .optional(),

    energyManageFactoryNumber: z
      .string()
      .max(50, VALIDATION_MESSAGES.MAX_NUMBERS(50))
      // .regex(/^[A-Za-z0-9]+$/)
      .optional(),
  })
  .refine(
    (data) => {
      // If isDesignatedEmitter is true, emitterCode must be present and non-empty
      if (data.isDesignatedEmitter === true) {
        return data.emitterCode && data.emitterCode.trim().length > 0;
      }
      return true; // No restriction if isDesignatedEmitter is false or undefined
    },
    {
      message: '特定排出者である場合、特定排出者コードは必須です',
      path: ['emitterCode'],
    },
  );

type OtherOperatorFormValues = FormValues['otherProjectOperator'];
