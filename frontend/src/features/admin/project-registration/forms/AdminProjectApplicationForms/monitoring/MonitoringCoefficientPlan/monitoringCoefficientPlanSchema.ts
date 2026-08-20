import { z } from 'zod';
import { monitoringPlanRowSchema } from '../monitoringPlanRow';

export const monitoringCoefficientPlanSchema = z.object({
  items: z.array(monitoringPlanRowSchema),
});
