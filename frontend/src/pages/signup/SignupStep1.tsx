import type { ComponentType, ReactNode } from 'react';
import { useEffect, useRef, useState } from 'react';
import { useSignup } from '@/contexts/SignupContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { ArrowRight, Building, Loader2, MapPin, User } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from '@/components/ui/form';
import {
  corporateNumberSchema,
  emailSchema,
  katakanaSchema,
  phoneNumberSchema,
  postalCodeSchema,
} from '@/lib/validations';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { logger } from '@/lib/logger';
import { RequiredLabel } from '@/components/forms/shared/RequiredLabel';
import { PREFECTURE_OPTIONS } from '@/lib/constants/prefectures';

const inputClassName =
  'h-11 border-slate-200 bg-white shadow-sm focus-visible:ring-emerald-700';

const twoColumnGridClassName = 'grid grid-cols-1 gap-4 md:grid-cols-2';

type ZipCloudResponse = {
  status: number;
  message: string | null;
  results: Array<{
    address1: string;
    address2: string;
    address3: string;
  }> | null;
};

type SectionProps = {
  children: ReactNode;
  description: string;
  icon: ComponentType<{ className?: string }>;
  title: string;
};

function FormSection({
  children,
  description,
  icon: Icon,
  title,
}: Readonly<SectionProps>) {
  return (
    <section className="rounded-lg border border-slate-200/80 bg-white p-4 shadow-sm sm:p-6">
      <div className="mb-5 flex items-start gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-emerald-50 text-emerald-700">
          <Icon className="h-5 w-5" />
        </div>
        <div>
          <h3 className="text-lg font-semibold tracking-normal text-slate-950">
            {title}
          </h3>
          <p className="mt-1 text-sm leading-6 text-slate-600">{description}</p>
        </div>
      </div>
      <div className="space-y-4">{children}</div>
    </section>
  );
}

const signupStep1Schema = z.object({
  corporateNumber: corporateNumberSchema,
  companyName: z
    .string()
    .min(1, '企業名を入力してください')
    .max(100, '企業名は100文字以内で入力してください'),
  companyNameKana: katakanaSchema
    .min(1, '企業名（カナ）を入力してください')
    .max(100, '企業名（カナ）は100文字以内で入力してください'),
  companyNameEnglish: z
    .string()
    .max(100, '企業名（英語）は100文字以内で入力してください'),
  corporateEmail: emailSchema,
  corporatePhoneNumber: phoneNumberSchema,

  postalCode: postalCodeSchema,
  prefecture: z.string().min(1, '都道府県を選択してください'),
  city: z
    .string()
    .min(1, '市区町村を入力してください')
    .max(50, '市区町村は50文字以内で入力してください'),
  address: z
    .string()
    .min(1, '住所を入力してください')
    .max(100, '住所は100文字以内で入力してください'),
  town: z
    .string()
    .min(1, '町名を入力してください')
    .max(100, '町名は100文字以内で入力してください'),
  blocknum: z
    .string()
    .min(1, '番地を入力してください')
    .max(100, '番地は100文字以内で入力してください'),

  lastName: z
    .string()
    .min(1, '名前（姓）を入力してください')
    .max(50, '名前（姓）は50文字以内で入力してください'),
  firstName: z
    .string()
    .min(1, '名前（名）を入力してください')
    .max(50, '名前（名）は50文字以内で入力してください'),
  lastNameKana: katakanaSchema
    .min(1, '名前（姓）（カナ）を入力してください')
    .max(50, '名前（姓）（カナ）は50文字以内で入力してください'),
  firstNameKana: katakanaSchema
    .min(1, '名前（名）（カナ）を入力してください')
    .max(50, '名前（名）（カナ）は50文字以内で入力してください'),
  position: z
    .string()
    .min(1, '役職を入力してください')
    .max(50, '役職は50文字以内で入力してください'),
  phoneNumber: phoneNumberSchema,
  faxNumber: phoneNumberSchema.or(z.literal('')),
  email: emailSchema,
});

