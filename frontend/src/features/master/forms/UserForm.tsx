import { useMemo, useState, type ReactNode } from 'react';
import { useForm } from 'react-hook-form';
import { X } from 'lucide-react';
import { ApiError } from '@/api/_shared/errors';
import { PasswordInput } from '@/components/inputs';
import { DeleteAlertDialog } from '@/components/feedback/DeleteAlertDialog/DeleteAlertDialog';
import {
  FormActions,
  FormCard,
  FormGrid,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { RequiredLabel } from '@/components/forms/shared';
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
import type { Site } from '@/api/site';
import type { UserFormData } from '@/api/users';
import { useDashboardStore } from '@/stores/dashboardStore';

export type { UserFormData } from '@/api/users';

const userBackendFieldMap: Partial<Record<string, keyof UserFormData>> = {
  username: 'username',
  lastName: 'lastName',
  firstName: 'firstName',
  lastNameKana: 'lastNameKana',
  firstNameKana: 'firstNameKana',
  phoneNumber: 'phoneNumber',
  userPosition: 'userPosition',
  position: 'userPosition',
  email: 'email',
  password: 'password',
  siteId: 'siteId',
};

function toValidationMessage(value: unknown): string {
  if (typeof value === 'string') return value;
  if (Array.isArray(value)) return value.map(toValidationMessage).filter(Boolean).join('\n');
  if (value && typeof value === 'object' && 'message' in value) {
    return toValidationMessage((value as { message?: unknown }).message);
  }
  return '';
}

interface UserFormProps {
  mode: 'create' | 'edit' | 'profile';
  defaultValues?: Partial<UserFormData>;
  originalData: UserFormData;
  onChange: (data: UserFormData) => void;
  onNext: (data: UserFormData) => void | Promise<void>;
  onBack: () => void;
  onDelete?: () => void;
  siteOptions?: Site[];
  isSiteOptionsLoading?: boolean;
  siteOptionsError?: string | null;
  embedded?: boolean;
  hideSiteSection?: boolean;
  hideUsernameField?: boolean;
  requireSite?: boolean;
  additionalSections?: ReactNode;
  submitLabel?: string;
  submitDisabled?: boolean;
  hasExternalChanges?: boolean;
}

export function UserForm({
  mode,
  defaultValues,
  originalData,
  onChange,
  onNext,
  onBack,
  onDelete,
  siteOptions,
  isSiteOptionsLoading = false,
  siteOptionsError = null,
  embedded = false,
  hideSiteSection = false,
  hideUsernameField = false,
  requireSite = false,
  additionalSections,
  submitLabel = '確認',
  submitDisabled = false,
  hasExternalChanges = false,
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
    selectedSite && !baseSites.some((site) => site.siteId === selectedSite.siteId)
      ? [...baseSites, selectedSite]
      : baseSites;

  const shouldShowSiteSelect =
    (mode === 'create' || mode === 'edit') && !hideSiteSection;

  const form = useForm<UserFormData>({
    defaultValues: {
      ...defaultValues,
      password: undefined,
      passwordConfirmation: undefined,
    },
  });

  const applyBackendValidationErrors = (error: unknown) => {
    if (!(error instanceof ApiError)) return false;

    const errors = error.payload?.errors;
    if (!errors || typeof errors !== 'object' || Array.isArray(errors)) {
      return false;
    }

    let applied = false;

    Object.entries(errors).forEach(([backendField, value]) => {
      const formField = userBackendFieldMap[backendField];
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

  const values = form.watch();

  const isTrulyDirty = useMemo(() => {
    const normalize = (obj?: Partial<UserFormData>) => {
      if (!obj) return obj;

      const { password, passwordConfirmation, siteId, ...rest } = obj;

      return {
        ...rest,
        siteId: siteId || undefined,
      };
    };

    const hasFieldChanges = (
      JSON.stringify(normalize(values)) !==
      JSON.stringify(normalize(originalData))
    );

    return hasFieldChanges || hasExternalChanges;
  }, [values, originalData, hasExternalChanges]);

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
            <FormSection number={1} title="個人情報">
              <FormGrid>
                <FormField
                  control={form.control}
                  name="lastName"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>姓</RequiredLabel>
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
                      <RequiredLabel>名</RequiredLabel>
                      <FormControl>
                        <Input placeholder="太郎" {...field} />
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
                      <RequiredLabel>姓（カナ）</RequiredLabel>
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
                      <RequiredLabel>名（カナ）</RequiredLabel>
                      <FormControl>
                        <Input placeholder="タロウ" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </FormGrid>

              <FormField
                control={form.control}
                name="phoneNumber"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>電話番号（ハイフン無し）</RequiredLabel>
                    <FormControl>
                      <Input placeholder="半角数字ハイフン不要" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="userPosition"
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
            </FormSection>

            <FormSection number={2} title="アカウント情報">
              {!hideUsernameField && (
                <FormField
                  control={form.control}
                  name="username"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>社員名</RequiredLabel>
                      <FormControl>
                        <Input placeholder="yamada_taro" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}

              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <RequiredLabel>メールアドレス</RequiredLabel>
                    <FormControl>
                      <Input
                        type="email"
                        placeholder="yamada.taro@example.com"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {mode === 'create' && (
                <FormField
                  control={form.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel>パスワード</RequiredLabel>
                      <FormControl>
                        <PasswordInput
                          placeholder="パスワードを入力"
                          {...field}
                        />
                      </FormControl>
                      <FormDescription>
                        8文字以上で、大文字・小文字・数字をそれぞれ1文字以上含むパスワードを設定してください。
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
            </FormSection>

            {shouldShowSiteSelect && (
              <FormSection number={3} title="拠点">
                <FormField
                  control={form.control}
                  name="siteId"
                  render={({ field }) => (
                    <FormItem>
                      <RequiredLabel required={requireSite}>対象する拠点</RequiredLabel>
                      <div className="flex items-center gap-2">
                        <Select
                          value={field.value ?? ''}
                          onValueChange={(value) => field.onChange(value)}
                          disabled={
                            isSiteOptionsLoading ||
                            Boolean(siteOptionsError) ||
                            sites.length === 0
                          }
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="拠点を選択してください" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {sites.map((site) => (
                              <SelectItem key={site.siteId} value={site.siteId}>
                                {site.siteName}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>

                        {field.value && (
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            aria-label="拠点をクリア"
                            onClick={() => field.onChange('')}
                          >
                            <X className="h-4 w-4 text-muted-foreground" />
                          </Button>
                        )}
                      </div>
                      {siteOptionsError && (
                        <FormDescription className="text-destructive">
                          {siteOptionsError}
                        </FormDescription>
                      )}
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </FormSection>
            )}

            {additionalSections}

            <FormActions
              leadingAction={
                onDelete ? (
                  <DeleteAlertDialog
                    label="社員"
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
                  onClick: onBack,
                },
                {
                  id: 'submit',
                  label: submitLabel,
                  type: 'submit',
                  disabled: !isTrulyDirty || submitDisabled,
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
      pageName={mode === 'create' ? '社員追加' : '社員更新'}
      title=""
      onBack={onBack}
      backText="戻る"
      contentMaxWidth="max-w-[768px]"
      wrapInCard={false}
    >
      {formContent}
    </FormPageWrapper>
  );
}

export default UserForm;
