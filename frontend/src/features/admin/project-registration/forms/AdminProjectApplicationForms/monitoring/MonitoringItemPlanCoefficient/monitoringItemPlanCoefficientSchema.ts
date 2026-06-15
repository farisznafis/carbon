import { z } from "zod";

export const monitoringItemPlanCoefficientSchema = z.object({
  symbol: z.string(),
  definition: z.string(),
  unit: z.string(),
});
