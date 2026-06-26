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

export function SiteConfirmation({
  mode,
  data,
  originalData,
  onBack,
  onSubmit,
}: Readonly<SiteConfirmationProps>) {
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!data) return <div>読み込み中...</div>;

  const isCreateMode = mode === 'create';
  const pageTitle = isCreateMode ? '拠点情報の確認' : '拠点編集の確認';
  const confirmLabel = isCreateMode ? '登録' : '更新';

  const handleSubmit = async () => {
    setIsSubmitting(true);

    try {
      await onSubmit();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <FormStandalonePage watermark={isCreateMode ? 'Site Add' : 'Site Edit'}>
      <PageBackButton
        onClick={onBack}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <div className="space-y-3">
          <FormPageTitle>{pageTitle}</FormPageTitle>
          <p className="text-small text-muted-foreground">
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
                onClick={handleSubmit}
                disabled={isSubmitting}
              >
                {isSubmitting ? '送信中...' : confirmLabel}
              </Button>
            </FormActions>
          </FormSectionStack>
        </FormCard>
      </FormPageContent>
    </FormStandalonePage>
  );
}
