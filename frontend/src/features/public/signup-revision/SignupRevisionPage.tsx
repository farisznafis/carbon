import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, CheckCircle2, FilePenLine, Loader2 } from 'lucide-react';
import { getSignupRevision, updateSignupRevision } from '@/api/company';
import { exchangeActionLink } from '@/api/auth';
import { ApiError } from '@/api/_shared/errors';
import {
  FormCard,
  FormActions,
  FormPageHeader,
  FormPageShell,
  WizardProgress,
} from '@/components/forms/form-layout';
import {
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import type { SignupData } from '@/contexts/SignupContext';
import { useToast } from '@/hooks/use-toast';
import { SignupStep1, type SignupStep1FormData } from '@/pages/signup/SignupStep1';
import { SignupStep2, type SignupStep2Data } from '@/pages/signup/SignupStep2';
import { SignupStep3 } from '@/pages/signup/SignupStep3';
import { paths } from '@/routes/paths';
import {
  getSignupRevisionErrorStep,
  mapFormDataToSignupRevision,
  mapSignupRevisionFieldErrors,
  mapSignupRevisionToFormData,
  type SignupRevisionFieldErrors,
  type SignupRevisionStep,
} from './signupRevisionMapper';
import { ActionButton } from '@/components/actions';
import { ActionSessionDeadline } from '@/components/feedback/ActionSessionDeadline';
import { clearActionSession, readActionSession, saveActionSession } from '@/lib/actionSession';

const SESSION_PAGE = 'signup-revision';

const revisionSteps = [
  { number: 1, title: '法人・個人情報' },
  { number: 2, title: '方法論・活動情報' },
  { number: 3, title: '確認' },
];

const rejectionReasonFallback =
  '却下理由を取得できませんでした。メールに記載された内容をご確認ください。';
const centeredStateShellClassName =
  'flex min-h-[calc(100dvh-4rem-1px)] items-center py-8';
const centeredStateCardClassName = 'mx-auto w-full max-w-lg';

type RevisionPageState =
  | 'loading'
  | 'load-error'
  | 'form'
  | 'invalid'
  | 'complete';

function getStatusCode(error: ApiError) {
  return String(error.payload?.statusCode ?? '');
}

function isInvalidActionSession(error: unknown) {
  if (!(error instanceof ApiError)) return false;

  const statusCode = getStatusCode(error).toUpperCase();
  return (
    error.status === 401 ||
    error.status === 403 ||
    statusCode.includes('INVALID') ||
    statusCode.includes('EXPIRED')
  );
}

function getPayloadErrors(error: ApiError): unknown {
  return error.payload?.errors;
}

function readRevisionTokenFromFragment() {
  const fragment = window.location.hash.startsWith('#')
    ? window.location.hash.slice(1)
    : window.location.hash;
  return new URLSearchParams(fragment).get('token')?.trim() || null;
}

export default function SignupRevisionPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const initializationStartedRef = useRef(false);
  const submitStartedRef = useRef(false);
  const actionSessionTokenRef = useRef<string | null>(null);
  const [pageState, setPageState] = useState<RevisionPageState>('loading');
  const [currentStep, setCurrentStep] = useState<SignupRevisionStep>(1);
  const [revisionData, setRevisionData] = useState<SignupData | null>(null);
  const [challengeId, setChallengeId] = useState<string | null>(null);
  const [rejectionReason, setRejectionReason] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<SignupRevisionFieldErrors>({});
  const [sessionExpiresAt, setSessionExpiresAt] = useState<number | null>(null);

  const loadRevision = useCallback(async (actionSessionToken: string) => {
    setPageState('loading');

    try {
      const response = await getSignupRevision(actionSessionToken);
      setRevisionData(mapSignupRevisionToFormData(response));
      setChallengeId(response.emailVerificationChallengeId || null);
      setRejectionReason(response.rejectionReason?.trim() || null);
      setFieldErrors({});
      setPageState('form');
    } catch (error) {
      if (isInvalidActionSession(error)) {
        actionSessionTokenRef.current = null;
        clearActionSession(SESSION_PAGE);
        setPageState('invalid');
        return;
      }

      setPageState('load-error');
    }
  }, []);

  useEffect(() => {
    if (initializationStartedRef.current) return;
    initializationStartedRef.current = true;

    const token = readRevisionTokenFromFragment();
    window.history.replaceState(
      window.history.state,
      '',
      `${window.location.pathname}${window.location.search}`,
    );

    if (!token) {
      // A refresh: the token left the URL, but the session from the first opening may still run.
      const stored = readActionSession(SESSION_PAGE);
      if (!stored) {
        setPageState('invalid');
        return;
      }
      actionSessionTokenRef.current = stored.actionSessionToken;
      setSessionExpiresAt(stored.expiresAt);
      void loadRevision(stored.actionSessionToken);
      return;
    }

    void exchangeActionLink(token)
      .then((response) => {
        if (!response.actionSessionToken) {
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
        return loadRevision(response.actionSessionToken);
      })
      .catch(() => {
        actionSessionTokenRef.current = null;
        clearActionSession(SESSION_PAGE);
        setPageState('invalid');
      });
  }, [loadRevision]);

  const handleCompanyStep = (companyData: SignupStep1FormData) => {
    setRevisionData((current) => current && { ...current, ...companyData });
    setFieldErrors((current) => ({
      methodology: current.methodology,
      activityHas: current.activityHas,
      activityInformation: current.activityInformation,
    }));
    setCurrentStep(2);
  };

  const handleMethodologyStep = (methodologyData: SignupStep2Data) => {
    setRevisionData((current) => current && { ...current, ...methodologyData });
    setFieldErrors({});
    setCurrentStep(3);
  };

  const handleSubmit = async () => {
    const actionSessionToken = actionSessionTokenRef.current;
    if (
      !actionSessionToken ||
      !revisionData ||
      submitStartedRef.current
    ) {
      return;
    }

    submitStartedRef.current = true;
    try {
      await updateSignupRevision(
        actionSessionToken,
        mapFormDataToSignupRevision(revisionData, challengeId),
      );
      actionSessionTokenRef.current = null;
      clearActionSession(SESSION_PAGE);
      setRevisionData(null);
      setChallengeId(null);
      setRejectionReason(null);
      setFieldErrors({});
      setPageState('complete');
    } catch (error) {
      if (isInvalidActionSession(error)) {
        actionSessionTokenRef.current = null;
        clearActionSession(SESSION_PAGE);
        setPageState('invalid');
        return;
      }

      if (error instanceof ApiError) {
        const mappedErrors = mapSignupRevisionFieldErrors(getPayloadErrors(error));
        const errorStep = getSignupRevisionErrorStep(mappedErrors);
        if (errorStep) {
          setFieldErrors(mappedErrors);
          setCurrentStep(errorStep);
          toast({
            title: '入力内容を確認してください',
            description: error.payload?.message || '入力内容に誤りがあります。',
            variant: 'destructive',
          });
          return;
        }
      }

      toast({
        title: '再申請できませんでした',
        description:
          error instanceof Error
            ? error.message
            : 'しばらくしてからもう一度お試しください。',
        variant: 'destructive',
      });
    } finally {
      submitStartedRef.current = false;
    }
  };

  if (pageState === 'loading') {
    return (
      <FormPageShell
        className={centeredStateShellClassName}
        contentClassName="flex justify-center"
      >
        <div className="flex items-center gap-3 text-sm text-foreground" role="status">
          <Loader2 className="h-5 w-5 animate-spin text-primary-strong" aria-hidden="true" />
          登録情報を読み込んでいます...
        </div>
      </FormPageShell>
    );
  }

  if (pageState === 'invalid') {
    return (
      <FormPageShell className={centeredStateShellClassName}>
        <FormCard className={centeredStateCardClassName}>
          <CardHeader>
            <CardTitle>リンクが無効または期限切れです</CardTitle>
            <CardDescription>
              このリンクは既に使用されたか、有効期限が切れています。再申請がお済みでない場合は、メールに記載されたリンクをもう一度開いてください。リンクを開いてから2時間が過ぎた場合は、運営事務局に修正用リンクの再送をご依頼ください。
            </CardDescription>
          </CardHeader>
          <CardContent>
            <FormActions
              actions={[
                {
                  id: 'login',
                  label: 'ログインページへ',
                  onClick: () => navigate(paths.login, { replace: true }),
                },
              ]}
            />
          </CardContent>
        </FormCard>
      </FormPageShell>
    );
  }

  if (pageState === 'load-error') {
    return (
      <FormPageShell className={centeredStateShellClassName}>
        <FormCard className={centeredStateCardClassName}>
          <CardHeader>
            <CardTitle>登録情報を取得できませんでした</CardTitle>
            <CardDescription>
              通信状況を確認して、もう一度お試しください。
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ActionButton
              kind="retry"
              className="w-full"
              onClick={() => {
                const token = actionSessionTokenRef.current;
                if (token) void loadRevision(token);
              }}
            >
              再読み込み
            </ActionButton>
          </CardContent>
        </FormCard>
      </FormPageShell>
    );
  }

  if (pageState === 'complete') {
    return (
      <FormPageShell className={centeredStateShellClassName}>
        <FormCard className={`${centeredStateCardClassName} text-center`}>
          <CardHeader className="items-center">
            <CheckCircle2 className="mb-2 h-10 w-10 text-primary-strong" aria-hidden="true" />
            <CardTitle>再申請が完了しました</CardTitle>
            <CardDescription>
              修正した登録情報を送信しました。審査結果はメールでお知らせします。
            </CardDescription>
          </CardHeader>
          <CardContent>
            <FormActions
              actions={[
                {
                  id: 'login',
                  label: 'ログインページへ',
                  onClick: () => navigate(paths.login, { replace: true }),
                },
              ]}
            />
          </CardContent>
        </FormCard>
      </FormPageShell>
    );
  }

  if (!revisionData) return null;

  const { emailVerificationChallengeId: _challengeError, ...formErrors } = fieldErrors;

  return (
    <FormPageShell>
      <div className="space-y-8">
        <FormPageHeader
          icon={<FilePenLine className="h-6 w-6" />}
          title="登録内容の修正"
          description="却下理由をご確認のうえ、登録内容を修正して再申請してください。"
        />

        <section
          aria-labelledby="rejection-reason-title"
          className="rounded-lg border border-destructive/20 bg-destructive/5 p-5"
        >
          <div className="flex items-start gap-3">
            <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-destructive-strong" aria-hidden="true" />
            <div className="min-w-0">
              <h2 id="rejection-reason-title" className="text-base font-bold">
                却下理由
              </h2>
              <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-foreground">
                {rejectionReason ?? rejectionReasonFallback}
              </p>
            </div>
          </div>
        </section>

        {sessionExpiresAt !== null && <ActionSessionDeadline expiresAt={sessionExpiresAt} />}

        <div className="space-y-6">
          <WizardProgress currentStep={currentStep} steps={revisionSteps} />
          <div key={currentStep} className="animate-slide-up">
            {currentStep === 1 && (
              <SignupStep1
                initialData={revisionData}
                onNext={handleCompanyStep}
                validateBeforeNext={false}
                redirectIfLoggedIn={false}
                serverErrors={formErrors}
              />
            )}
            {currentStep === 2 && (
              <SignupStep2
                initialData={revisionData}
                onBack={() => setCurrentStep(1)}
                onNext={handleMethodologyStep}
                validateBeforeNext={false}
                serverErrors={formErrors}
                submitLabel="修正内容を確認"
                useDefaultMethodologies={false}
              />
            )}
            {currentStep === 3 && (
              <SignupStep3
                data={revisionData}
                onBack={() => setCurrentStep(2)}
                onConfirm={handleSubmit}
                title="修正内容の確認"
                description="再申請する前に、修正内容をご確認ください。"
                confirmLabel="修正内容を再申請"
              />
            )}
          </div>
        </div>
      </div>
    </FormPageShell>
  );
}
