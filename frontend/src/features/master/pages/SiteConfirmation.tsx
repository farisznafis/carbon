import { useState } from 'react';
import { SiteFormData } from '@/features/master/forms/SiteForm';
import { ConfirmationItem } from '@/components/feedback/Confirmation/ConfirmationItem';
import { ConfirmationLayout } from '@/components/feedback/Confirmation/ConfirmationLayout';

interface SiteConfirmationProps {
  mode: 'create' | 'edit';
  data: SiteFormData;
  originalData: SiteFormData;
  onBack: () => void;
  onSubmit: () => void;
}

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

  const handleSubmit = async () => {
    setIsSubmitting(true);
    await onSubmit();
    setIsSubmitting(false);
  };

  return (
    <ConfirmationLayout
      pageName={isCreateMode ? '拠点情報の確認' : '拠点編集の確認'}
      title={isCreateMode ? '登録内容' : '更新内容'}
      description='以下の内容をご確認ください。'
      onBack={onBack}
      // backText={isCreateMode ? '拠点登録に戻る' : '拠点編集に戻る'}
      backText={isCreateMode ? '戻る' : '戻る'}
      onConfirm={handleSubmit}
      isSubmitting={isSubmitting}
    >
      <ConfirmationItem
        label='郵便番号'
        value={data.postalCode}
        originalValue={originalData.postalCode}
      />

      <ConfirmationItem
        label='都道府県'
        value={data.prefectureName}
        originalValue={originalData.prefectureName}
      />

      <ConfirmationItem
        label='市区町村'
        value={data.cityName}
        originalValue={originalData.cityName}
      />

      <ConfirmationItem
        label='町域'
        value={data.townName}
        originalValue={originalData.townName}
      />

      <ConfirmationItem
        label='番地'
        value={data.blockName || 'なし'}
        originalValue={originalData.blockName}
      />

      <ConfirmationItem
        label='建物名・部屋番号'
        value={data.addressDetail || 'なし'}
        originalValue={originalData.addressDetail}
      />

      {isCreateMode && (
        <>
          <ConfirmationItem
            label='拠点名'
            value={data.siteName}
            originalValue={originalData.siteName}
          />

          <ConfirmationItem
            label='拠点名（カナ）'
            value={data.siteNameKana}
            originalValue={originalData.siteNameKana}
          />

          <ConfirmationItem
            label='担当者（姓）'
            value={data.picLastName}
            originalValue={originalData.picLastName}
          />

          <ConfirmationItem
            label='担当者（姓カナ）'
            value={data.picLastNameKana}
            originalValue={originalData.picLastNameKana}
          />

          <ConfirmationItem
            label='担当者（名）'
            value={data.picFirstName}
            originalValue={originalData.picFirstName}
          />

          <ConfirmationItem
            label='担当者（名カナ）'
            value={data.picFirstNameKana}
            originalValue={originalData.picFirstNameKana}
          />

          <ConfirmationItem
            label='担当者メールアドレス'
            value={data.picEmail}
            originalValue={originalData.picEmail}
          />

          <ConfirmationItem
            label='担当者電話番号'
            value={data.picPhoneNumber}
            originalValue={originalData.picPhoneNumber}
          />

          <ConfirmationItem
            label='担当者役職'
            value={data.picPosition}
            originalValue={originalData.picPosition}
          />
        </>
      )}
    </ConfirmationLayout>
  );
}
