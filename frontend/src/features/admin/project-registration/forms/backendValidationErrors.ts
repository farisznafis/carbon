import type { FieldPath, UseFormReturn } from 'react-hook-form';
import { ApiError } from '@/api/_shared/errors';
import { logger } from '@/lib/logger';
import type { AdminProjectRegisterFormInputValues } from './projectFormTypes';

type AdminProjectForm = UseFormReturn<AdminProjectRegisterFormInputValues>;
type AdminProjectFieldPath = FieldPath<AdminProjectRegisterFormInputValues>;

const PROJECT_BASE_FIELDS = new Set([
  'projectName',
  'projectNumber',
  'projectStatus',
  'startedAt',
  'usePurpose',
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

/**
 * The form field a backend field name belongs to, or null when the form has no
 * such field. Only the project record is a form here; an error about anything
 * else is reported through the message alone.
 */
function toFieldPath(field: string): AdminProjectFieldPath | null {
  const normalized = field
    .replace(/^\$?\./, '')
    .replace(/^project\.0\./, '')
    .replace(/^project\[\d+\]\./, '')
    .replace(/^project\./, '');

  if (normalized.startsWith('projectBase.')) {
    return normalized as AdminProjectFieldPath;
  }

  if (PROJECT_BASE_ALIASES[normalized]) {
    return PROJECT_BASE_ALIASES[normalized];
  }

  if (PROJECT_BASE_FIELDS.has(normalized)) {
    return `projectBase.${normalized}` as AdminProjectFieldPath;
  }

  return null;
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

/**
 * Marks the fields a rejected save complained about and returns its message.
 */
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

  // The message is returned even when fields were marked: the section holding
  // them may be scrolled out of view, and a toast alone disappears.
  if (!applied) {
    logger.warn('Backend validation errors matched no form field', errors);
  }
  return error.message;
}
