import type { DocumentDataItem, DocumentDataSection } from '@/api/admin/projectDocumentData';
import type {
  EligibilityStatus,
  EvidenceFile,
  IndividualActivityDetail,
  MonitoringPoint,
  ReportReadiness,
} from '@/api/admin/individualActivity';
import type { StatusBadgeTone } from '@/components/data-display/StatusBadge';

/**
 * The 別紙10-1 会員情報 sheets: one row per equipment, columns C to the
 * monitoring end date (the cells after it are the Excel's own formulas).
 * 蓄電池あり differs only by its three battery columns after 公称最大出力.
 * Values use the sheet's pulldown wording (可/不可, ○/×), so a pasted row needs no fixing.
 */

export type BatterySheet = 'without' | 'with';

export const SHEETS: Record<BatterySheet, { label: string; sheetName: string; idPrefix: number }> = {
  without: { label: '蓄電池なし', sheetName: '会員情報(蓄電池なし)', idPrefix: 1 },
  with: { label: '蓄電池あり', sheetName: '会員情報(蓄電池あり)', idPrefix: 2 },
};

export const sheetOf = (hasBattery: boolean | null | undefined): BatterySheet => (hasBattery ? 'with' : 'without');

/** The ID column: numbered per sheet, 1-n or 2-n, so no ID appears on both sheets. */
export const sheetRowId = (sheet: BatterySheet, index: number) => `${SHEETS[sheet].idPrefix}-${index + 1}`;

/** The sheet's first data column (ID) is C. */
const FIRST_COLUMN = 2;

export const columnLetter = (index: number) => {
  let letters = '';
  for (let n = index + 1; n > 0; n = Math.floor((n - 1) / 26)) {
    letters = String.fromCharCode(65 + ((n - 1) % 26)) + letters;
  }
  return letters;
};

export const ELIGIBILITY_LABELS: Record<EligibilityStatus, { label: string; tone: StatusBadgeTone }> = {
  ELIGIBLE: { label: '対象', tone: 'success' },
  NOT_ELIGIBLE: { label: '対象外', tone: 'danger' },
  UNDETERMINED: { label: '未判定', tone: 'warning' },
  PENDING: { label: '未判定', tone: 'warning' },
};

/** 対象可否 as the sheet's pulldown reads it; undetermined stays blank. */
const ELIGIBILITY_CELL: Partial<Record<EligibilityStatus, string>> = { ELIGIBLE: '可', NOT_ELIGIBLE: '不可' };

export const READINESS_LABELS: Record<ReportReadiness, { label: string; tone: StatusBadgeTone }> = {
  READY: { label: '報告可', tone: 'success' },
  MISSING_EVIDENCE: { label: '証憑不足', tone: 'warning' },
  NO_BATCH_RESULT: { label: '集計結果なし', tone: 'neutral' },
};

/**
 * Where a blank value has to come from:
 * - manual: the system has no source; typed into the Excel by hand.
 * - equipment / monitoring: read off that evidence.
 * - member: not registered yet; asked of the member.
 */
export type ValueSource = 'manual' | 'equipment' | 'monitoring' | 'member';

export type ActivityItem = DocumentDataItem & { source: ValueSource; column: string };
export type ActivitySection = DocumentDataSection & { items: ActivityItem[] };
export type ActivityGroup = {
  key: string;
  title: string;
  /** Whose evidence the group's blanks are checked against; shown at its head. */
  evidence: 'equipment' | 'monitoring' | null;
  sections: ActivitySection[];
};

type Value = string | number | boolean | null | undefined;
type Column = Omit<ActivityItem, 'column'> & { section: string; group: string };

const text = (value: Value): string | null =>
  value === null || value === undefined || value === '' ? null : String(value);
const yesNo = (value: Value, yes: string, no: string) => (typeof value === 'boolean' ? (value ? yes : no) : text(value));

const GROUPS: Omit<ActivityGroup, 'sections'>[] = [
  { key: 'member', title: '会員', evidence: null },
  { key: 'equipment', title: '設備', evidence: 'equipment' },
  { key: 'certification', title: '認証・追加性', evidence: null },
  { key: 'monitoring', title: 'モニタリング情報', evidence: 'monitoring' },
];

