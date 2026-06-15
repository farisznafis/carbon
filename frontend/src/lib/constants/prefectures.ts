// THIS USED FOR SCHEMA THAT USE PREFECTURE LIST
// already used on projectApplication, ProfileForm.tsx

export const prefectures = [
  '北海道', '青森県', '岩手県', '宮城県', '秋田県', '山形県', '福島県',
  '茨城県', '栃木県', '群馬県', '埼玉県', '千葉県', '東京都', '神奈川県',
  '新潟県', '富山県', '石川県', '福井県', '山梨県', '長野県', '岐阜県',
  '静岡県', '愛知県', '三重県', '滋賀県', '京都府', '大阪府', '兵庫県',
  '奈良県', '和歌山県', '鳥取県', '島根県', '岡山県', '広島県', '山口県',
  '徳島県', '香川県', '愛媛県', '高知県', '福岡県', '佐賀県', '長崎県',
  '熊本県', '大分県', '宮崎県', '鹿児島県', '沖縄県'
];

export const PREFECTURES = [
  { label: "北海道", value: "北海道" }, // Hokkaido
  
  // 東北 (Tohoku Region)
  { label: "青森県", value: "青森県" }, // Aomori
  { label: "岩手県", value: "岩手県" }, // Iwate
  { label: "宮城県", value: "宮城県" }, // Miyagi
  { label: "秋田県", value: "秋田県" }, // Akita
  { label: "山形県", value: "山形県" }, // Yamagata
  { label: "福島県", value: "福島県" }, // Fukushima
  
  // 関東 (Kanto Region)
  { label: "茨城県", value: "茨城県" }, // Ibaraki
  { label: "栃木県", value: "栃木県" }, // Tochigi
  { label: "群馬県", value: "群馬県" }, // Gunma
  { label: "埼玉県", value: "埼玉県" }, // Saitama
  { label: "千葉県", value: "千葉県" }, // Chiba
  { label: "東京都", value: "東京都" }, // Tokyo
  { label: "神奈川県", value: "神奈川県" }, // Kanagawa
  
  // 中部 (Chubu Region)
  { label: "新潟県", value: "新潟県" }, // Niigata
  { label: "富山県", value: "富山県" }, // Toyama
  { label: "石川県", value: "石川県" }, // Ishikawa
  { label: "福井県", value: "福井県" }, // Fukui
  { label: "山梨県", value: "山梨県" }, // Yamanashi
  { label: "長野県", value: "長野県" }, // Nagano
  { label: "岐阜県", value: "岐阜県" }, // Gifu
  { label: "静岡県", value: "静岡県" }, // Shizuoka
  { label: "愛知県", value: "愛知県" }, // Aichi
  
  // 近畿 (Kinki/Kansai Region)
  { label: "三重県", value: "三重県" }, // Mie
  { label: "滋賀県", value: "滋賀県" }, // Shiga
  { label: "京都府", value: "京都府" }, // Kyoto
  { label: "大阪府", value: "大阪府" }, // Osaka
  { label: "兵庫県", value: "兵庫県" }, // Hyogo
  { label: "奈良県", value: "奈良県" }, // Nara
  { label: "和歌山県", value: "和歌山県" }, // Wakayama
  
  // 中国 (Chugoku Region)
  { label: "鳥取県", value: "鳥取県" }, // Tottori
  { label: "島根県", value: "島根県" }, // Shimane
  { label: "岡山県", value: "岡山県" }, // Okayama
  { label: "広島県", value: "広島県" }, // Hiroshima
  { label: "山口県", value: "山口県" }, // Yamaguchi
  
  // 四国 (Shikoku Region)
  { label: "徳島県", value: "徳島県" }, // Tokushima
  { label: "香川県", value: "香川県" }, // Kagawa
  { label: "愛媛県", value: "愛媛県" }, // Ehime
  { label: "高知県", value: "高知県" }, // Kochi
  
  // 九州・沖縄 (Kyushu & Okinawa Region)
  { label: "福岡県", value: "福岡県" }, // Fukuoka
  { label: "佐賀県", value: "佐賀県" }, // Saga
  { label: "長崎県", value: "長崎県" }, // Nagasaki
  { label: "熊本県", value: "熊本県" }, // Kumamoto
  { label: "大分県", value: "大分県" }, // Oita
  { label: "宮崎県", value: "宮崎県" }, // Miyazaki
  { label: "鹿児島県", value: "鹿児島県" }, // Kagoshima
  { label: "沖縄県", value: "沖縄県" }, // Okinawa
];

export type Prefecture = typeof PREFECTURES[number];

// for schema
export const PREFECTURE_OPTIONS = [
  "北海道",
  "青森県", "岩手県", "宮城県", "秋田県", "山形県", "福島県",
  "茨城県", "栃木県", "群馬県", "埼玉県", "千葉県", "東京都", "神奈川県",
  "新潟県", "富山県", "石川県", "福井県", "山梨県", "長野県",
  "岐阜県", "静岡県", "愛知県", "三重県",
  "滋賀県", "京都府", "大阪府", "兵庫県", "奈良県", "和歌山県",
  "鳥取県", "島根県", "岡山県", "広島県", "山口県",
  "徳島県", "香川県", "愛媛県", "高知県",
  "福岡県", "佐賀県", "長崎県", "熊本県", "大分県", "宮崎県", "鹿児島県",
  "沖縄県",
] as const;

export const PREFECTURE_VALIDATION = {
  required_error: "都道府県を選択してください",
};