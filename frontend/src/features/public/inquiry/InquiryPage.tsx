import { useEffect, useRef, useState } from 'react';
import { CheckCircle2, Mail } from 'lucide-react';
import { useForm, type UseFormSetError } from 'react-hook-form';
import { useLocation, useNavigate } from 'react-router-dom';

import { ApiError } from '@/api/_shared/errors';
import { createInquiry, type InquiryRequest } from '@/api/notifications';
import { FormActions, FormGrid } from '@/components/forms/form-layout';
import { RequiredLabel } from '@/components/forms/shared';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { useOptionalAuth } from '@/contexts/AuthContext';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';
import { HomeFooter } from '@/pages/home/HomeFooter';
import { getHomePath } from '@/utils/getHomePath';
import {
  PageBackButton,
  floatingPageBackButtonClassName,
} from '@/components/navigation/PageBackButton';

const SUCCESS_REDIRECT_DELAY_MS = 3_000;

const DEFAULT_SUCCESS_MESSAGE = 'お問い合わせありがとうございます。';

const INQUIRY_FIELDS = new Set<keyof InquiryRequest>([
  'companyName',
  'name',
  'email',
  'content',
]);

function getInquiryFieldName(path: string): keyof InquiryRequest | null {
  const fieldName = path
    .split(/::|[.[\]]/)
    .filter(Boolean)
    .at(-1);

  return fieldName && INQUIRY_FIELDS.has(fieldName as keyof InquiryRequest)
    ? (fieldName as keyof InquiryRequest)
    : null;
}

function getInquiryErrorMessage(error: unknown): string {
  if (typeof error === 'string') {
    return error;
  }

  if (Array.isArray(error)) {
    return error.map(getInquiryErrorMessage).filter(Boolean).join('\n');
  }

  if (error && typeof error === 'object' && 'message' in error) {
    return getInquiryErrorMessage(error.message);
  }

  return '';
}

function applyInquiryFieldErrors(
  error: unknown,
  setError: UseFormSetError<InquiryRequest>,
): boolean {
  if (!(error instanceof ApiError)) {
    return false;
  }

  const errors = error.payload?.errors;

  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) {
    return false;
  }

  let hasMappedError = false;

  Object.entries(errors).forEach(([path, value]) => {
    const fieldName = getInquiryFieldName(path);

    const message = getInquiryErrorMessage(value);

    if (!fieldName || !message) {
      return;
    }

    setError(fieldName, {
      type: 'server',
      message,
    });

    hasMappedError = true;
  });

  return hasMappedError;
}

/** Renders the informational side of the inquiry card. */
function InquiryInformationPanel() {
  return (
    <aside
      className='
        surface-brand-dark
        relative
        overflow-hidden
        p-6
        sm:p-8
      '
    >
      <div
        aria-hidden='true'
        className='
          pointer-events-none
          absolute
          -bottom-32
          -right-32
          h-80
          w-80
          rounded-full
          border
          border-brand-dark
        '
      />

      <div
        aria-hidden='true'
        className='
          pointer-events-none
          absolute
          -bottom-20
          -right-20
          h-56
          w-56
          rounded-full
          border
          border-brand-dark
        '
      />

      <div
        className='
          relative
          z-10
          flex
          h-full
          flex-col
        '
      >
        <div>
          <h1
            className='
              text-brand-dark
              text-form-title
              font-bold
              tracking-tight
            '
          >
            お問い合わせ
          </h1>
          <p
            className='
              text-brand-dark-muted
              mt-2
              max-w-sm
              text-sm
              leading-6
              [word-break:auto-phrase]
            '
          >
            サービスについてのご質問や導入のご相談はこちらから。
          </p>
        </div>

        <div
          className='
            mt-8
            border-t
            border-brand-dark
            pt-5
            lg:mt-auto
          '
        >
          <p
            className='
              text-brand-dark-muted
              text-sm
              leading-6
              [word-break:auto-phrase]
            '
          >
            通常、3営業日以内に担当者よりご返信します。
          </p>
        </div>
      </div>
    </aside>
  );
}

