import { useCallback, useEffect, useRef, useState } from 'react';
import { useForm, useWatch, type UseFormSetError } from 'react-hook-form';
import {
  AlertCircle,
  Building2,
  CheckCircle2,
  CircleAlert,
  Loader2,
  LockKeyhole,
  UserRound,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import { exchangeActionLink } from '@/api/auth';
import {
  completeUserInvitation,
  getUserInvitationContext,
  type CompleteUserInvitationPayload,
  type UserInvitationContext,
} from '@/api/user-invitations';
import {
  FormActions,
  FormCard,
  FormGrid,
  FormPageHeader,
  FormPageShell,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { RequiredLabel } from '@/components/forms/shared';
import { PasswordInput } from '@/components/inputs';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import {
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';

const INVITATION_PURPOSE = 'USER_INVITATION';
const INVALID_INVITATION_STATUS = 'INVALID_OR_EXPIRED_TOKEN';
const INVITATION_READONLY_DESCRIPTION_ID = 'invitation-readonly-note';

type InvitationReadonlyItemProps = {
  label: string;
  value: string;
  breakAll?: boolean;
};

type UserInvitationFormValues = CompleteUserInvitationPayload & {
  passwordConfirmation: string;
};

type PasswordConfirmationState = 'idle' | 'match' | 'mismatch';

type InvitationPageState =
  | 'loading'
  | 'form'
  | 'invalid'
  | 'unavailable'
  | 'load-error';

const emptyInvitationForm: UserInvitationFormValues = {
  username: '',
  firstName: '',
  firstNameKana: '',
  lastName: '',
  lastNameKana: '',
  phoneNumber: '',
  userPosition: '',
  password: '',
  passwordConfirmation: '',
};

/** Renders one label-value pair inside the immutable invitation summary. */
function InvitationReadonlyItem({
  label,
  value,
  breakAll = false,
}: Readonly<InvitationReadonlyItemProps>) {
  return (
    <div className="grid gap-1 py-4 sm:grid-cols-[10rem_minmax(0,1fr)] sm:gap-6">
      <dt className="text-sm font-medium text-muted-foreground">{label}</dt>
      <dd
        className={
          breakAll
            ? 'break-all text-sm font-bold text-foreground'
            : 'break-words text-sm font-bold text-foreground'
        }
      >
        {value}
      </dd>
    </div>
  );
}

/** Resolves the live password confirmation state without validating password policy. */
function getPasswordConfirmationState(
  password: string,
  passwordConfirmation: string,
): PasswordConfirmationState {
  if (!passwordConfirmation) return 'idle';
  return password === passwordConfirmation ? 'match' : 'mismatch';
}

/** Announces the current password match state while the user types. */
function PasswordConfirmationStatus({
  state,
}: Readonly<{ state: PasswordConfirmationState }>) {
  if (state === 'idle') {
    return (
      <FormDescription aria-live="polite">
        確認のため、パスワードをもう一度入力してください。
      </FormDescription>
    );
  }

  const isMatch = state === 'match';
  const StatusIcon = isMatch ? CheckCircle2 : CircleAlert;

  return (
    <p
      aria-live="polite"
      className={
        isMatch
          ? 'flex items-center gap-1.5 text-sm text-success'
          : 'flex items-center gap-1.5 text-sm text-destructive'
      }
    >
      <StatusIcon className="h-4 w-4 shrink-0" aria-hidden="true" />
      {isMatch
        ? 'パスワードが一致しています。'
        : 'パスワードが一致していません。'}
    </p>
  );
}

/** Reads the one-time invitation token exclusively from the URL fragment. */
function readInvitationToken(): string | null {
  const fragment = window.location.hash.startsWith('#')
    ? window.location.hash.slice(1)
    : window.location.hash;
  return new URLSearchParams(fragment).get('token')?.trim() || null;
}

/** Reads a stable backend status code from an API error. */
function getInvitationStatusCode(error: ApiError): string {
  return String(error.payload?.statusCode ?? '').toUpperCase();
}

/** Detects a consumed or expired invitation/action session. */
function isInvalidInvitation(error: unknown): boolean {
  return (
    error instanceof ApiError &&
    (error.status === 401 ||
      getInvitationStatusCode(error) === INVALID_INVITATION_STATUS)
  );
}

/** Converts a backend validation entry into text without changing its wording. */
function toInvitationFieldMessage(value: unknown): string {
  if (typeof value === 'string') return value;
  if (Array.isArray(value)) {
    return value.map(toInvitationFieldMessage).filter(Boolean).join('\n');
  }
  if (value && typeof value === 'object' && 'message' in value) {
    return toInvitationFieldMessage(
      (value as { message?: unknown }).message,
    );
  }
  return '';
}

/** Applies backend validation messages to fields with exactly matching request keys. */
function applyInvitationFieldErrors(
  error: ApiError,
  setError: UseFormSetError<UserInvitationFormValues>,
): boolean {
  const errors = error.payload?.errors;
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) {
    return false;
  }

  const supportedFields = new Set<keyof CompleteUserInvitationPayload>([
    'username',
    'firstName',
    'firstNameKana',
    'lastName',
    'lastNameKana',
    'phoneNumber',
    'userPosition',
    'password',
  ]);
  let applied = false;

  Object.entries(errors).forEach(([fieldName, value]) => {
    if (!supportedFields.has(fieldName as keyof CompleteUserInvitationPayload)) {
      return;
    }

    const message = toInvitationFieldMessage(value);
    if (!message) return;

    setError(fieldName as keyof CompleteUserInvitationPayload, {
      type: 'server',
      message,
    });
    applied = true;
  });

  return applied;
}

/** Renders a centered invitation state with a single route action. */
function InvitationStateCard({
  title,
  description,
  actionLabel = 'ログインページへ',
  onAction,
}: Readonly<{
  title: string;
  description: string;
  actionLabel?: string;
  onAction: () => void;
}>) {
  return (
    <FormPageShell className="flex min-h-[calc(100dvh-4rem-1px)] items-center py-8">
      <FormCard className="mx-auto w-full max-w-lg">
        <CardHeader className="px-0 pt-0">
          <AlertCircle
            className="mb-2 h-9 w-9 text-primary"
            aria-hidden="true"
          />
          <CardTitle>{title}</CardTitle>
          <CardDescription className="leading-6">
            {description}
          </CardDescription>
        </CardHeader>
        <CardContent className="px-0 pb-0">
          <Button className="w-full" onClick={onAction}>
            {actionLabel}
          </Button>
        </CardContent>
      </FormCard>
    </FormPageShell>
  );
}

/** Completes account registration from a one-time invitation action link. */
export default function UserInvitationPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const processedInvitationTokenRef = useRef<string | null>(null);
  const submitStartedRef = useRef(false);
  const actionSessionTokenRef = useRef<string | null>(null);
  const [pageState, setPageState] = useState<InvitationPageState>('loading');
  const [invitationContext, setInvitationContext] =
    useState<UserInvitationContext | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const form = useForm<UserInvitationFormValues>({
    defaultValues: emptyInvitationForm,
  });
  const clearFormErrors = form.clearErrors;
  const resetForm = form.reset;
  const password = useWatch({ control: form.control, name: 'password' });
  const passwordConfirmation = useWatch({
    control: form.control,
    name: 'passwordConfirmation',
  });
  const passwordConfirmationState = getPasswordConfirmationState(
    password,
    passwordConfirmation,
  );

  /** Loads immutable invitation information after the action link is exchanged. */
  const loadInvitationContext = useCallback(
    async (actionSessionToken: string, invitationToken: string) => {
      try {
        const context = await getUserInvitationContext(actionSessionToken);
        if (processedInvitationTokenRef.current !== invitationToken) return;

        setInvitationContext(context);
        setFormError(null);
        setPageState('form');
      } catch (error) {
        if (processedInvitationTokenRef.current !== invitationToken) return;

        if (isInvalidInvitation(error)) {
          actionSessionTokenRef.current = null;
          setPageState('invalid');
          return;
        }
        if (error instanceof ApiError && error.status === 404) {
          actionSessionTokenRef.current = null;
          setPageState('unavailable');
          return;
        }

        setFormError(
          error instanceof Error
            ? error.message
            : '招待情報を取得できませんでした。',
        );
        setPageState('load-error');
      }
    },
    [],
  );

  /** Exchanges each newly supplied fragment token once, including same-tab hash navigation. */
  const processInvitationToken = useCallback(() => {
    const invitationToken = readInvitationToken();

    if (!invitationToken) {
      if (!processedInvitationTokenRef.current) {
        setPageState('invalid');
      }
      return;
    }

    if (processedInvitationTokenRef.current === invitationToken) return;
    processedInvitationTokenRef.current = invitationToken;

    window.history.replaceState(
      window.history.state,
      '',
      paths.userInvitation,
    );
    actionSessionTokenRef.current = null;
    submitStartedRef.current = false;
    setInvitationContext(null);
    setFormError(null);
    setPageState('loading');
    clearFormErrors();
    resetForm(emptyInvitationForm);

    void exchangeActionLink(invitationToken)
      .then((response) => {
        if (processedInvitationTokenRef.current !== invitationToken) return;

        if (
          response.purpose !== INVITATION_PURPOSE ||
          !response.actionSessionToken
        ) {
          actionSessionTokenRef.current = null;
          setPageState('invalid');
          return;
        }

        actionSessionTokenRef.current = response.actionSessionToken;
        return loadInvitationContext(
          response.actionSessionToken,
          invitationToken,
        );
      })
      .catch((error: unknown) => {
        if (processedInvitationTokenRef.current !== invitationToken) return;

        actionSessionTokenRef.current = null;
        if (error instanceof ApiError && error.status === 404) {
          setPageState('unavailable');
          return;
        }
        if (isInvalidInvitation(error)) {
          setPageState('invalid');
          return;
        }

        setFormError(
          error instanceof Error
            ? error.message
            : '招待リンクを確認できませんでした。',
        );
        setPageState('load-error');
      });
  }, [clearFormErrors, loadInvitationContext, resetForm]);

  useEffect(() => {
    /** Reprocesses the invitation when a new fragment is opened in this tab. */
    const handleInvitationHashChange = () => processInvitationToken();

    processInvitationToken();
    window.addEventListener('hashchange', handleInvitationHashChange);

    return () => {
      window.removeEventListener('hashchange', handleInvitationHashChange);
    };
  }, [processInvitationToken]);

  /** Submits unvalidated form values and renders backend validation verbatim. */
  const handleSubmit = async (values: UserInvitationFormValues) => {
    const actionSessionToken = actionSessionTokenRef.current;
    if (!actionSessionToken || submitStartedRef.current) return;

    if (values.password !== values.passwordConfirmation) {
      form.setError('passwordConfirmation', {
        type: 'validate',
        message: 'パスワードが一致していません。',
      });
      return;
    }

    const { passwordConfirmation: _passwordConfirmation, ...payload } = values;

    submitStartedRef.current = true;
    setFormError(null);
    form.clearErrors();

    try {
      const response = await completeUserInvitation(
        actionSessionToken,
        payload,
      );
      actionSessionTokenRef.current = null;
      toast({
        title: 'アカウント登録が完了しました。',
        description: response.message || 'ログインしてください。',
        variant: 'success',
      });
      navigate(paths.login, { replace: true });
    } catch (error) {
      if (isInvalidInvitation(error)) {
        actionSessionTokenRef.current = null;
        setPageState('invalid');
        return;
      }
      if (error instanceof ApiError && error.status === 404) {
        actionSessionTokenRef.current = null;
        setPageState('unavailable');
        return;
      }

      if (error instanceof ApiError) {
        applyInvitationFieldErrors(error, form.setError);
        setFormError(error.message);
      } else {
        setFormError(
          error instanceof Error
            ? error.message
            : 'アカウントを登録できませんでした。',
        );
      }
    } finally {
      submitStartedRef.current = false;
    }
  };

  if (pageState === 'loading') {
    return (
      <FormPageShell className="flex min-h-[calc(100dvh-4rem-1px)] items-center justify-center">
        <div className="flex items-center gap-3" role="status">
          <Loader2 className="h-5 w-5 animate-spin text-primary" aria-hidden="true" />
          招待情報を読み込み中です...
        </div>
      </FormPageShell>
    );
  }

  if (pageState === 'invalid') {
    return (
      <InvitationStateCard
        title="招待リンクを利用できません"
        description="このリンクはすでに使用されたか、有効期限が切れています。新しい招待について、招待した方へお問い合わせください。"
        onAction={() => navigate(paths.login, { replace: true })}
      />
    );
  }

  if (pageState === 'unavailable') {
    return (
      <InvitationStateCard
        title="ユーザー招待機能は現在利用できません"
        description="この機能は現在準備中です。利用可能になるまでお待ちください。"
        onAction={() => navigate(paths.login, { replace: true })}
      />
    );
  }

  if (pageState === 'load-error') {
    return (
      <InvitationStateCard
        title="招待情報を取得できませんでした"
        description={formError || 'しばらくしてからもう一度お試しください。'}
        onAction={() => navigate(paths.login, { replace: true })}
      />
    );
  }

  if (!invitationContext) return null;

  return (
    <FormPageShell>
      <div className="space-y-6">
        <FormPageHeader
          icon={<UserRound className="h-6 w-6" />}
          title="アカウント登録"
          description="招待情報を確認し、個人情報とログイン情報を入力してください。"
        />

        <Form {...form}>
          <form
            noValidate
            onSubmit={form.handleSubmit(handleSubmit)}
          >
            <FormCard>
              <FormSectionStack spacing="relaxed">
                <FormSection
                  icon={<Building2 className="h-5 w-5" />}
                  title="招待情報"
                  meta={<StatusBadge>変更不可</StatusBadge>}
                  description={
                    <div id={INVITATION_READONLY_DESCRIPTION_ID}>
                      <span className="block">
                        招待リンクから自動入力されています。内容は変更できません。
                        内容に誤りがある場合は、招待元のマスターにお問い合わせください。
                      </span>
                      {/* <span className="block"> */}
                        {/* 内容に誤りがある場合は、招待元のマスターにお問い合わせください。 */}
                      {/* </span> */}
                    </div>
                  }
                >
                  <dl
                    aria-describedby={INVITATION_READONLY_DESCRIPTION_ID}
                    className="divide-y divide-border rounded-lg border border-border bg-muted/30 px-4"
                  >
                    <InvitationReadonlyItem
                      label="メールアドレス"
                      value={invitationContext.email}
                      breakAll
                    />
                    {/* <InvitationReadonlyItem
                      label="企業名"
                      value={invitationContext.companyName}
                    /> */}
                    <InvitationReadonlyItem
                      label="拠点名"
                      value={invitationContext.siteName || '指定なし'}
                    />
                  </dl>
                </FormSection>

                <FormSection
                  icon={<UserRound className="h-5 w-5" />}
                  title="個人情報"
                >
                  <FormGrid>
                    <FormField
                      control={form.control}
                      name="lastName"
                      render={({ field }) => (
                        <FormItem>
                          <RequiredLabel>姓</RequiredLabel>
                          <FormControl>
                            <Input aria-label="姓" placeholder="例）山田" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="firstName"
                      render={({ field }) => (
                        <FormItem>
                          <RequiredLabel>名</RequiredLabel>
                          <FormControl>
                            <Input aria-label="名" placeholder="例）太郎" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="lastNameKana"
                      render={({ field }) => (
                        <FormItem>
                          <RequiredLabel>姓（カナ）</RequiredLabel>
                          <FormControl>
                            <Input
                              aria-label="姓（カナ）"
                              placeholder="例）ヤマダ"
                              {...field}
                            />
                          </FormControl>
                          <FormDescription>
                            カタカナで入力してください。
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="firstNameKana"
                      render={({ field }) => (
                        <FormItem>
                          <RequiredLabel>名（カナ）</RequiredLabel>
                          <FormControl>
                            <Input
                              aria-label="名（カナ）"
                              placeholder="例）タロウ"
                              {...field}
                            />
                          </FormControl>
                          <FormDescription>
                            カタカナで入力してください。
                          </FormDescription>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </FormGrid>

                  <FormField
                    control={form.control}
                    name="phoneNumber"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>電話番号（ハイフン無し）</RequiredLabel>
                        <FormControl>
                          <Input
                            aria-label="電話番号（ハイフン無し）"
                            inputMode="numeric"
                            placeholder="例）09012345678"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="userPosition"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>役職</RequiredLabel>
                        <FormControl>
                          <Input aria-label="役職" placeholder="例）部長" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </FormSection>

                <FormSection
                  icon={<LockKeyhole className="h-5 w-5" />}
                  title="アカウント情報"
                >
                  <FormField
                    control={form.control}
                    name="username"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>ユーザーネーム</RequiredLabel>
                        <FormControl>
                          <Input
                            aria-label="ユーザーネーム"
                            placeholder="例）yamada_taro"
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
                          15～64文字。大文字・小文字・数字をそれぞれ1文字以上含めてください。
                        </FormDescription>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <FormField
                    control={form.control}
                    name="passwordConfirmation"
                    render={({ field, fieldState }) => (
                      <FormItem>
                        <RequiredLabel>パスワード（確認）</RequiredLabel>
                        <FormControl>
                          <PasswordInput
                            aria-label="パスワード（確認）"
                            aria-invalid={
                              fieldState.invalid ||
                              passwordConfirmationState === 'mismatch'
                            }
                            autoComplete="new-password"
                            placeholder="パスワードを再入力"
                            {...field}
                            onChange={(event) => {
                              field.onChange(event);
                              form.clearErrors('passwordConfirmation');
                            }}
                          />
                        </FormControl>
                        <PasswordConfirmationStatus
                          state={passwordConfirmationState}
                        />
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </FormSection>

                {formError && (
                  <p
                    className="rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
                    role="alert"
                  >
                    {formError}
                  </p>
                )}

                <FormActions
                  actions={[
                    {
                      id: 'submit',
                      label: '登録する',
                      pendingLabel: '登録中...',
                      type: 'submit',
                    },
                  ]}
                  pendingActionId={
                    form.formState.isSubmitting ? 'submit' : null
                  }
                />
              </FormSectionStack>
            </FormCard>
          </form>
        </Form>
      </div>
    </FormPageShell>
  );
}
