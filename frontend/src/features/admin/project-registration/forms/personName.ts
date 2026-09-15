/**
 * 氏名 as the plan stores it — one string, 姓 then 名 — and back.
 *
 * The separator is a half-width space because that is what the server writes
 * when it fills a name from 姓/名 columns (ProjectPlanSqlContext.fullName), so
 * a name typed here and a name filled from master data read the same.
 */
export type PersonNameParts = { lastName: string; firstName: string };

export function splitPersonName(value: unknown): PersonNameParts {
  if (typeof value !== 'string') return { lastName: '', firstName: '' };

  const trimmed = value.trim();
  // Full-width spaces too: names typed with a Japanese IME often carry them.
  const match = /^(\S+)[\s　]+(.+)$/.exec(trimmed);
  return match
    ? { lastName: match[1], firstName: match[2].trim() }
    : { lastName: trimmed, firstName: '' };
}

export function joinPersonName({ lastName, firstName }: PersonNameParts): string {
  return [lastName.trim(), firstName.trim()].filter(Boolean).join(' ');
}
