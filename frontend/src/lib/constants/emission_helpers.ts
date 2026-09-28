// THIS USED FOR SCHEMA THAT USE EMISSION UNITS LIST
// already used on emissionCalc.tsx

export const EMISSION_UNITS = [
  { label: "t-CO2/年", value: "t-CO2/年" }, 
  { label: "kg-CO2/年", value: "kg-CO2/年" }, 
];

export type Emission_Unit = (typeof EMISSION_UNITS)[number];

// for schema
export const EMISSION_UNIT_OPTIONS = [
  't-CO2/年',
  'kg-CO2/年',
] as const;

export const EMISSION_UNIT_VALIDATION = {
  required_error: '排出量の測定単位を選択してください',
};

/**
 * 算定単位 without the /年.
 *
 * The official 『【削除不可】プルダウン用』 sheet lists these separately from the
 * per-year pair above, and 4.排出削減計画 uses this group: each row is an
 * absolute figure for one 年度, and only an absolute figure can be summed into
 * the 合計 row beneath the table. The backend rejects anything else there.
 */
export const ABSOLUTE_EMISSION_UNITS = [
  { label: 't-CO2', value: 't-CO2' },
  { label: 'kg-CO2', value: 'kg-CO2' },
];

export const ABSOLUTE_EMISSION_UNIT_OPTIONS = ['t-CO2', 'kg-CO2'] as const;
