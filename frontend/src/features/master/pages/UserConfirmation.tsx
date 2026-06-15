import { UserFormData } from '@/features/master/forms/UserForm';
import { useState } from 'react';
import { useDashboardStore } from '@/stores/dashboardStore';
import { ConfirmationItem } from '@/components/feedback/Confirmation/ConfirmationItem';
import { ConfirmationLayout } from '@/components/feedback/Confirmation/ConfirmationLayout';

interface UserConfirmationProps {
    data: UserFormData;
    originalData: UserFormData;
    onBack: () => void;
    onSubmit: () => void;
}

export function UserConfirmation({ data, originalData, onBack, onSubmit }: Readonly<UserConfirmationProps>) {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const getSiteById = useDashboardStore(state => state.getSiteById);
    if (!data) return <div>読み込み中...</div>;

    const siteName = getSiteById(data?.siteId)?.siteName; //adjust so that if Master user, it should display 
    const originalSiteName = getSiteById(originalData?.siteId)?.siteName;

    const handleConfirm = async () => {
        setIsSubmitting(true);
        await onSubmit();
        setIsSubmitting(false);
    };

    return (

        <ConfirmationLayout
            pageName="拠点情報の確認"
            title="ユーザ内容"
            description="以下の内容をご確認ください。"
            onBack={onBack}
            // backText="ユーザ登録に戻る"
            backText="戻る"
            onConfirm={handleConfirm}
            isSubmitting={isSubmitting}
        >
            <ConfirmationItem label="ユーザー名" value={data.username} originalValue={originalData.username} />
            <ConfirmationItem label="姓" value={data.lastName} originalValue={originalData.lastName} />
            <ConfirmationItem label="名" value={data.firstName} originalValue={originalData.firstName} />
            <ConfirmationItem label="セイ" value={data.lastNameKana} originalValue={originalData.lastNameKana} />
            <ConfirmationItem label="メイ" value={data.firstNameKana} originalValue={originalData.firstNameKana} />
            <ConfirmationItem label="役職" value={data.userPosition} originalValue={originalData.userPosition} />
            <ConfirmationItem label="電話番号" value={data.phoneNumber} originalValue={originalData.phoneNumber} />

            {siteName && (
                <ConfirmationItem label="担当する拠点" value={siteName} originalValue={originalSiteName} />
            )}

            <ConfirmationItem label="メールアドレス" value={data.email} originalValue={originalData.email} />
        </ConfirmationLayout>

    );
}

export default UserConfirmation;
