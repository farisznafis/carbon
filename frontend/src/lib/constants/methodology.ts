// THIS USED FOR SCHEMA THAT USE METHODOLOGY LIST
// already used on projectApplication

export const METHODS = [
  { label: "EN-S", value: "EN-S" },
  { label: "EN-R", value: "EN-R" },
  { label: "IN", value: "IN" },
  { label: "AG", value: "AG" },
  { label: "WA", value: "WA" },
];

export type Method = typeof METHODS[number];

// Identifies the fallback option used when a participating company selects an unlisted methodology.
export const UNKNOWN_METHODOLOGY = "OTHER";

// Defines the methodologies currently available in participating-company registration.
export const PARTICIPATION_METHODOLOGIES = [
  { value: "EN-R-002", label: "太陽光発電設備の導入" },
  { value: "EN-S-006", label: "照明設備の導入" },
  {
    value: UNKNOWN_METHODOLOGY,
    label: "その他（該当する方法論が見つからない）",
  },
] as const;

// for schema
export const METHOD_OPTIONS = [
  "EN-S",
  "EN-R",
  "IN",
  "AG", 
  "WA", 
] as const;
