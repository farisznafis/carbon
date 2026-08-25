import { useState } from 'react';
import {
  FormActions,
  FormCard,
  FormPageContent,
  FormPageTitle,
  FormSection,
  FormSectionStack,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { ConfirmationItem } from '@/components/feedback/Confirmation/ConfirmationItem';
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { Button } from '@/components/ui/button';
import { SiteFormData } from '@/features/master/forms/SiteForm';
import { useDashboardStore } from '@/stores/dashboardStore';

interface SiteConfirmationProps {
  mode: 'create' | 'edit';
  data: SiteFormData;
  originalData: SiteFormData;
  onBack: () => void;
  onSubmit: () => void;
}

const displayValue = (value?: string | null) => {
  const trimmedValue = value?.trim();
  return trimmedValue ? trimmedValue : 'なし';
};

const originalValue = (
  value: string | undefined,
  isCreateMode: boolean,
) => (isCreateMode ? undefined : displayValue(value));

const formatUserName = (user?: {
  id: string;
  firstName?: string;
  lastName?: string;
  username?: string;
  email?: string;
}) => {
  if (!user) return undefined;

  const fullName = `${user.lastName ?? ''} ${user.firstName ?? ''}`.trim();

  if (fullName && user.username) {
    return `${fullName}（${user.username}）`;
  }

  return fullName || user.username || user.email || user.id;
};

export function SiteConfirmation({
  mode,
  data,
  originalData,
  onBack,
  onSubmit,
}: Readonly<SiteConfirmationProps>) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const getUser = useDashboardStore((state) => state.getUser);

  if (!data) return <div>読み込み中...</div>;

  const isCreateMode = mode === 'create';
  const pageTitle = isCreateMode ? '拠点情報の確認' : '拠点編集の確認';
  const confirmLabel = isCreateMode ? '登録' : '更新';
  const selectedUserName = data.userId
    ? formatUserName(getUser(data.userId)) ?? data.userId
    : null;
  const originalUserName = originalData.userId
    ? formatUserName(getUser(originalData.userId)) ?? originalData.userId
    : null;

  const handleSubmit = async () => {
    setIsSubmitting(true);

    try {
      await onSubmit();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <FormStandalonePage watermark={isCreateMode ? '' : ''}>
      <PageBackButton
        onClick={onBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <div className="space-y-3">
          <FormPageTitle>{pageTitle}</FormPageTitle>
          <p className="text-small text-foreground">
            内容をご確認ください。
          </p>
        </div>

        <FormCard>
          <FormSectionStack spacing="relaxed">
            <FormSection number={1} title="拠点情報">
              <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
                <ConfirmationItem
                  label="拠点名"
                  value={displayValue(data.siteName)}
                  originalValue={originalValue(originalData.siteName, isCreateMode)}
                />
                <ConfirmationItem
                  label="拠点名カナ"
                  value={displayValue(data.siteNameKana)}
                  originalValue={originalValue(originalData.siteNameKana, isCreateMode)}
                />
              </div>
            </FormSection>

            <FormSection number={2} title="所在地">
              <div className="grid grid-cols-1 gap-5 md:grid-cols-2">
                <ConfirmationItem
                  label="郵便番号"
                  value={displayValue(data.postalCode)}
                  originalValue={originalValue(originalData.postalCode, isCreateMode)}
                />
                <ConfirmationItem
                  label="都道府県"
                  value={displayValue(data.prefectureName)}
                  originalValue={originalValue(originalData.prefectureName, isCreateMode)}
                />
                <ConfirmationItem
                  label="市区町村"
                  value={displayValue(data.cityName)}
                  originalValue={originalValue(originalData.cityName, isCreateMode)}
                />
                <ConfirmationItem
                  label="番地"
                  value={displayValue(data.blockName)}
                  originalValue={originalValue(originalData.blockName, isCreateMode)}
                />
                <ConfirmationItem
                  label="建物名・部屋番号"
                  value={displayValue(data.addressDetail)}
                  originalValue={originalValue(originalData.addressDetail, isCreateMode)}
                />
              </div>
            </FormSection>

              <FormSection number={3} title="責任者">
                <ConfirmationItem
                  label="責任者"
                  value={selectedUserName ?? 'マスター'}
                  originalValue={
                    isCreateMode ? undefined : (originalUserName ?? 'マスター')
                  }
                />
              </FormSection>

            <FormActions
              pendingActionId={isSubmitting ? 'confirm' : null}
              actions={[
                {
                  id: 'back',
                  label: '戻る',
                  variant: 'outline',
                  onClick: onBack,
                },
                {
                  id: 'confirm',
                  label: confirmLabel,
                  pendingLabel: '送信中...',
                  onClick: handleSubmit,
                },
              ]}
            />
          </FormSectionStack>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
}
