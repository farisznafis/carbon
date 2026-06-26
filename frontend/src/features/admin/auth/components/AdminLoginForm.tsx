import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { LogIn } from 'lucide-react';
import { PasswordInput } from '@/components/inputs';
import { LoginForm } from '@/components/forms/shared/LoginForm';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import type { AdminLoginFormData } from '@/lib/schemas/loginSchemas';

type AdminLoginFormProps = {
  onSubmit: (values: AdminLoginFormData) => Promise<void> | void;
};

export function AdminLoginForm({ onSubmit }: AdminLoginFormProps) {
  const [error, setError] = useState<string | null>(null);

  const form = useForm<AdminLoginFormData>({
    defaultValues: {
      identifier: '',
      password: '',
    },
  });

  const handleSubmit = async (values: AdminLoginFormData) => {
    setError(null);

    try {
      await onSubmit(values);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'ログインに失敗しました');
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} noValidate>
        <LoginForm
          submitLabel="ログイン"
          error={error}
          isSubmitting={form.formState.isSubmitting}
          header={
            <header className="text-center">
              <div className="mx-auto mb-5 flex h-12 w-12 items-center justify-center rounded-xl bg-secondary text-primary">
                <LogIn className="h-6 w-6" />
              </div>
              <h1>管理者ログイン</h1>
              <p className="mt-4 text-[20px] font-medium leading-[28px] text-foreground">
                管理画面にアクセスするにはログインしてください。
              </p>
            </header>
          }
        >
          <FormField
            control={form.control}
            name="identifier"
            render={({ field }) => (
              <FormItem>
                <FormLabel>ユーザー名またはメールアドレス</FormLabel>
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
              <FormItem>
                <FormLabel>パスワード</FormLabel>
                <FormControl>
                  <PasswordInput placeholder="パスワードを入力" {...field} />
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
