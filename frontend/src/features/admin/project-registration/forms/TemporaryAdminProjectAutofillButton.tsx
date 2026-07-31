import type { UseFormReturn } from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { type AdminProjectRegisterFormInputValues } from './projectFormTypes';
import {
  MethodCategorySchema,
  ProjectStatusSchema,
  RegionalCategorySchema,
} from './projectBaseSchema';
import {
  APPLICATION_CATEGORY_OPTIONS,
  LOCATION_OPTIONS,
} from './AdminProjectApplicationForms/project/ProjectApplication/projectApplicationSchema';
import { METHOD_OPTIONS } from '@/lib/constants/methodology';
import { PREFECTURE_OPTIONS } from '@/lib/constants/prefectures';
import { REGIONAL_OPTIONS } from '@/lib/constants/regionals';
import { REVIEWER_NAME_OPTIONS } from '@/lib/constants/reviewer_names';
import {
  IS_DESIGNATED_EMITTER_OPTIONS,
  SELECT_TYPE_REQUIRED_OPTIONS,
} from '@/lib/constants/select_types';

type TemporaryAdminProjectAutofillButtonProps = {
  form: UseFormReturn<AdminProjectRegisterFormInputValues>;
};

type ProjectApplicationAutofillValues = NonNullable<
  AdminProjectRegisterFormInputValues['projectApplication']
>;

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
  projectApplication: {
    applicationCategory: APPLICATION_CATEGORY_OPTIONS[1],
    isUsingCostSupport: SELECT_TYPE_REQUIRED_OPTIONS[1],
    projectOperatorKana: 'トウキョウエナジーカブシキガイシャ',
    projectOperatorJa: '東京エネルギー株式会社',
    projectOperatorEn: 'Tokyo Energy Co., Ltd.',
    corpNumber: '1234567890123',
    corpPosition: '代表取締役',
    corpName: '山田 太郎',
    prefecture: PREFECTURE_OPTIONS[12],
    city: '千代田区',
    address: '丸の内1-1',
    isDesignatedEmitter: IS_DESIGNATED_EMITTER_OPTIONS[1],
    emitterCode: '123456789',
    hasEnergyActPeriodReport: false,
    energyActOpeNumber: '123456789',
    energyManageFactoryNumber: '1234567890',
    representativePosition: '代表取締役',
    isChangeRepresentativePosition: SELECT_TYPE_REQUIRED_OPTIONS[1],
    representativeName: '山田 太郎',
    isChangeRepresentativeName: SELECT_TYPE_REQUIRED_OPTIONS[1],
    picPosition: '事業担当',
    isChangePicPosition: SELECT_TYPE_REQUIRED_OPTIONS[1],
    picName: '佐藤 花子',
    isChangePicName: SELECT_TYPE_REQUIRED_OPTIONS[1],
    picEmail: 'hanako.sato@example.com',
    isChangePicEmail: SELECT_TYPE_REQUIRED_OPTIONS[1],
    picPhone: '0312345678',
    isChangePicPhone: SELECT_TYPE_REQUIRED_OPTIONS[1],
    otherContactBusinessName: '東京エネルギー株式会社',
    isChangeOtherContactBusinessName: SELECT_TYPE_REQUIRED_OPTIONS[1],
    otherContactPicPosition: '管理担当',
    isChangeOtherContactPicPosition: SELECT_TYPE_REQUIRED_OPTIONS[1],
    otherContactPicName: '鈴木 一郎',
    isChangeOtherContactPicName: SELECT_TYPE_REQUIRED_OPTIONS[1],
    otherContactPicEmail: 'ichiro.suzuki@example.com',
    isChangeOtherContactPicEmail: SELECT_TYPE_REQUIRED_OPTIONS[1],
    otherContactPicPhone: '0312345678',
    isChangeOtherContactPicPhone: SELECT_TYPE_REQUIRED_OPTIONS[1],
    projectNumber: 'P10001',
    projectName: '太陽光発電設備導入プロジェクト',
    methodologyCategory: METHOD_OPTIONS[0],
    methodologyNumber: '002',
    methodology: '太陽光発電設備の導入',
    location: LOCATION_OPTIONS.options[1],
    SaaS: 'Review Agency A',
    projectOutline: '太陽光発電設備を導入し、電力使用に伴う排出量を削減する。',
    regionCategory: REGIONAL_OPTIONS[2],
    groupName: '東京エネルギーグループ',
    groupNameFurigana: 'トウキョウエナジーグループ',
    isParticipatingCnAct: false,
    validationCheckReviewerName: REVIEWER_NAME_OPTIONS[0],
    // applicationDate: '2026-06-03',
    // registrationDate: '2026-06-03',
    reviewerName: REVIEWER_NAME_OPTIONS[0],
    // applicationChangeDate: '2026-06-03',
  },
} satisfies AdminProjectRegisterFormInputValues;

const PROJECT_APPLICATION_EXCLUDED_AUTOFILL_FIELDS = new Set<
  keyof ProjectApplicationAutofillValues
>([
  'corpNumber',
  'corpPosition',
  'corpName',
  'prefecture',
  'city',
  'address',
  'representativePosition',
  'representativeName',
  'picPosition',
  'projectName',
  'methodologyCategory',
  'methodologyNumber',
  'methodology',
]);

const PROJECT_APPLICATION_AUTOFILL_VALUES = Object.fromEntries(
  Object.entries(AUTOFILL_VALUES.projectApplication).filter(
    ([key]) =>
      !PROJECT_APPLICATION_EXCLUDED_AUTOFILL_FIELDS.has(
        key as keyof ProjectApplicationAutofillValues,
      ),
  ),
) as Partial<ProjectApplicationAutofillValues>;

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

export function TemporaryAdminProjectApplicationAutofillButton({
  form,
}: Readonly<TemporaryAdminProjectAutofillButtonProps>) {
  const handleAutofill = () => {
    const currentProjectApplication = form.getValues('projectApplication') ?? {};

    form.setValue(
      'projectApplication',
      {
        ...currentProjectApplication,
        ...PROJECT_APPLICATION_AUTOFILL_VALUES,
      },
      {
        shouldDirty: true,
        shouldTouch: true,
        shouldValidate: true,
      },
    );
    form.clearErrors('projectApplication');
  };

  return (
    <Button
      type='button'
      variant='outline'
      onClick={handleAutofill}
    >
      申請情報自動入力
    </Button>
  );
}
