import type { SiteProjectEquipment } from '@/api/site';

export type EquipmentDisplayType =
  | 'boolean'
  | 'date'
  | 'eligibility'
  | 'value';

export type EquipmentDisplayField = {
  label: string;
  key: keyof SiteProjectEquipment;
  type?: EquipmentDisplayType;
  unit?: string;
  condition?: (equipment: SiteProjectEquipment) => boolean;
};

export type EquipmentDetailSection = {
  title: string;
  fields: readonly EquipmentDisplayField[];
};

const hasBattery = (equipment: SiteProjectEquipment) =>
  equipment.hasBattery === true;

const hasAdditionalEquipment = (equipment: SiteProjectEquipment) =>
  equipment.additionalEquipment === true;

const hasSubsidy = (equipment: SiteProjectEquipment) =>
  equipment.hasSubsidy === true;

export const equipmentSummaryFields: readonly EquipmentDisplayField[] = [
  { label: '設備ID', key: 'equipmentId' },
  { label: '設備種別', key: 'equipmentType' },
  { label: '方法論番号', key: 'methodologyNumber' },
  { label: '適格性', key: 'isEligible', type: 'eligibility' },
  { label: '稼働開始日', key: 'equipmentOpDate', type: 'date' },
  { label: '定格最大出力', key: 'nominalMaxOutput', unit: 'kW' },
  { label: '蓄電池有無', key: 'hasBattery', type: 'boolean' },
  { label: '追加設備有無', key: 'additionalEquipment', type: 'boolean' },
];

export const equipmentDetailSections: readonly EquipmentDetailSection[] = [
  {
    title: '基本情報',
    fields: [
      // { label: '一時ID', key: 'tempId' },
      { label: '設備ID', key: 'equipmentId' },
      { label: '方法論番号', key: 'methodologyNumber' },
      { label: '設備種別', key: 'equipmentType' },
      { label: '適格性', key: 'isEligible', type: 'eligibility' },
      { label: '稼働開始日', key: 'equipmentOpDate', type: 'date' },
      { label: '定格最大出力', key: 'nominalMaxOutput', unit: 'kW' },
      { label: '台数', key: 'unitCount', unit: '台' },
      { label: '機器番号', key: 'deviceNo' },
      { label: '機器固有番号', key: 'deviceUniqueNo' },
    ],
  },
  {
    title: 'PCS情報',
    fields: [
      { label: 'PCSメーカー', key: 'pcsManufacturer' },
      { label: 'PCS型式', key: 'pcsModel' },
      { label: 'パワコン台数', key: 'powerCondUnitCount', unit: '台' },
      { label: 'パワコン待機電力', key: 'powerCondStandby', unit: 'W' },
    ],
  },
  {
    title: '蓄電池情報',
    fields: [
      { label: '蓄電池有無', key: 'hasBattery', type: 'boolean' },
      { label: '蓄電池メーカー', key: 'batteryMaker', condition: hasBattery },
      { label: '蓄電池型式', key: 'batteryModel', condition: hasBattery },
      {
        label: '蓄電池容量',
        key: 'batteryCapacity',
        unit: 'kWh',
        condition: hasBattery,
      },
    ],
  },
  {
    title: 'ICT機器情報',
    fields: [
      {
        label: 'ICT機器消費電力',
        key: 'ictEquipmentPowerConsumption',
        unit: 'W',
      },
    ],
  },
  {
    title: '追加設備情報',
    fields: [
      {
        label: '追加設備有無',
        key: 'additionalEquipment',
        type: 'boolean',
      },
      {
        label: '追加設備日',
        key: 'additionalEquipmentDate',
        type: 'date',
        condition: hasAdditionalEquipment,
      },
      {
        label: '追加設備種別',
        key: 'additionalEquipmentType',
        condition: hasAdditionalEquipment,
      },
      {
        label: '追加設備台数',
        key: 'additionalUnitCount',
        unit: '台',
        condition: hasAdditionalEquipment,
      },
      {
        label: '追加設備固有番号',
        key: 'additionalDeviceUniqueNo',
        condition: hasAdditionalEquipment,
      },
    ],
  },
  {
    title: '投資・補助金情報',
    fields: [
      { label: '総投資額', key: 'totalInvestment', unit: '円' },
      { label: '補助金有無', key: 'hasSubsidy', type: 'boolean' },
      { label: '補助金名', key: 'subsidyName', condition: hasSubsidy },
      {
        label: '補助金額',
        key: 'subsidyAmount',
        unit: '円',
        condition: hasSubsidy,
      },
    ],
  },
  {
    title: '事業前単価情報',
    fields: [
      {
        label: '事業前単価',
        key: 'preProjectUnitPrice',
        unit: '円/kWh',
      },
    ],
  },
];

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date);
}

export function formatEquipmentField(
  equipment: SiteProjectEquipment,
  field: EquipmentDisplayField,
) {
  const fieldValue = equipment[field.key];

  if (fieldValue === null || fieldValue === undefined || fieldValue === '') {
    return '-';
  }

  if (field.type === 'boolean') {
    return fieldValue === true ? 'あり' : 'なし';
  }

  if (field.type === 'eligibility') {
    return fieldValue === true ? '適格' : '対象外';
  }

  if (field.type === 'date' && typeof fieldValue === 'string') {
    return formatDate(fieldValue);
  }

  const formattedValue =
    typeof fieldValue === 'number'
      ? new Intl.NumberFormat('ja-JP').format(fieldValue)
      : String(fieldValue);

  return field.unit ? `${formattedValue} ${field.unit}` : formattedValue;
}
