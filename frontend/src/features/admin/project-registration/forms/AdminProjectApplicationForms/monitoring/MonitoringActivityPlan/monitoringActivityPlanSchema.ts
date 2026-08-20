import { z } from 'zod';
import { monitoringPlanRowSchema } from '../monitoringPlanRow';

export const monitoringActivityPlanSchema = z.object({
  items: z.array(monitoringPlanRowSchema),
});
