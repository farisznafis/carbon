import { useCallback, useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AlertCircle, KeyRound, Loader2, LockKeyhole } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import { exchangeActionLink, setupMasterAccount } from '@/api/auth';
import {
  FormActions,
  FormCard,
  FormPageHeader,
  FormPageShell,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { RequiredLabel } from '@/components/forms/shared';
import { PasswordInput } from '@/components/inputs';
import { Button } from '@/components/ui/button';
import { CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { ActionSessionDeadline } from '@/components/feedback/ActionSessionDeadline';
import { useToast } from '@/hooks/use-toast';
import { clearActionSession, readActionSession, saveActionSession } from '@/lib/actionSession';
import { passwordSchema, usernameSchema } from '@/lib/validations';
import { paths } from '@/routes/paths';

const SETUP_PURPOSE = 'MASTER_ACCOUNT_SETUP';
const SESSION_PAGE = 'master-setup';
/** Marks a page view that resumed a stored session instead of exchanging a token. */
const STORED_SESSION_MARKER = 'stored-session';
const INVALID_LINK_STATUS = 'INVALID_OR_EXPIRED_TOKEN';
const USED_USERNAME_STATUS = 'USED_USERNAME';

const setupSchema = z
  .object({
    username: usernameSchema,
    password: passwordSchema,
    passwordConfirmation: z.string().min(1, '確認のため、パスワードをもう一度入力してください。'),
  })
  .refine((values) => values.password === values.passwordConfirmation, {
    message: 'パスワードが一致していません。',
    path: ['passwordConfirmation'],
  });

type SetupFormValues = z.infer<typeof setupSchema>;
type SetupPageState = 'loading' | 'form' | 'invalid' | 'unavailable' | 'load-error';

const emptySetupForm: SetupFormValues = { username: '', password: '', passwordConfirmation: '' };

/** Reads the one-time setup token exclusively from the URL fragment, which never reaches a server log. */
function readSetupToken(): string | null {
  const fragment = window.location.hash.startsWith('#')
    ? window.location.hash.slice(1)
    : window.location.hash;
  return new URLSearchParams(fragment).get('token')?.trim() || null;
}

function getStatusCode(error: ApiError): string {
  return String(error.payload?.statusCode ?? '').toUpperCase();
}

/** A consumed or expired link, or a session the backend no longer accepts. */
function isInvalidLink(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    (error.status === 401 || getStatusCode(error) === INVALID_LINK_STATUS)
  );
}

/** 404 means FEATURE_MASTER_SETUP_LINK is off on the backend. */
function isUnavailable(error: unknown): boolean {
  return error instanceof ApiError && error.status === 404;
}

function toFieldMessage(value: unknown): string {
  if (typeof value === 'string') return value;
  if (Array.isArray(value)) return value.map(toFieldMessage).filter(Boolean).join('\n');
  if (value && typeof value === 'object' && 'message' in value) {
    return toFieldMessage((value as { message?: unknown }).message);
  }
  return '';
}

/** Puts backend messages under their fields; returns whether any field took one. */
function applySetupFieldErrors(
  error: ApiError,
  setError: (field: 'username' | 'password', message: string) => void,
): boolean {
  // A taken username arrives as a status, not as errors.username.
  if (getStatusCode(error) === USED_USERNAME_STATUS) {
    setError('username', error.payload?.message || error.message);
    return true;
  }

  const errors = error.payload?.errors;
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) return false;

  let applied = false;
  Object.entries(errors).forEach(([field, value]) => {
    const message = toFieldMessage(value);
    if ((field === 'username' || field === 'password') && message) {
      setError(field, message);
      applied = true;
    }
  });
  return applied;
}

function SetupStateCard({
  title,
  description,
  onAction,
}: Readonly<{ title: string; description: string; onAction: () => void }>) {
  return (
    <FormPageShell className="flex min-h-[calc(100dvh-4rem-1px)] items-center py-8">
      <FormCard className="mx-auto w-full max-w-lg">
        <CardHeader className="px-0 pt-0">
          <AlertCircle className="mb-2 h-9 w-9 text-primary-strong" aria-hidden="true" />
          <CardTitle>{title}</CardTitle>
          <CardDescription className="leading-6">{description}</CardDescription>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          <Button className="w-full" onClick={onAction}>
            ログインページへ
          </Button>
        </CardContent>
      </FormCard>
    </FormPageShell>
  );
}

