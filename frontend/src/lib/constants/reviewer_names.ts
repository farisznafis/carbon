// 審査機関 (validation/verification bodies).
//
// Fixed official list, taken from the 【削除不可】プルダウン用 sheet of J-Credit's
// sakuseirei_keikaku_solar_program.xlsx. Do not add entries that are not on
// that sheet, and keep the order the same so it matches the paper form.
//
// Note 日本海事検定キューエイ株式会社 and 日本検査キューエイ株式会社 are two different
// bodies despite the near-identical names — neither is a typo of the other.
//
// Several names contain half-width Latin, spaces or parentheses, so any backend
// field storing one must not be restricted to full-width characters.

// Single source of truth. `as const` keeps the literal types z.enum needs.
export const REVIEWER_NAME_OPTIONS = [
  'ペリージョンソンレジストラークリーンディベロップメントメカニズム株式会社',
  '一般社団法人 日本能率協会',
  '一般財団法人日本品質保証機構',
  '一般財団法人日本海事協会',
  'ソコテック・サーティフィケーション・ジャパン株式会社',
  '環境省(ダミーの審査機関)',
  '株式会社トーマツ審査評価機構',
  '日本海事検定キューエイ株式会社',
  '株式会社 JACO CDM',
  'ロイド レジスター クオリティ アシュアランス リミテッドジャパン',
  'SGS ジャパン株式会社 認証サービス事業部',
  'ビューローベリタスジャパン株式会社 システム認証事業本部',
  '日本検査キューエイ株式会社',
  '株式会社日本スマートエナジー認証機構',
] as const;

export type Reviewer_Name = (typeof REVIEWER_NAME_OPTIONS)[number];

/**
 * Select options, derived so the dropdown and the schema can never disagree.
 */
export const REVIEWER_NAMES: { label: string; value: string }[] =
  REVIEWER_NAME_OPTIONS.map((name) => ({ label: name, value: name }));

export const REVIEWER_NAME_VALIDATION = {
  required_error: '審査機関名を選択してください',
};
