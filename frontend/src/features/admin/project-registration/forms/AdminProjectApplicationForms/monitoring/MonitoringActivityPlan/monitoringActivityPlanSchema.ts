import { z } from 'zod';
import { monitoringActivityPlanRowSchema } from '../monitoringPlanRow';

export const monitoringActivityPlanSchema = z.object({
  items: z.array(monitoringActivityPlanRowSchema),
});
