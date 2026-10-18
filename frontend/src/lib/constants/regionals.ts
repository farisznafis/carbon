// THIS USED FOR SCHEMA THAT USE REGIONAL LIST
// already used on projectBase, projectApplication

export const REGIONALS = [
  { label: "北海道", value: "北海道" },
  { label: "東北", value: "東北" },
  { label: "関東", value: "関東" },
  { label: "中部", value: "中部" },
  { label: "近畿", value: "近畿" },
  { label: "中国", value: "中国" },
  { label: "四国", value: "四国" },
  { label: "九州・沖縄", value: "九州・沖縄" },
  { label: "全国", value: "全国" },
];

export type Regional = (typeof REGIONALS)[number];

// for schema
export const REGIONAL_OPTIONS = [
  "北海道", // Hokkaido
  "東北", // Tohoku
  "関東", // Kanto
  "中部", // Chubu
  "近畿", // Kansai
  "中国", // Chugoku
  "四国", // Shikoku
  "九州・沖縄", // Kyushu・Okinawa
  "全国", // Nationwide
] as const;

export const REGIONAL_VALIDATION = {
    required_error: "地域区分を選択してください",
};
