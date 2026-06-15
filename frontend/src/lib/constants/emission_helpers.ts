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
