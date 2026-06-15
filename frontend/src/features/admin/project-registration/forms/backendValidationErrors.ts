import type { FieldPath, UseFormReturn } from 'react-hook-form';
import { ApiError } from '@/api/_shared/errors';
import type { AdminProjectRegisterFormInputValues } from './projectFormTypes';

type AdminProjectForm = UseFormReturn<AdminProjectRegisterFormInputValues>;
type AdminProjectFieldPath = FieldPath<AdminProjectRegisterFormInputValues>;

const PROJECT_BASE_FIELDS = new Set([
  'projectName',
  'projectNumber',
  'projectStatus',
  'baselineEmissions',
  'postProjectEmission',
  'repFirstName',
  'repFirstNameKana',
  'repLastName',
  'repLastNameKana',
  'repEmail',
  'repPhoneNumber',
  'repPosition',
  'picFirstName',
  'picFirstNameKana',
  'picLastName',
  'picLastNameKana',
  'picEmail',
  'picPhoneNumber',
  'picPosition',
  'picDataManagement',
  'dataManagementPic',
  'regionalCategory',
  'newMembersTotal',
  'withdrawnMembers',
  'cumulativeMembers',
  'membersTotal',
  'methodologyVersion',
  'methodCategory',
  'methodNumber',
  'methodName',
  'methodMainEmission',
  'methodCombiFeasibility',
  'methodDetails',
  'operatorCompanyId',
]);

const PROJECT_BASE_ALIASES: Record<string, AdminProjectFieldPath> = {
  dataManagementPic: 'projectBase.picDataManagement',
};

function toMessage(value: unknown): string {
  if (Array.isArray(value)) {
    return value.map(toMessage).filter(Boolean).join('\n');
  }

  if (value && typeof value === 'object' && 'message' in value) {
    return toMessage((value as { message: unknown }).message);
  }

  return typeof value === 'string' ? value : '';
}

function toFieldPath(field: string): AdminProjectFieldPath | null {
  const normalized = field
    .replace(/^\$?\./, '')
    .replace(/^project\.0\./, '')
    .replace(/^project\[\d+\]\./, '')
    .replace(/^project\./, '');

  if (normalized.startsWith('projectBase.') || normalized.startsWith('projectApplication.')) {
    return normalized as AdminProjectFieldPath;
  }

  if (PROJECT_BASE_ALIASES[normalized]) {
    return PROJECT_BASE_ALIASES[normalized];
  }

  if (PROJECT_BASE_FIELDS.has(normalized)) {
    return `projectBase.${normalized}` as AdminProjectFieldPath;
  }

  return `projectApplication.${normalized}` as AdminProjectFieldPath;
}

function applyRecordErrors(
  form: AdminProjectForm,
  errors: Record<string, unknown>,
): boolean {
  let applied = false;

  Object.entries(errors).forEach(([field, value]) => {
    const message = toMessage(value);
    const path = toFieldPath(field);

    if (!message || !path) return;

    form.setError(path, {
      type: 'server',
      message,
    });
    applied = true;
  });

  return applied;
}

function applyArrayErrors(
  form: AdminProjectForm,
  errors: unknown[],
): boolean {
  let applied = false;

  errors.forEach((item) => {
    if (!item || typeof item !== 'object') return;

    const error = item as {
      field?: unknown;
      path?: unknown;
      property?: unknown;
      message?: unknown;
      messages?: unknown;
    };
    const field = error.field ?? error.path ?? error.property;

    if (typeof field !== 'string') return;

    const path = toFieldPath(field);
    const message = toMessage(error.message ?? error.messages);

    if (!message || !path) return;

    form.setError(path, {
      type: 'server',
      message,
    });
    applied = true;
  });

  return applied;
}

export function applyBackendValidationErrors(
  form: AdminProjectForm,
  error: unknown,
): string | null {
  if (!(error instanceof ApiError)) {
    return error instanceof Error ? error.message : null;
  }

  const errors = error.payload?.errors;
  let applied = false;

  if (Array.isArray(errors)) {
    applied = applyArrayErrors(form, errors);
  } else if (errors && typeof errors === 'object') {
    applied = applyRecordErrors(form, errors as Record<string, unknown>);
  }

  return applied ? null : error.message;
}
