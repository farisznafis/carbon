/**
 * 郵便番号 → address, via zipcloud (the service SiteForm, ProfileForm and
 * SignupStep1 already call).
 */
export type PostalAddress = {
  /** 都道府県, e.g. 東京都 */
  prefecture: string;
  /** 市区町村, e.g. 千代田区 */
  city: string;
  /** 町域, e.g. 丸の内 — the start of 市区町村以降の住所 */
  town: string;
};

/** The seven digits of a postal code, or null if it does not have exactly seven. */
export function normalizePostalCode(value: string): string | null {
  // Full-width digits and hyphens are what a Japanese IME types.
  const digits = value
    .replace(/[０-９]/g, (d) => String.fromCharCode(d.charCodeAt(0) - 0xfee0))
    .replace(/[^0-9]/g, '');
  return digits.length === 7 ? digits : null;
}

/** The address for a seven-digit code, or null when zipcloud knows none. */
export async function lookupPostalCode(
  digits: string,
  signal?: AbortSignal,
): Promise<PostalAddress | null> {
  const response = await fetch(
    `https://zipcloud.ibsnet.co.jp/api/search?zipcode=${digits}`,
    { signal },
  );
  const data = (await response.json()) as {
    results?: Array<{ address1?: string; address2?: string; address3?: string }> | null;
  };
  const result = data.results?.[0];
  if (!result) return null;

  return {
    prefecture: result.address1 ?? '',
    city: result.address2 ?? '',
    town: result.address3 ?? '',
  };
}