/** Every column of one sheet's row, in the sheet's order. */
function sheetColumns(detail: IndividualActivityDetail, sheet: BatterySheet, rowId: string): Column[] {
  const { member, equipment, additionality, registrationRequirements: req, monitoring } = detail;
  const flags = member?.businessOperatorFlags;
  const pcs = equipment?.pcs;
  const battery = equipment?.battery;
  const columns: Column[] = [];
  let group = '';
  let section = '';

  const at = (nextGroup: string, nextSection: string) => {
    group = nextGroup;
    section = nextSection;
  };
  const push = (source: ValueSource, key: string, label: string, value: string | null, type = 'TEXT') =>
    columns.push({ key, label, value, unit: null, type, source, section, group });
  const col = (source: ValueSource) => ({
    text: (key: string, label: string, value: Value) => push(source, key, label, text(value)),
    num: (key: string, label: string, value: Value) => push(source, key, label, text(value), 'NUMBER'),
    date: (key: string, label: string, value: Value) => push(source, key, label, text(value), 'DATE'),
    flag: (key: string, label: string, value: Value) => push(source, key, label, yesNo(value, '有', '無')),
    check: (key: string, label: string, value: Value) => push(source, key, label, yesNo(value, '○', '×')),
  });
  const m = col('member');
  const e = col('equipment');
  const mo = col('monitoring');
  const manual = col('manual');

  const point = (prefix: string, value: MonitoringPoint | null | undefined) => {
    mo.date(`${prefix}ReadingDate`, '積算値データの取得日', value?.readingDate);
    manual.date(`${prefix}UtilityReadingDate`, '電力会社発行の明細に記載された検針日', value?.utilityMeterReadingDate);
    mo.num(`${prefix}GenerationKwh`, '発電量 (kWh)', value?.generationKwh);
    mo.num(`${prefix}SaleKwh`, '売電量 (kWh)', value?.saleKwh);
  };

  at('member', '会員基本情報');
  push('member', 'id', 'ID', rowId);
  push('member', 'eligibilityStatus', '対象可否', member?.eligibilityStatus ? ELIGIBILITY_CELL[member.eligibilityStatus] ?? null : null);
  m.date('membershipApplicationDate', '入会申込日', member?.membershipApplicationDate);
  m.text('companyName', '会員名', member?.companyName);
  m.text('address', '設置場所住所', member?.address);
  m.flag('carbonNeutralActionPlan', 'カーボンニュートラル行動計画への参加有無', flags?.carbonNeutralActionPlan);
  m.flag('energySavingActSpecifiedOperator', '省エネ法特定事業者の該当有無', flags?.energySavingActSpecifiedOperator);
  m.flag('warmingActSpecifiedEmitter', '温対法特定排出者の該当有無', flags?.warmingActSpecifiedEmitter);

  at('equipment', 'パワーコンディショナー設備情報');
  e.text('pcsManufacturer', 'メーカー', pcs?.manufacturer);
  e.text('pcsModel', '型番', pcs?.model);
  e.num('pcsUnitCount', '台数', pcs?.unitCount);
  e.text('pcsSerialNumber', '機器固有番号', pcs?.serialNumber);
  e.num('pcsStandbyPowerKw', 'パワーコンディショナー夜間待機電力 (kW)', pcs?.standbyPowerKw);
  manual.text('pcsMeterClass', '分類B or 分類C', pcs?.meterClass);
  manual.num('pcsMaxErrorRatePercent', '（分類Cの場合）最大誤差率 (%)', pcs?.maxErrorRatePercent);

  at('equipment', '太陽光発電設備情報');
  e.num('nominalOutputKw', '公称最大出力 (kW)', equipment?.nominalOutputKw);

  if (sheet === 'with') {
    at('equipment', '蓄電池');
    e.num('batteryCapacityKwh', '蓄電池容量 (kWh)', battery?.capacityKwh);
    manual.num('batteryChargeEfficiency', '充電効率 (%)', battery?.chargeEfficiencyPercent);
    manual.num('batteryDischargeEfficiency', '放電効率 (%)', battery?.dischargeEfficiencyPercent);
  }

  at('equipment', 'ICT機器');
  manual.num('ictConsumptionKw', '消費電力 (kW)', equipment?.ictConsumptionKw);

  at('equipment', '追加的設備情報');
  manual.text('isAdditionalEquipment', '追加的設備による登録か', null);
  manual.date('additionalEquipmentStartDate', '追加的設備の導入日', null);
  manual.text('additionalEquipmentKind', '種類', null);
  manual.num('additionalEquipmentUnitCount', '台数', null);
  manual.text('additionalEquipmentSerialNumber', '機器固有番号', null);

  at('equipment', '基準日');
  e.date('operationStartDate', '設備稼働日（発電開始日）', equipment?.operationStartDate);
  manual.date('saleStartDate', 'モニタリングが可能になった日（売電開始日）', null);

  at('certification', '認証対象期間');
  manual.text('baselineExtended', 'ベースラインの見直しによる延長済みか', null);
  manual.date('periodStart', '開始日（入会申込日及び売電開始日のいずれか遅い日）', null);
  manual.date('periodEnd', '終了日（退会日）', null);

  at('certification', '追加性評価');
  manual.num('expectedSelfConsumption', '想定自家消費量 (kWh/年)', null);
  m.num('preProjectUnitPrice', '導入前1年間の平均電力単価 (円/kWh)', additionality?.preProjectUnitPrice);
  manual.num('expectedSale', '想定売電量 (kWh/年)', null);
  manual.num('saleUnitPrice', '売電単価 (円/kWh)', null);
  m.num('totalInvestment', '総投資額 (円)', additionality?.totalInvestment);
  m.num('subsidyAmount', '補助金額 (円)', additionality?.subsidyAmount);
  manual.num('paybackYears', '投資回収年数 (年)', null);

  at('certification', 'プロジェクト登録要件');
  m.check('operatingInJapan', '日本国内で実施', req?.operatingInJapan);
  m.check('withinTwoYearsOfApplication', '入会申し込み日の2年前の日以降に実施', req?.withinTwoYearsOfApplication);
  manual.check('hasAdditionality', '追加性を有する', req?.hasAdditionality);
  manual.check('basedOnMethodology', '方法論に基づいて実施される', null);
  manual.check('noDuplicateRegistration', '削減活動の二重登録が無い', req?.noDuplicateRegistration);
  manual.check('sharesMethodology', '全ての削減活動において方法論、モニタリング項目が共通', null);

  at('monitoring', 'モニタリング');
  manual.text('samplingStatus', 'サンプリング', monitoring?.samplingStatus);
  manual.text('meteringPosition', '発電電力量のモニタリング位置 (A/B)', monitoring?.meteringPosition);
  manual.text('generationMethod', 'モニタリング方法（発電量）', monitoring?.generationMethod);
  manual.text('saleMethod', 'モニタリング方法（売電量）', monitoring?.saleMethod);

  at('monitoring', '今回認証のモニタリング開始日');
  point('windowStart', monitoring?.windowStart);
  at('monitoring', '今回認証のモニタリング終了日');
  point('windowEnd', monitoring?.windowEnd);

  return columns;
}

