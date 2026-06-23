import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { Loader2 } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { profileFormSchema } from '@/lib/validations';
import { useRef, useState } from 'react';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { useProfileStore } from '@/stores/FormStore';
import { PREFECTURE_OPTIONS } from '@/lib/constants/prefectures';

type ProfileFormData = z.infer<typeof profileFormSchema>;

type ZipCloudResponse = {
  status: number;
  message: string | null;
  results: Array<{
    address1: string;
    address2: string;
    address3: string;
  }> | null;
};

interface ProfileFormProps {
  onNext: (data: ProfileFormData) => void;
  onBack: () => void
  onDelete?: (userId: string) => void;
}

export function ProfileForm({ onNext, onBack, onDelete }: Readonly<ProfileFormProps>) {
  useRedirectIfLoggedIn();
  const { tempProfile, setTempProfile } = useProfileStore();
  const { toast } = useToast();
  const [isLoadingAddress, setIsLoadingAddress] = useState(false);
  const postalCodeLookupAbortRef = useRef<AbortController | null>(null);
  const profileDefaultValues = (tempProfile ?? {}) as Partial<ProfileFormData>;

  const form = useForm<ProfileFormData>({
    // resolver: zodResolver(profileFormSchema),
    defaultValues: {
      corporateNumber: profileDefaultValues.corporateNumber || '',
      companyName: profileDefaultValues.companyName || '',
      companyNameKana: profileDefaultValues.companyNameKana || '',
      companyNameEnglish: profileDefaultValues.companyNameEnglish || '',
      postalCode: profileDefaultValues.postalCode || '',
      prefecture: profileDefaultValues.prefecture || '',
      city: profileDefaultValues.city || '',
      address: profileDefaultValues.address || '',
      building: profileDefaultValues.building || '',
      lastName: profileDefaultValues.lastName || '',
      firstName: profileDefaultValues.firstName || '',
      lastNameKana: profileDefaultValues.lastNameKana || '',
      firstNameKana: profileDefaultValues.firstNameKana || '',
      position: profileDefaultValues.position || '',
      phoneNumber: profileDefaultValues.phoneNumber || '',
      faxNumber: profileDefaultValues.faxNumber || '',
      email: profileDefaultValues.email || '',
    },
  });

  const handlePostalCodeChange = async (postalCode: string, onChange: (value: string) => void) => {
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
      const response = await fetch(`https://zipcloud.ibsnet.co.jp/api/search?zipcode=${cleanedCode}`, {
        signal: controller.signal,
      });
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
      form.setValue('prefecture', result.address1 || '', { shouldDirty: true, shouldValidate: true });
      form.setValue('city', result.address2 || '', { shouldDirty: true, shouldValidate: true });
      form.setValue('address', result.address3 || '', { shouldDirty: true, shouldValidate: true });
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') {
        return;
      }

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

  const onSubmit = (data: ProfileFormData) => {
    setTempProfile(data);
    onNext(data);
  }

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
                <FormLabel>法人番号 <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
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
                <FormLabel>企業名 <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
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
                <FormLabel>企業名（カナ） <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
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
                <FormLabel>企業名（英語）</FormLabel>
                <FormControl>
                  <Input placeholder="Sample Corporation" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <div className="space-y-4">
            <p className="font-medium">所在地 <span className="text-red-500 text-xs ml-1">必須</span></p>

            <FormField
              control={form.control}
              name="postalCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-sm">郵便番号</FormLabel>
                  <div className="flex gap-2 items-center">
                    <FormControl>
                      <Input
                        placeholder="1234567"
                        maxLength={8}
                        {...field}
                        value={field.value ?? ''}
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
                    <FormLabel>都道府県</FormLabel>
                    <FormControl>
                      <Select onValueChange={field.onChange} value={field.value ?? ''}>
                        <SelectTrigger>
                          <SelectValue placeholder="選択してください" />
                        </SelectTrigger>
                        <SelectContent>
                          {PREFECTURE_OPTIONS.map((pref) => (
                            <SelectItem key={pref} value={pref}>
                              {pref}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />


              <FormField
                control={form.control}
                name="city"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-sm">市区町村</FormLabel>
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
                  <FormLabel className="text-sm">番地</FormLabel>
                  <FormControl>
                    <Input placeholder="桜丘町1-1" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="building"
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-sm">建物名</FormLabel>
                  <FormControl>
                    <Input placeholder="サンプルビル 3F" {...field} />
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
                  <FormLabel>名前（姓） <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
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
                  <FormLabel>名前（名） <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
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
                  <FormLabel>名前（姓）（カナ） <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
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
                  <FormLabel>名前（名）（カナ） <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
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
                <FormLabel>役職 <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
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
                <FormLabel>電話番号 <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
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
                <FormLabel>メールアドレス <span className="text-red-500 text-xs ml-1">必須</span></FormLabel>
                <FormControl>
                  <Input type="email" placeholder="yamada@company.com" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
          更新
        </Button>
      </form>
    </Form>
  );
};
