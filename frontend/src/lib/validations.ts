import { z } from 'zod';

export const REGEX_PATTERNS = {
  KANJI: /^[一-龯々〆〤\s]*$/,
  JAPANESE_ONLY: /^[ぁ-んァ-ヶ一-龯々〆ヵヶ\s　]+$/, // 日本語 | 記号制限：-
  HALFWIDTH_ORI: /^[A-Za-z0-9]+$/, // 半角英数 | 記号制限：-
  HALFWIDTH_SYMBOLS: /^[A-Za-z0-9&.,'\s\-]+$/,
  HALFWIDTH_NO_SYMBOLS: /^[\x00-\x7F]+$/,
  HALFWIDTH_NUMERIC_ONLY: /^\d+$/, // 半角数字
  HALFWIDTH_ALPHA_ONLY: /^[A-Za-z]+$/, // 半角英字
  
  FULLWIDTH_ORI: /^[^\x00-\x7F]+$/, // 全角文字 | 記号制限：-
  FULLWIDTH_KATAKANA: /^[ァ-ヶー　]*$/, // 全角カタカナのみ
  FULLWIDTH_SYMBOLS: /^[\u3000-\u9FFF＆．，’－\s]+$/, // kanji, kana, symbols
  FULLWIDTH_NO_SYMBOLS: /^[\u3000-\u9FFF\u3040-\u30FFＡ-Ｚａ-ｚ０-９]+$/, // kanji, kana, no symbols

  KATAKANA_MINUS: /^[ァ-ヶー\-]+$/, // only katakana and - symbol
  KANJI_SYMBOLS: /^[一-龯々〆ヵヶ&.,'\s\-]+$/, // only kanji and &.,’- symbol
  JAPANESE_ENGLISH: /^[ぁ-んァ-ヶ一-龯々〆ヵヶA-Za-z0-9\s　]+$/, // japanese and english 日本語・英数字可
  JAPANESE_ENGLISH_NO_NUM: /^[ぁ-んァ-ヶ一-龯々〆ヵヶA-Za-z\s　]+$/, // japanese and english no number
  PROJECT_NUM: /^P[0-9]+$/, // number no symbols
  
  // Contact validations
  PHONE_NUMBER_DIGITS: /^\+?\d{9,20}$/, // backend @PhoneNumber: digits only, no hyphens
  NUMERIC_3_DIGITS: /^\d{3}$/,                
  NUMERIC_9_DIGITS: /^\d{9}$/,                
  NUMERIC_13_DIGITS: /^\d{13}$/, 
  NUMERIC_12_1: /^\d{1,11}(\.\d{1})?$/,          

  EMAIL: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
  METHOD_VER: /^Ver.[0-9]+(.[0-9]+)?$/,
  METHOD_CATEGORY: /^EN-[A-Z]$/
} as const;

export const REGEX_MESSAGES = {
  KANJI: "漢字のみ入力してください",
  JAPANESE_ONLY: "日本語（ひらがな・カタカナ・漢字）で入力してください",
  HALFWIDTH_ORI: "半角英数字で入力してください",
  HALFWIDTH_SYMBOLS: "半角英数字および「& . , ' -」のみで入力してください",
  HALFWIDTH_NO_SYMBOLS: "半角英数字のみで入力してください",
  HALFWIDTH_NUMERIC_ONLY: "半角数字で入力してください",
  HALFWIDTH_ALPHA_ONLY: "半角英字で入力してください",
  
  FULLWIDTH_ORI: "全角文字で入力してください",
  FULLWIDTH_KATAKANA: "全角カタカナのみ使用できます",
  FULLWIDTH_SYMBOLS: "全角文字のみで入力してください",
  FULLWIDTH_NO_SYMBOLS: "全角文字（記号を除く）で入力してください",

  KATAKANA_MINUS: "全角カタカナ、長音記号(ー)、ハイフン(-)のみ使用できます",
  KANJI_SYMBOLS: "漢字、「& . , ’ -」のみ使用できます",

  JAPANESE_ENGLISH: "日本語・英数字・スペースのみ使用できます",
  JAPANESE_ENGLISH_NO_NUM: "日本語・英数字・スペースのみ使用できます",
  PROJECT_NUM: "「P」で始まる半角数字のみで入力してください",
  
  PHONE_NUMBER_DIGITS: "電話番号はハイフンなしの半角数字で入力してください（例: 0312345678）",
  NUMERIC_3_DIGITS: "3桁の数字で入力してください", // exactly 3 digits no less and more
  NUMERIC_9_DIGITS: "9桁の数字で入力してください", // exactly 9 digits no less and more
  NUMERIC_13_DIGITS: "13桁の数字で入力してください", // exactly 13 digits
  NUMERIC_12_1: "最大11桁（小数点以下1桁まで）で入力してください（小数点は「.」", // decimal 11 digits and 1 digit after comma

  EMAIL: "有効なメールアドレスを入力してください",
  METHOD_VER: "有効な方法論バージョンを入力してください",
  METHOD_CATEGORY: "適用方法論を入力してください"
} as const;

export const VALIDATION_MESSAGES = {
  REQUIRED: "この項目は必須です",
  MAX_LENGTH: (max: number) => `文字列は最大${max}文字までです`,
  MIN_LENGTH: (min: number) => `文字列は最低${min}文字必要です`,
  MAX_NUMBERS: (max: number) => `最大${max}までです`,
  MIN_NUMBERS: (min: number) => `最低${min}以上必要です`,
  MAX_CHARACTERS: (max: number) => `最大${max}単語までです`,
  RANGE_LENGTH: (min: number, max: number) => `${min}文字から${max}文字の間で入力してください`,
  RANGE_NUMBER: (min: number, max: number) => `${min}から${max}の間で入力してください`,
}

export const requiredNumberSchema = (label: string) =>
  z.preprocess((v) => {
    if (v === '' || v == null) return undefined;
    const n = typeof v === 'string' ? Number(v) : v;
    return Number.isFinite(n) ? n : undefined;
  }, z.number({
    required_error: `${label}を入力してください`,
    invalid_type_error: `${label}は数値で入力してください`,
  }));

export const requiredFileSchema = (label: string) =>
  z.any().refine((v) => v instanceof File, {
    message: `${label}を選択してください`,
  });
  
// ADRIAN-SAN CODE --------------------------------------------------

// Japanese postal code validation (7 digits)
export const postalCodeSchema = z
  .string()
  .regex(/^\d{7}$/, '郵便番号は7桁の数字で入力してください')
  .or(z.string().regex(/^\d{3}-\d{4}$/, '郵便番号は「123-4567」の形式で入力してください'));

// Japanese phone number validation
export const phoneNumberSchema = z
  .string()
  .regex(/^0\d{9,10}$/, '有効な電話番号を入力してください（例: 0312345678）')
  .or(z.string().regex(/^0\d{1,4}-\d{1,4}-\d{4}$/, '電話番号は「03-1234-5678」の形式で入力してください'));

// Katakana validation
export const katakanaSchema = z
  .string()
  .regex(/^[ァ-ヶー]+$/, 'カタカナで入力してください');

// Corporate number validation (13 digits)
export const corporateNumberSchema = z
  .string()
  .regex(/^\d{13}$/, '法人番号は13桁の数字で入力してください');

// Email validation with custom message
export const emailSchema = z
  .string()
  .min(1, 'メールアドレスを入力してください')
  .email('有効なメールアドレスを入力してください')
  .max(255, 'メールアドレスは255文字以内で入力してください');

/** Mirrors the backend policy; the common-password blocklist is only checked server-side (errors.newPassword). */
export const passwordSchema = z
  .string()
  .min(8, "パスワードは8文字以上で入力してください")
  .max(64, "パスワードは64文字以内で入力してください")
  .regex(/^[\x20-\x7E]+$/, "パスワードは半角英数字・記号で入力してください")
  .regex(
    /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)/,
    "大文字・小文字・数字をそれぞれ1文字以上含めてください"
  );

export const profileFormSchema = z.object({
  // Company Information
  corporateNumber: corporateNumberSchema,
  companyName: z.string().min(1, '企業名を入力してください').max(100, '企業名は100文字以内で入力してください'),
  companyNameKana: katakanaSchema.min(1, '企業名（カナ）を入力してください').max(100, '企業名（カナ）は100文字以内で入力してください'),
  companyNameEnglish: z.string().max(100, '企業名（英語）は100文字以内で入力してください').optional(),

  // Address
  postalCode: postalCodeSchema,
  prefecture: z.string().min(1, '都道府県を選択してください'),
  city: z.string().min(1, '市区町村を入力してください').max(50, '市区町村は50文字以内で入力してください'),
  address: z.string().min(1, '番地を入力してください').max(100, '番地は100文字以内で入力してください'),
  building: z.string().max(100, '建物名は100文字以内で入力してください').optional(),

  // Personal Information
  lastName: z.string().min(1, '名前（姓）を入力してください').max(50, '名前（姓）は50文字以内で入力してください'),
  firstName: z.string().min(1, '名前（名）を入力してください').max(50, '名前（名）は50文字以内で入力してください'),
  lastNameKana: katakanaSchema.min(1, '名前（姓）（カナ）を入力してください').max(50, '名前（姓）（カナ）は50文字以内で入力してください'),
  firstNameKana: katakanaSchema.min(1, '名前（名）（カナ）を入力してください').max(50, '名前（名）（カナ）は50文字以内で入力してください'),
  position: z.string().min(1, '役職を入力してください').max(50, '役職は50文字以内で入力してください'),
  phoneNumber: phoneNumberSchema,
  faxNumber: phoneNumberSchema.optional().or(z.literal('')),
  email: emailSchema,
});

export const userSchema = z.object({
  username: z.string().min(1, 'ユーザー名を入力してください').max(50, 'ユーザー名は50文字以内で入力してください'),
  lastName: z.string().min(1, '姓を入力してください').max(50, '姓は50文字以内で入力してください'),
  firstName: z.string().min(1, '名を入力してください').max(50, '名は50文字以内で入力してください'),
  lastNameKana: katakanaSchema,
  firstNameKana: katakanaSchema,
  userPosition: z.string().min(1, '担当者の役職を入力してください。').max(50, '担当者の役職は50文字以内で入力してください'),
  phoneNumber: phoneNumberSchema,
  siteId: z.string().min(1, '拠点を選択してください').nullable(),
  email: emailSchema,
  password: passwordSchema.optional(),
  passwordConfirmation: passwordSchema.optional(),
}).refine((data) => data.password === data.passwordConfirmation, {
  message: 'パスワードが一致しません',
  path: ['passwordConfirmation'],
});

export const profileEditSchema = z.object({
  lastName: z.string().min(1),
  firstName: z.string().min(1),
  lastNameKana: katakanaSchema,
  firstNameKana: katakanaSchema,
  phoneNumber: phoneNumberSchema.optional(),
  position: z.string().optional(),
  email: emailSchema,
  emailConfirmation: emailSchema,
}).refine((data) => data.email === data.emailConfirmation, {
  message: 'メールアドレスが一致しません',
  path: ['emailConfirmation'],
});

export const SiteSchema = z.object({
  siteName: z.string().min(1, '拠点名を入力してください').max(100, '拠点名は100文字以内で入力してください'),
  siteNameKana: katakanaSchema.min(1, '拠点名（カナ）を入力してください').max(100, '拠点名（カナ）は100文字以内で入力してください'),
  postalCode: postalCodeSchema,
  prefectureName: z.string().min(1, '都道府県を選択してください'),
  cityName: z.string().min(1, '市区町村を入力してください').max(50, '市区町村は50文字以内で入力してください'),
  townName: z.string().min(1, '町域を入力してください').max(50, '町域は50文字以内で入力してください'),
  blockName: z.string().min(1, '番地を入力してください').max(100, '番地は100文字以内で入力してください'),
  addressDetail: z.string().max(100, '建物名・部屋番号は100文字以内で入力してください').optional(),

  picFirstName: z.string().min(1, '担当者（名）を入力してください').max(50, '担当者（名）は50文字以内で入力してください'),
  picFirstNameKana: katakanaSchema.min(1, '担当者（名カナ）を入力してください').max(50, '担当者（名カナ）は50文字以内で入力してください'),
  picLastName: z.string().min(1, '担当者（姓）を入力してください').max(50, '担当者（姓）は50文字以内で入力してください'),
  picLastNameKana: katakanaSchema.min(1, '担当者（姓カナ）を入力してください').max(50, '担当者（姓カナ）は50文字以内で入力してください'),
  picEmail: emailSchema,
  picPhoneNumber: phoneNumberSchema,
  picPosition: z.string().min(1, '担当者役職を入力してください').max(50, '担当者役職は50文字以内で入力してください'),
});



export const agreementsSchema = z.object({
  // Required checkboxes
  useRegisteredInfo: z.boolean().refine((val) => val === true, {
    message: '同意が必要です',
  }),
  provideAdditionalInfo: z.boolean().refine((val) => val === true, {
    message: '同意が必要です',
  }),
  transferEnvironmentalValue: z.boolean().refine((val) => val === true, {
    message: '同意が必要です',
  }),
  notRegisteredElsewhere: z.boolean().refine((val) => val === true, {
    message: '同意が必要です',
  }),

  // Required radio fields
  keidanrenParticipation: z.string().min(1, '経団連カーボンニュートラル行動計画への参加状況を選択してください'),
  specifiedEmitterStatus: z.string().min(1, '特定排出者の区分を選択してください'),
  energyRationalizationCompliance: z.string().min(1, 'エネルギー使用合理化法の対象者区分を選択してください'),

  // Conditional fields - optional in base schema
  emissionAdditionAgreement: z.boolean().optional(),
  specifiedEmitterCode: z.string().optional(),
  energyLawType: z.string().optional(),
  energyLawBusinessNumber: z.string().optional(),
  energyLawFactoryNumber: z.string().optional(),
}).superRefine((data, ctx) => {
  // Validate conditional fields based on parent selections

  // If user is a specified emitter, these fields are required
  if (data.specifiedEmitterStatus === 'isSpecifiedEmitter') {
    if (!data.emissionAdditionAgreement) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: '同意が必要です',
        path: ['emissionAdditionAgreement'],
      });
    }

    if (!data.specifiedEmitterCode || data.specifiedEmitterCode.trim() === '') {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: '特定排出者コードを入力してください',
        path: ['specifiedEmitterCode'],
      });
    } else if (!/^[0-9]{9}$/.test(data.specifiedEmitterCode)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: '特定排出者コードは9桁の数字で入力してください',
        path: ['specifiedEmitterCode'],
      });
    }
  }

  // If energy law applies, these fields are required
  if (data.energyRationalizationCompliance === 'applicable') {
    if (!data.energyLawType) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: '種別を選択してください',
        path: ['energyLawType'],
      });
    }

    if (!data.energyLawBusinessNumber || data.energyLawBusinessNumber.trim() === '') {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: '省エネ法特定事業者番号を入力してください',
        path: ['energyLawBusinessNumber'],
      });
    } else if (!/^[0-9]{7}$/.test(data.energyLawBusinessNumber)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: '省エネ法特定事業者番号は7桁の数字で入力してください',
        path: ['energyLawBusinessNumber'],
      });
    }

    if (!data.energyLawFactoryNumber || data.energyLawFactoryNumber.trim() === '') {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: '省エネ法指定工場番号を入力してください',
        path: ['energyLawFactoryNumber'],
      });
    } else if (!/^[0-9]{7}$/.test(data.energyLawFactoryNumber)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: '省エネ法指定工場番号は7桁の数字で入力してください',
        path: ['energyLawFactoryNumber'],
      });
    }
  }
});