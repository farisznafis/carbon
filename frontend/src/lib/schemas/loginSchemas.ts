import z from 'zod';

// No trim() here: the password is sent exactly as typed, but whitespace-only still counts as empty.
const passwordSchema = z
  .string()
  .max(100, 'パスワードは100文字以内で入力してください')
  .refine((value) => value.trim().length > 0, 'パスワードを入力してください。');

// trim() so a whitespace-only value counts as empty.
export const userLoginSchema = z.object({
  companyId: z
    .string()
    .trim()
    .min(1, '企業IDを入力してください。')
    .max(50, '企業IDは50文字以内で入力してください'),
  // Accepts an email or a username, so there is no email-format rule.
  email: z
    .string()
    .trim()
    .min(1, 'メールアドレスまたはユーザー名を入力してください。')
    .max(255, 'メールアドレスまたはユーザー名は255文字以内で入力してください'),
  password: passwordSchema,
});

export const adminLoginSchema = z.object({
  identifier: z
    .string()
    .trim()
    .min(1, 'メールアドレスまたはユーザー名を入力してください。')
    .max(255, 'メールアドレスまたはユーザー名は255文字以内で入力してください'),
  password: passwordSchema,
});

export type UserLoginFormData = z.infer<typeof userLoginSchema>;
export type AdminLoginFormData = z.infer<typeof adminLoginSchema>;
