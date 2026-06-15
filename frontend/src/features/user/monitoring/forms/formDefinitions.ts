import { monitoringSolarFormDefinition } from "./solar/solarFormDefinition";

export const formDefinitions = {
  solar: monitoringSolarFormDefinition,
  // aircon: monitoringAirconFormDefinition,
  
} as const;

export type ProjectType = keyof typeof formDefinitions;
