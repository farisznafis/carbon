import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
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
  const { handleError, handleSuccess } = useErrorHandler({
    context: 'forgot-password',
  });

  const form = useForm<ForgotPasswordFormData>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: {
      email: '',
    },
  });

  const onSubmit = async (data: ForgotPasswordFormData) => {
    try {
      await requestPasswordReset(data.email);

      handleSuccess("パスワードリセット用のメールを送信しました");

      // Redirect back to login after a short delay
      setTimeout(() => {
        navigate(paths.login);
      }, 2000);
    } catch (error) {
      handleError(error);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      
      <Card className="w-full max-w-md animate-slide-up">
        <CardHeader className="space-y-1">
          <CardTitle className="text-3xl">パスワードをお忘れですか</CardTitle>
          <CardDescription>
            登録されたメールアドレスを入力してください。パスワードリセット用のリンクをお送りします。
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
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
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
                {form.formState.isSubmitting ? '送信中...' : 'リセットリンクを送信'}
              </Button>
            </form>
          </Form>
          <div className="mt-4 text-center text-sm">
            <button
              onClick={() => navigate(paths.login)}
              className="text-primary hover:underline"
            >
              ログインページに戻る
            </button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default ForgotPassword;
