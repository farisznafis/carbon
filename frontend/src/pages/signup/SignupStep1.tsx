import { validateSignupCompanyInfo } from '@/api/company';
import { ApiError } from '@/api/_shared/errors';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useSignup } from '@/contexts/SignupContext';
import { useToast } from '@/hooks/use-toast';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { PREFECTURE_OPTIONS } from '@/lib/constants/prefectures';
import { logger } from '@/lib/logger';
import { Loader2 } from 'lucide-react';
import { useRef, useState, type ReactNode } from 'react';
import { useForm, type Control, type UseFormReturn } from 'react-hook-form';
import chevronRight from '@/assets/figma/signup/chevron-right.svg';

type SignupStep1FormData = {
  corporateNumber: string;
  companyName: string;
  companyNameKana: string;
  companyNameEnglish: string;
  corporateEmail: string;
  corporatePhoneNumber: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  blocknum: string;
  buildingName: string;
  lastName: string;
  firstName: string;
  lastNameKana: string;
  firstNameKana: string;
  position: string;
  phoneNumber: string;
  faxNumber: string;
  email: string;
};

type SignupCompanyValidationPayload = {
  corporateNumber: string;
  corporateName: string;
  corporateNameKana: string;
  companyNameEnglish: string;
  corporateEmail: string;
  corporatePhoneNumber: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  buildingName: string;
  blocknum: string;
  lastName: string;
  firstName: string;
  lastNameKana: string;
  firstNameKana: string;
  position: string;
  phoneNumber: string;
  faxNumber: string;
  email: string;
};

const backendFieldToFormField: Partial<Record<string, keyof SignupStep1FormData>> = {
  corporateNumber: 'corporateNumber',
  corporateName: 'companyName',
  corporateNameKana: 'companyNameKana',
  companyNameEnglish: 'companyNameEnglish',
  corporateEmail: 'corporateEmail',
  corporatePhoneNumber: 'corporatePhoneNumber',
  postalCode: 'postalCode',
  prefecture: 'prefecture',
  municipality: 'municipality',
  blocknum: 'blocknum',
  buildingName: 'buildingName',
  lastName: 'lastName',
  firstName: 'firstName',
  lastNameKana: 'lastNameKana',
  firstNameKana: 'firstNameKana',
  position: 'position',
  phoneNumber: 'phoneNumber',
  faxNumber: 'faxNumber',
  email: 'email',
};

function toMessage(value: unknown): string {
  if (typeof value === 'string') {
    return value;
  }

  if (Array.isArray(value)) {
    return value.map(toMessage).filter(Boolean).join('\n');
  }

  if (value && typeof value === 'object' && 'message' in value) {
    return toMessage((value as { message?: unknown }).message);
  }

  return '';
}

function applyBackendValidationErrors(
  form: UseFormReturn<SignupStep1FormData>,
  error: unknown,
): string | null {
  if (!(error instanceof ApiError)) {
    return error instanceof Error ? error.message : null;
  }

  const errors = error.payload?.errors;
  let applied = false;

  if (errors && typeof errors === 'object' && !Array.isArray(errors)) {
    Object.entries(errors).forEach(([backendField, value]) => {
      const formField = backendFieldToFormField[backendField];
      const message = toMessage(value);

      if (!formField || !message) {
        return;
      }

      form.setError(formField, {
        type: 'server',
        message,
      });
      applied = true;
    });
  }

  if (applied) {
    return null;
  }

  return error.payload?.message || error.message;
}

type SignupFieldProps = {
  control: Control<SignupStep1FormData>;
  name: keyof SignupStep1FormData;
  label: string;
  placeholder: string;
  type?: string;
  maxLength?: number;
  onValueChange?: (value: string, onChange: (value: string) => void) => void;
  trailing?: ReactNode;
};

const inputClassName =
  'h-10 rounded-md border-[#cbd5e1] bg-white px-3 py-2 text-base leading-6 text-[#0f172a] shadow-none placeholder:text-[#94a3b8] focus-visible:ring-[#699267] focus-visible:ring-offset-0';

function SignupField({
  control,
  name,
  label,
  placeholder,
  type = 'text',
  maxLength,
  onValueChange,
  trailing,
}: SignupFieldProps) {
  return (
    <FormField
      control={control}
      name={name}
      render={({ field }) => (
        <FormItem className="space-y-1.5">
          <FormLabel className="text-sm font-medium leading-5 text-[#0f172a]">
            {label}
          </FormLabel>
          <div className="relative">
            <FormControl>
              <Input
                inputSize="sm"
                type={type}
                maxLength={maxLength}
                placeholder={placeholder}
                className={inputClassName}
                {...field}
                onChange={(event) => {
                  const nextValue = event.target.value;
                  if (onValueChange) {
                    onValueChange(nextValue, field.onChange);
                    return;
                  }

                  field.onChange(nextValue);
                }}
              />
            </FormControl>
            {trailing}
          </div>
          <FormMessage />
        </FormItem>
      )}
    />
  );
}

