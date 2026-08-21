import { VALIDATION_MESSAGES } from '@/lib/validations';
import { z } from 'zod';
import { specifiedInstrumentRowSchema } from '../measureInstrumentRow';

export const measureSpecifiedSchema = z.object({
  instruments: z.array(specifiedInstrumentRowSchema),

  /** 12.1（２）— one diagram covers the whole sub-section. */
  monitoringPointDiagram: z
    .string()
    .min(1, '必須項目です')
    .max(1000, VALIDATION_MESSAGES.MAX_CHARACTERS(1000)),
});
