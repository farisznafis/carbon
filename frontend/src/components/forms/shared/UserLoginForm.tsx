import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';
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
  header?: ReactNode;
};

export function UserLoginForm({ onSubmit, header }: UserLoginFormProps) {
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
      <form onSubmit={form.handleSubmit(handleSubmit)} noValidate>
        <LoginForm
          submitLabel="ログイン"
          error={error}
          isSubmitting={form.formState.isSubmitting}
          header={header}
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
              <FormItem>
                <FormLabel>社員ID</FormLabel>
                <FormControl>
                  <Input type="text" placeholder="1234567890111" {...field} />
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
                <FormLabel>メールアドレス</FormLabel>
                <FormControl>
                  <Input type="email" placeholder="name@company.com" {...field} />
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

          <div className="text-center">
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