function SignupSection({
  number,
  title,
  children,
}: Readonly<{
  number: number;
  title: string;
  children: ReactNode;
}>) {
  return (
    <section className="space-y-4 sm:space-y-5">
      <div className="flex items-center gap-3">
        <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-[#eef2ce] text-xl font-bold leading-7 text-[#699267] sm:h-10 sm:w-10">
          {number}
        </span>
        <h2 className="text-2xl font-bold leading-8 text-[#1f2628]">
          {title}
        </h2>
      </div>
      <div className="space-y-5">{children}</div>
    </section>
  );
}

export const SignupStep1 = () => {
  useRedirectIfLoggedIn();

  const { signupData, updateSignupData, setCurrentStep } = useSignup();
  const { toast } = useToast();
  const [isLoadingAddress, setIsLoadingAddress] = useState(false);
  const [isValidatingCompanyInfo, setIsValidatingCompanyInfo] = useState(false);
  const postalCodeLookupAbortRef = useRef<AbortController | null>(null);

  const form = useForm<SignupStep1FormData>({
    defaultValues: {
      corporateNumber: signupData.corporateNumber || '',
      companyName: signupData.companyName || '',
      companyNameKana: signupData.companyNameKana || '',
      companyNameEnglish: signupData.companyNameEnglish || '',
      corporateEmail: signupData.corporateEmail || '',
      corporatePhoneNumber: signupData.corporatePhoneNumber || '',
      postalCode: signupData.postalCode || '',
      prefecture: signupData.prefecture || '',
      municipality: signupData.municipality || '',
      blocknum: signupData.blocknum || '',
      buildingName: signupData.buildingName || '',
      lastName: signupData.lastName || '',
      firstName: signupData.firstName || '',
      lastNameKana: signupData.lastNameKana || '',
      firstNameKana: signupData.firstNameKana || '',
      position: signupData.position || '',
      phoneNumber: signupData.phoneNumber || '',
      faxNumber: signupData.faxNumber || '',
      email: signupData.email || '',
    },
  });

  const handlePostalCodeChange = async (
    postalCode: string,
    onChange: (value: string) => void,
  ) => {
    onChange(postalCode);

    const cleanedCode = postalCode.replace(/[^0-9]/g, '');

    if (cleanedCode.length !== 7) {
      return;
    }

    postalCodeLookupAbortRef.current?.abort();
    const controller = new AbortController();
    postalCodeLookupAbortRef.current = controller;

    setIsLoadingAddress(true);

    try {
      const response = await fetch(`https://zipcloud.ibsnet.co.jp/api/search?zipcode=${cleanedCode}`, {
        signal: controller.signal,
      });
      const data = await response.json();

      if (data.results && data.results.length > 0) {
        const result = data.results[0];
        const municipalityWithTown = `${result.address2 ?? ''}${result.address3 ?? ''}`;

        form.setValue('prefecture', result.address1, { shouldDirty: true });
        form.setValue('municipality', municipalityWithTown, { shouldDirty: true });
      }
    } catch (error) {
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        logger.error('Failed to fetch address:', error);
      }
    } finally {
      if (postalCodeLookupAbortRef.current === controller) {
        postalCodeLookupAbortRef.current = null;
        setIsLoadingAddress(false);
      }
    }
  };

  const onSubmit = async (data: SignupStep1FormData) => {
    const payload: SignupCompanyValidationPayload = {
      corporateNumber: data.corporateNumber,
      corporateName: data.companyName,
      corporateNameKana: data.companyNameKana,
      companyNameEnglish: data.companyNameEnglish || '',
      corporateEmail: data.corporateEmail,
      corporatePhoneNumber: data.corporatePhoneNumber,
      postalCode: data.postalCode,
      prefecture: data.prefecture,
      municipality: data.municipality,
      buildingName: data.buildingName || '',
      blocknum: data.blocknum,
      lastName: data.lastName,
      firstName: data.firstName,
      lastNameKana: data.lastNameKana,
      firstNameKana: data.firstNameKana,
      position: data.position,
      phoneNumber: data.phoneNumber,
      faxNumber: data.faxNumber || '',
      email: data.email,
    };

    form.clearErrors();
    setIsValidatingCompanyInfo(true);

    try {
      await validateSignupCompanyInfo(payload);

      updateSignupData({
        ...data,
        companyNameEnglish: data.companyNameEnglish || '',
        faxNumber: data.faxNumber || '',
      });
      setCurrentStep(2);
    } catch (error) {
      const message = applyBackendValidationErrors(form, error);

      toast({
        title: '入力内容を確認してください',
        description: message || '入力内容に誤りがあります。',
        variant: 'destructive',
      });
    } finally {
      setIsValidatingCompanyInfo(false);
    }
  };

  return (
    <Form {...form}>
      <form noValidate onSubmit={form.handleSubmit(onSubmit)}>
        <div className="w-full rounded-2xl border border-[#e2e8f0] bg-white p-6 shadow-[0_4px_6px_rgba(0,0,0,0.05)] sm:rounded-xl sm:border-border sm:p-10 sm:shadow-app-sm">
          <div className="space-y-8 sm:space-y-10">
            <SignupSection number={1} title="法人情報">
              <SignupField
                control={form.control}
                name="corporateNumber"
                label="法人番号"
                placeholder="1234567890111"
              />
              <SignupField
                control={form.control}
                name="companyName"
                label="企業名"
                placeholder="株式会社サンプル"
              />
              <SignupField
                control={form.control}
                name="companyNameKana"
                label="企業名（カナ）"
                placeholder="カブシキガイシャサンプル"
              />
              <SignupField
                control={form.control}
                name="companyNameEnglish"
                label="企業名（英語）"
                placeholder="Sample Corporation"
              />
              <SignupField
                control={form.control}
                name="corporateEmail"
                label="法人メールアドレス"
                type="email"
                placeholder="name@company.com"
              />
              <SignupField
                control={form.control}
                name="corporatePhoneNumber"
                label="法人電話番号"
                type="tel"
                placeholder="03-1234-5678"
              />
            </SignupSection>

            <div className="h-px w-full bg-[#e2e8f0]" />

            <SignupSection number={2} title="所在地">
              <SignupField
                control={form.control}
                name="postalCode"
                label="郵便番号"
                maxLength={8}
                placeholder="123-4567"
                onValueChange={handlePostalCodeChange}
                trailing={
                  isLoadingAddress ? (
                    <Loader2 className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-[#64748b]" />
                  ) : null
                }
              />
              <FormField
                control={form.control}
                name="prefecture"
                render={({ field }) => (
                  <FormItem className="space-y-1.5">
                    <FormLabel className="text-sm font-medium leading-5 text-[#0f172a]">
                      都道府県
                    </FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger
                          selectSize="sm"
                          className="h-10 rounded-md border-[#cbd5e1] bg-white px-3 py-2 text-base leading-6 text-[#0f172a] shadow-none focus:ring-[#699267] focus:ring-offset-0"
                        >
                          <SelectValue placeholder="東京都" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent className="z-50 bg-background">
                        {PREFECTURE_OPTIONS.map((prefecture) => (
                          <SelectItem key={prefecture} value={prefecture}>
                            {prefecture}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <SignupField
                control={form.control}
                name="municipality"
                label="市区町村"
                placeholder="渋谷区"
              />
              <SignupField
                control={form.control}
                name="blocknum"
                label="住所"
                placeholder="桜丘町1-1"
              />
              <div className="hidden sm:block">
                <SignupField
                  control={form.control}
                  name="buildingName"
                  label="建物名"
                  placeholder="サンプルビル 3F"
                />
              </div>
            </SignupSection>

            <div className="h-px w-full bg-[#e2e8f0]" />

            <SignupSection number={3} title="個人情報">
              <SignupField
                control={form.control}
                name="lastName"
                label="名前（性）"
                placeholder="山田"
              />
              <SignupField
                control={form.control}
                name="firstName"
                label="名前（名）"
                placeholder="太郎"
              />
              <div className="hidden sm:block">
                <SignupField
                  control={form.control}
                  name="lastNameKana"
                  label="姓（カナ）"
                  placeholder="ヤマダ"
                />
              </div>
              <div className="hidden sm:block">
                <SignupField
                  control={form.control}
                  name="firstNameKana"
                  label="名（カナ）"
                  placeholder="タロウ"
                />
              </div>
              <SignupField
                control={form.control}
                name="position"
                label="役職"
                placeholder="代表取締役"
              />
              <div className="hidden sm:block">
                <SignupField
                  control={form.control}
                  name="phoneNumber"
                  label="電話番号"
                  type="tel"
                  placeholder="03-1234-5678"
                />
              </div>
              <div className="hidden sm:block">
                <SignupField
                  control={form.control}
                  name="faxNumber"
                  label="FAX番号"
                  type="tel"
                  placeholder="03-1234-5679"
                />
              </div>
              <SignupField
                control={form.control}
                name="email"
                label="メールアドレス"
                type="email"
                placeholder="yamada.taro@example.co.jp"
              />
            </SignupSection>

            <Button
              type="submit"
              size="sm"
              className="h-14 w-full rounded-lg bg-[#699267] py-4 text-base font-bold text-white shadow-none hover:bg-[#5f855d] sm:h-10 sm:rounded-full sm:py-0 sm:text-xs"
              disabled={form.formState.isSubmitting || isValidatingCompanyInfo}
            >
              {isValidatingCompanyInfo ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : null}
              方法論へ続く
              <img src={chevronRight} alt="" className="h-3 w-2" aria-hidden="true" />
            </Button>
          </div>
        </div>
      </form>
    </Form>
  );
};
