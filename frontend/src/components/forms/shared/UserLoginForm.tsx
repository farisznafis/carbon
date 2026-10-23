import { useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import { PasswordInput } from '@/components/inputs';
import { LoginForm } from '@/components/forms/shared/LoginForm';
import { resolveLoginError } from '@/components/forms/shared/loginError';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { userLoginSchema, type UserLoginFormData } from '@/lib/schemas/loginSchemas';
import { paths } from '@/routes/paths';

// The backend calls the email-or-username field "identifier".
const FIELD_MAP = { companyId: 'companyId', identifier: 'email', password: 'password' } as const;

type UserLoginFormProps = {
  onSubmit: (values: UserLoginFormData) => Promise<void> | void;
};

export function UserLoginForm({ onSubmit }: UserLoginFormProps) {
  const [error, setError] = useState<string | null>(null);

  const form = useForm<UserLoginFormData>({
    resolver: zodResolver(userLoginSchema),
    defaultValues: {
      companyId: '',
      email: '',
      password: '',
    },
  });

  const handleSubmit = async (values: UserLoginFormData) => {
    try {
      await onSubmit(values);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) form.setValue('password', '');
      setError(resolveLoginError(err, FIELD_MAP, (field, message) => form.setError(field, { message })));
    }
  };

  return (
    <Form {...form}>
      <form
        className="absolute inset-0 h-full w-full min-w-0 max-w-full overflow-hidden"
        onSubmit={(event) => {
          setError(null);
          return form.handleSubmit(handleSubmit)(event);
        }}
        noValidate
      >
        <LoginForm
          title="ログイン"
          // description="GXデータの登録・管理を続けるにはログインしてください。"
          submitLabel="ログイン"
          error={error}
          isSubmitting={form.formState.isSubmitting}
          // footer={
          //   <a href="/contact" className="text-sm font-medium text-foreground hover:text-primary-strong">
          //     ログインに関するお問い合わせはこちら
          //   </a>
          // }
        >
          <FormField
            control={form.control}
            name="companyId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>
                  企業ID
                </FormLabel>
                <FormControl>
                  <Input
                    type="text"
                    placeholder="例）1234567890111"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>
                  メールアドレスまたはユーザー名
                </FormLabel>
                <FormControl>
                  <Input
                    type="text"
                    autoComplete="username"
                    placeholder="例）user@example.com または name_company"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="password"
            render={({ field }) => (
              <FormItem>
                <FormLabel>
                  パスワード
                </FormLabel>
                <FormControl>
                  <PasswordInput
                    autoComplete="current-password"
                    placeholder="パスワードを入力"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="!mt-3 text-right">
            <Link
              to={paths.forgotPassword}
              className="rounded-sm text-sm font-medium text-primary-strong underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              パスワードをお忘れの方
            </Link>
          </div>
        </LoginForm>
      </form>
    </Form>
  );
}

export default UserLoginForm;
