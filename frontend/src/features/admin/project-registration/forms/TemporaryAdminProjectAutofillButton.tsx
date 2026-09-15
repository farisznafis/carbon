import type { UseFormReturn } from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { type AdminProjectRegisterFormInputValues } from './projectFormTypes';
import {
  MethodCategorySchema,
  ProjectStatusSchema,
  RegionalCategorySchema,
} from './projectBaseSchema';

type TemporaryAdminProjectAutofillButtonProps = {
  form: UseFormReturn<AdminProjectRegisterFormInputValues>;
};

const AUTOFILL_VALUES = {
  projectBase: {
    projectName: '太陽光発電設備導入プロジェクト',
    projectNumber: 'P10001',
    projectStatus: ProjectStatusSchema.options[1],
    repFirstName: '太郎',
    repFirstNameKana: 'タロウ',
    repLastName: '山田',
    repLastNameKana: 'ヤマダ',
    repEmail: 'taro.yamada@example.com',
    repPhoneNumber: '0312345678',
    repPosition: '代表取締役',
    picFirstName: '花子',
    picFirstNameKana: 'ハナコ',
    picLastName: '佐藤',
    picLastNameKana: 'サトウ',
    picEmail: 'hanako.sato@example.com',
    picPhoneNumber: '0312345678',
    picPosition: '事業担当',
    picDataManagement: '管理担当',
    // emissionReduction: 1200,
    baselineEmissions: 3200,
    postProjectEmission: 2000,
    // companyName: '東京エネルギー株式会社',
    regionalCategory: RegionalCategorySchema.options[2],
    newMembersTotal: 10,
    withdrawnMembers: 0,
    cumulativeMembers: 10,
    membersTotal: 10,
    methodologyVersion: 'Ver.2.0',
    methodCategory: MethodCategorySchema.options[0],
    methodNumber: '150',
    methodName: '太陽光発電設備の導入',
    methodMainEmission: 'CO2',
    methodCombiFeasibility: false,
    methodDetails: '太陽光発電設備を導入し、系統電力由来の排出量を削減する。',
    operatorCompanyId: 'OP000000001',
  },
} satisfies AdminProjectRegisterFormInputValues;

export function TemporaryAdminProjectAutofillButton({
  form,
}: Readonly<TemporaryAdminProjectAutofillButtonProps>) {
  const handleAutofill = () => {
    form.setValue('projectBase', AUTOFILL_VALUES.projectBase, {
      shouldDirty: true,
      shouldTouch: true,
      shouldValidate: true,
    });
    form.clearErrors();
  };

  return (
    <Button
      type='button'
      variant='outline'
      onClick={handleAutofill}
    >
      自動入力
    </Button>
  );
}
