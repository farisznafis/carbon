import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { PasswordInput } from '@/components/inputs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
  FormMessage,
  FormDescription,
} from '@/components/ui/form';
import { useMemo, useState } from 'react';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { userSchema } from '@/lib/validations';
import { useDashboardStore } from '@/stores/dashboardStore';
import { Separator } from '@/components/ui/separator';
import { DeleteAlertDialog } from '@/components/feedback/DeleteAlertDialog/DeleteAlertDialog';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import type { UserFormData } from '@/api/users';
import type { Site } from '@/api/site';

export type { UserFormData } from '@/api/users';

interface UserFormProps {
  mode: 'create' | 'edit' | 'profile';
  defaultValues?: Partial<UserFormData>;
  originalData: UserFormData;
  onChange: (data: UserFormData) => void;
  onNext: () => void;
  onBack: () => void;
  onDelete?: () => void;
  schema: typeof userSchema;
  siteOptions?: Site[];
  isSiteOptionsLoading?: boolean;
  siteOptionsError?: string | null;
}

export function UserForm({
  mode,
  defaultValues,
  originalData,
  onChange,
  onNext,
  onBack,
  onDelete,
  schema,
  siteOptions,
  isSiteOptionsLoading = false,
  siteOptionsError = null,
}: Readonly<UserFormProps>) {
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const { joinableSites, getSiteById } = useDashboardStore();

  const originalSite = originalData?.siteId
    ? getSiteById(originalData.siteId)
    : undefined;

  const selectedSite = defaultValues?.siteId
    ? getSiteById(defaultValues.siteId)
    : originalSite;
  const storeSites = [...joinableSites(), ...(originalSite ? [originalSite] : [])];
  const baseSites = siteOptions ?? storeSites;
  const sites =
    selectedSite && !baseSites.some((site) => site.id === selectedSite.id)
      ? [...baseSites, selectedSite]
      : baseSites;

  const shouldShowSiteSelect = () => mode === 'create' || mode === 'edit';

  const form = useForm<UserFormData>({
    // resolver: zodResolver(schema),
    defaultValues: {
      ...defaultValues,
      password: undefined, //password field will be empty
      passwordConfirmation: undefined,
    },
  });

  const { formState, watch } = form;
  const values = watch();

  //Checks to see if form is dirty
  const isTrulyDirty = useMemo(() => {
    const normalize = (obj?: Partial<UserFormData>) => {
      if (!obj) return obj;

      const { password, passwordConfirmation, siteId, ...rest } = obj;

      return {
        ...rest,
        // normalize siteId: treat "", null, undefined as the same
        siteId: siteId || undefined,
      };
    };

    return (
      JSON.stringify(normalize(values)) !==
      JSON.stringify(normalize(originalData))
    );
  }, [values, originalData]);

  return (
    <FormPageWrapper
      pageName={mode === 'create' ? 'ユーザ登録' : 'ユーザ更新'}
      title=''
      onBack={() => onBack()}
      backText={
        // mode === 'profile' ? 'プロファイルに戻る' : 'ダッシュボードに戻る'
        mode === 'profile' ? '戻る' : '戻る'
      }
    >
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((values) => {
            onChange(values as UserFormData); // update parent state
            onNext(); // go to confirmation
          })}
          className='space-y-4'
        >
          <div className='text-2xl pb-2'>個人情報</div>
          <div className='grid grid-cols-1 md:grid-cols-2 gap-4'>
            <FormField
              control={form.control}
              name='lastName'
              render={({ field }) => (
                <FormItem>
                  <FormLabel>姓</FormLabel>
                  <FormControl>
                    <Input
                      placeholder='山田'
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name='firstName'
              render={({ field }) => (
                <FormItem>
                  <FormLabel>名</FormLabel>
                  <FormControl>
                    <Input
                      placeholder='太郎'
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <div className='grid grid-cols-1 md:grid-cols-2 gap-4'>
            <FormField
              control={form.control}
              name='lastNameKana'
              render={({ field }) => (
                <FormItem>
                  <FormLabel>セイ</FormLabel>
                  <FormControl>
                    <Input
                      placeholder='ヤマダ'
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name='firstNameKana'
              render={({ field }) => (
                <FormItem>
                  <FormLabel>メイ</FormLabel>
                  <FormControl>
                    <Input
                      placeholder='タロウ'
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>

          <FormField
            control={form.control}
            name='phoneNumber'
            render={({ field }) => (
              <FormItem>
                <FormLabel>電話番号（ハイフン無し）</FormLabel>
                <FormControl>
                  <Input
                    placeholder='0312345678'
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name='userPosition'
            render={({ field }) => (
              <FormItem>
                <FormLabel>役職</FormLabel>
                <FormControl>
                  <Input
                    placeholder='営業部部長'
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <Separator />
          <br></br>
          <div className='text-2xl pb-2'>アカウント情報</div>
          <FormField
            control={form.control}
            name='username'
            render={({ field }) => (
              <FormItem>
                <FormLabel>ユーザー名</FormLabel>
                <FormControl>
                  <Input
                    placeholder='tanaka_taro'
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name='email'
            render={({ field }) => (
              <FormItem>
                <FormLabel>メールアドレス</FormLabel>
                <FormControl>
                  <Input
                    type='email'
                    placeholder='example@company.com'
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {/* <FormField control={form.control} name="emailConfirmation" render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>メールアドレス（確認）</FormLabel>
                                        <FormControl><Input type="email" placeholder="example@company.com" {...field} /></FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )} /> */}

          {mode === 'create' && (
            <>
              <FormField
                control={form.control}
                name='password'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>パスワード</FormLabel>
                    <FormControl>
                      <PasswordInput
                        placeholder='••••••••'
                        {...field}
                      />
                    </FormControl>

                    <FormDescription>
                      8文字以上のパスワードを設定してください
                    </FormDescription>

                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* <FormField
                control={form.control}
                name='passwordConfirmation'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>パスワード（確認）</FormLabel>
                    <FormControl>
                      <Input
                        type='password'
                        placeholder='••••••••'
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              /> */}
            </>
          )}
          <Separator />
          <br />
          {shouldShowSiteSelect() && (
            <div className='text-2xl pb-2'>拠点</div>
          )}
          {shouldShowSiteSelect() && (
            <FormField
              control={form.control}
              name='siteId'
              render={({ field }) => (
                <FormItem>
                  <FormLabel>担当する拠点</FormLabel>
                  <Select
                    value={field.value ?? ''}
                    onValueChange={field.onChange}
                    disabled={isSiteOptionsLoading || Boolean(siteOptionsError) || sites.length === 0}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder='拠点を選択してください' />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {sites?.map((site) => (
                        <SelectItem
                          key={site.id}
                          value={site.id}
                        >
                          {site.siteName}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
          )}

          <div className='flex justify-between items-center gap-4 w-full py-8'>
            {onDelete && defaultValues?.siteId !== null ? (
              <DeleteAlertDialog
                label='ユーザ'
                open={isDeleteDialogOpen}
                onOpenChange={setIsDeleteDialogOpen}
                onDelete={onDelete}
              />
            ) : (
              <div className='w-[72px]'></div>
            )}

            <div className='flex gap-4 justify-end'>
              <Button
                type='button'
                variant='outline'
                onClick={onBack}
              >
                キャンセル
              </Button>
              <Button
                type='submit'
                disabled={!isTrulyDirty}
              >
                {mode === 'create' ? '登録' : '更新'}
              </Button>
            </div>
          </div>
          <Separator />
        </form>
      </Form>
    </FormPageWrapper>
  );
}

export default UserForm;
