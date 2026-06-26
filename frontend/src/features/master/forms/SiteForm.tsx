import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import {
  FormActions,
  FormCard,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { RequiredLabel } from '@/components/forms/shared';
import { DeleteAlertDialog } from '@/components/feedback/DeleteAlertDialog/DeleteAlertDialog';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { SiteFormData } from '@/api/site';
import { prefectures } from '@/lib/constants/prefectures';

export type { SiteFormData } from '@/api/site';

interface SiteFormProps {
  mode: 'create' | 'edit';
  originalData: SiteFormData;
  onChange: (data: SiteFormData) => void;
  onNext: () => void;
  onDelete?: () => void;
  defaultValues?: Partial<SiteFormData>;
  disablePicFields?: boolean;
  disableSubmit?: boolean;
  embedded?: boolean;
}

export function SiteForm({
  mode,
  originalData,
  onChange,
  onNext,
  onDelete,
  defaultValues,
  disableSubmit = false,
  embedded = false,
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

  const form = useForm<SiteFormData>({
    defaultValues: defaultData,
  });

  useEffect(() => {
    form.reset(defaultData);
  }, [defaultData, form]);

  const values = form.watch();

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

  const submitLabel = isCreateMode ? '確認' : '更新';

  const formContent = (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((formValues) => {
          onChange(formValues);
          onNext();
        })}
      >
        <FormCard>
          <FormSectionStack spacing="relaxed">
            {isCreateMode && (
              <FormSection number={1} title="拠点情報">
                <FormField
                  control={form.control}
                  name="siteName"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>拠点名</RequiredLabel>
                      <FormControl>
                        <Input inputSize="sm" placeholder="東京本社" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="siteNameKana"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>拠点名（カナ）</RequiredLabel>
                      <FormControl>
                        <Input inputSize="sm" placeholder="トウキョウホンシャ" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </FormSection>
            )}

            <FormSection number={isCreateMode ? 2 : 1} title="所在地">
              <FormField
                control={form.control}
                name="postalCode"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>郵便番号（ハイフン無し）</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="1000001" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="prefectureName"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>都道府県</RequiredLabel>
                    <Select onValueChange={field.onChange} value={field.value ?? ''}>
                      <FormControl>
                        <SelectTrigger selectSize="sm">
                          <SelectValue placeholder="都道府県を選択" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent className="z-50 bg-background">
                        {prefectures.map((prefecture) => (
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
                name="cityName"
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

              <FormField
                control={form.control}
                name="townName"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>町域</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="千代田" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="blockName"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>番地</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="1-1-1" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="addressDetail"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel required={false}>建物名・部屋番号</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="東京ビル3F305室" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormSection>

            {isCreateMode && (
              <>
                <input type="hidden" {...form.register('picLastName')} />
                <input type="hidden" {...form.register('picLastNameKana')} />
                <input type="hidden" {...form.register('picFirstName')} />
                <input type="hidden" {...form.register('picFirstNameKana')} />
                <input type="hidden" {...form.register('picEmail')} />
                <input type="hidden" {...form.register('picPhoneNumber')} />
                <input type="hidden" {...form.register('picPosition')} />
              </>
            )}

            <FormActions className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {!isCreateMode && onDelete ? (
                <DeleteAlertDialog
                  label="拠点"
                  open={isDeleteDialogOpen}
                  onOpenChange={setIsDeleteDialogOpen}
                  onDelete={onDelete}
                />
              ) : (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={() => navigate(-1)}
                >
                  キャンセル
                </Button>
              )}

              <Button
                type="submit"
                size="sm"
                className="w-full"
                disabled={!isDirty || disableSubmit}
              >
                {submitLabel}
              </Button>
            </FormActions>
          </FormSectionStack>
        </FormCard>
      </form>
    </Form>
  );

  if (embedded) {
    return formContent;
  }

  return (
    <FormPageWrapper
      pageName={mode === 'create' ? '拠点登録' : '拠点編集'}
      title=""
      onBack={() => navigate(-1)}
      backText="戻る"
      contentMaxWidth="max-w-[768px]"
      wrapInCard={false}
    >
      {formContent}
    </FormPageWrapper>
  );
}

export default SiteForm;
