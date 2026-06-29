import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { X } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
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
  FormDescription,
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
import type { UserList } from '@/api/users';
import { prefectures } from '@/lib/constants/prefectures';

export type { SiteFormData } from '@/api/site';

interface SiteFormProps {
  mode: 'create' | 'edit';
  originalData: SiteFormData;
  onChange: (data: SiteFormData) => void;
  onNext: () => void;
  onDelete?: () => void;
  defaultValues?: Partial<SiteFormData>;
  disableSubmit?: boolean;
  userOptions?: UserList[];
  isUserOptionsLoading?: boolean;
  userOptionsError?: string | null;
  embedded?: boolean;
}

const formatUserOptionLabel = (user: UserList) => {
  const fullName = `${user.lastName ?? ''} ${user.firstName ?? ''}`.trim();

  if (fullName && user.username) {
    return `${fullName}（${user.username}）`;
  }

  return fullName || user.username || user.email || user.id;
};

export function SiteForm({
  mode,
  originalData,
  onChange,
  onNext,
  onDelete,
  defaultValues,
  disableSubmit = false,
  userOptions = [],
  isUserOptionsLoading = false,
  userOptionsError = null,
  embedded = false,
}: Readonly<SiteFormProps>) {
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const postalCodeLookupAbortRef = useRef<AbortController | null>(null);
  const navigate = useNavigate();
  const isCreateMode = mode === 'create';

  const defaultData = useMemo(
    () => ({
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
      userId: defaultValues?.userId ?? '',
    }),
    [defaultValues],
  );

  const initialFormData = useMemo(
    () => ({
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
      userId: originalData?.userId ?? '',
    }),
    [originalData],
  );

  const form = useForm<SiteFormData>({
    defaultValues: defaultData,
  });

  useEffect(() => {
    form.reset(defaultData);
  }, [defaultData, form]);

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

    try {
      const response = await fetch(`https://zipcloud.ibsnet.co.jp/api/search?zipcode=${cleanedCode}`, {
        signal: controller.signal,
      });
      const data = await response.json();

      if (data.results && data.results.length > 0) {
        const result = data.results[0];
        form.setValue('prefectureName', result.address1, { shouldDirty: true });
        form.setValue('cityName', result.address2, { shouldDirty: true });
      }
    } catch (error) {
      if (!(error instanceof DOMException && error.name === 'AbortError')) {
        console.error('Failed to fetch address:', error);
      }
    }
  };

  const values = form.watch();

  const isDirty = useMemo(() => {
    const currentValues = isCreateMode
      ? values
      : {
          siteName: values.siteName,
          postalCode: values.postalCode,
          prefectureName: values.prefectureName,
          cityName: values.cityName,
          blockName: values.blockName,
          addressDetail: values.addressDetail,
          userId: values.userId || '',
        };

    const initialValues = isCreateMode
      ? initialFormData
      : {
          siteName: initialFormData.siteName,
          postalCode: initialFormData.postalCode,
          prefectureName: initialFormData.prefectureName,
          cityName: initialFormData.cityName,
          blockName: initialFormData.blockName,
          addressDetail: initialFormData.addressDetail,
          userId: initialFormData.userId || '',
        };

    return JSON.stringify(currentValues) !== JSON.stringify(initialValues);
  }, [values, initialFormData, isCreateMode]);

  const submitLabel = isCreateMode ? '登録' : '更新';

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

              {isCreateMode && (
                <FormField
                  control={form.control}
                  name="siteNameKana"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>拠点名カナ</RequiredLabel>
                      <FormControl>
                        <Input inputSize="sm" placeholder="トウキョウホンシャ" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
            </FormSection>

            <FormSection number={2} title="所在地">
              <FormField
                control={form.control}
                name="postalCode"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>郵便番号（ハイフン無し）</RequiredLabel>
                    <FormControl>
                      <Input
                        inputSize="sm"
                        placeholder="1000001"
                        {...field}
                        onChange={(e) => handlePostalCodeChange(e.target.value, field.onChange)}
                      />
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
                    <RequiredLabel required={true}>建物名・部屋番号</RequiredLabel>
                    <FormControl>
                      <Input inputSize="sm" placeholder="東京ビル3F 305号" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormSection>

              <FormSection number={3} title="担当者">
                <FormField
                  control={form.control}
                  name="userId"
                  render={({ field }) => {
                    const hasNoSelectableUsers =
                      !isUserOptionsLoading &&
                      !userOptionsError &&
                      userOptions.length === 0;

                    return (
                      <FormItem>
                        <RequiredLabel required={false}>担当者</RequiredLabel>
                        <div className="flex items-center gap-2">
                          <Select
                            value={field.value ?? ''}
                            onValueChange={(value) => field.onChange(value)}
                            disabled={
                              isUserOptionsLoading ||
                              Boolean(userOptionsError) ||
                              userOptions.length === 0
                            }
                          >
                            <FormControl>
                              <SelectTrigger selectSize="sm">
                                <SelectValue placeholder="担当者を選択してください" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              {userOptions.map((user) => (
                                <SelectItem key={user.id} value={user.id}>
                                  <span className="flex items-center gap-2">
                                    {formatUserOptionLabel(user)}
                                    {user.role === 'master' && (
                                      <Badge variant="default" className="bg-blue-500 text-white text-[10px] px-1.5 py-0 leading-none">
                                        マスター
                                      </Badge>
                                    )}
                                  </span>
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>

                          {field.value && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon"
                              aria-label="担当者をクリア"
                              onClick={() => field.onChange('')}
                            >
                              <X className="h-4 w-4 text-muted-foreground" />
                            </Button>
                          )}
                        </div>
                        {isUserOptionsLoading && (
                          <FormDescription>
                            担当者候補を読み込み中です。
                          </FormDescription>
                        )}
                        {hasNoSelectableUsers && (
                          <FormDescription>
                            選択可能なユーザーがいません。未選択の場合はマスターが担当者になります。
                          </FormDescription>
                        )}
                        {userOptionsError && (
                          <FormDescription className="text-destructive">
                            {userOptionsError}
                          </FormDescription>
                        )}
                        <FormMessage />
                      </FormItem>
                    );
                  }}
                />
              </FormSection>

            <FormActions className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {!isCreateMode && onDelete ? (
                <div className="flex gap-3">
                  <DeleteAlertDialog
                    label="拠点"
                    open={isDeleteDialogOpen}
                    onOpenChange={setIsDeleteDialogOpen}
                    onDelete={onDelete}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="flex-1"
                    onClick={() => navigate(-1)}
                  >
                    キャンセル
                  </Button>
                </div>
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
