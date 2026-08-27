import { ApiError } from '@/api/_shared/errors';
import type {
  AdminOperator,
  CreateAdminOperatorPayload,
  UpdateAdminOperatorPayload,
} from '@/api/admin/operators';
import type { UserFormData } from '@/api/users';

export const EMPTY_OPERATOR_VALUES: UserFormData = {
  username: '',
  lastName: '',
  firstName: '',
  lastNameKana: '',
  firstNameKana: '',
  userPosition: '',
  phoneNumber: '',
  siteId: '',
  email: '',
  password: '',
};

/** Extracts one operator field message from a structured backend validation error. */
export function getOperatorFieldError(
  error: unknown,
  fieldName: string,
): string | null {
  if (!(error instanceof ApiError)) return null;
  const errors = error.payload?.errors;
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) return null;
  const fieldError = errors[fieldName];
  return typeof fieldError === 'string' ? fieldError : null;
}

/** Maps shared user-form fields into the common operator request fields. */
function mapOperatorRequestFields(
  values: UserFormData,
  permissionCodes: string[],
): UpdateAdminOperatorPayload {
  return {
    username: values.username.trim(),
    firstName: values.firstName.trim(),
    firstNameKana: values.firstNameKana.trim(),
    lastName: values.lastName.trim(),
    lastNameKana: values.lastNameKana.trim(),
    email: values.email.trim(),
    phoneNumber: String(values.phoneNumber ?? '').replace(/-/g, ''),
    permissionCodes,
  };
}

/** Maps the shared user form into POST /admin/companies/operator-user payload. */
export function mapOperatorCreatePayload(
  values: UserFormData,
  permissionCodes: string[],
): CreateAdminOperatorPayload {
  return {
    ...mapOperatorRequestFields(values, permissionCodes),
    password: values.password ?? '',
    role: 'operator',
  };
}

/** Maps the shared user form into PATCH /admin/companies/operator-user/{userId} payload. */
export function mapOperatorUpdatePayload(
  values: UserFormData,
  permissionCodes: string[],
): UpdateAdminOperatorPayload {
  return mapOperatorRequestFields(values, permissionCodes);
}

/** Converts an operator API record into editable shared user-form values. */
export function mapOperatorToFormValues(
  operator: AdminOperator,
): UserFormData {
  return {
    username: operator.username,
    lastName: operator.lastName,
    firstName: operator.firstName,
    lastNameKana: operator.lastNameKana,
    firstNameKana: operator.firstNameKana,
    userPosition: operator.userPosition,
    phoneNumber: operator.phoneNumber,
    siteId: operator.siteId ?? '',
    email: operator.email,
  };
}
