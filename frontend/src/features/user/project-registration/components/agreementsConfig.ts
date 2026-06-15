export type AgreementFieldType = 'checkbox' | 'radio' | 'text';

export interface AgreementField {
  id: string;
  label: string;
  type: AgreementFieldType;
  required: boolean;
  // For radio fields
  options?: Array<{
    value: string;
    label: string;
  }>;
  // Conditional visibility
  showIf?: {
    fieldId: string;
    value: string;
  };
  // For text inputs
  placeholder?: string;
  validation?: {
    pattern?: string;
    minLength?: number;
    maxLength?: number;
    message?: string;
  };
}

export const agreementsConfig: AgreementField[] = [
  {
    id: 'useRegisteredInfo',
    label: 'Ｊ－クレジット制度における各種申請に際し、本入会届に記載された情報を、プログラム型運営・管理者名が使用することに同意します。',
    type: 'checkbox',
    required: true,
  },
  {
    id: 'provideAdditionalInfo',
    label: 'Ｊ－クレジット制度における各種申請に際し、本入会届に記載された以外の情報について、プログラム型運営・管理者名が必要とする場合は提供することに同意します。',
    type: 'checkbox',
    required: true,
  },
  {
    id: 'transferEnvironmentalValue',
    label: '太陽光発電設備を使用することによる自家消費分についての環境価値 (温室効果ガス排出量の削減効果=Ｊ－クレジット) をプログラム型運営・管理者名へ譲渡すること、その結果として「太陽光発電設備を使用することで温室効果ガス排出量を削減」したことを主張できなくなることに同意します。',
    type: 'checkbox',
    required: true,
  },
  {
    id: 'notRegisteredElsewhere',
    label: '「プログラム名」に登録する太陽光発電設備は、他の類似制度及びＪ－クレジット制度における他のプロジェクトのいずれにおいても登録していません。',
    type: 'checkbox',
    required: true,
  },
  {
    id: 'keidanrenParticipation',
    label: '日本経済団体連合会における経団連カーボンニュートラル行動計画への参加については以下の通りです。',
    type: 'radio',
    required: true,
    options: [
      { value: 'participating', label: '参加しています' },
      { value: 'notParticipating', label: '参加していません' },
    ],
  },
  {
    id: 'specifiedEmitterStatus',
    label: '「地球温暖化対策の推進に関する法律」に基づく「算定・報告・公表制度」において、貴社は対象事業者に該当しますか？',
    type: 'radio',
    required: true,
    options: [
      { value: 'isSpecifiedEmitter', label: '特定排出者に該当します' },
      { value: 'notSpecifiedEmitter', label: '特定排出者ではありません' },
    ],
  },
  {
    id: 'emissionAdditionAgreement',
    label: '特定排出者に該当する場合、プログラム型運営・管理者へ譲渡した環境価値 (Ｊ－クレジット) に相当する排出量を、当該年度の調整後排出量の計算において加算することに同意します。',
    type: 'checkbox',
    required: true,
    showIf: {
      fieldId: 'specifiedEmitterStatus',
      value: 'isSpecifiedEmitter',
    },
  },
  {
    id: 'specifiedEmitterCode',
    label: '特定排出者コード (9ケタの値)',
    type: 'text',
    required: true,
    placeholder: '例: 123456789',
    showIf: {
      fieldId: 'specifiedEmitterStatus',
      value: 'isSpecifiedEmitter',
    },
    validation: {
      pattern: '^[0-9]{9}$',
      minLength: 9,
      maxLength: 9,
      message: '9桁の数字を入力してください',
    },
  },
  {
    id: 'energyRationalizationCompliance',
    label: '「エネルギーの使用の合理化に関する法律」に基づく定期報告の対象者に該当しますか？',
    type: 'radio',
    required: true,
    options: [
      { value: 'applicable', label: '対象者に該当します' },
      { value: 'notApplicable', label: '対象者に該当しません' },
    ],
  },
  {
    id: 'energyLawType',
    label: '種別',
    type: 'radio',
    required: true,
    options: [
      { value: 'type1', label: '一種' },
      { value: 'type2', label: '二種' },
    ],
    showIf: {
      fieldId: 'energyRationalizationCompliance',
      value: 'applicable',
    },
  },
  {
    id: 'energyLawBusinessNumber',
    label: '省エネ法特定事業者番号又は特定連鎖化事業者番号 (7ケタの値)',
    type: 'text',
    required: true,
    placeholder: '例: 1234567',
    showIf: {
      fieldId: 'energyRationalizationCompliance',
      value: 'applicable',
    },
    validation: {
      pattern: '^[0-9]{7}$',
      minLength: 7,
      maxLength: 7,
      message: '7桁の数字を入力してください',
    },
  },
  {
    id: 'energyLawFactoryNumber',
    label: '省エネ法指定工場番号 (7ケタの値)',
    type: 'text',
    required: true,
    placeholder: '例: 1234567',
    showIf: {
      fieldId: 'energyRationalizationCompliance',
      value: 'applicable',
    },
    validation: {
      pattern: '^[0-9]{7}$',
      minLength: 7,
      maxLength: 7,
      message: '7桁の数字を入力してください',
    },
  },
];

// Type for the form values
export type AgreementFormValues = {
  useRegisteredInfo: boolean;
  provideAdditionalInfo: boolean;
  transferEnvironmentalValue: boolean;
  notRegisteredElsewhere: boolean;
  keidanrenParticipation: string;
  specifiedEmitterStatus: string;
  energyRationalizationCompliance: string;
  emissionAdditionAgreement?: boolean;
  specifiedEmitterCode?: string;
  energyLawType?: string;
  energyLawBusinessNumber?: string;
  energyLawFactoryNumber?: string;
};

// Helper function to check if a field should be visible
export const shouldShowField = (
  field: AgreementField,
  formValues: Partial<AgreementFormValues>
): boolean => {
  if (!field.showIf) return true;
  
  const dependentValue = formValues[field.showIf.fieldId as keyof AgreementFormValues];
  return dependentValue === field.showIf.value;
};

// Validation helper
export const validateField = (
  field: AgreementField,
  value: string | boolean | undefined
): { isValid: boolean; message?: string } => {
  // Check required fields
  if (field.required && !value) {
    return { isValid: false, message: 'この項目は必須です' };
  }

  // Validate text inputs
  if (field.type === 'text' && typeof value === 'string' && field.validation) {
    if (field.validation.pattern && !new RegExp(field.validation.pattern).test(value)) {
      return { isValid: false, message: field.validation.message || '入力形式が正しくありません' };
    }
    if (field.validation.minLength && value.length < field.validation.minLength) {
      return { isValid: false, message: `${field.validation.minLength}文字以上入力してください` };
    }
    if (field.validation.maxLength && value.length > field.validation.maxLength) {
      return { isValid: false, message: `${field.validation.maxLength}文字以内で入力してください` };
    }
  }

  return { isValid: true };
};
