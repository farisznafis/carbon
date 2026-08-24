import { z } from 'zod';
import {
  PARTICIPATION_METHODOLOGIES,
  UNKNOWN_METHODOLOGY,
} from '@/lib/constants/methodology';

export { UNKNOWN_METHODOLOGY };

// Keeps the signup form and public homepage aligned to one methodology source of truth.
export const participationMethodologies = PARTICIPATION_METHODOLOGIES;

export const activityHasOptions = [
  { value: 'yes', label: 'あり' },
  { value: 'no', label: 'なし' },
] as const;

export const methodologyFormSchema = z
  .object({
    methodology: z
      .array(z.string())
      .min(1, '参加したいプロジェクトを選択してください'),
    activityHas: z.enum(['yes', 'no']),
    activityInformation: z.string(),
  })
  .superRefine((data, ctx) => {
    if (data.activityHas === 'yes' && !data.activityInformation.trim()) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['activityInformation'],
        message: '現在の活動情報を入力してください',
      });
    }
  });

export type MethodologyFormData = z.infer<typeof methodologyFormSchema>;

export const defaultMethodologyValues: MethodologyFormData = {
  methodology: [],
  activityHas: 'yes',
  activityInformation: '',
};