type SignupStep1FormData = z.infer<typeof signupStep1Schema>;

export const SignupStep1 = () => {
  useRedirectIfLoggedIn();

  const { signupData, updateSignupData, setCurrentStep } = useSignup();
  const { toast } = useToast();
  const [isLoadingAddress, setIsLoadingAddress] = useState(false);
  const postalCodeLookupAbortRef = useRef<AbortController | null>(null);

  const form = useForm<SignupStep1FormData>({
    resolver: zodResolver(signupStep1Schema),
    mode: 'onBlur',
    defaultValues: {
      corporateNumber: signupData.corporateNumber || '',
      companyName: signupData.companyName || '',
      companyNameKana: signupData.companyNameKana || '',
      companyNameEnglish: signupData.companyNameEnglish || '',
      corporateEmail: signupData.corporateEmail || '',
      corporatePhoneNumber: signupData.corporatePhoneNumber || '',

      postalCode: signupData.postalCode || '',
      prefecture: signupData.prefecture || '',
      city: signupData.city || '',
      address: signupData.address || '',
      town: signupData.town || '',
      blocknum: signupData.blocknum || '',

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

  useEffect(() => {
    return () => {
      postalCodeLookupAbortRef.current?.abort();
    };
  }, []);

  const handlePostalCodeChange = async (
    postalCode: string,
    onChange: (value: string) => void,
  ) => {
    onChange(postalCode);

    const cleanedCode = postalCode.replace(/[^0-9]/g, '');
    postalCodeLookupAbortRef.current?.abort();

    if (cleanedCode.length !== 7) {
      setIsLoadingAddress(false);
      return;
    }

    const controller = new AbortController();
    postalCodeLookupAbortRef.current = controller;
    setIsLoadingAddress(true);

    try {
      const response = await fetch(
        `https://zipcloud.ibsnet.co.jp/api/search?zipcode=${cleanedCode}`,
        { signal: controller.signal },
      );
      const data = (await response.json()) as ZipCloudResponse;

      if (!response.ok || data.status !== 200 || !data.results?.length) {
        toast({
          title: '住所検索エラー',
          description: data.message || '郵便番号から住所を取得できませんでした',
          variant: 'destructive',
        });
        return;
      }

      const result = data.results[0];
      form.setValue('prefecture', result.address1 || '', {
        shouldDirty: true,
        shouldValidate: true,
      });
      form.setValue('city', result.address2 || '', {
        shouldDirty: true,
        shouldValidate: true,
      });
      form.setValue('town', result.address3 || '', {
        shouldDirty: true,
        shouldValidate: true,
      });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return;
      }

      logger.error('Failed to fetch address:', error);
      toast({
        title: '住所検索エラー',
        description: '郵便番号から住所を取得できませんでした',
        variant: 'destructive',
      });
    } finally {
      if (postalCodeLookupAbortRef.current === controller) {
        postalCodeLookupAbortRef.current = null;
        setIsLoadingAddress(false);
      }
    }
  };

  const onSubmit = (data: SignupStep1FormData) => {
    updateSignupData({
      ...data,
      companyNameEnglish: data.companyNameEnglish || '',
      faxNumber: data.faxNumber || '',
    });
    setCurrentStep(2);
  };

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(onSubmit)}
        className="space-y-5"
        noValidate
      >
        <FormSection
          icon={Building}
          title="法人情報"
          description="法人番号と会社の基本連絡先を入力してください。"
        >
          <div className={twoColumnGridClassName}>
            <FormField
              control={form.control}
              name="corporateNumber"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>法人番号</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="1234567890123"
                      className={inputClassName}
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
                  <RequiredLabel>企業名</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="株式会社サンプル"
                      className={inputClassName}
                      {...field}
                    />
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
                  <RequiredLabel>企業名（カナ）</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="カブシキガイシャサンプル"
                      className={inputClassName}
                      {...field}
                    />
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
                  <RequiredLabel required={false}>企業名（英語）</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="Sample Corporation"
                      className={inputClassName}
                      {...field}
                    />
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
                    <Input
                      type="email"
                      placeholder="corporate@company.com"
                      className={inputClassName}
                      {...field}
                    />
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
                    <Input
                      type="tel"
                      placeholder="03-1234-5678"
                      className={inputClassName}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </FormSection>

        <FormSection
          icon={MapPin}
          title="所在地"
          description="郵便番号を入力すると、都道府県・市区町村・町名を自動入力できます。"
        >
          <div className={twoColumnGridClassName}>
            <FormField
              control={form.control}
              name="postalCode"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>郵便番号</RequiredLabel>
                  <div className="relative">
                    <FormControl>
                      <Input
                        placeholder="1234567"
                        maxLength={8}
                        className={`${inputClassName} pr-10`}
                        {...field}
                        onChange={(event) =>
                          void handlePostalCodeChange(
                            event.target.value,
                            field.onChange,
                          )
                        }
                      />
                    </FormControl>
                    {isLoadingAddress && (
                      <Loader2 className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-emerald-700" />
                    )}
                  </div>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="prefecture"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>都道府県</RequiredLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger className={inputClassName}>
                        <SelectValue placeholder="選択してください" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent className="z-50 bg-background">
                      {PREFECTURE_OPTIONS.map((pref) => (
                        <SelectItem key={pref} value={pref}>
                          {pref}
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
              name="city"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>市区町村</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="渋谷区"
                      className={inputClassName}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="town"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>町名</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="桜丘町"
                      className={inputClassName}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="blocknum"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>番地</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="1-1"
                      className={inputClassName}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="address"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>住所</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="桜丘町1-1 サンプルビル"
                      className={inputClassName}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </FormSection>

        <FormSection
          icon={User}
          title="担当者情報"
          description="登録内容の確認や連絡に使用する担当者情報を入力してください。"
        >
          <div className={twoColumnGridClassName}>
            <FormField
              control={form.control}
              name="lastName"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>名前（姓）</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="山田"
                      className={inputClassName}
                      {...field}
                    />
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
                  <RequiredLabel>名前（名）</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="太郎"
                      className={inputClassName}
                      {...field}
                    />
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
                  <RequiredLabel>名前（姓）（カナ）</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="ヤマダ"
                      className={inputClassName}
                      {...field}
                    />
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
                  <RequiredLabel>名前（名）（カナ）</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="タロウ"
                      className={inputClassName}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="position"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>役職</RequiredLabel>
                  <FormControl>
                    <Input
                      placeholder="部長"
                      className={inputClassName}
                      {...field}
                    />
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
                    <Input
                      type="email"
                      placeholder="yamada@company.com"
                      className={inputClassName}
                      {...field}
                    />
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
                    <Input
                      type="tel"
                      placeholder="03-1234-5678"
                      className={inputClassName}
                      {...field}
                    />
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
                    <Input
                      type="tel"
                      placeholder="03-1234-5679"
                      className={inputClassName}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </FormSection>

        <div className="rounded-lg border border-emerald-900/10 bg-white/90 p-4 shadow-sm sm:flex sm:items-center sm:justify-between sm:gap-4">
          <p className="text-sm leading-6 text-slate-600">
            次のステップで参加方法論を選択します。入力内容は確認画面で見直せます。
          </p>
          <Button
            type="submit"
            className="mt-4 h-11 w-full bg-emerald-700 px-6 text-white hover:bg-emerald-800 sm:mt-0 sm:w-auto"
            disabled={form.formState.isSubmitting}
          >
            方法論へ進む
            <ArrowRight className="h-4 w-4" />
          </Button>
        </div>
      </form>
    </Form>
  );
};
