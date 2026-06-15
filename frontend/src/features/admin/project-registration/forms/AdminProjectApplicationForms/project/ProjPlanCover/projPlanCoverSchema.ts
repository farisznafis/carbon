import {
  REGEX_MESSAGES,
  REGEX_PATTERNS,
  VALIDATION_MESSAGES,
} from '@/lib/validations';
import { z } from 'zod';

export const projPlanCoverSchema = z.object({
  version: z
    .string()
    .max(20, VALIDATION_MESSAGES.MAX_CHARACTERS(20))
    .regex(REGEX_PATTERNS.HALFWIDTH_SYMBOLS, REGEX_MESSAGES.HALFWIDTH_SYMBOLS),

  projectName: z
    .string()
    .max(100, VALIDATION_MESSAGES.MAX_CHARACTERS(100))
    // .regex(REGEX_PATTERNS.FULLWIDTH_ORI, REGEX_MESSAGES.FULLWIDTH_ORI)
    ,
  
  projectNumber: z
    .string()
    .max(10, VALIDATION_MESSAGES.MAX_CHARACTERS(10))
    .regex(REGEX_PATTERNS.PROJECT_NUM, REGEX_MESSAGES.PROJECT_NUM),
    
  projectOperatorName: z
    .string()
    .max(50, VALIDATION_MESSAGES.MAX_CHARACTERS(50))
    // .regex(REGEX_PATTERNS.FULLWIDTH_ORI, REGEX_MESSAGES.FULLWIDTH_ORI)
    ,

  applicationDate: z.coerce.date(),
  projectRegistrationDate: z.coerce.date(),
});

export type projPlanCoverValue = z.infer<typeof projPlanCoverSchema>;