/** One sheet row as its columns, grouped for reading and in order for pasting. */
export function toSheetRow(detail: IndividualActivityDetail, sheet: BatterySheet, rowId: string) {
  const items: (ActivityItem & { section: string; group: string })[] = sheetColumns(detail, sheet, rowId).map(
    (column, index) => ({ ...column, column: columnLetter(FIRST_COLUMN + index) }),
  );

  const groups: ActivityGroup[] = GROUPS.map((group) => {
    const sections: ActivitySection[] = [];
    for (const { section: title, group: key, ...item } of items) {
      if (key !== group.key) continue;
      let section = sections.find((entry) => entry.title === title);
      if (!section) {
        // Keys carry the equipment so open rows never share a copy control or heading id.
        section = { key: `${detail.equipmentId}-${title}`, title, items: [], table: null };
        sections.push(section);
      }
      section.items.push(item);
    }
    return { ...group, sections };
  });

  // Tab-separated, pasted at the ID cell: blanks stay empty cells so every value lands in its column.
  const tsv = items.map((item) => (item.value ?? '').replace(/[\t\r\n]+/g, ' ')).join('\t');
  const lastColumn = items[items.length - 1]?.column ?? '';

  return { groups, tsv, range: `${columnLetter(FIRST_COLUMN)}〜${lastColumn}` };
}

/** Blank items, split into those to look up and those to type in by hand. */
export function countBlanks(groups: ActivityGroup[]) {
  const blanks = groups.flatMap((group) => group.sections.flatMap((s) => s.items)).filter((i) => i.value === null);
  const manual = blanks.filter((i) => i.source === 'manual').length;
  return { toCheck: blanks.length - manual, manual };
}

export type EvidenceLink = { url: string; fileName: string; fileType: string | null };

/** The file's name from its (presigned) URL, for a readable label. */
const fileNameOf = (url: string) => {
  try {
    return decodeURIComponent(new URL(url).pathname.split('/').pop() || '') || '証憑ファイル';
  } catch {
    return '証憑ファイル';
  }
};

export function evidenceLinks(files: EvidenceFile[] | null | undefined): EvidenceLink[] {
  return (files ?? []).flatMap((file) =>
    file.url ? [{ url: file.url, fileName: fileNameOf(file.url), fileType: file.fileType }] : [],
  );
}
