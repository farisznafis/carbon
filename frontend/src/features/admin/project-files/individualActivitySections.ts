import type { DocumentDataItem, DocumentDataSection } from '@/api/admin/projectDocumentData';
import type {
  EligibilityStatus,
  EvidenceFile,
  IndividualActivityDetail,
  MonitoringPoint,
  ReportReadiness,
} from '@/api/admin/individualActivity';
import type { StatusBadgeTone } from '@/components/data-display/StatusBadge';
import { formatEquipmentFileType } from '@/features/user/equipment/equipmentFileDisplay';

/**
 * The 別紙10-1 会員情報 sheets: one row per equipment, its columns in the
 * sheet's order and wording. 蓄電池あり differs only by three battery
 * columns. Values use the sheet's pulldown wording (可/不可, ○/×).
 */

export type BatterySheet = 'without' | 'with';

export const SHEETS: Record<BatterySheet, { label: string; sheetName: string }> = {
  without: { label: '蓄電池なし', sheetName: '会員情報(蓄電池なし)' },
  with: { label: '蓄電池あり', sheetName: '会員情報(蓄電池あり)' },
};

export const sheetOf = (hasBattery: boolean | null | undefined): BatterySheet => (hasBattery ? 'with' : 'without');

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

/** The monitoring evidence has no type of its own yet; its files share one kind. */
export const MONITORING_EVIDENCE = 'monitoring';

export const evidenceLabel = (kind: string) =>
  kind === MONITORING_EVIDENCE ? 'モニタリングの証憑' : formatEquipmentFileType(kind);

/**
 * Where a value comes from, kept for pointing blanks at their file later:
 * - evidence: read off the file named by `evidence` (the type the member uploaded it as).
 * - member: not registered yet; asked of the member.
 * - manual: the system has no source; typed into the Excel by hand.
 */
export type ActivityItem = DocumentDataItem & {
  source: 'evidence' | 'member' | 'manual';
  evidence: string | null;
};
export type ActivitySection = DocumentDataSection & { items: ActivityItem[] };
export type ActivityGroup = { key: string; title: string; sections: ActivitySection[] };

export const ACTIVITY_GROUPS = [
  { key: 'member', title: '会員' },
  { key: 'equipment', title: '設備' },
  { key: 'certification', title: '認証・追加性' },
  { key: 'monitoring', title: 'モニタリング' },
] as const;

type Value = string | number | boolean | null | undefined;

const text = (value: Value): string | null =>
  value === null || value === undefined || value === '' ? null : String(value);
const yesNo = (value: Value, yes: string, no: string) => (typeof value === 'boolean' ? (value ? yes : no) : text(value));

