export const BOOLEAN_OPTIONS = {
  YES_NO: [
    { label: 'はい', value: true },
    { label: 'いいえ', value: false },
  ],
  SEND_EMAIL: [
    { label: '送信する', value: true },
    { label: '送信しない', value: false },
  ],
} as const;

export type BooleanOption = {
  label: string;
  value: boolean;
};

export const booleanToLabel = (
  value: boolean,
  type: keyof typeof BOOLEAN_OPTIONS,
): string => BOOLEAN_OPTIONS[type].find((option) => option.value === value)?.label ?? '';
