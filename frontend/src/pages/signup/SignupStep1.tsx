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
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { PREFECTURE_OPTIONS } from '@/lib/constants/prefectures';
import { corporateNumberSchema, emailSchema, katakanaSchema, phoneNumberSchema, postalCodeSchema } from '@/lib/validations';
import { logger } from '@/lib/logger';
import { Loader2 } from 'lucide-react';
import { useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

const signupStep1Schema = z.object({
  corporateNumber: corporateNumberSchema,
  companyName: z.string().min(1, '企業名を入力してください').max(100, '企業名は100文字以内で入力してください'),
  companyNameKana: katakanaSchema.min(1, '企業名（カナ）を入力してください').max(100, '企業名（カナ）は100文字以内で入力してください'),
  companyNameEnglish: z.string().max(100, '企業名（英語）は100文字以内で入力してください'),
  corporateEmail: emailSchema,
  corporatePhoneNumber: phoneNumberSchema,
  postalCode: postalCodeSchema,
  prefecture: z.string().min(1, '都道府県を選択してください'),
  municipality: z.string().min(1, '市区町村を入力してください').max(50, '市区町村は50文字以内で入力してください'),
  town: z.string().max(100, '町名は100文字以内で入力してください'),
  blocknum: z.string().min(1, '番地を入力してください').max(100, '番地は100文字以内で入力してください'),
  buildingName: z.string().max(100, '建物名は100文字以内で入力してください'),
  lastName: z.string().min(1, '名前（姓）を入力してください').max(50, '名前（姓）は50文字以内で入力してください'),
  firstName: z.string().min(1, '名前（名）を入力してください').max(50, '名前（名）は50文字以内で入力してください'),
  lastNameKana: katakanaSchema.min(1, '名前（姓）（カナ）を入力してください').max(50, '名前（姓）（カナ）は50文字以内で入力してください'),
  firstNameKana: katakanaSchema.min(1, '名前（名）（カナ）を入力してください').max(50, '名前（名）（カナ）は50文字以内で入力してください'),
  position: z.string().min(1, '役職を入力してください').max(50, '役職は50文字以内で入力してください'),
  phoneNumber: phoneNumberSchema,
  faxNumber: z.string().max(20, 'FAX番号は20文字以内で入力してください'),
  email: emailSchema,
});

type SignupStep1FormData = z.infer<typeof signupStep1Schema>;

export const SignupStep1 = () => {
  useRedirectIfLoggedIn();

  const { signupData, updateSignupData, setCurrentStep } = useSignup();
  const [isLoadingAddress, setIsLoadingAddress] = useState(false);
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

    setIsLoadingAddress(true);

    try {
      const response = await fetch(`https://zipcloud.ibsnet.co.jp/api/search?zipcode=${cleanedCode}`);
      const data = await response.json();

      if (data.results && data.results.length > 0) {
        const result = data.results[0];
        form.setValue('prefecture', result.address1);
        form.setValue('municipality', result.address2);
        form.setValue('town', result.address3);
      }
    } catch (error) {
      logger.error('Failed to fetch address:', error);
    } finally {
      setIsLoadingAddress(false);
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
      <form onSubmit={form.handleSubmit(onSubmit)}>
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
                      <Input inputSize="sm" placeholder="1234567890111" {...field} />
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
                    <RequiredLabel>企業名（カナ）</RequiredLabel>
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
                    <RequiredLabel required={false}>企業名（英語）</RequiredLabel>
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
                      <Input inputSize="sm" type="tel" placeholder="1234567890111" {...field} />
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
                          placeholder="1234567890111"
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
                            <SelectValue placeholder="岩手県" />
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
                        <Input inputSize="sm" placeholder="桜丘町" {...field} />
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
                      <Input inputSize="sm" placeholder="桜丘町1-1" {...field} />
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
                    <RequiredLabel required={true}>建物名</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="サンプルビル 3F" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormSection>

            <FormSection number={3} title="個人情報">
              <FormGrid>
                <FormField
                  control={form.control}
                  name="lastName"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>名前（姓）</RequiredLabel>
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
                      <RequiredLabel>名前（名）</RequiredLabel>
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
                      <RequiredLabel>名前（姓）（カナ）</RequiredLabel>
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
                      <RequiredLabel>名前（名）（カナ）</RequiredLabel>
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
                      <Input inputSize="sm" placeholder="会長" {...field} />
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
                      <Input inputSize="sm" type="tel" placeholder="03-1234-5678" {...field} />
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
                      <Input inputSize="sm" type="tel" placeholder="03-1234-5678" {...field} />
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
                      <Input inputSize="sm" type="email" placeholder="1109yamada.taro@tokyo-energy.co.jp" {...field} />
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
                disabled={form.formState.isSubmitting}
              >
                方法論へ続く
              </Button>
            </FormActions>
          </FormSectionStack>
        </FormCard>
      </form>
    </Form>
  );
};
