import { REGEX_MESSAGES, REGEX_PATTERNS, VALIDATION_MESSAGES } from "@/lib/validations";
import { z } from "zod";

/**
 * Project status is a closed set of domain values
 * → z.enum is the correct choice
 */
export const ProjectStatusSchema = z.enum(
  [
    "作成済み",
    "準備中",
    "変更中",
    "妥当性確認処理",
    "却下",
    "実施中",
    "検証中",
    "クレジット申請中",
    "終了",
  ],
  {
    required_error: "ステータスを選択してください",
  },
);

export const RegionalCategorySchema = z.enum([
  "北海道",
  "東北",
  "関東",
  "中部",
  "近畿",
  "中国",
  "四国",
  "九州・沖縄",
  "全国",
]);

export const MethodCategorySchema = z.enum(["EN-S", "EN-R", "IN", "AG", "WA"]);

export const projectBaseSchema = z.object({
  // Project
  projectName: z
    .string()
    .min(1, "必須項目です")
    .max(100, "文字列は最大100文字までです"),
  projectNumber: z
    .string()
    .min(1, "必須項目です")
    .max(10, "文字列は最大10文字までです")
    .regex(REGEX_PATTERNS.PROJECT_NUM, REGEX_MESSAGES.PROJECT_NUM),

  projectStatus: ProjectStatusSchema,

  // 会則 quotes both of these, so the backend refuses a project without them.
  // startedAt is the yyyy-MM-dd the date input holds; the payload mapper turns
  // it into the Instant the backend stores.
  startedAt: z
    .string()
    .min(1, "必須項目です")
    .regex(/^\d{4}-\d{2}-\d{2}$/, "日付の形式が正しくありません"),
  usePurpose: z
    .string()
    .min(1, "必須項目です")
    .max(1000, "文字列は最大1000文字までです"),

  // Representative
  repFirstName: z
    .string()
    .min(1, "必須項目です")
    .max(50, "文字列は最大50文字までです")
    .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS),
  // old code
  // .regex(/^[0-9A-Za-z０-９Ａ-Ｚａ-ｚ]+$/, "全角英数字のみ使用できます"),
  repFirstNameKana: z
    .string()
    .min(1, "必須項目です")
    .max(50, "文字列は最大50文字までです")
    .regex(
      REGEX_PATTERNS.FULLWIDTH_KATAKANA,
      REGEX_MESSAGES.FULLWIDTH_KATAKANA,
    ),

  repLastName: z
    .string()
    .min(1, "必須項目です")
    .max(50, "文字列は最大50文字までです")
    .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS),

  repLastNameKana: z
    .string()
    .min(1, "必須項目です")
    .max(50, "文字列は最大50文字までです")
    .regex(
      REGEX_PATTERNS.FULLWIDTH_KATAKANA,
      REGEX_MESSAGES.FULLWIDTH_KATAKANA,
    ),
  repEmail: z
    .string()
    .max(254, VALIDATION_MESSAGES.MAX_CHARACTERS(254))
    .email('メールアドレスの形式が正しくありません'),

  repPhoneNumber: z
    .string()
    .min(8, "必須項目です")
    .max(20, "文字列は最大20文字までです")
    .regex(REGEX_PATTERNS.PHONE_NUMBER_DIGITS, REGEX_MESSAGES.PHONE_NUMBER_DIGITS),
  repPosition: z
    .string()
    .min(1, "必須項目です")
    .max(100, "文字列は最大100文字までです"),

  // PIC
  picFirstName: z
    .string()
    .min(1, "必須項目です")
    .max(50, "文字列は最大50文字までです")
    .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS),
  picFirstNameKana: z
    .string()
    .min(1, "必須項目です")
    .max(50, "文字列は最大50文字までです")
    .regex(
      REGEX_PATTERNS.FULLWIDTH_KATAKANA,
      REGEX_MESSAGES.FULLWIDTH_KATAKANA,
    ),
  picLastName: z
    .string()
    .min(1, "必須項目です")
    .max(50, "文字列は最大50文字までです")
    .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS),
  picLastNameKana: z
    .string()
    .min(1, "必須項目です")
    .max(50, "文字列は最大50文字までです")
    .regex(
      REGEX_PATTERNS.FULLWIDTH_KATAKANA,
      REGEX_MESSAGES.FULLWIDTH_KATAKANA,
    ),
  picEmail: z
    .string()
    .max(254, VALIDATION_MESSAGES.MAX_CHARACTERS(254))
    .email('メールアドレスの形式が正しくありません'),
    
  picPhoneNumber: z
    .string()
    .min(8, "必須項目です")
    .max(20, "文字列は最大20文字までです")
    .regex(REGEX_PATTERNS.PHONE_NUMBER_DIGITS, REGEX_MESSAGES.PHONE_NUMBER_DIGITS),
  picPosition: z
    .string()
    .min(1, "必須項目です")
    .max(100, "文字列は最大100文字までです"),
  picDataManagement: z
    .string()
    .min(1, "必須項目です")
    .max(50, "文字列は最大50文字までです")
    .regex(REGEX_PATTERNS.FULLWIDTH_SYMBOLS, REGEX_MESSAGES.FULLWIDTH_SYMBOLS),

  baselineEmissions: z.coerce.number().optional(),
  postProjectEmission: z.coerce.number().optional(),

  regionalCategory: RegionalCategorySchema,

  // Members

  // 方法論
  methodologyVersion: z
    .string()
    .min(1, "方法論バージョンは必須です")
    .max(10, "文字列は最大10文字までです")
    .regex(REGEX_PATTERNS.METHOD_VER, REGEX_MESSAGES.METHOD_VER),

  methodCategory: MethodCategorySchema,

  methodNumber: z
    .string()
    .min(1, "方法論番号は必須です")
    .max(3, "文字列は最大3文字までです"),

  methodName: z
    .string()
    .min(1, "方法論名称は必須です")
    .max(255, "文字列は最大255文字までです"),
    // .regex(REGEX_PATTERNS.METHOD_VER, REGEX_MESSAGES.METHOD_VER),

  methodMainEmission: z
    .string()
    .min(1, "排出源／活動は必須です")
    .max(255, "文字列は最大255文字までです"),

  methodCombiFeasibility: z.boolean(),
  methodDetails: z
    .string()
    .min(1, "必須項目です")
    .max(1000, "文字列は最大1000文字までです"),

  operatorCompanyId: z.string().min(1, "必須項目です"),
});
