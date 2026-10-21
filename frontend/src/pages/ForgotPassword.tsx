import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MailCheck } from 'lucide-react';
import { FormActions } from '@/components/forms/form-layout';
import { Input } from '@/components/ui/input';
import { AuthPageLayout } from '@/components/forms/shared/AuthPageLayout';
import { useErrorHandler } from '@/hooks/useErrorHandler';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { requestPasswordReset } from '@/api/auth';
import { paths } from '@/routes/paths';

const forgotPasswordSchema = z.object({
  email: z
    .string()
    .min(1, 'メールアドレスを入力してください')
    .email('有効なメールアドレスを入力してください')
    .max(255, 'メールアドレスは255文字以内で入力してください'),
});

type ForgotPasswordFormData = z.infer<typeof forgotPasswordSchema>;

const ForgotPassword = () => {
  const navigate = useNavigate();
  const { handleError } = useErrorHandler({
    context: 'forgot-password',
  });
  const [view, setView] = useState<'form' | 'success'>('form');

  const form = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: '',
    },
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    try {
      await requestPasswordReset(data.email);
      setView('success');
    } catch (error) {
      handleError(error);
    }
  };

  if (view === 'success') {
    return (
      <AuthPageLayout
        key="password-reset-success"
        title="パスワード再設定メールを送信しました"
        headerIcon={<MailCheck className="h-8 w-8" aria-hidden="true" />}
      >
        <p role="status" className="text-sm leading-6 text-foreground sm:text-base">
          ご入力いただいたメールアドレスが登録されている場合、パスワード再設定用のリンクをお送りします。メールが届かない場合は、迷惑メールフォルダをご確認ください。
        </p>
        <FormActions
          context="auth"
          actions={[
            {
              id: 'login',
              label: 'ログインへ戻る',
              onClick: () => navigate(paths.login),
            },
          ]}
        />
      </AuthPageLayout>
    );
  }

  return (
    <AuthPageLayout
      key="password-reset-form"
      title="パスワードをお忘れですか"
      description="登録されたメールアドレスを入力してください。パスワードリセット用のリンクをお送りします。"
      footer={
        <button
          type="button"
          onClick={() => navigate(paths.login)}
          className="relative text-sm font-medium text-foreground transition-colors hover:text-primary-strong before:absolute before:inset-x-0 before:-inset-y-3 before:content-[''] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        >
          ログインページに戻る
        </button>
      }
    >
      <Form {...form}>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
          <FormField
            control={form.control}
            name="email"
            render={({ field }) => (
              <FormItem>
                <FormLabel>メールアドレス</FormLabel>
                <FormControl>
                  <Input
                    type="email"
                    placeholder="you@company.com"
                    autoComplete="email"
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormActions
            context="auth"
            pendingActionId={form.formState.isSubmitting ? 'submit' : null}
            actions={[
              {
                id: 'submit',
                label: 'リセットリンクを送信',
                pendingLabel: '送信中...',
                type: 'submit',
              },
            ]}
          />
        </form>
      </Form>
    </AuthPageLayout>
  );
};

export default ForgotPassword;
