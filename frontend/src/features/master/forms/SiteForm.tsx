import { useNavigate } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Resolver, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import type { SiteFormData } from '@/api/site';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { SiteSchema } from '@/lib/validations';
import { prefectures } from '@/lib/constants/prefectures';
import { Separator } from '@/components/ui/separator';
import { DeleteAlertDialog } from '@/components/feedback/DeleteAlertDialog/DeleteAlertDialog';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';

export type { SiteFormData } from '@/api/site';

const editSiteSchema = SiteSchema.pick({
  postalCode: true,
  prefectureName: true,
  cityName: true,
  townName: true,
  blockName: true,
  addressDetail: true,
});

interface SiteFormProps {
  mode: 'create' | 'edit';
  originalData: SiteFormData;
  onChange: (data: SiteFormData) => void;
  onNext: () => void;
  onDelete?: () => void;
  defaultValues?: Partial<SiteFormData>;
  disablePicFields?: boolean;
  disableSubmit?: boolean;
}

export function SiteForm({
  mode,
  originalData,
  onChange,
  onNext,
  onDelete,
  defaultValues,
  disablePicFields = false,
  disableSubmit = false,
}: Readonly<SiteFormProps>) {
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const navigate = useNavigate();

  const isCreateMode = mode === 'create';

  const defaultData = useMemo(() => ({
    siteName: defaultValues?.siteName ?? '',
    siteNameKana: defaultValues?.siteNameKana ?? '',
    postalCode: defaultValues?.postalCode ?? '',
    prefectureName: defaultValues?.prefectureName ?? '',
    cityName: defaultValues?.cityName ?? '',
    townName: defaultValues?.townName ?? '',
    blockName: defaultValues?.blockName ?? '',
    addressDetail: defaultValues?.addressDetail ?? '',
    picFirstName: defaultValues?.picFirstName ?? '',
    picFirstNameKana: defaultValues?.picFirstNameKana ?? '',
    picLastName: defaultValues?.picLastName ?? '',
    picLastNameKana: defaultValues?.picLastNameKana ?? '',
    picEmail: defaultValues?.picEmail ?? '',
    picPhoneNumber: defaultValues?.picPhoneNumber ?? '',
    picPosition: defaultValues?.picPosition ?? '',
  }), [defaultValues]);

  const initialFormData = useMemo(() => ({
    siteName: originalData?.siteName ?? '',
    siteNameKana: originalData?.siteNameKana ?? '',
    postalCode: originalData?.postalCode ?? '',
    prefectureName: originalData?.prefectureName ?? '',
    cityName: originalData?.cityName ?? '',
    townName: originalData?.townName ?? '',
    blockName: originalData?.blockName ?? '',
    addressDetail: originalData?.addressDetail ?? '',
    picFirstName: originalData?.picFirstName ?? '',
    picFirstNameKana: originalData?.picFirstNameKana ?? '',
    picLastName: originalData?.picLastName ?? '',
    picLastNameKana: originalData?.picLastNameKana ?? '',
    picEmail: originalData?.picEmail ?? '',
    picPhoneNumber: originalData?.picPhoneNumber ?? '',
    picPosition: originalData?.picPosition ?? '',
  }), [originalData]);

  const formSchema = isCreateMode ? SiteSchema : editSiteSchema;
  const shouldDisablePicFields = isCreateMode && disablePicFields;
  const picInputClassName = shouldDisablePicFields
    ? 'disabled:cursor-not-allowed disabled:bg-muted disabled:text-muted-foreground disabled:opacity-100'
    : undefined;

  const form = useForm<SiteFormData>({
    // resolver: zodResolver(formSchema) as Resolver<SiteFormData>,
    defaultValues: defaultData,
  });

  useEffect(() => {
    form.reset(defaultData);
  }, [defaultData, form]);

  const { watch } = form;
  const values = watch();

  const isDirty = useMemo(() => {
    const currentValues = isCreateMode
      ? values
      : {
          postalCode: values.postalCode,
          prefectureName: values.prefectureName,
          cityName: values.cityName,
          townName: values.townName,
          blockName: values.blockName,
          addressDetail: values.addressDetail,
        };

    const initialValues = isCreateMode
      ? initialFormData
      : {
          postalCode: initialFormData.postalCode,
          prefectureName: initialFormData.prefectureName,
          cityName: initialFormData.cityName,
          townName: initialFormData.townName,
          blockName: initialFormData.blockName,
          addressDetail: initialFormData.addressDetail,
        };

    return JSON.stringify(currentValues) !== JSON.stringify(initialValues);
  }, [values, initialFormData, isCreateMode]);

  return (
    <FormPageWrapper
      pageName={mode === 'create' ? '拠点登録' : '拠点編集'}
      title=''
      onBack={() => navigate(-1)}
      backText='戻る'
    >
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit((formValues) => {
            onChange(formValues);
            onNext();
          })}
          className='space-y-4'
        >
          {isCreateMode && (
            <>
              <div className='text-2xl pb-2'>拠点情報</div>

              <FormField
                control={form.control}
                name='siteName'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>拠点名</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='東京本社'
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name='siteNameKana'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>拠点名（カナ）</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='トウキョウホンシャ'
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <Separator />
              <br />

              <div className='text-2xl pb-2'>所在地</div>
            </>
          )}

          {!isCreateMode && (
            <>
              <div className='text-2xl pb-2'>所在地</div>
            </>
          )}

          <div className='flex items-end gap-2'>
            <FormField
              control={form.control}
              name='postalCode'
              render={({ field }) => (
                <FormItem className='flex-1'>
                  <FormLabel>郵便番号（ハイフン無し）</FormLabel>
                  <FormControl>
                    <Input
                      placeholder='1000001'
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* <Button
              type='button'
              variant='outline'
            >
              検索
            </Button> */}
          </div>

          <FormField
            control={form.control}
            name='prefectureName'
            render={({ field }) => (
              <FormItem>
                <FormLabel>都道府県</FormLabel>
                <Select
                  onValueChange={field.onChange}
                  value={field.value ?? ''}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder='都道府県を選択' />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {prefectures.map((pref) => (
                      <SelectItem
                        key={pref}
                        value={pref}
                      >
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
            name='cityName'
            render={({ field }) => (
              <FormItem>
                <FormLabel>市区町村</FormLabel>
                <FormControl>
                  <Input
                    placeholder='千代田区'
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name='townName'
            render={({ field }) => (
              <FormItem>
                <FormLabel>町域</FormLabel>
                <FormControl>
                  <Input
                    placeholder='千代田'
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name='blockName'
            render={({ field }) => (
              <FormItem>
                <FormLabel>番地</FormLabel>
                <FormControl>
                  <Input
                    placeholder='1-1-1'
                    {...field}
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          {isCreateMode && (
            <>
              <FormField
                control={form.control}
                name='addressDetail'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>建物名・部屋番号</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='東京ビル3F305室'
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* <div className='text-2xl pb-2 pt-4'>担当者情報</div> */}
              <br />
              <Separator />
              <div className='space-y-4 rounded-md border bg-muted/40 p-4'>
                <div>
                  <div className='text-2xl pb-1'>担当者情報</div>
                  <p className='text-sm text-muted-foreground'>
                    参加企業アカウント情報
                  </p>
                </div>

              <FormField
                control={form.control}
                name='picLastName'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>担当者（姓）</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='山田'
                        className={picInputClassName}
                        disabled={shouldDisablePicFields}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name='picLastNameKana'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>担当者（姓カナ）</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='ヤマダ'
                        className={picInputClassName}
                        disabled={shouldDisablePicFields}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name='picFirstName'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>担当者（名）</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='太郎'
                        className={picInputClassName}
                        disabled={shouldDisablePicFields}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name='picFirstNameKana'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>担当者（名カナ）</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='タロウ'
                        className={picInputClassName}
                        disabled={shouldDisablePicFields}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name='picEmail'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>担当者メールアドレス</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='taro@example.com'
                        className={picInputClassName}
                        disabled={shouldDisablePicFields}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name='picPhoneNumber'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>担当者電話番号</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='03-1234-5678'
                        className={picInputClassName}
                        disabled={shouldDisablePicFields}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name='picPosition'
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>担当者役職</FormLabel>
                    <FormControl>
                      <Input
                        placeholder='部長'
                        className={picInputClassName}
                        disabled={shouldDisablePicFields}
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              </div>
            </>
          )}

          <div className='flex justify-between items-center gap-4 w-full py-8'>
            {isCreateMode ? <div className='w-[72px]' /> : (
              onDelete ? (
                <DeleteAlertDialog
                  label='拠点'
                  open={isDeleteDialogOpen}
                  onOpenChange={setIsDeleteDialogOpen}
                  onDelete={onDelete}
                />
              ) : (
                <div className='w-[72px]' />
              )
            )}

            <div className='flex gap-4'>
              <Button
                type='button'
                variant='outline'
                onClick={() => navigate(-1)}
              >
                キャンセル
              </Button>
              <Button
                type='submit'
                disabled={!isDirty || disableSubmit}
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

export default SiteForm;
