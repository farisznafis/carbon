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

// for schema
export const METHOD_OPTIONS = [
  "EN-S",
  "EN-R",
  "IN",
  "AG", 
  "WA", 
] as const;
