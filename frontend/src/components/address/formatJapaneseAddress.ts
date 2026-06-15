export type Address = {
  postalCode: string;   // "160-0022"
  prefecture: string;   // "東京都"
  city: string;         // "新宿区"
  addressDetail: string;  // "新宿3-1-5 〇〇ビル5F"
};

export function formatJapaneseAddress(postalCode: string, prefectureName: string, cityName: string, addressDetail: string): string {
  const line1 = postalCode ? `〒${postalCode}` : "";
  const line2 = `${prefectureName ?? ""}${cityName ?? ""}${
    addressDetail ?? ""
  }`.trim();

  return [line1, line2].filter(Boolean).join("\n");
}