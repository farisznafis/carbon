import { useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
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
import { adminLoginSchema, type AdminLoginFormData } from '@/lib/schemas/loginSchemas';

const FIELD_MAP = { identifier: 'identifier', password: 'password' } as const;

type AdminLoginFormProps = {
  onSubmit: (values: AdminLoginFormData) => Promise<void> | void;
};

export function AdminLoginForm({ onSubmit }: AdminLoginFormProps) {
  const [error, setError] = useState<string | null>(null);

  const form = useForm<AdminLoginFormData>({
    resolver: zodResolver(adminLoginSchema),
    defaultValues: {
      identifier: '',
      password: '',
    },
  });

  const handleSubmit = async (values: AdminLoginFormData) => {
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
          title="管理者ログイン"
          description="管理画面にアクセスするにはログインしてください。"
          submitLabel="ログイン"
          error={error}
          isSubmitting={form.formState.isSubmitting}
        >
          <FormField
            control={form.control}
            name="identifier"
            render={({ field }) => (
              <FormItem className="[@media(max-height:700px)]:space-y-1">
                <FormLabel className="[@media(max-height:700px)]:text-sm [@media(max-height:700px)]:leading-5">
                  管理者ID
                </FormLabel>
                <FormControl>
                  <Input
                    type="text"
                    placeholder="ユーザー名またはメールアドレス"
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
              <FormItem className="[@media(max-height:700px)]:space-y-1">
                <FormLabel className="[@media(max-height:700px)]:text-sm [@media(max-height:700px)]:leading-5">
                  パスワード
                </FormLabel>
                <FormControl>
                  <PasswordInput
                    placeholder="パスワードを入力"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </LoginForm>
      </form>
    </Form>
  );
}

export default AdminLoginForm;
