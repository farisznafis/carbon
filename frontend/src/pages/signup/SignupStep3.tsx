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
      <div className="mb-1 text-sm font-medium text-muted-foreground">
        {label}
      </div>
      <div className="text-foreground">{value || missingValue}</div>
    </div>
  );
}

export const SignupStep3 = () => {
  const { signupData, setCurrentStep } = useSignup();
  const { toast } = useToast();
  const selectedMethodologies = getSelectedMethodologies(signupData);

  const handleConfirm = async () => {
    try {
      const res = await signUp(buildSignUpPayload(signupData));

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
    }
  };

  return (
    <FormCard>
      <FormSectionStack>
        <div className="text-center">
          <h3 className="mb-2 text-2xl font-semibold">入力内容の確認</h3>
          <p className="text-muted-foreground">
            登録を完了する前に、入力内容を確認してください。
          </p>
        </div>

        <FormSection number={1} title="法人情報">
          <FormGrid>
            <DetailRow label="法人番号" value={signupData.corporateNumber} />
            <DetailRow label="法人名" value={signupData.companyName} />
            <DetailRow label="法人名カナ" value={signupData.companyNameKana} />
            <DetailRow label="会社名（英語）" value={signupData.companyNameEnglish} />
            <DetailRow label="法人電話番号" value={signupData.corporatePhoneNumber} />
          </FormGrid>
        </FormSection>

        <FormSection number={2} title="所在地">
          <FormGrid>
            <DetailRow label="郵便番号" value={signupData.postalCode} />
            <DetailRow label="都道府県" value={signupData.prefecture} />
            <DetailRow label="市区町村" value={signupData.municipality} />
            <DetailRow label="番地" value={signupData.blocknum} />
            <DetailRow label="建物名" value={signupData.buildingName} />
          </FormGrid>
        </FormSection>

        <FormSection number={3} title="責任者情報">
          <FormGrid>
            <DetailRow label="姓" value={signupData.lastName} />
            <DetailRow label="名" value={signupData.firstName} />
            <DetailRow label="姓（カナ）" value={signupData.lastNameKana} />
            <DetailRow label="名（カナ）" value={signupData.firstNameKana} />
            <DetailRow label="役職" value={signupData.position} />
            <DetailRow label="電話番号" value={signupData.phoneNumber} />
            <DetailRow label="FAX番号" value={signupData.faxNumber} />
            <DetailRow label="メールアドレス" value={signupData.email} />
          </FormGrid>
        </FormSection>

        <FormSection number={4} title="方法論・活動情報">
          <div className="space-y-5">
            <div>
              <div className="mb-2 text-sm font-medium text-muted-foreground">
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
                  <p className="text-sm text-muted-foreground">{missingValue}</p>
                )}
              </div>
            </div>

            <DetailRow
              label="現在の活動情報"
              value={signupData.activityInformation}
            />
          </div>
        </FormSection>

        <FormActions className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Button type="button" variant="outline" size="sm" onClick={() => setCurrentStep(2)} className="w-full">
            戻る
          </Button>
          <Button type="button" size="sm" onClick={handleConfirm} className="w-full">
            確認して登録
          </Button>
        </FormActions>
      </FormSectionStack>
    </FormCard>
  );
};
