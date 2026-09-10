/** Normalizes a displayed grouped number into the raw numeric string stored by forms. */
export function normalizeGroupedNumberValue(
  value: string,
  allowDecimal = false,
): string {
  const normalizedValue = value.normalize('NFKC').replace(/[\s,]/g, '');
  if (!allowDecimal) return normalizedValue.replace(/\D/g, '');

  const decimalIndex = normalizedValue.indexOf('.');
  const integerPart = (
    decimalIndex >= 0
      ? normalizedValue.slice(0, decimalIndex)
      : normalizedValue
  ).replace(/\D/g, '');
  if (decimalIndex < 0) return integerPart;

  const decimalPart = normalizedValue
    .slice(decimalIndex + 1)
    .replace(/\D/g, '');
  return `${integerPart || '0'}.${decimalPart}`;
}

/** Adds thousands separators without changing decimal precision or trailing input. */
export function formatGroupedNumberValue(
  value: string,
  allowDecimal = false,
): string {
  const normalizedValue = normalizeGroupedNumberValue(value, allowDecimal);
  if (!normalizedValue) return '';

  const [integerPart, decimalPart] = normalizedValue.split('.');
  const groupedInteger = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  return decimalPart === undefined
    ? groupedInteger
    : `${groupedInteger}.${decimalPart}`;
}
