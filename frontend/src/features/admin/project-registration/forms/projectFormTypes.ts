// Form values of the project record (プロジェクト作成).
//
// The 計画書 itself is not typed into the system: it is written in the official
// Excel 様式 and uploaded as a file. What remains here is the project record.

import { z } from 'zod';
import { projectBaseSchema } from './projectBaseSchema';

export const formSchema = z.object({
  projectBase: projectBaseSchema,
});

export type FormValues = z.output<typeof formSchema>;

export const adminProjectRegisterSchema = formSchema;

export type AdminProjectRegisterFormInputValues = z.input<typeof adminProjectRegisterSchema>;
export type AdminProjectRegisterFormValues = z.output<typeof adminProjectRegisterSchema>;
