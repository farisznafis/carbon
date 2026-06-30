import { useState } from 'react';
import {
  FormActions,
  FormCard,
  FormGrid,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { ConfirmationItem } from '@/components/feedback/Confirmation/ConfirmationItem';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import { UserFormData } from '@/features/master/forms/UserForm';
import { useDashboardStore } from '@/stores/dashboardStore';

interface UserConfirmationProps {
  mode: 'create' | 'edit';
  data?: UserFormData;
  originalData: UserFormData;
  onBack: () => void;
  onSubmit: () => void;
  hideSiteSection?: boolean;
  hideUsernameField?: boolean;
}

const displayValue = (value?: string | null) => {
  const trimmedValue = value?.trim();
  return trimmedValue ? trimmedValue : 'なし';
};

const originalValue = (
  value: string | undefined,
  isCreateMode: boolean,
) => (isCreateMode ? undefined : displayValue(value));

export function UserConfirmation({
  mode,
  data,
  originalData,
  onBack,
  onSubmit,
  hideSiteSection = false,
  hideUsernameField = false,
}: Readonly<UserConfirmationProps>) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const getSiteById = useDashboardStore((state) => state.getSiteById);

  if (!data) return <div>読み込み中...</div>;

  const isCreateMode = mode === 'create';
  const siteName = data.siteId ? getSiteById(data.siteId)?.siteName : undefined;
  const originalSiteName = originalData.siteId
    ? getSiteById(originalData.siteId)?.siteName
    : undefined;

  const handleConfirm = async () => {
    setIsSubmitting(true);

    try {
      await onSubmit();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={onBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <div className="space-y-3">
          <FormPageTitle>
            {isCreateMode ? 'ユーザー情報の確認' : 'ユーザー更新の確認'}
          </FormPageTitle>
          <p className="text-small text-muted-foreground">
            内容をご確認ください。
          </p>
        </div>

        <FormCard>
          <FormSectionStack spacing="relaxed">
            <FormSection number={1} title="個人情報">
              <FormGrid>
                <ConfirmationItem
                  label="姓"
                  value={displayValue(data.lastName)}
                  originalValue={originalValue(originalData.lastName, isCreateMode)}
                />
                <ConfirmationItem
                  label="名"
                  value={displayValue(data.firstName)}
                  originalValue={originalValue(originalData.firstName, isCreateMode)}
                />
                <ConfirmationItem
                  label="姓（カナ）"
                  value={displayValue(data.lastNameKana)}
                  originalValue={originalValue(originalData.lastNameKana, isCreateMode)}
                />
                <ConfirmationItem
                  label="名（カナ）"
                  value={displayValue(data.firstNameKana)}
                  originalValue={originalValue(originalData.firstNameKana, isCreateMode)}
                />
              </FormGrid>

              <FormGrid>
                <ConfirmationItem
                  label="役職"
                  value={displayValue(data.userPosition)}
                  originalValue={originalValue(originalData.userPosition, isCreateMode)}
                />
                <ConfirmationItem
                  label="電話番号"
                  value={displayValue(data.phoneNumber)}
                  originalValue={originalValue(originalData.phoneNumber, isCreateMode)}
                />
              </FormGrid>
            </FormSection>

            <FormSection number={2} title="アカウント情報">
              <FormGrid>
                {!hideUsernameField && (
                  <ConfirmationItem
                    label="ユーザー名"
                    value={displayValue(data.username)}
                    originalValue={originalValue(originalData.username, isCreateMode)}
                  />
                )}
                <ConfirmationItem
                  label="メールアドレス"
                  value={displayValue(data.email)}
                  originalValue={originalValue(originalData.email, isCreateMode)}
                />
              </FormGrid>
            </FormSection>

            {!hideSiteSection && (
              <FormSection number={3} title="拠点">
                <ConfirmationItem
                  label="担当する拠点"
                  value={displayValue(siteName)}
                  originalValue={isCreateMode ? undefined : displayValue(originalSiteName)}
                />
              </FormSection>
            )}

            <FormActions className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="w-full"
                onClick={onBack}
                disabled={isSubmitting}
              >
                戻る
              </Button>
              <Button
                type="button"
                size="sm"
                className="w-full"
                onClick={handleConfirm}
                disabled={isSubmitting}
              >
                {isSubmitting ? '送信中...' : isCreateMode ? '登録' : '更新'}
              </Button>
            </FormActions>
          </FormSectionStack>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
}

export default UserConfirmation;
