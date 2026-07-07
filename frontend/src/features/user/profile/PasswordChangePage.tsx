import { useNavigate } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { ApiError } from '@/api/_shared/errors';
import { changePassword } from '@/api/auth';
import { PasswordInput } from '@/components/inputs';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Separator } from '@/components/ui/separator';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';

const passwordSchema = z
  .object({
    current: z.string().min(1, '現在のパスワードを入力してください'),
    new: z
      .string()
      .min(8, 'パスワードは8文字以上で入力してください')
      .max(100, 'パスワードは100文字以内で入力してください'),
    confirm: z.string().min(1, '確認用パスワードを入力してください'),
  })
  .refine((data) => data.new === data.confirm, {
    message: '新しいパスワードが一致しません',
    path: ['confirm'],
  });

type PasswordData = z.infer<typeof passwordSchema>;

const PasswordChange = () => {
  const navigate = useNavigate();
  const { toast } = useToast();
  const { user, logout } = useAuth();

  const passwordForm = useForm<PasswordData>({
    // resolver: zodResolver(passwordSchema),
    defaultValues: {
      current: '',
      new: '',
      confirm: '',
    },
  });

  const handlePasswordChange = async (data: PasswordData) => {
    if (!user) {
      toast({
        title: 'エラー',
        description: 'ログイン情報を確認できませんでした',
        variant: 'destructive',
      });
      return;
    }

    try {
      await changePassword({
        userId: user.id,
        currentPassword: data.current,
        newPassword: data.new,
      });

      toast({
        title: 'パスワード変更完了',
        description: '新しいパスワードで再度ログインしてください',
      });

      passwordForm.reset();
      await logout();
      navigate('/login', { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.payload?.errors) {
        const errors = error.payload.errors as Record<string, string>;
        if (errors.newPassword) {
          passwordForm.setError('new', { message: errors.newPassword });
          return;
        }
        return;
      }
      toast({
        title: 'エラー',
        description:
          error instanceof Error
            ? error.message
            : 'パスワードの更新に失敗しました',
        variant: 'destructive',
      });
    }
  };

  return (
    <FormPageWrapper
      pageName='パスワード変更'
      title=''
      description='8文字以上のパスワードを設定してください'
      onBack={() => navigate('/profile')}
      // backText='プロフィールに戻る'
      backText='戻る'
    >
      <Form {...passwordForm}>
        <form
          onSubmit={passwordForm.handleSubmit(handlePasswordChange)}
          className='space-y-4'
        >
          <FormField
            control={passwordForm.control}
            name='current'
            render={({ field }) => (
              <FormItem>
                <FormLabel>現在のパスワード</FormLabel>
                <FormControl>
                  <PasswordInput
                    autoComplete='current-password'
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <Separator />
          <FormField
            control={passwordForm.control}
            name='new'
            render={({ field }) => (
              <FormItem>
                <FormLabel>新しいパスワード</FormLabel>
                <FormControl>
                  <PasswordInput
                    autoComplete='new-password'
                    {...field}
                  />
                </FormControl>
                <FormDescription>
                  8文字以上で、大文字・小文字・数字をそれぞれ1文字以上含むパスワードを設定してください。
                </FormDescription>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={passwordForm.control}
            name='confirm'
            render={({ field }) => (
              <FormItem>
                <FormLabel>新しいパスワード（確認）</FormLabel>
                <FormControl>
                  <PasswordInput
                    autoComplete='new-password'
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <div className='flex gap-3 pt-4 justify-end'>
            <Button
              type='button'
              variant='outline'
              onClick={() => navigate('/profile')}
            >
              キャンセル
            </Button>
            <Button
              type='submit'
              disabled={passwordForm.formState.isSubmitting}
            >
              {passwordForm.formState.isSubmitting
                ? '更新中...'
                : 'パスワードを変更'}
            </Button>
          </div>
        </form>
      </Form>
    </FormPageWrapper>
  );
};

export default PasswordChange;
