import { useSignup } from '@/contexts/SignupContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { postalCodeSchema, phoneNumberSchema, katakanaSchema, corporateNumberSchema, emailSchema } from '@/lib/validations';
import { useState } from 'react';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { logger } from '@/lib/logger';
import { RequiredLabel } from '@/components/forms/shared/RequiredLabel';

const prefectures = [
  '北海道', '青森県', '岩手県', '宮城県', '秋田県', '山形県', '福島県',
  '茨城県', '栃木県', '群馬県', '埼玉県', '千葉県', '東京都', '神奈川県',
  '新潟県', '富山県', '石川県', '福井県', '山梨県', '長野県', '岐阜県',
  '静岡県', '愛知県', '三重県', '滋賀県', '京都府', '大阪府', '兵庫県',
  '奈良県', '和歌山県', '鳥取県', '島根県', '岡山県', '広島県', '山口県',
  '徳島県', '香川県', '愛媛県', '高知県', '福岡県', '佐賀県', '長崎県',
  '熊本県', '大分県', '宮崎県', '鹿児島県', '沖縄県'
];

const signupStep1Schema = z.object({
  // Company Information
  corporateNumber: corporateNumberSchema,
  companyName: z.string().min(1, '企業名を入力してください').max(100, '企業名は100文字以内で入力してください'),
  companyNameKana: katakanaSchema.min(1, '企業名（カナ）を入力してください').max(100, '企業名（カナ）は100文字以内で入力してください'),
  companyNameEnglish: z.string().min(1, '企業名（英語）を入力してください').max(100, '企業名（英語）は100文字以内で入力してください'),
  corporateEmail: emailSchema,
  corporatePhoneNumber: phoneNumberSchema,
  
  // Address
  postalCode: postalCodeSchema,
  prefecture: z.string().min(1, '都道府県を選択してください'),
  city: z.string().min(1, '市区町村を入力してください').max(50, '市区町村は50文字以内で入力してください'),
  address: z.string().min(1, '番地を入力してください').max(100, '番地は100文字以内で入力してください'),
  town: z.string().min(1, '町名を入力してください').max(100, '町名は100文字以内で入力してください'),
  blocknum: z.string().min(1, '番地を入力してください').max(100, '番地は100文字以内で入力してください'),
  
  // Personal Information
  lastName: z.string().min(1, '名前（姓）を入力してください').max(50, '名前（姓）は50文字以内で入力してください'),
  firstName: z.string().min(1, '名前（名）を入力してください').max(50, '名前（名）は50文字以内で入力してください'),
  lastNameKana: katakanaSchema.min(1, '名前（姓）（カナ）を入力してください').max(50, '名前（姓）（カナ）は50文字以内で入力してください'),
  firstNameKana: katakanaSchema.min(1, '名前（名）（カナ）を入力してください').max(50, '名前（名）（カナ）は50文字以内で入力してください'),
  position: z.string().min(1, '役職を入力してください').max(50, '役職は50文字以内で入力してください'),
  phoneNumber: phoneNumberSchema,
  faxNumber: phoneNumberSchema,
  email: emailSchema,
});

type SignupStep1FormData = z.infer<typeof signupStep1Schema>;

export const SignupStep1 = () => {
  useRedirectIfLoggedIn(); 

  const { signupData, updateSignupData, setCurrentStep } = useSignup();
  const { toast } = useToast();
  const [isLoadingAddress, setIsLoadingAddress] = useState(false);

  const form = useForm<SignupStep1FormData>({
    // VALIDATE FORM (uncomment to enable validation)
    // resolver: zodResolver(signupStep1Schema),
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

  const handlePostalCodeChange = async (postalCode: string, onChange: (value: string) => void) => {
    onChange(postalCode);
    
    const cleanedCode = postalCode.replace(/[^0-9]/g, '');
    
    if (cleanedCode.length === 7) {
      setIsLoadingAddress(true);
      try {
        const response = await fetch(`https://zipcloud.ibsnet.co.jp/api/search?zipcode=${cleanedCode}`);
        const data = await response.json();
        
        if (data.results && data.results.length > 0) {
          const result = data.results[0];
          form.setValue('prefecture', result.address1);
          form.setValue('city', result.address2);
          form.setValue('town', result.address3);
        }
      } catch (error) {
        logger.error('Failed to fetch address:', error);
      } finally {
        setIsLoadingAddress(false);
      }
    }
  };

  const onSubmit = async (data: SignupStep1FormData) => {
    updateSignupData(data);
    setCurrentStep(2);
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        <div className="space-y-4">
          <h3 className="text-lg font-semibold">法人情報</h3>
          
          <FormField
            control={form.control}
            name="corporateNumber"
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>法人番号</RequiredLabel>
                <FormControl>
                  <Input placeholder="1234567890123" {...field} />
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
                  <Input placeholder="株式会社サンプル" {...field} />
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
                  <Input placeholder="カブシキガイシャサンプル" {...field} />
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
                <RequiredLabel>企業名（英語）</RequiredLabel>
                <FormControl>
                  <Input placeholder="Sample Corporation" {...field} />
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
                  <Input type="email" placeholder="corporate@company.com" {...field} />
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
                  <Input type="tel" placeholder="03-1234-5678" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="space-y-4">
            <h3 className="text-lg font-semibold">住所情報</h3>
            <FormField
              control={form.control}
              name="postalCode"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>郵便番号</RequiredLabel>
                  <div className="flex gap-2 items-center">
                    <FormControl>
                      <Input
                        placeholder="1234567"
                        maxLength={8}
                        {...field}
                        onChange={(e) => handlePostalCodeChange(e.target.value, field.onChange)}
                      />
                    </FormControl>
                    {isLoadingAddress && <Loader2 className="h-4 w-4 animate-spin" />}
                  </div>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="prefecture"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>都道府県</RequiredLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="選択してください" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent className="bg-background z-50">
                        {prefectures.map((pref) => (
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
                      <Input placeholder="渋谷区" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="address"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>住所</RequiredLabel>
                  <FormControl>
                    <Input placeholder="桜丘町" {...field} />
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
                    <Input placeholder="桜丘町" {...field} />
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
                    <Input placeholder="1-1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </div>

        <div className="space-y-4">
          <h3 className="text-lg font-semibold">個人情報</h3>
          
          <div className="grid grid-cols-2 gap-4">
            <FormField
              control={form.control}
              name="lastName"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>名前（姓）</RequiredLabel>
                  <FormControl>
                    <Input placeholder="山田" {...field} />
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
                    <Input placeholder="太郎" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <FormField
              control={form.control}
              name="lastNameKana"
              render={({ field }) => (
                <FormItem>
                  <RequiredLabel>名前（姓）（カナ）</RequiredLabel>
                  <FormControl>
                    <Input placeholder="ヤマダ" {...field} />
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
                    <Input placeholder="タロウ" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <FormField
            control={form.control}
            name="position"
            render={({ field }) => (
              <FormItem>
                <RequiredLabel>役職</RequiredLabel>
                <FormControl>
                  <Input placeholder="部長" {...field} />
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
                  <Input type="tel" placeholder="03-1234-5678" {...field} />
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
                <FormLabel>FAX番号</FormLabel>
                <FormControl>
                  <Input type="tel" placeholder="03-1234-5679" {...field} />
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
                  <Input type="email" placeholder="yamada@company.com" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
          方法論へ続く
        </Button>
      </form>
    </Form>
  );
};
