import { z } from 'zod';
import { monitoringCoefficientPlanRowSchema } from '../monitoringPlanRow';

export const monitoringCoefficientPlanSchema = z.object({
  items: z.array(monitoringCoefficientPlanRowSchema),
});
