import type { DocumentDataSection } from '@/api/admin/projectDocumentData';

/**
 * Values are shown and copied exactly as the server sends them, units left
 * out: each one is pasted into its own cell of an Excel form that already has
 * its labels and units.
 */

export const EMPTY_VALUE = '—';

const hasValue = (value: string | null | undefined): value is string =>
  value !== null && value !== undefined && value !== '';

/** What a copy puts on the clipboard: the value as sent, or nothing when blank. */
export const copyValueText = (value: string | null | undefined): string =>
  hasValue(value) ? value : '';

/** What a value reads as on screen: the value as sent, or a dash when blank. */
export const displayValue = (value: string | null | undefined): string =>
  hasValue(value) ? value : EMPTY_VALUE;

export const isNumericType = (type: string) => type === 'NUMBER';

/**
 * Keeps the items (and table rows) matching a filter, case-insensitive on
 * label or value; a section whose title matches is kept whole.
 */
export function filterSections(sections: DocumentDataSection[], query: string): DocumentDataSection[] {
  const needle = query.trim().toLowerCase();
  if (!needle) return sections;
  const matches = (text: string | null | undefined) => (text ?? '').toLowerCase().includes(needle);

  return sections.flatMap((section) => {
    if (matches(section.title)) return [section];
    const items = section.items.filter((item) => matches(item.label) || matches(item.value));
    const table =
      section.table && section.table.columns.some((column) => matches(column.label))
        ? section.table
        : section.table && {
            ...section.table,
            rows: section.table.rows.filter((row) => row.some(matches)),
          };
    const keptTable = table && table.rows.length > 0 ? table : null;
    return items.length > 0 || keptTable ? [{ ...section, items, table: keptTable }] : [];
  });
}

/** Writes text to the clipboard, falling back to execCommand where the Clipboard API is missing. */
export async function copyToClipboard(text: string): Promise<void> {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', '');
  textarea.style.position = 'fixed';
  textarea.style.opacity = '0';
  document.body.appendChild(textarea);
  textarea.select();
  try {
    if (!document.execCommand('copy')) throw new Error('copy failed');
  } finally {
    textarea.remove();
  }
}
