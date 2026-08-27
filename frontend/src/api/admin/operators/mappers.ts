import type {
  AdminOperator,
  AdminOperatorApiItem,
  AdminOperatorListApiResponse,
  AdminOperatorListResponse,
  AdminOperatorProfileApiResponse,
} from './types';

const RETIRED_OPERATOR_PERMISSION_CODES = new Set(['PROJECT_VIEW']);

/** Keeps non-empty API permissions while excluding retired operator permissions. */
function isActiveOperatorPermission(permission: unknown): permission is string {
  return (
    typeof permission === 'string' &&
    permission.trim().length > 0 &&
    !RETIRED_OPERATOR_PERMISSION_CODES.has(permission)
  );
}

/** Normalizes one operator record so UI formatters never depend on nullable API fields. */
export function mapAdminOperator(item: AdminOperatorApiItem): AdminOperator {
  const firstName = item.firstName?.trim() ?? '';
  const lastName = item.lastName?.trim() ?? '';

  return {
    id: item.id === null || item.id === undefined ? '' : String(item.id),
    username: item.username?.trim() ?? '',
    name: item.name?.trim() ?? [lastName, firstName].filter(Boolean).join(' '),
    role: item.role?.trim() ?? 'operator',
    email: item.email?.trim() ?? '',
    firstName,
    firstNameKana: item.firstNameKana?.trim() ?? '',
    lastName,
    lastNameKana: item.lastNameKana?.trim() ?? '',
    phoneNumber: item.phoneNumber?.trim() ?? '',
    userPosition: item.userPosition?.trim() ?? '',
    siteId:
      item.siteId === null || item.siteId === undefined
        ? null
        : String(item.siteId),
    siteName: item.siteName?.trim() ?? '',
    companyId: item.companyId?.trim() ?? '',
    permissions: Array.isArray(item.permissions)
      ? item.permissions.filter(isActiveOperatorPermission)
      : [],
  };
}

/** Normalizes GET /admin/companies/operator-user into a stable list response. */
export function mapAdminOperatorListResponse(
  response: AdminOperatorListApiResponse,
): AdminOperatorListResponse {
  return {
    statusCode: response.statusCode ?? 'OK',
    message: response.message ?? '',
    users: Array.isArray(response.users)
      ? response.users.map(mapAdminOperator).filter((operator) => operator.id)
      : [],
  };
}

/** Extracts the operator profile returned by the admin detail endpoint. */
export function mapAdminOperatorProfileResponse(
  response: AdminOperatorProfileApiResponse,
): AdminOperator {
  if (!response.profile) {
    throw new Error('運用者のプロファイルが見つかりません。');
  }

  return mapAdminOperator(response.profile);
}
