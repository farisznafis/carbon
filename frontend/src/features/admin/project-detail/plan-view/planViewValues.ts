import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import type { ProjectPlanConditionalField } from '@/api/admin/types';

/** Separates a table's name from a column inside its rows: `years[].startDate`. */
const ROW_MARKER = '[]';

type Condition = { field: string; value: unknown };

const EMPTY = '—';

/** Reads a dotted path (`projectOverview.corpNumber`) out of nested values. */
export function readPath(values: unknown, path: string): unknown {
  return path.split('.').reduce<unknown>((current, key) => {
    if (!current || typeof current !== 'object' || Array.isArray(current)) return undefined;
    return (current as Record<string, unknown>)[key];
  }, values);
}

/**
 * The text shown for one stored value.
 *
 * Select values are shown by their label, because what is stored is often a
 * code or a boolean the operator never saw. A boolean with no options came from
 * a checkbox.
 */
export function formatPlanValue(field: FieldConfig, value: unknown): string {
  if (value == null || value === '') return EMPTY;

  const option = field.options?.find((item) => Object.is(item.value, value)) ??
    field.options?.find((item) => String(item.value) === String(value));
  if (option) return option.label;

  if (typeof value === 'boolean') return value ? 'はい' : 'いいえ';

  if (Array.isArray(value)) {
    return value.length > 0 ? value.map(String).join('、') : EMPTY;
  }

  return String(value);
}

function asConditions(rule: FieldConfig['showWhen']): Condition[] {
  if (!rule) return [];
  return (Array.isArray(rule) ? rule : [rule]) as Condition[];
}

function matches(condition: Condition, read: (path: string) => unknown) {
  const actual = read(condition.field);
  return Array.isArray(condition.value)
    ? condition.value.includes(actual)
    : actual === condition.value;
}

/**
 * Whether the wizard would show this field for these values.
 *
 * Mirrors `DynamicFormRenderer`: a field the operator was never shown holds
 * nothing meaningful, so listing it here as 「—」 would suggest they skipped it.
 */
export function isShownFor(field: FieldConfig, read: (path: string) => unknown): boolean {
  const hide = asConditions(field.hideWhen);
  if (hide.length > 0 && hide.every((condition) => matches(condition, read))) {
    return false;
  }

  const show = asConditions(field.showWhen);
  return show.every((condition) => matches(condition, read));
}


/**
 * Whether the page's 有/無 rules let this field apply.
 *
 * Same rule as the wizard (`MultiStepForm.applies`): rules are keyed by bare
 * form field name (translate them with `rulesInFormNames` from
 * `projectPlanPages`), the toggle is read under the
 * field's own namespace, and an unanswered toggle hides nothing.
 */
export function appliesUnderRules(
  fieldName: string,
  rules: ReadonlyArray<ProjectPlanConditionalField>,
  read: (path: string) => unknown,
): boolean {
  const bare = fieldName.split('.').pop() ?? '';
  const rule = rules.find((candidate) => candidate.field === bare);
  if (!rule) return true;

  const prefix = fieldName.slice(0, fieldName.length - bare.length);
  const toggle = read(`${prefix}${rule.when}`);
  if (typeof toggle !== 'boolean') return true;
  return toggle === rule.is;
}

/**
 * Whether a table cell applies to its row.
 *
 * A row-relative rule (`rows[].detail` when `rows[].hasDetail`) is decided per
 * row, since only some rows answer 有.
 */
export function cellAppliesUnderRules(
  tableName: string,
  columnName: string,
  row: Record<string, unknown>,
  rules: ReadonlyArray<ProjectPlanConditionalField>,
): boolean {
  const bareTable = tableName.split('.').pop() ?? tableName;
  const rule = rules.find(
    (candidate) => candidate.field === `${bareTable}${ROW_MARKER}.${columnName}`,
  );
  if (!rule) return true;

  const toggleName = rule.when.split(`${ROW_MARKER}.`).pop() ?? rule.when;
  const toggle = row[toggleName];
  if (typeof toggle !== 'boolean') return true;
  return toggle === rule.is;
}

/**
 * The name to show for a stored attachment.
 *
 * Keys look like `projects/{id}/plan/{slug}/{field}_{file}`; the field prefix
 * is storage bookkeeping, not part of what the operator uploaded.
 */
export function fileNameFromKey(s3Key: string): string {
  const last = s3Key.split('/').pop() ?? s3Key;
  const separator = last.indexOf('_');
  return separator > 0 && separator < last.length - 1 ? last.slice(separator + 1) : last;
}

export function isImageKey(s3Key: string): boolean {
  return /\.(png|jpe?g)$/i.test(s3Key);
}
