import { useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { ApiError } from '@/api/_shared/errors';
import { changePassword } from '@/api/auth';
import { PasswordInput } from '@/components/inputs';
import {
  FormActions,
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { useAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { getHomePath } from '@/utils/getHomePath';

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
  const [searchParams] = useSearchParams();
  const { toast } = useToast();
  const { user, logout } = useAuth();
  const requestedUserId = searchParams.get('userId')?.trim() || null;
  const isManagedUserPasswordChange = Boolean(
    user?.role === 'master' &&
      requestedUserId &&
      requestedUserId !== user.id,
  );
  const targetUserId =
    isManagedUserPasswordChange && requestedUserId
      ? requestedUserId
      : user?.id;
  const backPath =
    isManagedUserPasswordChange && requestedUserId
      ? paths.masterUserProfile(requestedUserId)
      : paths.profile;
  const homePath = getHomePath(user);
  const profileBreadcrumbLabel = isManagedUserPasswordChange
    ? '社員プロファイル'
    : 'プロファイル';
  usePageBreadcrumb([
    isManagedUserPasswordChange
      ? { label: '社員', to: `${paths.masterDashboard}?tab=users` }
      : { label: 'ダッシュボード', to: homePath },
    { label: profileBreadcrumbLabel, to: backPath },
    { label: 'パスワード変更' },
  ]);
  const pageDescription = isManagedUserPasswordChange
    ? '社員アカウントのパスワードを変更します。'
    : '安全のため、現在のパスワードを確認して新しいパスワードを設定します。';

  const passwordForm = useForm<PasswordData>({
    // resolver: zodResolver(passwordSchema),
    defaultValues: {
      current: '',
      new: '',
      confirm: '',
    },
  });

  /** Changes the selected account password while preserving the master session for employee updates. */
  const handlePasswordChange = async (data: PasswordData) => {
    if (!user || !targetUserId) {
      toast({
        title: 'エラー',
        description: 'ログイン情報を確認できませんでした',
        variant: 'destructive',
      });
      return;
    }

    try {
      await changePassword({
        userId: targetUserId,
        currentPassword: isManagedUserPasswordChange ? null : data.current,
        newPassword: data.new,
      });

      toast({
        title: 'パスワード変更完了',
        description: isManagedUserPasswordChange
          ? '社員のパスワードを変更しました'
          : '新しいパスワードで再度ログインしてください',
      });

      passwordForm.reset();
      if (isManagedUserPasswordChange) {
        navigate(backPath, { replace: true });
        return;
      }

      await logout();
      navigate(paths.login, { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.payload?.errors) {
        const errors = error.payload.errors as Record<string, string>;
        if (errors.newPassword) {
          passwordForm.setError('new', { message: errors.newPassword });
          return;
        }
        if (!isManagedUserPasswordChange && errors.currentPassword) {
          passwordForm.setError('current', { message: errors.currentPassword });
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
    <FormStandalonePage>
      <FormPageContent>
        <header>
          <FormPageTitle as="h2">パスワード変更</FormPageTitle>
          <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
            {pageDescription}
          </p>
        </header>

        <FormCard>
          <Form {...passwordForm}>
            <form onSubmit={passwordForm.handleSubmit(handlePasswordChange)}>
              <FormSectionStack spacing="relaxed">
                {!isManagedUserPasswordChange && (
                  <FormField
                    control={passwordForm.control}
                    name="current"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>現在のパスワード</FormLabel>
                        <FormControl>
                          <PasswordInput
                            autoComplete="current-password"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                )}

                {/* <FormSection number={2} title="新しいパスワード"> */}
                  <div className="space-y-5">
                    <FormField
                      control={passwordForm.control}
                      name="new"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>新しいパスワード</FormLabel>
                          <FormControl>
                            <PasswordInput
                              autoComplete="new-password"
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
                      name="confirm"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>新しいパスワード（確認）</FormLabel>
                          <FormControl>
                            <PasswordInput
                              autoComplete="new-password"
                              {...field}
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                {/* </FormSection> */}
              </FormSectionStack>

              <FormActions
                className="pt-8"
                pendingActionId={
                  passwordForm.formState.isSubmitting ? 'submit' : null
                }
                actions={[
                  {
                    id: 'cancel',
                    label: 'キャンセル',
                    variant: 'outline',
                    onClick: () => navigate(backPath),
                  },
                  {
                    id: 'submit',
                    label: 'パスワードを変更',
                    pendingLabel: '更新中...',
                    type: 'submit',
                  },
                ]}
              />
            </form>
          </Form>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
};

export default PasswordChange;
