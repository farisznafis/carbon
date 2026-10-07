// Form values of the project record (プロジェクト作成).
//
// The 計画書 itself is no longer typed into the system page by page: it is
// written in the official Excel 様式 and uploaded as a file. What remains here
// is the project record; the few plan values the system uses have their own
// small form (see features/admin/project-plan/planSummary.ts).

import { z } from 'zod';
import { projectBaseSchema } from './projectBaseSchema';

export const formSchema = z.object({
  projectBase: projectBaseSchema,
});

export type FormValues = z.output<typeof formSchema>;

export const adminProjectRegisterSchema = formSchema;

export type AdminProjectRegisterFormInputValues = z.input<typeof adminProjectRegisterSchema>;
export type AdminProjectRegisterFormValues = z.output<typeof adminProjectRegisterSchema>;
