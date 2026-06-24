// import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from '@/components/ui/form';
import { RequiredLabel } from '@/components/forms/shared/RequiredLabel';
import { PasswordInput } from '@/components/inputs';
import { Input } from '@/components/ui/input';
import { LoginForm } from '@/components/forms/shared/LoginForm';
import {
  UserLoginFormData,
  // userLoginSchema,
} from '@/lib/schemas/loginSchemas';

type UserLoginFormProps = {
  onSubmit: (values: UserLoginFormData) => Promise<void> | void;
};

export function UserLoginForm({ onSubmit }: UserLoginFormProps) {
  const [error, setError] = useState<string | null>(null);

  const form = useForm<UserLoginFormData>({
    // resolver: zodResolver(userLoginSchema),
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
      setError(err instanceof Error ? err.message : 'エラーが発生しました');
    }
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} noValidate>
        <LoginForm
          title='ログイン'
          submitLabel='ログイン'
          error={error}
          isSubmitting={form.formState.isSubmitting}
          footer={
            <>
              <div className='text-center py-6'>
                <Link
                  to='/forgot-password'
                  className='text-sm text-primary hover:underline'
                >
                  企業IDまたはパスワードをお忘れの方
                </Link>
              </div>
              <div className='mt-8 border-t border-gray-300 pt-4 text-center'>
                <a
                  href='/contact'
                  className='text-sm text-primary hover:underline'
                >
                  ログインに関するお問い合わせはこちら
                </a>
              </div>
            </>
          }
        >
          <FormField
            control={form.control}
            name='companyId'
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>会員ID</RequiredLabel>
                <FormControl>
                  <Input type='text' placeholder='会員IDを入力' {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name='email'
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>メールアドレス</RequiredLabel>
                <FormControl>
                  <Input type='email' placeholder='you@company.com' {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name='password'
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>パスワード</RequiredLabel>
                <FormControl>
                  <PasswordInput
                    placeholder='パスワードを入力'
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

export default UserLoginForm;