/** The sheet row for one equipment, grouped and in the sheet's column order. */
export function toActivityGroups(detail: IndividualActivityDetail, sheet: BatterySheet): ActivityGroup[] {
  const { member, equipment, additionality, registrationRequirements: req, monitoring } = detail;
  const flags = member?.businessOperatorFlags;
  const pcs = equipment?.pcs;
  const battery = equipment?.battery;
  const groups: ActivityGroup[] = ACTIVITY_GROUPS.map((group) => ({ ...group, sections: [] }));
  let items: ActivityItem[] = [];

  const at = (groupKey: (typeof ACTIVITY_GROUPS)[number]['key'], title: string) => {
    items = [];
    // Keys carry the equipment so two rows never share a copy control or heading id.
    groups.find((group) => group.key === groupKey)!.sections.push({
      key: `${detail.equipmentId}-${title}`,
      title,
      items,
      table: null,
    });
  };
  const push = (source: ActivityItem['source'], evidence: string | null) =>
    (key: string, label: string, value: string | null, type = 'TEXT') =>
      items.push({ key, label, value, unit: null, type, source, evidence });
  const columns = (source: ActivityItem['source'], evidence: string | null = null) => {
    const add = push(source, evidence);
    return {
      text: (key: string, label: string, value: Value) => add(key, label, text(value)),
      num: (key: string, label: string, value: Value) => add(key, label, text(value), 'NUMBER'),
      date: (key: string, label: string, value: Value) => add(key, label, text(value), 'DATE'),
      flag: (key: string, label: string, value: Value) => add(key, label, yesNo(value, '有', '無')),
      check: (key: string, label: string, value: Value) => add(key, label, yesNo(value, '○', '×')),
    };
  };
  // Which file proves which column: the same pairing the member's equipment registration uses.
  const m = columns('member');
  const manual = columns('manual');
  const pcsSpec = columns('evidence', 'pcs_spec');
  const panel = columns('evidence', 'panel_catalog');
  const batteryCatalog = columns('evidence', 'battery_catalog');
  const contract = columns('evidence', 'solar_contract');
  const invoice = columns('evidence', 'construction_invoice');
  const subsidy = columns('evidence', 'subsidy_notice');
  const bill = columns('evidence', 'electricity_bill');
  const reading = columns('evidence', MONITORING_EVIDENCE);

  const point = (prefix: string, value: MonitoringPoint | null | undefined) => {
    reading.date(`${prefix}ReadingDate`, '積算値データの取得日', value?.readingDate);
    manual.date(`${prefix}UtilityReadingDate`, '電力会社発行の明細に記載された検針日', value?.utilityMeterReadingDate);
    reading.num(`${prefix}GenerationKwh`, '発電量 (kWh)', value?.generationKwh);
    reading.num(`${prefix}SaleKwh`, '売電量 (kWh)', value?.saleKwh);
  };

  // The sheet's ID column is the operator's own numbering, so it has no value here.
  at('member', '会員基本情報');
  push('member', null)(
    'eligibilityStatus',
    '対象可否',
    member?.eligibilityStatus ? ELIGIBILITY_CELL[member.eligibilityStatus] ?? null : null,
  );
  m.date('membershipApplicationDate', '入会申込日', member?.membershipApplicationDate);
  m.text('companyName', '会員名', member?.companyName);
  m.text('address', '設置場所住所', member?.address);
  m.flag('carbonNeutralActionPlan', 'カーボンニュートラル行動計画への参加有無', flags?.carbonNeutralActionPlan);
  m.flag('energySavingActSpecifiedOperator', '省エネ法特定事業者の該当有無', flags?.energySavingActSpecifiedOperator);
  m.flag('warmingActSpecifiedEmitter', '温対法特定排出者の該当有無', flags?.warmingActSpecifiedEmitter);

  at('equipment', 'パワーコンディショナー設備情報');
  pcsSpec.text('pcsManufacturer', 'メーカー', pcs?.manufacturer);
  pcsSpec.text('pcsModel', '型番', pcs?.model);
  pcsSpec.num('pcsUnitCount', '台数', pcs?.unitCount);
  pcsSpec.text('pcsSerialNumber', '機器固有番号', pcs?.serialNumber);
  pcsSpec.num('pcsStandbyPowerKw', 'パワーコンディショナー夜間待機電力 (kW)', pcs?.standbyPowerKw);
  manual.text('pcsMeterClass', '分類B or 分類C', pcs?.meterClass);
  manual.num('pcsMaxErrorRatePercent', '（分類Cの場合）最大誤差率 (%)', pcs?.maxErrorRatePercent);

  at('equipment', '太陽光発電設備情報');
  panel.num('nominalOutputKw', '公称最大出力 (kW)', equipment?.nominalOutputKw);

  if (sheet === 'with') {
    at('equipment', '蓄電池');
    batteryCatalog.num('batteryCapacityKwh', '蓄電池容量 (kWh)', battery?.capacityKwh);
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
  contract.date('operationStartDate', '設備稼働日（発電開始日）', equipment?.operationStartDate);
  manual.date('saleStartDate', 'モニタリングが可能になった日（売電開始日）', null);

  at('certification', '認証対象期間');
  manual.text('baselineExtended', 'ベースラインの見直しによる延長済みか', null);
  manual.date('periodStart', '開始日（入会申込日及び売電開始日のいずれか遅い日）', null);
  manual.date('periodEnd', '終了日（退会日）', null);

  at('certification', '追加性評価');
  manual.num('expectedSelfConsumption', '想定自家消費量 (kWh/年)', null);
  bill.num('preProjectUnitPrice', '導入前1年間の平均電力単価 (円/kWh)', additionality?.preProjectUnitPrice);
  manual.num('expectedSale', '想定売電量 (kWh/年)', null);
  manual.num('saleUnitPrice', '売電単価 (円/kWh)', null);
  invoice.num('totalInvestment', '総投資額 (円)', additionality?.totalInvestment);
  subsidy.num('subsidyAmount', '補助金額 (円)', additionality?.subsidyAmount);
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

  return groups;
}

/** The order the member uploads equipment evidence in; any other type follows. */
const EVIDENCE_ORDER = [
  'solar_contract',
  'panel_catalog',
  'pcs_spec',
  'battery_catalog',
  'construction_invoice',
  'subsidy_notice',
  'electricity_bill',
];

export type EvidenceLink = {
  /** An https URL, or the S3 key to sign when the file is opened. */
  location: string;
  kind: string;
  /** The stored file name, for its extension (PDF / Excel preview). */
  fileName: string;
};

/** The response gives `s3://bucket/key`; only the key can be signed. */
const locationOf = (url: string) => (url.startsWith('s3://') ? url.replace(/^s3:\/\/[^/]+\//, '') : url);

/** Every evidence file of the equipment: equipment ones in upload order, then monitoring ones. */
export function evidenceFilesOf(detail: IndividualActivityDetail): EvidenceLink[] {
  const links = (files: EvidenceFile[] | null | undefined, kindOf: (file: EvidenceFile) => string) =>
    (files ?? []).flatMap((file) => {
      if (!file.url) return [];
      const location = locationOf(file.url);
      return [{ location, kind: kindOf(file), fileName: location.split('/').pop() ?? '' }];
    });
  const rank = (kind: string) => (EVIDENCE_ORDER.includes(kind) ? EVIDENCE_ORDER.indexOf(kind) : EVIDENCE_ORDER.length);
  return [
    ...links(detail.equipment?.evidenceFiles, (file) => file.fileType ?? 'other').sort((a, b) => rank(a.kind) - rank(b.kind)),
    ...links(detail.monitoring?.evidenceFiles, () => MONITORING_EVIDENCE),
  ];
}
