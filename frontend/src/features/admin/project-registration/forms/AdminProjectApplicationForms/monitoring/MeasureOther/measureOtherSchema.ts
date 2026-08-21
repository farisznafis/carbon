import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';
import { otherInstrumentRowSchema } from '../measureInstrumentRow';

export const measureOtherSchema = z.object({
  instruments: z.array(otherInstrumentRowSchema),

  monitoringPointDiagram: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),
});
