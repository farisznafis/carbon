import { useCallback, useEffect, useRef, useState } from 'react';
import { useForm, type UseFormSetError } from 'react-hook-form';
import { AlertCircle, Building2, Loader2, LockKeyhole, UserRound } from 'lucide-react';
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
import { Label } from '@/components/ui/label';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';

const INVITATION_PURPOSE = 'USER_INVITATION';
const INVALID_INVITATION_STATUS = 'INVALID_OR_EXPIRED_TOKEN';

type InvitationPageState =
  | 'loading'
  | 'form'
  | 'invalid'
  | 'unavailable'
  | 'load-error';

const emptyInvitationForm: CompleteUserInvitationPayload = {
  username: '',
  firstName: '',
  firstNameKana: '',
  lastName: '',
  lastNameKana: '',
  phoneNumber: '',
  userPosition: '',
  password: '',
};

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
  setError: UseFormSetError<CompleteUserInvitationPayload>,
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
  const initializationStartedRef = useRef(false);
  const submitStartedRef = useRef(false);
  const actionSessionTokenRef = useRef<string | null>(null);
  const [pageState, setPageState] = useState<InvitationPageState>('loading');
  const [invitationContext, setInvitationContext] =
    useState<UserInvitationContext | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const form = useForm<CompleteUserInvitationPayload>({
    defaultValues: emptyInvitationForm,
  });

  /** Loads immutable invitation information after the action link is exchanged. */
  const loadInvitationContext = useCallback(
    async (actionSessionToken: string) => {
      try {
        const context = await getUserInvitationContext(actionSessionToken);
        setInvitationContext(context);
        setFormError(null);
        setPageState('form');
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

  useEffect(() => {
    if (initializationStartedRef.current) return;
    initializationStartedRef.current = true;

    const invitationToken = readInvitationToken();
    window.history.replaceState(
      window.history.state,
      '',
      paths.userInvitation,
    );

    if (!invitationToken) {
      setPageState('invalid');
      return;
    }

    void exchangeActionLink(invitationToken)
      .then((response) => {
        if (
          response.purpose !== INVITATION_PURPOSE ||
          !response.actionSessionToken
        ) {
          setPageState('invalid');
          return;
        }

        actionSessionTokenRef.current = response.actionSessionToken;
        return loadInvitationContext(response.actionSessionToken);
      })
      .catch((error: unknown) => {
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
  }, [loadInvitationContext]);

  /** Submits unvalidated form values and renders backend validation verbatim. */
  const handleSubmit = async (values: CompleteUserInvitationPayload) => {
    const actionSessionToken = actionSessionTokenRef.current;
    if (!actionSessionToken || submitStartedRef.current) return;

    submitStartedRef.current = true;
    setFormError(null);
    form.clearErrors();

    try {
      const response = await completeUserInvitation(actionSessionToken, values);
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
          description="招待されたアカウント情報を入力してください。"
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
                >
                  <FormGrid>
                    <div className="space-y-2">
                      <Label htmlFor="invitation-readonly-email" className="text-small">
                        メールアドレス
                      </Label>
                      <Input
                        id="invitation-readonly-email"
                        value={invitationContext.email}
                        readOnly
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="invitation-company-name" className="text-small">
                        企業名
                      </Label>
                      <Input
                        id="invitation-company-name"
                        value={invitationContext.companyName}
                        readOnly
                      />
                    </div>
                  </FormGrid>
                  <div className="space-y-2">
                    <Label htmlFor="invitation-site-name" className="text-small">
                      拠点名
                    </Label>
                    <Input
                      id="invitation-site-name"
                      value={invitationContext.siteName || '指定なし'}
                      readOnly
                    />
                  </div>
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
                            <Input aria-label="姓" placeholder="山田" {...field} />
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
                            <Input aria-label="名" placeholder="太郎" {...field} />
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
                              placeholder="ヤマダ"
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
                              placeholder="タロウ"
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
                            placeholder="09012345678"
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
                          <Input aria-label="役職" placeholder="部長" {...field} />
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
                            placeholder="yamada_taro"
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
