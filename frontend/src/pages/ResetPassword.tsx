import { useEffect, useRef, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { z } from 'zod';
import {
  completePasswordReset,
  exchangePasswordResetActionLink,
} from '@/api/auth';
import { ApiError } from '@/api/_shared/errors';
import { PasswordInput } from '@/components/inputs';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';

const INVALID_TOKEN_STATUS = 'INVALID_OR_EXPIRED_TOKEN';

const resetPasswordSchema = z
  .object({
    newPassword: z.string(),
    passwordConfirmation: z.string(),
  })
  .refine((values) => values.newPassword === values.passwordConfirmation, {
    message: '確認用パスワードが一致しません',
    path: ['passwordConfirmation'],
  });

type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
type ResetPasswordPageState = 'exchanging' | 'ready' | 'invalid';

const getStatusCode = (error: ApiError) =>
  String(error.payload?.statusCode ?? '');

const getNewPasswordError = (error: ApiError) => {
  const errors = error.payload?.errors;
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) {
    return null;
  }

  const entry = Object.entries(errors).find(([fieldName]) =>
    fieldName.endsWith('newPassword'),
  );
  return typeof entry?.[1] === 'string' ? entry[1] : null;
};

const formatRemainingTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = seconds % 60;
  return `${minutes}:${String(remainingSeconds).padStart(2, '0')}`;
};

export default function ResetPassword() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const exchangeAttemptedRef = useRef(false);
  const submitStartedRef = useRef(false);
  const [pageState, setPageState] =
    useState<ResetPasswordPageState>('exchanging');
  const [actionSessionToken, setActionSessionToken] = useState<string | null>(
    null,
  );
  const [secondsRemaining, setSecondsRemaining] = useState<number | null>(null);

  const form = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: {
      newPassword: '',
      passwordConfirmation: '',
    },
  });

  useEffect(() => {
    if (exchangeAttemptedRef.current) return;
    exchangeAttemptedRef.current = true;

    const hashParams = new URLSearchParams(window.location.hash.slice(1));
    const token = hashParams.get('token');
    window.history.replaceState(window.history.state, '', paths.resetPassword);

    if (!token) {
      setPageState('invalid');
      return;
    }

    void exchangePasswordResetActionLink(token)
      .then((response) => {
        if (
          response.purpose !== 'PASSWORD_RESET' ||
          !response.actionSessionToken
        ) {
          setPageState('invalid');
          return;
        }

        setActionSessionToken(response.actionSessionToken);
        setSecondsRemaining(response.expiresInSeconds);
        setPageState('ready');
      })
      .catch(() => {
        setPageState('invalid');
      });
  }, []);

  useEffect(() => {
    if (pageState !== 'ready' || secondsRemaining === null) return;
    if (secondsRemaining <= 0) {
      setActionSessionToken(null);
      setPageState('invalid');
      return;
    }

    const timerId = window.setTimeout(() => {
      setSecondsRemaining((current) =>
        current === null ? null : Math.max(0, current - 1),
      );
    }, 1000);

    return () => window.clearTimeout(timerId);
  }, [pageState, secondsRemaining]);

  const handleSubmit = async (values: ResetPasswordFormValues) => {
    if (!actionSessionToken || submitStartedRef.current) return;
    submitStartedRef.current = true;

    try {
      await completePasswordReset(actionSessionToken, values.newPassword);
      setActionSessionToken(null);
      toast({
        title: 'パスワードを再設定しました',
        description: '新しいパスワードでログインしてください。',
        variant: 'success',
      });
      navigate(paths.login, { replace: true });
    } catch (error) {
      if (error instanceof ApiError) {
        const statusCode = getStatusCode(error);

        if (
          error.status === 401 ||
          statusCode === INVALID_TOKEN_STATUS
        ) {
          setActionSessionToken(null);
          setPageState('invalid');
          return;
        }

        if (statusCode === 'SAME_PASSWORD') {
          form.setError('newPassword', {
            type: 'server',
            message: '新しいパスワードは現在のパスワードと異なるものを入力してください。',
          });
          submitStartedRef.current = false;
          return;
        }

        const newPasswordError = getNewPasswordError(error);
        if (newPasswordError) {
          form.setError('newPassword', {
            type: 'server',
            message: newPasswordError,
          });
          submitStartedRef.current = false;
          return;
        }
      }

      toast({
        title: 'パスワードを再設定できませんでした',
        description:
          error instanceof Error
            ? error.message
            : 'しばらくしてからもう一度お試しください。',
        variant: 'destructive',
      });
      submitStartedRef.current = false;
    }
  };

  if (pageState === 'exchanging') {
    return (
      <div className="flex min-h-screen items-center justify-center px-4 py-12">
        <p className="text-sm text-foreground">
          パスワードリセットリンクを確認しています...
        </p>
      </div>
    );
  }

  if (pageState === 'invalid') {
    return (
      <div className="flex min-h-screen items-center justify-center px-4 py-12">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>リンクが無効または期限切れです</CardTitle>
            <CardDescription>
              パスワードリセットリンクは既に使用されたか、有効期限が切れています。
              新しいリンクをリクエストしてください。
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button
              type="button"
              className="w-full"
              onClick={() => navigate(paths.forgotPassword, { replace: true })}
            >
              新しいリンクをリクエスト
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-12">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>新しいパスワードを設定</CardTitle>
          <CardDescription>
            15～64文字の半角英数字・記号を使用してください。一般的なパスワードは避けてください。
          </CardDescription>
          {secondsRemaining !== null && (
            <p className="text-sm text-foreground">
              この画面の有効期限：{formatRemainingTime(secondsRemaining)}
            </p>
          )}
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form
              className="space-y-4"
              onSubmit={form.handleSubmit(handleSubmit)}
            >
              <FormField
                control={form.control}
                name="newPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>新しいパスワード</FormLabel>
                    <FormControl>
                      <PasswordInput
                        {...field}
                        autoComplete="new-password"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="passwordConfirmation"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>新しいパスワード（確認）</FormLabel>
                    <FormControl>
                      <PasswordInput
                        {...field}
                        autoComplete="new-password"
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <Button
                type="submit"
                className="w-full"
                disabled={form.formState.isSubmitting}
              >
                {form.formState.isSubmitting
                  ? '設定中...'
                  : 'パスワードを再設定'}
              </Button>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  );
}
