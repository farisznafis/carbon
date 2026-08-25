// THIS USED FOR SCHEMA THAT USE REVIEWER NAME LIST
// already used on projectApplication.ts

export const REVIEWER_NAMES = [
  { label: "ペリージョンソンレジストラークリーンディベロップメントメカニズム株式会社", value: "ペリージョンソンレジストラークリーンディベロップメントメカニズム株式会社" },
  { label: "一般社団法人 日本能率協会", value: "一般社団法人 日本能率協会" },
  { label: "一般財団法人日本品質保証機構", value: "一般財団法人日本品質保証機構" },
  { label: "一般財団法人日本海事協会", value: "一般財団法人日本海事協会" },
  { label: "ソコテック・サーティフィケーション・ジャパン株式会社", value: "ソコテック・サーティフィケーション・ジャパン株式会社" },
  { label: "環境省(ダミーの審査機関)", value: "環境省(ダミーの審査機関)" },
  { label: "株式会社トーマツ審査評価機構", value: "株式会社トーマツ審査評価機構" },
  { label: "日本海事検定キューエイ株式会社", value: "日本海事検定キューエイ株式会社" },
  { label: "株式会社 JACO CDM", value: "株式会社 JACO CDM" },
  { label: "ロイド レジスター クオリティ アシュアランス リミテッドジャパン", value: "ロイド レジスター クオリティ アシュアランス リミテッドジャパン" },
  { label: "SGS ジャパン株式会社 認証サービス事業部", value: "SGS ジャパン株式会社 認証サービス事業部" },
  { label: "ビューローベリタスジャパン株式会社 システム認証事業本部", value: "ビューローベリタスジャパン株式会社 システム認証事業本部" },
];

export type Reviewer_Name = (typeof REVIEWER_NAMES)[number];

// for schema
export const REVIEWER_NAME_OPTIONS = [
  "ペリージョンソンレジストラークリーンディベロップメントメカニズム株式会社", // Hokkaido
  "一般社団法人 日本能率協会", 
  "一般財団法人日本品質保証機構",
  "一般財団法人日本海事協会", 
  "ソコテック・サーティフィケーション・ジャパン株式会社", 
  "環境省(ダミーの審査機関)", 
  "株式会社トーマツ審査評価機構", 
  "日本海事検定キューエイ株式会社",
  "株式会社 JACO CDM",
  "ロイド レジスター クオリティ アシュアランス リミテッドジャパン", 
  "SGS ジャパン株式会社 認証サービス事業部", 
  "ビューローベリタスジャパン株式会社 システム認証事業本部", 
] as const;

export const REVIEWER_NAME_VALIDATION = {
    required_error: "審査機関名を選択してください",
};
