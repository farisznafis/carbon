import type {
  SignupRevisionRequest,
  SignupRevisionResponse,
} from '@/api/company';
import type { SignupData } from '@/contexts/SignupContext';

export type SignupRevisionStep = 1 | 2 | 3;

export type SignupRevisionFieldErrors = Partial<
  Record<keyof SignupData | 'emailVerificationChallengeId', string>
>;

const API_TO_FORM_FIELD: Record<string, keyof SignupData | 'emailVerificationChallengeId'> = {
  emailVerificationChallengeId: 'emailVerificationChallengeId',
  corporateNumber: 'corporateNumber',
  corporateName: 'companyName',
  corporateNameKana: 'companyNameKana',
  companyNameEnglish: 'companyNameEnglish',
  corporatePhoneNumber: 'corporatePhoneNumber',
  postalCode: 'postalCode',
  prefecture: 'prefecture',
  municipality: 'municipality',
  blocknum: 'blocknum',
  buildingName: 'buildingName',
  lastName: 'lastName',
  firstName: 'firstName',
  lastNameKana: 'lastNameKana',
  firstNameKana: 'firstNameKana',
  position: 'position',
  phoneNumber: 'phoneNumber',
  faxNumber: 'faxNumber',
  email: 'email',
  methodology: 'methodology',
  activityInformation: 'activityInformation',
};

const METHODOLOGY_FIELDS = new Set<keyof SignupData>([
  'methodology',
  'activityInformation',
  'activityHas',
]);

function normalizeBackendFieldName(fieldName: string) {
  return fieldName
    .replaceAll('request::', '')
    .replaceAll('revision::', '')
    .replace(/^.*\./, '')
    .replace(/\[\d+\]$/, '');
}

function errorMessage(value: unknown): string | null {
  if (typeof value === 'string') return value;
  if (Array.isArray(value)) {
    const messages = value
      .map(errorMessage)
      .filter((message): message is string => Boolean(message));
    return messages.length > 0 ? messages.join('、') : null;
  }
  if (!value || typeof value !== 'object') return null;

  const record = value as Record<string, unknown>;
  return errorMessage(record.message) ?? errorMessage(record.constraints);
}

export function mapSignupRevisionToFormData(
  response: SignupRevisionResponse,
): SignupData {
  const hasActivity = response.activityInformation !== '-';

  return {
    corporateNumber: response.corporateNumber ?? '',
    companyName: response.corporateName ?? '',
    companyNameKana: response.corporateNameKana ?? '',
    companyNameEnglish: response.companyNameEnglish ?? '',
    corporateEmail: '',
    corporatePhoneNumber: response.corporatePhoneNumber ?? '',
    postalCode: response.postalCode ?? '',
    prefecture: response.prefecture ?? '',
    municipality: response.municipality ?? '',
    buildingName: response.buildingName ?? '',
    blocknum: response.blocknum ?? '',
    lastName: response.lastName ?? '',
    firstName: response.firstName ?? '',
    lastNameKana: response.lastNameKana ?? '',
    firstNameKana: response.firstNameKana ?? '',
    position: response.position ?? '',
    phoneNumber: response.phoneNumber ?? '',
    faxNumber: response.faxNumber ?? '',
    email: response.email ?? '',
    methodology: Array.isArray(response.methodology)
      ? response.methodology
      : [],
    activityHas: hasActivity ? 'yes' : 'no',
    activityInformation: hasActivity
      ? response.activityInformation ?? ''
      : '-',
  };
}

export function mapFormDataToSignupRevision(
  data: SignupData,
  emailVerificationChallengeId: string | null,
): SignupRevisionRequest {
  return {
    ...(emailVerificationChallengeId
      ? { emailVerificationChallengeId }
      : {}),
    corporateNumber: data.corporateNumber,
    corporateName: data.companyName,
    corporateNameKana: data.companyNameKana,
    companyNameEnglish: data.companyNameEnglish,
    corporatePhoneNumber: data.corporatePhoneNumber,
    postalCode: data.postalCode,
    prefecture: data.prefecture,
    municipality: data.municipality,
    blocknum: data.blocknum,
    buildingName: data.buildingName,
    lastName: data.lastName,
    firstName: data.firstName,
    lastNameKana: data.lastNameKana,
    firstNameKana: data.firstNameKana,
    position: data.position,
    phoneNumber: data.phoneNumber,
    faxNumber: data.faxNumber,
    email: data.email,
    methodology: data.methodology,
    activityInformation:
      data.activityHas === 'no' ? '-' : data.activityInformation.trim(),
  };
}

export function mapSignupRevisionFieldErrors(
  errors: unknown,
): SignupRevisionFieldErrors {
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) return {};

  return Object.entries(errors as Record<string, unknown>).reduce<
    SignupRevisionFieldErrors
  >((mappedErrors, [backendField, value]) => {
    const normalizedField = normalizeBackendFieldName(backendField);
    const formField = API_TO_FORM_FIELD[normalizedField];
    const message = errorMessage(value);
    if (formField && message && !mappedErrors[formField]) {
      mappedErrors[formField] = message;
    }
    return mappedErrors;
  }, {});
}

export function getSignupRevisionErrorStep(
  errors: SignupRevisionFieldErrors,
): SignupRevisionStep | null {
  const fields = Object.keys(errors) as Array<keyof SignupRevisionFieldErrors>;
  if (fields.some((field) => field === 'emailVerificationChallengeId')) {
    return null;
  }
  const formFields = fields.filter(
    (field): field is keyof SignupData => field !== 'emailVerificationChallengeId',
  );
  if (formFields.some((field) => !METHODOLOGY_FIELDS.has(field))) return 1;
  return formFields.length > 0 ? 2 : null;
}
