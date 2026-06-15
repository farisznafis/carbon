import z from 'zod';

export const userLoginSchema = z.object({
  companyId: z
    .string()
    .min(1, '会員IDを入力してください')
    .max(50, '会員IDは50文字以内で入力してください'),
  email: z
    .string()
    .min(1, 'メールアドレスを入力してください')
    .email('有効なメールアドレスを入力してください')
    .max(255, 'メールアドレスは255文字以内で入力してください'),
  password: z
    .string()
    .min(1, 'パスワードを入力してください')
    .max(100, 'パスワードは100文字以内で入力してください'),
});

export const adminLoginSchema = z.object({
  identifier: z
    .string()
    .min(1, '識別子を入力してください')
    .max(255, '識別子は255文字以内で入力してください'),
  password: z
    .string()
    .min(1, 'パスワードを入力してください')
    .max(100, 'パスワードは100文字以内で入力してください'),
});

export type UserLoginFormData = z.infer<typeof userLoginSchema>;
export type AdminLoginFormData = z.infer<typeof adminLoginSchema>;