function InquirySuccess({
  message,
  returnLabel,
  onReturnHome,
}: Readonly<{
  message: string;
  /** Where the page returns to: ホーム, or ヘルプ・FAQ when opened from it. */
  returnLabel: string;
  onReturnHome: () => void;
}>) {
  return (
    <div
      role='status'
      className='
        flex
        min-h-[22rem]
        flex-col
        items-center
        justify-center
        px-4
        text-center
      '
    >
      <span
        className='
          flex
          h-16
          w-16
          items-center
          justify-center
          rounded-full
          bg-accent
          text-primary-strong
        '
      >
        <CheckCircle2
          className='h-8 w-8'
          aria-hidden='true'
        />
      </span>

      <p
        className='
          mt-7
          text-xs
          font-bold
          tracking-widest
          text-primary-strong
        '
      >
        THANK YOU
      </p>

      <h2
        className='
          mt-3
          text-2xl
          font-bold
          leading-tight
          tracking-tight
          text-foreground
          sm:text-form-title
        '
      >
        お問い合わせを受け付けました。
      </h2>

      <p
        className='
          mt-5
          max-w-md
          text-sm
          leading-7
          text-muted-foreground
          sm:text-base
        '
      >
        {message}
      </p>

      <p
        className='
          mt-2
          text-sm
          text-muted-foreground
        '
      >
        3秒後に{returnLabel}へ戻ります。
      </p>

      <FormActions
        context='auth'
        className='mt-8 w-full sm:w-fit'
        actions={[
          {
            id: 'home',
            label: `${returnLabel}へ戻る`,
            onClick: onReturnHome,
          },
        ]}
      />
    </div>
  );
}

/** Renders the inquiry form and its submission result. */
/** A caller page passed in navigation state, e.g. the ヘルプ・FAQ page. */
function getReturnTo(state: unknown): string | null {
  const returnTo = (state as { returnTo?: unknown } | null)?.returnTo;
  return typeof returnTo === 'string' && returnTo.startsWith('/')
    ? returnTo
    : null;
}