/** Lets the master of a newly approved company choose a username and password from the emailed link. */
export default function MasterSetupPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const processedTokenRef = useRef<string | null>(null);
  const actionSessionTokenRef = useRef<string | null>(null);
  const [pageState, setPageState] = useState<SetupPageState>('loading');
  const [formError, setFormError] = useState<string | null>(null);
  const [sessionExpiresAt, setSessionExpiresAt] = useState<number | null>(null);

  const form = useForm<SetupFormValues>({
    resolver: zodResolver(setupSchema),
    defaultValues: emptySetupForm,
  });
  const resetForm = form.reset;

  /** Exchanges each newly supplied fragment token once, including same-tab hash navigation. */
  const processSetupToken = useCallback(() => {
    const token = readSetupToken();

    if (!token) {
      if (processedTokenRef.current) return;
      // A refresh: the token left the URL, but the session from the first opening may still run.
      const stored = readActionSession(SESSION_PAGE);
      processedTokenRef.current = STORED_SESSION_MARKER;
      if (!stored) {
        setPageState('invalid');
        return;
      }
      actionSessionTokenRef.current = stored.actionSessionToken;
      setSessionExpiresAt(stored.expiresAt);
      setPageState('form');
      return;
    }
    if (processedTokenRef.current === token) return;
    processedTokenRef.current = token;

    // Drop the token from the address bar and history as soon as it is read.
    window.history.replaceState(window.history.state, '', paths.masterAccountSetup);
    actionSessionTokenRef.current = null;
    setFormError(null);
    setPageState('loading');
    resetForm(emptySetupForm);

    void exchangeActionLink(token)
      .then((response) => {
        if (processedTokenRef.current !== token) return;

        if (response.purpose !== SETUP_PURPOSE || !response.actionSessionToken) {
          clearActionSession(SESSION_PAGE);
          setPageState('invalid');
          return;
        }
        actionSessionTokenRef.current = response.actionSessionToken;
        const session = saveActionSession(
          SESSION_PAGE,
          response.actionSessionToken,
          response.expiresInSeconds,
        );
        setSessionExpiresAt(session.expiresAt);
        setPageState('form');
      })
      .catch((error: unknown) => {
        if (processedTokenRef.current !== token) return;

        if (isUnavailable(error)) {
          clearActionSession(SESSION_PAGE);
          setPageState('unavailable');
          return;
        }
        if (isInvalidLink(error)) {
          clearActionSession(SESSION_PAGE);
          setPageState('invalid');
          return;
        }
        setFormError(error instanceof Error ? error.message : '設定リンクを確認できませんでした。');
        setPageState('load-error');
      });
  }, [resetForm]);

  useEffect(() => {
    processSetupToken();
    window.addEventListener('hashchange', processSetupToken);
    return () => window.removeEventListener('hashchange', processSetupToken);
  }, [processSetupToken]);

  const handleSubmit = async ({ username, password }: SetupFormValues) => {
    const actionSessionToken = actionSessionTokenRef.current;
    if (!actionSessionToken) return;

    setFormError(null);

    try {
      const response = await setupMasterAccount(actionSessionToken, { username, password });
      actionSessionTokenRef.current = null;
      clearActionSession(SESSION_PAGE);
      toast({
        title: 'マスターアカウントの設定が完了しました。',
        description: response.message || '設定したユーザー名とパスワードでログインしてください。',
        variant: 'success',
      });
      navigate(paths.login, { replace: true });
    } catch (error) {
      if (isInvalidLink(error)) {
        actionSessionTokenRef.current = null;
        clearActionSession(SESSION_PAGE);
        setPageState('invalid');
        return;
      }
      if (isUnavailable(error)) {
        actionSessionTokenRef.current = null;
        clearActionSession(SESSION_PAGE);
        setPageState('unavailable');
        return;
      }
      if (error instanceof ApiError) {
        const applied = applySetupFieldErrors(error, (field, message) =>
          form.setError(field, { type: 'server', message }),
        );
        if (!applied) setFormError(error.payload?.message || error.message);
        return;
      }
      setFormError(error instanceof Error ? error.message : 'マスターアカウントを設定できませんでした。');
    }
  };

  const toLogin = () => navigate(paths.login, { replace: true });

  if (pageState === 'loading') {
    return (
      <FormPageShell className="flex min-h-[calc(100dvh-4rem-1px)] items-center justify-center">
        <div className="flex items-center gap-3" role="status">
          <Loader2 className="h-5 w-5 animate-spin text-primary-strong" aria-hidden="true" />
          設定リンクを確認しています...
        </div>
      </FormPageShell>
    );
  }

  if (pageState === 'invalid') {
    return (
      <SetupStateCard
        title="設定リンクを利用できません"
        description="このリンクはすでに使用されたか、有効期限が切れています。設定がお済みでない場合は、メールに記載されたリンクをもう一度開いてください。リンクを開いてから30分が過ぎた場合や、72時間の有効期限が切れた場合は、運営事務局に設定リンクの再送をご依頼ください。"
        onAction={toLogin}
      />
    );
  }

  if (pageState === 'unavailable') {
    return (
      <SetupStateCard
        title="マスターアカウント設定は現在利用できません"
        description="この機能は現在準備中です。利用可能になるまでお待ちください。"
        onAction={toLogin}
      />
    );
  }

  if (pageState === 'load-error') {
    return (
      <SetupStateCard
        title="設定リンクを確認できませんでした"
        description={formError || 'しばらくしてからもう一度お試しください。'}
        onAction={toLogin}
      />
    );
  }

  return (
    <FormPageShell>
      <div className="space-y-6">
        <FormPageHeader
          icon={<KeyRound className="h-6 w-6" />}
          title="マスターアカウントの設定"
          description="ログインに使うユーザー名とパスワードを設定してください。設定が完了すると、企業アカウントが有効になります。"
        />

        <Form {...form}>
          <form noValidate onSubmit={form.handleSubmit(handleSubmit)}>
            <FormCard>
              <FormSectionStack spacing="relaxed">
                <FormSection icon={<LockKeyhole className="h-5 w-5" />} title="アカウント情報">
                  <FormField
                    control={form.control}
                    name="username"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>ユーザー名</RequiredLabel>
                        <FormControl>
                          <Input
                            aria-label="ユーザー名"
                            autoComplete="username"
                            placeholder="例）yamada_taro"
                            {...field}
                          />
                        </FormControl>
                        <FormDescription>3～50文字。半角英数字、スペース、@、_ が使えます。</FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="password"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>パスワード</RequiredLabel>
                        <FormControl>
                          <PasswordInput
                            aria-label="パスワード"
                            autoComplete="new-password"
                            placeholder="パスワードを入力"
                            {...field}
                          />
                        </FormControl>
                        <FormDescription>
                          8～64文字の半角英数字・記号で、大文字・小文字・数字をそれぞれ1文字以上含めてください。
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="passwordConfirmation"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>パスワード（確認）</RequiredLabel>
                        <FormControl>
                          <PasswordInput
                            aria-label="パスワード（確認）"
                            autoComplete="new-password"
                            placeholder="パスワードを再入力"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </FormSection>

                {formError && (
                  <p
                    className="rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive-strong"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                {sessionExpiresAt !== null && (
                  <ActionSessionDeadline expiresAt={sessionExpiresAt} />
                )}

                <FormActions
                  actions={[{ id: 'submit', label: '設定する', pendingLabel: '設定中...', type: 'submit' }]}
                  pendingActionId={form.formState.isSubmitting ? 'submit' : null}
                />
              </FormSectionStack>
            </FormCard>
          </form>
        </Form>
      </div>
    </FormPageShell>
  );
}
