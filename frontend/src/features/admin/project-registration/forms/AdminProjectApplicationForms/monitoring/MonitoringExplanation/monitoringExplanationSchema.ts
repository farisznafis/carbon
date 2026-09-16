import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';
import { measureOtherSchema } from '../MeasureOther/measureOtherSchema';

export const monitoringExplanationSchema = z.object({
  monitoringItem: z
    .string()
    .min(1, '必須項目です')
    .max(200, VALIDATION_MESSAGES.MAX_CHARACTERS(200)),

  monitoringDesc: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  estimationMethod: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),

  monitoringPointDiagram: z.string().min(1, '必須項目です'),
});
