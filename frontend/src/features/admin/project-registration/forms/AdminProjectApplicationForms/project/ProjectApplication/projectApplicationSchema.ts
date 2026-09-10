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
import { REGIONAL_OPTIONS } from '@/lib/constants/regionals';
import { METHOD_OPTIONS } from '@/lib/constants/methodology';
import {
  REVIEWER_NAME_OPTIONS,
  REVIEWER_NAME_VALIDATION,
} from '@/lib/constants/reviewer_names';
import {
  SELECT_TYPE_REQUIRED_VALIDATION,
} from '@/lib/constants/select_types';

export const LOCATION_OPTIONS = z.enum([
  '家庭', // household
  '事業所', // business
  '家庭および事業所', // household and business
  '水田', // rice paddy
  '農家', // farmhouse
  '公共施設', // public facility
  '建築物', // building
]);

export const APPLICATION_CATEGORY_OPTIONS = [
  '登録',
  '計画変更',
  '認証権限保有主体変更',
] as const;

export const projectApplicationSchema = z
  .object({
    applicationCategory: z.enum(APPLICATION_CATEGORY_OPTIONS),

    isUsingCostSupport: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION),

    projectOperatorKana: z
      .string()
      .min(1, 'プロジェクト運営者（カナ）は必須です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(
        REGEX_PATTERNS.FULLWIDTH_KATAKANA,
        REGEX_MESSAGES.FULLWIDTH_KATAKANA,
      )
      .optional(),

    projectOperatorJa: z
      .string()
      .min(1, 'プロジェクト運営者（日本語）は必須です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .optional(),
    // .regex(
    //   REGEX_PATTERNS.FULLWIDTH_NO_SYMBOLS,
    //   REGEX_MESSAGES.FULLWIDTH_NO_SYMBOLS,
    // )
    projectOperatorEn: z
      .string()
      .min(1, 'プロジェクト運営者（英語）は必須です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .optional(),
    // .regex(REGEX_PATTERNS.HALFWIDTH_SYMBOLS, REGEX_MESSAGES.HALFWIDTH_SYMBOLS)
    corpNumber: z
      .string()
      .min(1, '法人番号（13桁の数字）は必須です')
      .max(13, VALIDATION_MESSAGES.MAX_NUMBERS(13))
      .regex(REGEX_PATTERNS.NUMERIC_13_DIGITS, REGEX_MESSAGES.NUMERIC_13_DIGITS)
      .optional(),

    corpPosition: z
      .string()
      .min(1, '法人代表者の役職は必須です')
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100))
      .optional(),

    corpName: z
      .string()
      .min(1, '法人代表者の氏名は必須です')
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .optional(),

    prefecture: z.enum(PREFECTURE_OPTIONS, PREFECTURE_VALIDATION).optional(),

    city: z
      .string()
      .min(1, '市区町村は必須です')
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100))
      .optional(),
    // .regex(REGEX_PATTERNS.JAPANESE_ENGLISH, REGEX_MESSAGES.JAPANESE_ENGLISH),

    address: z
      .string()
      .min(1, '市区町村以降の住所は必須です')
      .max(500, VALIDATION_MESSAGES.MAX_CHARACTERS(500))
      .optional(),

    isDesignatedEmitter: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    // show when isDesignatedEmitter is yes
    emitterCode: z
      .string()
      .min(1, '特定排出者コードは必須です')
      .max(9, VALIDATION_MESSAGES.MAX_CHARACTERS(9))
      .regex(REGEX_PATTERNS.NUMERIC_9_DIGITS, REGEX_MESSAGES.NUMERIC_9_DIGITS)
      .optional(),

    hasEnergyActPeriodReport: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    energyActOpeNumber: z
      .string()
      .min(1, '特定排出者コードは必須です')
      .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20)),
    // .regex(REGEX_PATTERNS.HALFWIDTH_SYMBOLS, REGEX_MESSAGES.HALFWIDTH_SYMBOLS)
    // .optional(),

    energyManageFactoryNumber: z
      .string()
      .min(1, '特定排出者コードは必須です')
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
      // .regex(
      //   REGEX_PATTERNS.HALFWIDTH_SYMBOLS,
      //   REGEX_MESSAGES.HALFWIDTH_SYMBOLS,
      // )
      .optional(),

    representativePosition: z
      .string()
      .min(1, '特定排出者コードは必須です')
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100))
      // .regex(
      //   REGEX_PATTERNS.FULLWIDTH_NO_SYMBOLS,
      //   REGEX_MESSAGES.FULLWIDTH_NO_SYMBOLS,
      // )
      .optional(),

    isChangeRepresentativePosition: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    representativeName: z
      .string()
      .min(1, '必須項目です')
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
      .optional(),

    isChangeRepresentativeName: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    picPosition: z
      .string()
      .min(1, '必須項目です')
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100))
      .optional(),
    // .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS)

    isChangePicPosition: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    picName: z
      .string()
      .min(1, '必須項目です')
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
      .optional(),
    // .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS)
    isChangePicName: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    picEmail: z
      .string()
      .min(1, '必須項目です')
      .max(254, VALIDATION_MESSAGES.MAX_CHARACTERS(254))
      .email('メールアドレスの形式が正しくありません')
      .optional(),

    isChangePicEmail: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    picPhone: z
      .string()
      .min(1, '必須項目です')
      .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20))
      .regex(REGEX_PATTERNS.PHONE_NUMBER, REGEX_MESSAGES.PHONE_NUMBER)
      .optional(),

    isChangePicPhoneNumber: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    otherContactBusinessName: z
      .string()
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      // .regex(
      //   REGEX_PATTERNS.FULLWIDTH_NO_SYMBOLS,
      //   REGEX_MESSAGES.FULLWIDTH_NO_SYMBOLS,
      // )

      .optional(),

    isChangeOtherContactBusinessName: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    otherContactPicPosition: z
      .string()
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100))
      // .regex(
      //   REGEX_PATTERNS.FULLWIDTH_NO_SYMBOLS,
      //   REGEX_MESSAGES.FULLWIDTH_NO_SYMBOLS,
      // )

      .optional(),

    isChangeOtherContactPicPosition: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    otherContactsPicName: z
      .string()
      .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
      .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS)
      .optional(),

    isChangeOtherContactsPicName: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    otherContactsPicEmail: z
      .string()
      .max(254, VALIDATION_MESSAGES.MAX_CHARACTERS(254))
      .email('メールアドレスの形式が正しくありません')
      .optional(),

    isChangeOtherContactsPicEmail: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    otherContactsPicPhoneNumber: z
      .string()
      .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20))
      .regex(REGEX_PATTERNS.PHONE_NUMBER, REGEX_MESSAGES.PHONE_NUMBER)
      .optional(),

    isChangeOtherContactsPicPhoneNumber: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    projectNumber: z
      .string()
      .max(10, VALIDATION_MESSAGES.MAX_CHARACTERS(10))
      .regex(
        REGEX_PATTERNS.HALFWIDTH_NO_SYMBOLS,
        REGEX_MESSAGES.HALFWIDTH_NO_SYMBOLS,
      )
      .optional(),

    projectName: z
      .string()
      .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100))
      .regex(REGEX_PATTERNS.JAPANESE_ENGLISH, REGEX_MESSAGES.JAPANESE_ENGLISH)
      .optional(),

    methodologyCategory: z.enum(METHOD_OPTIONS).optional(),

    methodologyNumber: z
      .string()
      .max(3, VALIDATION_MESSAGES.MAX_CHARACTERS(3))
      .regex(
        REGEX_PATTERNS.HALFWIDTH_NO_SYMBOLS,
        REGEX_MESSAGES.HALFWIDTH_NO_SYMBOLS,
      )
      .optional(),

    methodology: z
      .string()
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .optional(),

    location: LOCATION_OPTIONS.optional(),

    // 参加企業/管理会社/SaaS
    // not sure
    SaaS: z.string().min(1, '妥当性確認審査機関名は必須です').optional(),
    // project_outline
    // not sure
    projectOutline: z
      .string()
      .min(1, '妥当性確認審査機関名は必須です')
      .optional(),

    regionCategory: z.enum(REGIONAL_OPTIONS).optional(),

    groupName: z.string().max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255)),
    // .regex(REGEX_PATTERNS.JAPANESE_ENGLISH, REGEX_MESSAGES.JAPANESE_ENGLISH)
    groupNameFurigana: z
      .string()
      .max(255, VALIDATION_MESSAGES.MAX_CHARACTERS(255))
      .regex(REGEX_PATTERNS.KATAKANA_MINUS, REGEX_MESSAGES.KATAKANA_MINUS),

    isParticipatingCnAct: z.boolean(SELECT_TYPE_REQUIRED_VALIDATION).optional(),

    // not sure
    validationCheckReviewerName: z
      .enum(REVIEWER_NAME_OPTIONS, REVIEWER_NAME_VALIDATION)
      .optional(),

    applicationDate: z.coerce.date().optional(),
    // registrationDate: z.string().min(1, "登録申請日（西暦年月日）は必須です"),
    registrationDate: z.coerce.date().optional(),

    // not sure
    reviewerName: z
      .enum(REVIEWER_NAME_OPTIONS, REVIEWER_NAME_VALIDATION)
      .optional(),

    applicationChangeDate: z.coerce.date().optional(),
  })
  .refine(
    (data) => {
      if (data.applicationCategory === '認証権限保有主体変更') {
        return true;
      } else {
        // Check required fields
        const baseRequired =
          data.isUsingCostSupport !== undefined &&
          data.projectOperatorKana &&
          data.projectOperatorJa &&
          data.projectOperatorEn &&
          data.corpNumber &&
          data.corpPosition &&
          data.corpName &&
          data.prefecture &&
          data.city &&
          data.address &&
          data.isDesignatedEmitter !== undefined &&
          data.hasEnergyActPeriodReport !== undefined &&
          data.picPosition &&
          data.picName &&
          data.picEmail &&
          data.picPhone &&
          data.projectNumber &&
          data.projectName &&
          data.methodologyCategory &&
          data.methodologyNumber &&
          data.methodology &&
          data.location &&
          data.SaaS &&
          data.projectOutline &&
          data.regionCategory &&
          data.isParticipatingCnAct !== undefined &&
          data.validationCheckReviewerName &&
          data.reviewerName;
        // Conditional required
        const emitterRequired =
          data.isDesignatedEmitter === true
            ? data.emitterCode
            : true;
        const energyRequired =
          data.hasEnergyActPeriodReport === true
            ? data.energyActOpeNumber
            : true;
        return baseRequired && emitterRequired && energyRequired;
      }
    },
    { message: '必須項目が入力されていません' },
  );

export type projectApplicationValues = FormValues['projectApplication'];
