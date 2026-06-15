import { z } from 'zod';

export const UNKNOWN_METHODOLOGY = 'OTHER';

export const participationMethodologies = [
  { value: 'EN-R-002', label: '太陽光発電設備の導入' },
  { value: 'EN-S-006', label: '照明設備の導入' },
  { value: UNKNOWN_METHODOLOGY, label: 'その他（該当する方法論が見つからない）' },
];

export const methodologyFormSchema = z.object({
  methodology: z
    .array(z.string())
    .min(1, '参加したいプロジェクトを選択してください'),
  activityInformation: z
    .string()
    .trim()
    .min(1, '現在の活動情報を入力してください'),
});

export type MethodologyFormData = z.infer<typeof methodologyFormSchema>;

export const defaultMethodologyValues: MethodologyFormData = {
  methodology: [],
  activityInformation: '',
};
