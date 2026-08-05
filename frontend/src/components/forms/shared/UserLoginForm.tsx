import { useState } from 'react';
import { useForm } from 'react-hook-form';
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
import type { UserLoginFormData } from '@/lib/schemas/loginSchemas';
import { paths } from '@/routes/paths';

type UserLoginFormProps = {
  onSubmit: (values: UserLoginFormData) => Promise<void> | void;
};

export function UserLoginForm({ onSubmit }: UserLoginFormProps) {
  const [error, setError] = useState<string | null>(null);

  const form = useForm<UserLoginFormData>({
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
          // footer={
          //   <a href="/contact" className="text-sm font-medium text-foreground hover:text-primary">
          //     ログインに関するお問い合わせはこちら
          //   </a>
          // }
        >
          <FormField
            control={form.control}
            name="companyId"
            render={({ field }) => (
              <FormItem className="[@media(max-height:700px)]:space-y-1">
                <FormLabel className="[@media(max-height:700px)]:text-sm [@media(max-height:700px)]:leading-5">
                  社員ID
                </FormLabel>
                <FormControl>
                  <Input
                    type="text"
                    placeholder="例）1234567890111"
                    className="[@media(max-height:700px)]:h-9 [@media(max-height:700px)]:py-1.5 [@media(max-height:480px)]:h-8"
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
                    className="[@media(max-height:700px)]:h-9 [@media(max-height:700px)]:py-1.5 [@media(max-height:480px)]:h-8"
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
                    className="[@media(max-height:700px)]:h-9 [@media(max-height:700px)]:py-1.5 [@media(max-height:480px)]:h-8"
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
