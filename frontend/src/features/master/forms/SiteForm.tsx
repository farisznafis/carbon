import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import {
  FormActions,
  FormCard,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { RequiredBadge, RequiredLabel } from '@/components/forms/shared';
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
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { SiteFormData } from '@/api/site';
import type { UserList } from '@/api/users';
import { prefectures } from '@/lib/constants/prefectures';
import { UserPlus } from 'lucide-react';

export type { SiteFormData } from '@/api/site';

const ADD_USER_OPTION_VALUE = '__add_user__';

const siteBackendFieldMap: Partial<Record<string, keyof SiteFormData>> = {
  siteName: 'siteName',
  siteNameKana: 'siteNameKana',
  postalCode: 'postalCode',
  prefecture: 'prefectureName',
  municipality: 'cityName',
  blocknum: 'blockName',
  buildingName: 'addressDetail',
  userId: 'userId',
  picuserId: 'userId',
};

function toValidationMessage(value: unknown): string {
  if (typeof value === 'string') return value;
  if (Array.isArray(value)) return value.map(toValidationMessage).filter(Boolean).join('\n');
  if (value && typeof value === 'object' && 'message' in value) {
    return toValidationMessage((value as { message?: unknown }).message);
  }
  return '';
}

interface SiteFormProps {
  mode: 'create' | 'edit';
  originalData: SiteFormData;
  onChange: (data: SiteFormData) => void;
  onNext: (data: SiteFormData) => void | Promise<void>;
  onDelete?: () => void;
  defaultValues?: Partial<SiteFormData>;
  disableSubmit?: boolean;
  userOptions?: UserList[];
  isUserOptionsLoading?: boolean;
  userOptionsError?: string | null;
  userOptionsHelperText?: string;
  onAddUser?: (draft: SiteFormData) => void;
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
  userOptionsHelperText,
  onAddUser,
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

  const applyBackendValidationErrors = (error: unknown) => {
    if (!(error instanceof ApiError)) return false;

    const errors = error.payload?.errors;
    if (!errors || typeof errors !== 'object' || Array.isArray(errors)) {
      return false;
    }

    let applied = false;

    Object.entries(errors).forEach(([backendField, value]) => {
      const formField = siteBackendFieldMap[backendField];
      const message = toValidationMessage(value);

      if (!formField || !message) return;

      form.setError(formField, {
        type: 'server',
        message,
      });
      applied = true;
    });

    return applied;
  };

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
        const cityNameWithTown = `${result.address2 ?? ''}${result.address3 ?? ''}`;
        form.setValue('prefectureName', result.address1, { shouldDirty: true });
        form.setValue('cityName', cityNameWithTown, { shouldDirty: true });
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

  const submitLabel = '確認';

  const formContent = (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit(async (formValues) => {
          form.clearErrors();
          onChange(formValues);

          try {
            await onNext(formValues);
          } catch (error) {
            applyBackendValidationErrors(error);
          }
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
                      <Input placeholder="東京本社" {...field} />
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
                        <Input placeholder="トウキョウホンシャ" {...field} />
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
                        <SelectTrigger>
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
                      <Input placeholder="千代田区" {...field} />
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
                      <Input placeholder="1-1-1" {...field} />
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
                      <Input placeholder="東京ビル3F 305号" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </FormSection>

              <FormSection number={3} title={<>責任者 <RequiredBadge /></>}>
                {userOptionsHelperText && (
                  <p className="-mt-2 mb-1 text-sm text-foreground">
                    {userOptionsHelperText}
                  </p>
                )}
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
                        <div className="flex items-center gap-2">
                          <Select
                            value={field.value ?? ''}
                            onValueChange={(value) => {
                              if (value === ADD_USER_OPTION_VALUE) {
                                onAddUser?.(form.getValues());
                                return;
                              }

                              field.onChange(value);
                            }}
                            disabled={
                              !onAddUser &&
                              (isUserOptionsLoading ||
                                Boolean(userOptionsError) ||
                                userOptions.length === 0)
                            }
                          >
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue placeholder="担当者を選択してください" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              {userOptions.map((user) => (
                                <SelectItem key={user.id} value={user.id}>
                                  <span className="flex items-center gap-2">
                                    {formatUserOptionLabel(user)}
                                    {user.role === 'master' && (
                                      <StatusBadge tone="info">
                                        マスター
                                      </StatusBadge>
                                    )}
                                  </span>
                                </SelectItem>
                              ))}
                              {onAddUser && (
                                <>
                                  {userOptions.length > 0 && <SelectSeparator />}
                                  <SelectItem
                                    value={ADD_USER_OPTION_VALUE}
                                    className="font-medium text-primary focus:text-primary"
                                  >
                                    <span className="flex items-center gap-2">
                                      <UserPlus
                                        className="h-4 w-4"
                                        aria-hidden="true"
                                      />
                                      社員を追加
                                    </span>
                                  </SelectItem>
                                </>
                              )}
                            </SelectContent>
                          </Select>

                        </div>
                        {isUserOptionsLoading && (
                          <FormDescription>
                            担当者候補を読み込み中です。
                          </FormDescription>
                        )}
                        {hasNoSelectableUsers && (
                          <FormDescription>
                            選択可能な社員がいません。未選択の場合はマスターが担当者になります。
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

            <FormActions
              leadingAction={
                !isCreateMode && onDelete ? (
                  <DeleteAlertDialog
                    label="拠点"
                    open={isDeleteDialogOpen}
                    onOpenChange={setIsDeleteDialogOpen}
                    onDelete={onDelete}
                    iconOnly
                  />
                ) : undefined
              }
              actions={[
                {
                  id: 'cancel',
                  label: 'キャンセル',
                  variant: 'outline',
                  onClick: () => navigate(-1),
                },
                {
                  id: 'submit',
                  label: submitLabel,
                  type: 'submit',
                  disabled: !isDirty || disableSubmit,
                },
              ]}
            />
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
