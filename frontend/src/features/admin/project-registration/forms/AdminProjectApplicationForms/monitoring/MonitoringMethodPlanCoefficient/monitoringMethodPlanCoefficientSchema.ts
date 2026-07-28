import { z } from "zod";

export const monitoringMethodPlanCoefficientSchema = z.object({
  classification: z.string(),
  overview: z.string(),
  frequency: z.string(),
});
