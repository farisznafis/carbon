import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle2 } from 'lucide-react';
import { useForm, type UseFormSetError } from 'react-hook-form';
import { ApiError } from '@/api/_shared/errors';
import {
  createInquiry,
  type InquiryRequest,
} from '@/api/notifications';
import {
  FormActions,
} from '@/components/forms/form-layout';
import { RequiredLabel } from '@/components/forms/shared';
import { Card, CardContent } from '@/components/ui/card';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/hooks/use-toast';
import { paths } from '@/routes/paths';

const SUCCESS_REDIRECT_DELAY_MS = 3_000;
const DEFAULT_SUCCESS_MESSAGE = 'お問い合わせありがとうございます。';
const INQUIRY_FIELDS = new Set<keyof InquiryRequest>([
  'companyName',
  'name',
  'email',
  'content',
]);

/** Resolves a backend validation path to one of the inquiry form fields. */
function getInquiryFieldName(path: string): keyof InquiryRequest | null {
  const fieldName = path.split(/::|[.[\]]/).filter(Boolean).at(-1);

  return fieldName && INQUIRY_FIELDS.has(fieldName as keyof InquiryRequest)
    ? (fieldName as keyof InquiryRequest)
    : null;
}

/** Converts a backend field error value into displayable text. */
function getInquiryErrorMessage(error: unknown): string {
  if (typeof error === 'string') return error;
  if (Array.isArray(error)) {
    return error.map(getInquiryErrorMessage).filter(Boolean).join('\n');
  }
  if (error && typeof error === 'object' && 'message' in error) {
    return getInquiryErrorMessage(error.message);
  }

  return '';
}

/** Maps backend validation errors to their matching react-hook-form controls. */
function applyInquiryFieldErrors(
  error: unknown,
  setError: UseFormSetError<InquiryRequest>,
): boolean {
  if (!(error instanceof ApiError)) return false;

  const errors = error.payload?.errors;
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) {
    return false;
  }

  let hasMappedError = false;
  Object.entries(errors).forEach(([path, value]) => {
    const fieldName = getInquiryFieldName(path);
    const message = getInquiryErrorMessage(value);
    if (!fieldName || !message) return;

    setError(fieldName, { type: 'server', message });
    hasMappedError = true;
  });

  return hasMappedError;
}

/** Renders the Figma contact illustration and inquiry guidance. */
function InquiryInformationCard() {
  return (
    <Card className="flex w-full max-w-[30rem] flex-col justify-end overflow-hidden rounded-3xl border-0 pt-8 shadow-app-md lg:min-h-[42.25rem] lg:self-stretch">
      <div className="relative mx-auto mb-[-1.875rem] aspect-[444/336] min-h-64 w-[calc(100%-2rem)] max-w-[27.75rem] flex-1 overflow-hidden sm:min-h-72 sm:w-[calc(100%-2.25rem)]">
        <img
          src="/assets/contact/inquiry-guide.png"
          alt=""
          className="absolute bottom-[-15%] left-1/2 h-auto w-[130%] max-w-none -translate-x-1/2"
        />
      </div>
      <div className="relative z-10 -mt-8 space-y-4 rounded-xl bg-primary p-8 text-primary-foreground sm:p-9">
        <h1 className="text-3xl font-bold leading-9 tracking-tight">
          お問い合わせ
        </h1>
        <div className="space-y-1 text-base leading-7 sm:text-xl">
          <p>
            サービスに関するご質問、導入のご相談、申請手続きに関するお問い合わせは、以下のフォームよりお気軽にご連絡ください。
          </p>
          <p>通常、3営業日以内に担当者よりご返信いたします。</p>
        </div>
      </div>
    </Card>
  );
}

