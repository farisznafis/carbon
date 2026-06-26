import { validateSignupCompanyInfo } from '@/api/company';
import { ApiError } from '@/api/_shared/errors';
import {
  FormActions,
  FormCard,
  FormGrid,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { RequiredLabel } from '@/components/forms/shared/RequiredLabel';
import { Button } from '@/components/ui/button';
import { Form, FormControl, FormField, FormItem, FormMessage } from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useSignup } from '@/contexts/SignupContext';
import { useToast } from '@/hooks/use-toast';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { PREFECTURE_OPTIONS } from '@/lib/constants/prefectures';
import { logger } from '@/lib/logger';
import { Loader2 } from 'lucide-react';
import { useRef, useState } from 'react';
import { useForm, type UseFormReturn } from 'react-hook-form';

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
  town: string;
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
  town: string;
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
  town: 'town',
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
      town: signupData.town || '',
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
        form.setValue('prefecture', result.address1, { shouldDirty: true });
        form.setValue('municipality', result.address2, { shouldDirty: true });
        form.setValue('town', result.address3, { shouldDirty: true });
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
      town: data.town,
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
        <FormCard>
          <FormSectionStack>
            <FormSection number={1} title="法人情報">
              <FormField
                control={form.control}
                name="corporateNumber"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>法人番号</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="1234567890123" {...field} />
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
                    <RequiredLabel>法人名</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="株式会社サンプル" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="companyNameKana"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>法人名カナ</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="カブシキガイシャサンプル" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="companyNameEnglish"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel required={false}>会社名（英語）</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="Sample Corporation" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="corporateEmail"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>法人メールアドレス</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" type="email" placeholder="name@company.com" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="corporatePhoneNumber"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>法人電話番号</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" type="tel" placeholder="0312345678" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormSection>

            <FormSection number={2} title="所在地">
              <FormField
                control={form.control}
                name="postalCode"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>郵便番号</RequiredLabel>
                    <div className="relative">
                      <FormControl>
                        <Input
                          inputSize="sm"
                          maxLength={8}
                          placeholder="1000001"
                          {...field}
                          onChange={(e) => handlePostalCodeChange(e.target.value, field.onChange)}
                        />
                      </FormControl>
                      {isLoadingAddress && (
                        <Loader2 className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-muted-foreground" />
                      )}
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormGrid>
                <FormField
                  control={form.control}
                  name="prefecture"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>都道府県</RequiredLabel>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <FormControl>
                          <SelectTrigger selectSize="sm">
                            <SelectValue placeholder="都道府県を選択" />
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

                <FormField
                  control={form.control}
                  name="municipality"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>市区町村</RequiredLabel>
                      <FormControl>
                        <Input inputSize="sm" placeholder="千代田区" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </FormGrid>

              <FormField
                control={form.control}
                name="blocknum"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>番地</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="1-1" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="buildingName"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel required={false}>建物名</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="サンプルビル 3F" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormSection>

            <FormSection number={3} title="担当者情報">
              <FormGrid>
                <FormField
                  control={form.control}
                  name="lastName"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>姓</RequiredLabel>
                      <FormControl>
                        <Input inputSize="sm" placeholder="山田" {...field} />
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
                        <Input inputSize="sm" placeholder="太郎" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </FormGrid>

              <FormGrid>
                <FormField
                  control={form.control}
                  name="lastNameKana"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>姓（カナ）</RequiredLabel>
                      <FormControl>
                        <Input inputSize="sm" placeholder="ヤマダ" {...field} />
                      </FormControl>
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
                        <Input inputSize="sm" placeholder="タロウ" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </FormGrid>

              <FormField
                control={form.control}
                name="position"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>役職</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="部長" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="phoneNumber"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>電話番号</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" type="tel" placeholder="0312345678" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="faxNumber"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel required={false}>FAX番号</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" type="tel" placeholder="0312345678" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>メールアドレス</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" type="email" placeholder="yamada.taro@tokyo-energy.co.jp" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormSection>

            <FormActions>
              <Button
                type="submit"
                size="lg"
                className="w-full"
                disabled={form.formState.isSubmitting || isValidatingCompanyInfo}
              >
                {isValidatingCompanyInfo && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                方法論へ続く
              </Button>
            </FormActions>
          </FormSectionStack>
        </FormCard>
      </form>
    </Form>
  );
};
