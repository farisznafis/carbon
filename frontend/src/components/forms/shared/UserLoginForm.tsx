import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
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
import { userLoginSchema, type UserLoginFormData } from '@/lib/schemas/loginSchemas';
import { paths } from '@/routes/paths';

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
          title="ログイン"
          description="GXデータの登録・管理を続けるにはログインしてください。"
          submitLabel="ログイン"
          error={error}
          isSubmitting={form.formState.isSubmitting}
          footer={
            import.meta.env.VITE_USE_MOCK === 'true' && (
              <p className="text-xs text-muted-foreground">
                デモ: 企業ID <code>C0001</code> / <code>master@green-energy.example.jp</code>
                (一般: <code>user@green-energy.example.jp</code>) / パスワード任意
              </p>
            )
          }
        >
          <FormField
            control={form.control}
            name="companyId"
            render={({ field }) => (
              <FormItem className="[@media(max-height:700px)]:space-y-1">
                <FormLabel className="[@media(max-heigght:700px)]:text-sm [@media(max-height:700px)]:leading-5">
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
              <FormItem className="[@media(max-height:700px)]:space-y-1">
                <FormLabel className="[@media(max-height:700px)]:text-sm [@media(max-height:700px)]:leading-5">
                  メールアドレスまたはユーザー名
                </FormLabel>
                <FormControl>
                  <Input
                    type="text"
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

          <div className="text-right">
            <Link
              to={paths.forgotPassword}
              className="text-sm font-medium text-foreground hover:text-primary"
            >
              企業IDまたはパスワードをお忘れの方
            </Link>
          </div>
        </LoginForm>
      </form>
    </Form>
  );
}

export default UserLoginForm;