/** Renders the public inquiry form and its temporary success state. */
export default function InquiryPage() {
  const navigate = useNavigate();
  const { toast } = useToast();
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const form = useForm<InquiryRequest>({
    defaultValues: {
      email: '',
      companyName: '',
      name: '',
      content: '',
    },
  });

  useEffect(() => {
    if (successMessage === null) return;

    const timerId = window.setTimeout(() => {
      navigate(paths.home, { replace: true });
    }, SUCCESS_REDIRECT_DELAY_MS);

    return () => window.clearTimeout(timerId);
  }, [navigate, successMessage]);

  /** Submits raw form values and delegates every validation decision to the API. */
  const handleSubmit = async (inquiry: InquiryRequest) => {
    form.clearErrors();

    try {
      const response = await createInquiry(inquiry);
      setSuccessMessage(response.message?.trim() || DEFAULT_SUCCESS_MESSAGE);
    } catch (error) {
      const hasFieldErrors = applyInquiryFieldErrors(error, form.setError);
      if (hasFieldErrors) return;

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

  /** Returns immediately to the homepage from the success state. */
  const handleReturnHome = () => {
    navigate(paths.home, { replace: true });
  };

  return (
    <main className="min-h-[calc(100dvh-4rem-1px)] bg-background px-4 py-10 sm:px-6 sm:py-14 lg:px-8 lg:py-20 xl:py-24">
      <div className="mx-auto grid w-full max-w-page items-stretch justify-center gap-10 lg:grid-cols-[minmax(20rem,30rem)_minmax(0,45rem)] lg:gap-14 xl:gap-[7.5rem]">
        <InquiryInformationCard />

        <Card className="w-full max-w-[45rem] self-stretch rounded-xl border-0 shadow-app-md lg:min-h-[42.25rem]">
          <CardContent className="flex h-full flex-col justify-center p-7 sm:p-10 lg:p-12">
            {successMessage !== null ? (
              <div
                role="status"
                className="flex min-h-96 flex-col items-center justify-center space-y-6 text-center"
              >
                <span className="flex h-16 w-16 items-center justify-center rounded-full bg-success/10 text-success">
                  <CheckCircle2 className="h-8 w-8" aria-hidden="true" />
                </span>
                <div className="space-y-3">
                  <h2 className="text-2xl font-bold sm:text-3xl">
                    お問い合わせを受け付けました
                  </h2>
                  <p className="text-base leading-7 text-foreground">
                    {successMessage}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    3秒後にホームへ戻ります。
                  </p>
                </div>
                <FormActions
                  context="auth"
                  className="w-full sm:w-fit"
                  actions={[
                    {
                      id: 'home',
                      label: 'ホームへ戻る',
                      onClick: handleReturnHome,
                    },
                  ]}
                />
              </div>
            ) : (
              <Form {...form}>
                <form
                  noValidate
                  onSubmit={form.handleSubmit(handleSubmit)}
                  className="space-y-5"
                >
                  <FormField
                    control={form.control}
                    name="email"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>メールアドレス</RequiredLabel>
                        <FormControl>
                          <Input
                            type="email"
                            autoComplete="email"
                            placeholder="例）info@company.com"
                            aria-required="true"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="companyName"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>会社名</RequiredLabel>
                        <FormControl>
                          <Input
                            autoComplete="organization"
                            placeholder="例）株式会社ソーラーファーム"
                            aria-required="true"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="name"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>お名前</RequiredLabel>
                        <FormControl>
                          <Input
                            autoComplete="name"
                            placeholder="例）田中 太郎"
                            aria-required="true"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormField
                    control={form.control}
                    name="content"
                    render={({ field }) => (
                      <FormItem>
                        <RequiredLabel>お問い合わせ内容</RequiredLabel>
                        <FormControl>
                          <Textarea
                            placeholder="例）サービスの導入時期、申請手続き、料金について相談したいです。"
                            className="min-h-44 resize-y"
                            aria-required="true"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  <FormActions
                    context="auth"
                    className="w-full pt-3 sm:ml-auto sm:w-fit"
                    pendingActionId={
                      form.formState.isSubmitting ? 'submit' : null
                    }
                    actions={[
                      {
                        id: 'submit',
                        label: '送信する',
                        pendingLabel: '送信中...',
                        type: 'submit',
                      },
                    ]}
                  />
                </form>
              </Form>
            )}
          </CardContent>
        </Card>
      </div>
    </main>
  );
}