export default function InquiryPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { toast } = useToast();
  const auth = useOptionalAuth();
  // Signed-in accounts reach this from the in-app ヘルプ・FAQ and should stay in the app.
  const signedInUser = auth?.isAuthenticated ? auth.user : null;
  const returnTo = signedInUser ? getReturnTo(location.state) : null;
  const exitPath = signedInUser
    ? (returnTo ?? getHomePath(signedInUser))
    : paths.home;
  const exitLabel = returnTo === paths.adminHelpFaq ? 'ヘルプ・FAQ' : 'ホーム';

  usePageBreadcrumb(
    signedInUser
      ? [{ label: exitLabel, to: exitPath }, { label: 'お問い合わせ' }]
      : [],
  );
  const handleBack = () => {
    navigate(paths.home);
  };

  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // AWS WAF CAPTCHA
  const captchaContainerRef = useRef<HTMLDivElement>(null);
  const [captchaCompleted, setCaptchaCompleted] = useState(false);

  const form = useForm<InquiryRequest>({
    // Saves signed-in accounts retyping what the app already knows.
    defaultValues: {
      companyName: signedInUser?.companyName ?? '',
      name: signedInUser?.name ?? '',
      email: signedInUser?.email ?? '',
      content: '',
    },
  });

  useEffect(() => {
    const container = captchaContainerRef.current;

    if (!container || !window.AwsWafCaptcha) {
      return;
    }

    window.AwsWafCaptcha.renderCaptcha(container, {
      apiKey: import.meta.env.VITE_AWS_WAF_CAPTCHA_API_KEY,

      onSuccess: (_wafToken: string) => {
        setCaptchaCompleted(true);
      },

      onError: () => {
        setCaptchaCompleted(false);
      },

      defaultLocale: 'ja-JP',
    });
  }, []);

  useEffect(() => {
    if (successMessage === null) {
      return;
    }

    const timerId = window.setTimeout(() => {
      navigate(exitPath, {
        replace: true,
      });
    }, SUCCESS_REDIRECT_DELAY_MS);

    return () => {
      window.clearTimeout(timerId);
    };
  }, [exitPath, navigate, successMessage]);

  const handleSubmit = async (inquiry: InquiryRequest) => {
    form.clearErrors();

    try {
      const response = await createInquiry(inquiry);

      setSuccessMessage(response.message?.trim() || DEFAULT_SUCCESS_MESSAGE);
    } catch (error) {
      const hasFieldErrors = applyInquiryFieldErrors(error, form.setError);

      if (hasFieldErrors) {
        return;
      }

      toast({
        title: 'お問い合わせを送信できませんでした',
        description:
          error instanceof Error
            ? error.message
            : '時間をおいて、もう一度お試しください。',
        variant: 'destructive',
      });
    }
  };

  const handleReturnHome = () => {
    navigate(exitPath, {
      replace: true,
    });
  };

  return (
    <>
      <main
        className='
          bg-background
          text-foreground
        '
      >
        <section
          className='
            mx-auto
            flex
            min-h-[calc(100dvh-var(--app-header-height))]
            w-full
            max-w-[min(84vw,100rem)]
            flex-col
            justify-center
            px-4
            py-6
            sm:px-6
            lg:px-8
          '
        >
          {/* <PageBackButton
            onClick={handleBack}
            label=""
            aria-label="ホームに戻る"
            className={floatingPageBackButtonClassName}
          /> */}
          <div
            className='
              overflow-hidden
              rounded-lg
              border
              border-border/70
              bg-card
              shadow-app-md
              lg:grid
              lg:flex-1
              lg:grid-cols-[0.36fr_0.64fr]
            '
          >
            <InquiryInformationPanel />

            <div
              className='
                flex
                flex-col
                p-6
                sm:p-8
              '
            >
              {successMessage !== null ? (
                <InquirySuccess
                  message={successMessage}
                  returnLabel={exitLabel}
                  onReturnHome={handleReturnHome}
                />
              ) : (
                <Form {...form}>
                  <form
                    className='flex flex-1 flex-col'
                    noValidate
                    onSubmit={form.handleSubmit(handleSubmit)}
                  >
                    {/* <div
                      className='
                        border-b
                        border-border
                        pb-5
                      '
                    >
                      <h2
                        className='
                          text-xl
                          font-bold
                          tracking-tight
                          text-foreground
                        '
                      >
                        お問い合わせフォーム
                      </h2>

                      <p
                        className='
                          mt-2
                          text-sm
                          text-muted-foreground
                        '
                      >
                        すべての項目をご入力ください。
                      </p>
                    </div> */}

                    <div
                      className='
                        mt-6
                        flex
                        flex-1
                        flex-col
                        space-y-5
                      '
                    >
                      <FormGrid
                        columns={2}
                        className='
                          gap-5
                        '
                      >
                        <FormField
                          control={form.control}
                          name='companyName'
                          render={({ field }) => (
                            <FormItem>
                              <RequiredLabel>会社名</RequiredLabel>

                              <FormControl>
                                <Input
                                  autoComplete='organization'
                                  placeholder='例）株式会社ソーラーファーム'
                                  aria-required='true'
                                  {...field}
                                />
                              </FormControl>

                              <FormMessage />
                            </FormItem>
                          )}
                        />

                        <FormField
                          control={form.control}
                          name='name'
                          render={({ field }) => (
                            <FormItem>
                              <RequiredLabel>お名前</RequiredLabel>

                              <FormControl>
                                <Input
                                  autoComplete='name'
                                  placeholder='例）田中 太郎'
                                  aria-required='true'
                                  {...field}
                                />
                              </FormControl>

                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </FormGrid>

                      <FormField
                        control={form.control}
                        name='email'
                        render={({ field }) => (
                          <FormItem>
                            <RequiredLabel>メールアドレス</RequiredLabel>

                            <FormControl>
                              <Input
                                type='email'
                                autoComplete='email'
                                placeholder='例）info@company.com'
                                aria-required='true'
                                {...field}
                              />
                            </FormControl>

                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      <FormField
                        control={form.control}
                        name='content'
                        render={({ field }) => (
                          <FormItem className='flex flex-1 flex-col'>
                            <RequiredLabel>お問い合わせ内容</RequiredLabel>

                            <FormControl>
                              <Textarea
                                placeholder='お問い合わせ内容をご入力ください。'
                                className='
                                  min-h-28
                                  flex-1
                                  resize-y
                                '
                                aria-required='true'
                                {...field}
                              />
                            </FormControl>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                      <div
                        ref={captchaContainerRef}
                        className="mt-6"
                      />
                    </div>

                    <div
                      className='
                        mt-6
                      '
                    >
                      <div
                        className='
                          flex
                          justify-end
                        '
                      >
                        <FormActions
                          context='auth'
                          className='
                            w-full
                            sm:w-fit
                          '
                          pendingActionId={
                            form.formState.isSubmitting ? 'submit' : null
                          }
                          actions={[
                            {
                              id: 'submit',
                              label: '送信する',
                              pendingLabel: '送信中...',
                              type: 'submit',
                              disabled: !captchaCompleted,
                            }
                          ]}
                        />
                      </div>
                    </div>
                  </form>
                </Form>
              )}
            </div>
          </div>
        </section>
      </main>

      {/* The marketing footer belongs to the public site, not the app shell. */}
      {!signedInUser && <HomeFooter />}
    </>
  );
}
