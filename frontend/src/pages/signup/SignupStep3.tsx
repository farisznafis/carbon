import { signUp, type SignUpData } from '@/api/auth';
import {
  FormActions,
  FormCard,
  FormGrid,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { Button } from '@/components/ui/button';
import { useSignup, type SignupData } from '@/contexts/SignupContext';
import { useToast } from '@/hooks/use-toast';
import { getErrorToastContent, parseError } from '@/lib/errors';
import { useState } from 'react';
import { participationMethodologies } from './signupStep2Model';

const methodologyLabels = Object.fromEntries(
  participationMethodologies.map((methodology) => [methodology.value, methodology.label]),
);

const missingValue = '未入力';

function getSelectedMethodologies(signupData: SignupData): string[] {
  return signupData.methodology;
}

function buildSignUpPayload(signupData: SignupData): SignUpData {
  const activityInformation = signupData.activityInformation || '';

  return {
    corporateNumber: signupData.corporateNumber,
    corporateName: signupData.companyName,
    corporateNameKana: signupData.companyNameKana,
    companyNameEnglish: signupData.companyNameEnglish,
    corporateEmail: signupData.corporateEmail,
    corporatePhoneNumber: signupData.corporatePhoneNumber,
    postalCode: signupData.postalCode,
    prefecture: signupData.prefecture,
    municipality: signupData.municipality,
    buildingName: signupData.buildingName,
    blocknum: signupData.blocknum,
    lastName: signupData.lastName,
    firstName: signupData.firstName,
    lastNameKana: signupData.lastNameKana,
    firstNameKana: signupData.firstNameKana,
    position: signupData.position,
    phoneNumber: signupData.phoneNumber,
    faxNumber: signupData.faxNumber,
    email: signupData.email,
    methodology: getSelectedMethodologies(signupData),
    activityInformation,
  };
}

function DetailRow({
  label,
  value,
}: {
  label: string;
  value?: string;
}) {
  return (
    <div>
      <div className="mb-1 text-sm font-medium text-foreground">
        {label}
      </div>
      <div className="text-foreground">{value || missingValue}</div>
    </div>
  );
}

type SignupStep3Props = {
  data?: SignupData;
  onBack?: () => void;
  onConfirm?: () => Promise<void> | void;
  title?: string;
  description?: string;
  confirmLabel?: string;
};

export const SignupStep3 = ({
  data,
  onBack,
  onConfirm,
  title = '入力内容の確認',
  description = '登録を完了する前に、入力内容を確認してください。',
  confirmLabel = '確認して登録',
}: SignupStep3Props = {}) => {
  const { signupData, setCurrentStep } = useSignup();
  const { toast } = useToast();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const reviewData = data ?? signupData;
  const selectedMethodologies = getSelectedMethodologies(reviewData);

  const handleConfirm = async () => {
    if (isSubmitting) return;

    setIsSubmitting(true);
    try {
      if (onConfirm) {
        await onConfirm();
        return;
      }

      const res = await signUp(buildSignUpPayload(reviewData));

      setCurrentStep(4);
      toast?.({
        title: '登録しました',
        description: res.message || 'Success',
      });
    } catch (err) {
      const apiError = parseError(err);
      const errorToast = getErrorToastContent(apiError);

      const duplicateEmail =
        apiError.statusCode === 'DUPLICATE_EMAIL' ||
        apiError.message.toLowerCase().includes('email');

      const duplicateCompanyNumber =
        apiError.statusCode === 'DUPLICATE_COMPANY_NUMBER' ||
        apiError.message.toLowerCase().includes('corporate number');

      toast?.({
        title: errorToast.title,
        description: duplicateEmail
          ? '法人メールアドレスはすでに使用されています。'
          : duplicateCompanyNumber
            ? '法人番号はすでに使用されています。'
            : errorToast.description,
        variant: 'destructive',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <FormCard>
      <FormSectionStack>
        <div className="text-center">
          <h3 className="mb-2 text-2xl font-bold">{title}</h3>
          <p className="text-foreground">{description}</p>
        </div>

        <FormSection number={1} title="法人情報">
          <FormGrid>
            <DetailRow label="法人番号" value={reviewData.corporateNumber} />
            <DetailRow label="法人名" value={reviewData.companyName} />
            <DetailRow label="法人名カナ" value={reviewData.companyNameKana} />
            <DetailRow label="会社名（英語）" value={reviewData.companyNameEnglish} />
            <DetailRow label="法人電話番号" value={reviewData.corporatePhoneNumber} />
          </FormGrid>
        </FormSection>

        <FormSection number={2} title="所在地">
          <FormGrid>
            <DetailRow label="郵便番号" value={reviewData.postalCode} />
            <DetailRow label="都道府県" value={reviewData.prefecture} />
            <DetailRow label="市区町村" value={reviewData.municipality} />
            <DetailRow label="番地" value={reviewData.blocknum} />
            <DetailRow label="建物名" value={reviewData.buildingName} />
          </FormGrid>
        </FormSection>

        <FormSection number={3} title="責任者情報">
          <FormGrid>
            <DetailRow label="姓" value={reviewData.lastName} />
            <DetailRow label="名" value={reviewData.firstName} />
            <DetailRow label="姓（カナ）" value={reviewData.lastNameKana} />
            <DetailRow label="名（カナ）" value={reviewData.firstNameKana} />
            <DetailRow label="役職" value={reviewData.position} />
            <DetailRow label="電話番号" value={reviewData.phoneNumber} />
            <DetailRow label="FAX番号" value={reviewData.faxNumber} />
            <DetailRow label="メールアドレス" value={reviewData.email} />
          </FormGrid>
        </FormSection>

        <FormSection number={4} title="方法論・活動情報">
          <div className="space-y-5">
            <div>
              <div className="mb-2 text-sm font-medium text-foreground">
                参加したいプロジェクト
              </div>
              <div className="space-y-2">
                {selectedMethodologies.length > 0 ? (
                  selectedMethodologies.map((methodology) => (
                    <div
                      key={methodology}
                      className="rounded-md border bg-muted/20 px-4 py-3 font-medium"
                    >
                      {methodologyLabels[methodology] ?? methodology}
                    </div>
                  ))
                ) : (
                  <p className="text-sm text-foreground">{missingValue}</p>
                )}
              </div>
            </div>

            <DetailRow
              label="現在の活動情報"
              value={reviewData.activityInformation}
            />
          </div>
        </FormSection>

        <FormActions
          pendingActionId={isSubmitting ? 'confirm' : null}
          actions={[
            {
              id: 'back',
              label: '戻る',
              variant: 'outline',
              onClick: onBack ?? (() => setCurrentStep(2)),
            },
            {
              id: 'confirm',
              label: confirmLabel,
              pendingLabel: '送信中...',
              onClick: handleConfirm,
            },
          ]}
        />
      </FormSectionStack>
    </FormCard>
  );
};
