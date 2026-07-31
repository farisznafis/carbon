import { useState } from 'react';
import { useForm } from 'react-hook-form';
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
      <form
        className="absolute inset-0 h-full w-full min-w-0 max-w-full overflow-hidden"
        onSubmit={form.handleSubmit(handleSubmit)}
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
              <FormItem className="[@media(max-height:480px)]:space-y-0.5">
                <FormLabel className="[@media(max-height:480px)]:text-sm [@media(max-height:480px)]:leading-5">
                  管理者ID
                </FormLabel>
                <FormControl>
                  <Input
                    type="text"
                    placeholder="社員名またはメールアドレス"
                    className="[@media(max-height:480px)]:h-8 [@media(max-height:480px)]:py-1"
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
              <FormItem className="[@media(max-height:480px)]:space-y-0.5">
                <FormLabel className="[@media(max-height:480px)]:text-sm [@media(max-height:480px)]:leading-5">
                  パスワード
                </FormLabel>
                <FormControl>
                  <PasswordInput
                    placeholder="パスワードを入力"
                    className="[@media(max-height:480px)]:h-8 [@media(max-height:480px)]:py-1"
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
