import { z } from "zod";

export const expectedProjectPlanCoefficientSchema = z.object({
  expectedValue: z.string(),
  basis: z.string(),
  remarks: z.string(),
});
