// THIS USED FOR SCHEMA THAT USE SELECT TYPES LIST
// already used on

export const SELECTS_REQUIRED = [
  // { label: '', value: '' },
  { label: '有', value: true },
  { label: '無', value: false },
];

export const SELECTS_OPTIONAL = [
  { label: '未選択', value: '' },
  { label: '有', value: true },
  { label: '無', value: false },
];
export const IS_DESIGNATED_EMITTERS = [
  { label: '特定排出者である', value: true },
  { label: '特定排出者でない', value: false },
];
export const MONITOR_CALC_BASELINES = [
  { label: '排出量の算定を行う', value: '排出量の算定を行う' },
  { label: '排出量の算定を行わない', value: '排出量の算定を行わない' },
];
export const MONITOR_CALC_POSTS = [
  { label: '排出量の算定を行う', value: '排出量の算定を行う' },
  { label: '排出量の算定を省略する', value: '排出量の算定を省略する' },
  {
    label:
      '排出量の算定を行う。ただし、排出量のモニタリングを省略し、影響度により排出量を評価する。',
    value:
      '排出量の算定を行う。ただし、排出量のモニタリングを省略し、影響度により排出量を評価する。',
  },
];
export const IS_NEED_REVALIDATIONS = [
  {
    label: '必要（妥当性確認報告書を添付）',
    value: '必要（妥当性確認報告書を添付）',
  },
  { label: '不要（確認書を添付）', value: '不要（確認書を添付）' },
];
export const REDUCTION_ACTIVITY_INCLUDED = [
  {
    label:
      'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含む',
    value:
      'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含む',
  },
  {
    label:
      'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含まない',
    value:
      'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含まない',
  },
];
export const IS_STANDARD_MONITORS = [
  { label: '全削減活動においてモニタリング・算定を実施する排出活動を統一する。', value: '全削減活動においてモニタリング・算定を実施する排出活動を統一する。' },
  { label: '付随的排出量のモニタリング・算定は実施しない。', value: '付随的排出量のモニタリング・算定は実施しない。' },
  { label: 'その他（以下に確認方法をご記入ください。）', value: 'その他（以下に確認方法をご記入ください。）' },
];
export const IS_USE_SAMPLING = [
  { label: 'サンプリングを利用する', value: 'サンプリングを利用する' },
  { label: 'サンプリングを利用しない', value: 'サンプリングを利用しない' },
];
export const IS_SIMILAR = [
  { label: '登録している', value: '登録している' },
  { label: '登録していない', value: '登録していない' },
];
export const IS_INDEPENDENT_OF_LEGALS = [
  { label: '法令等の義務履行によるものである。', value: '法令等の義務履行によるものである。' },
  { label: '法令等の義務履行によるものではない。', value: '法令等の義務履行によるものではない。' },
];

export type Select_Req = (typeof SELECTS_REQUIRED)[number];
export type Select_Optional = (typeof SELECTS_OPTIONAL)[number];
export type Is_Designated_Emitter = (typeof IS_DESIGNATED_EMITTERS)[number];
export type Monitor_Calc_Baseline = (typeof MONITOR_CALC_BASELINES)[number];
export type Monitor_Calc_Post = (typeof MONITOR_CALC_POSTS)[number];
export type Is_Need_Revalidation = (typeof IS_NEED_REVALIDATIONS)[number];
export type Reduction_Activity_Included =
  (typeof REDUCTION_ACTIVITY_INCLUDED)[number];
export type Is_Standard_Monitor = (typeof IS_STANDARD_MONITORS)[number];
export type Is_Use_Sampling = (typeof IS_USE_SAMPLING)[number];
export type Is_Similar = (typeof IS_SIMILAR)[number];
export type Is_Independent_Of_Legal = (typeof IS_INDEPENDENT_OF_LEGALS)[number];

// for schema
export const SELECT_TYPE_REQUIRED_OPTIONS = [true, false] as const;
export const SELECT_TYPE_OPTIONAL_OPTIONS = ['未選択', '有', '無'] as const;
export const IS_DESIGNATED_EMITTER_OPTIONS = [true, false] as const;
export const MONITOR_CALC_BASELINE_OPTIONS = [
  '排出量の算定を行う',
  '排出量の算定を行わない',
] as const;
export const MONITOR_CALC_POST_OPTIONS = [
  '排出量の算定を行う',
  '排出量の算定を省略する',
  '排出量の算定を行う。ただし、排出量のモニタリングを省略し、影響度により排出量を評価する。',
] as const;
export const IS_NEED_REVALIDATION_OPTIONS = [
  '必要（妥当性確認報告書を添付）',
  '不要（確認書を添付）',
] as const;
export const REDUCTION_ACTIVITY_INCLUDED_OPTIONS = [
  'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含む',
  'カーボンニュートラル行動計画に参加する削減活動の実施者による削減活動を含まない',
] as const;
export const IS_STANDARD_MONITOR_OPTIONS = [
  '全削減活動においてモニタリング・算定を実施する排出活動を統一する。',
  '付随的排出量のモニタリング・算定は実施しない。',
  'その他（以下に確認方法をご記入ください。）',
] as const;
export const IS_USE_SAMPLING_OPTIONS = [
  'サンプリングを利用する',
  'サンプリングを利用しない',
] as const;
export const IS_INDEPENDENT_OF_LEGAL_OPTIONS = [
  '法令等の義務履行によるものである。',
  '法令等の義務履行によるものではない。',
] as const;
export const IS_SIMILAR_OPTIONS = ['登録している', '登録していない'] as const;

export const SELECT_TYPE_REQUIRED_VALIDATION = {
  required_error: '項目を選択してください',
};
export const IS_DESIGNATED_EMITTER_VALIDATION = {
  required_error: '項目を選択してください',
};
export const MONITOR_CALC_BASELINE_VALIDATION = {
  required_error: '項目を選択してください',
};
export const MONITOR_CALC_POST_VALIDATION = {
  required_error: '項目を選択してください',
};
export const IS_NEED_REVALIDATION_VALIDATION = {
  required_error: '項目を選択してください',
};
export const REDUCTION_ACTIVITY_INCLUDED_VALIDATION = {
  required_error: '項目を選択してください',
};
export const IS_STANDARD_MONITOR_VALIDATION = {
  required_error: '項目を選択してください',
};
export const IS_USE_SAMPLING_VALIDATION = {
  required_error: '項目を選択してください',
};
export const IS_SIMILAR_VALIDATION = {
  required_error: '項目を選択してください',
};
export const IS_INDEPENDENT_OF_LEGAL_VALIDATION = {
  required_error: '項目を選択してください',
};
