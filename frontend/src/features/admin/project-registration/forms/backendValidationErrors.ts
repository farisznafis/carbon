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

/**
 * The plan page an error belongs to.
 *
 * Without it, a bare field name can only be guessed at, and the guess is wrong
 * for every page past the second: pages carry names like `picEmail` that also
 * exist on the project record, so their errors landed on step 1's fields and
 * the operator saw a banner with nothing marked.
 */
export type PlanPageTarget = {
  /** Form key holding this page's values, e.g. `otherProjectOperator`. */
  formKey: string;
  /** Form field name -> backend field name, as the page declares it. */
  renames?: Readonly<Record<string, string>>;
};

function toFormName(backendName: string, renames?: Readonly<Record<string, string>>) {
  if (!renames) return backendName;
  const match = Object.entries(renames).find(([, backend]) => backend === backendName);
  return match ? match[0] : backendName;
}

function toFieldPath(
  field: string,
  page?: PlanPageTarget,
): AdminProjectFieldPath | null {
  const normalized = field
    .replace(/^\$?\./, '')
    .replace(/^project\.0\./, '')
    .replace(/^project\[\d+\]\./, '')
    .replace(/^project\./, '');

  if (normalized.startsWith('projectBase.') || normalized.startsWith('projectApplication.')) {
    return normalized as AdminProjectFieldPath;
  }

  // A named page wins over the project-record heuristics below: the backend
  // reports a bare field name, and several of those names exist on both.
  if (page) {
    const formName = toFormName(normalized, page.renames);
    return `${page.formKey}.${formName}` as AdminProjectFieldPath;
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
  page?: PlanPageTarget,
): boolean {
  let applied = false;

  Object.entries(errors).forEach(([field, value]) => {
    const message = toMessage(value);
    const path = toFieldPath(field, page);

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
  page?: PlanPageTarget,
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

    const path = toFieldPath(field, page);
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
 *
 * @param page the plan page being saved; omit for the project record itself
 */
export function applyBackendValidationErrors(
  form: AdminProjectForm,
  error: unknown,
  page?: PlanPageTarget,
): string | null {
  if (!(error instanceof ApiError)) {
    return error instanceof Error ? error.message : null;
  }

  const errors = error.payload?.errors;
  let applied = false;

  if (Array.isArray(errors)) {
    applied = applyArrayErrors(form, errors, page);
  } else if (errors && typeof errors === 'object') {
    applied = applyRecordErrors(form, errors as Record<string, unknown>, page);
  }

  // The message is returned even when fields were marked, because those
  // fields may belong to a step the operator is not looking at: a project
  // record rejected while they are several pages into the plan would
  // otherwise show nothing but a toast that disappears.
  if (!applied) {
    logger.warn('Backend validation errors matched no form field', errors);
  }
  return error.message;
}
