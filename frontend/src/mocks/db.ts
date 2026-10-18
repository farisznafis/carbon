/**
 * In-memory demo data. Mutations from the UI (adding a site, approving a review, …)
 * change these objects, so the demo behaves consistently until the page is reloaded.
 */
import type { AdminCompanyListItem, CompanySuspension } from '@/api/admin/types';
import type { AdminOperatorApiItem } from '@/api/admin/operators/types';
import type { ProjectFile } from '@/api/admin/projectFiles';
import type { MonitoringCancelRequestListItem } from '@/api/monitoring/types';
import type { SiteProjectEquipmentApiItem } from '@/api/site/types';

export const COMPANY = {
  companyId: 'C0001',
  corpNumber: '1234567890123',
  nameJA: 'グリーンエナジー株式会社',
  nameFurigana: 'グリーンエナジーカブシキガイシャ',
  nameEN: 'Green Energy Co., Ltd.',
  email: 'info@green-energy.example.jp',
  phoneNumber: '0312345678',
  fax: '0312345679',
  postalCode: '1000005',
  prefecture: '東京都',
  municipality: '千代田区',
  blocknum: '丸の内1-1-1',
  buildingName: 'グリーンタワー10F',
  address: '東京都千代田区丸の内1-1-1 グリーンタワー10F',
};

export type MockUser = {
  id: string;
  username: string;
  role: 'master' | 'user';
  firstName: string;
  lastName: string;
  firstNameKana: string;
  lastNameKana: string;
  email: string;
  phoneNumber: string;
  position: string;
  siteId: string | null;
  siteName?: string;
};

export const users: MockUser[] = [
  {
    id: '1',
    username: 'master01',
    role: 'master',
    firstName: '太郎',
    lastName: '山田',
    firstNameKana: 'タロウ',
    lastNameKana: 'ヤマダ',
    email: 'master@green-energy.example.jp',
    phoneNumber: '09011112222',
    position: '環境推進部長',
    siteId: null,
  },
  {
    id: '2',
    username: 'tokyo01',
    role: 'user',
    firstName: '花子',
    lastName: '佐藤',
    firstNameKana: 'ハナコ',
    lastNameKana: 'サトウ',
    email: 'user@green-energy.example.jp',
    phoneNumber: '09033334444',
    position: '設備担当',
    siteId: 'S001',
    siteName: '東京本社',
  },
  {
    id: '3',
    username: 'osaka01',
    role: 'user',
    firstName: '健',
    lastName: '鈴木',
    firstNameKana: 'ケン',
    lastNameKana: 'スズキ',
    email: 'suzuki@green-energy.example.jp',
    phoneNumber: '09055556666',
    position: '工場長',
    siteId: 'S002',
    siteName: '大阪工場',
  },
];

export type MockSite = {
  siteId: string;
  siteName: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  blocknum: string;
  buildingName: string;
  picUserId: string | null;
  projects: string[];
  createdAt: string;
  metrics: { sumEmissionReduction: string; sumEmission: string; jcredit: string };
};

export const sites: MockSite[] = [
  {
    siteId: 'S001',
    siteName: '東京本社',
    postalCode: '1000005',
    prefecture: '東京都',
    municipality: '千代田区',
    blocknum: '丸の内1-1-1',
    buildingName: 'グリーンタワー10F',
    picUserId: '2',
    projects: ['太陽光発電設備導入プロジェクト'],
    createdAt: '2025-04-10T09:00:00Z',
    metrics: { sumEmissionReduction: '128.4', sumEmission: '412.7', jcredit: '96' },
  },
  {
    siteId: 'S002',
    siteName: '大阪工場',
    postalCode: '5300001',
    prefecture: '大阪府',
    municipality: '大阪市北区',
    blocknum: '梅田2-2-2',
    buildingName: '',
    picUserId: '3',
    projects: ['太陽光発電設備導入プロジェクト', '蓄電池併設プロジェクト'],
    createdAt: '2025-05-22T09:00:00Z',
    metrics: { sumEmissionReduction: '264.9', sumEmission: '803.1', jcredit: '210' },
  },
  {
    siteId: 'S003',
    siteName: '名古屋物流センター',
    postalCode: '4500002',
    prefecture: '愛知県',
    municipality: '名古屋市中村区',
    blocknum: '名駅3-3-3',
    buildingName: '',
    picUserId: null,
    projects: [],
    createdAt: '2025-09-01T09:00:00Z',
    metrics: { sumEmissionReduction: '0', sumEmission: '150.2', jcredit: '0' },
  },
];

const PROJECT_BASE = {
  repFirstName: '一郎',
  repFirstNameKana: 'イチロウ',
  repLastName: '高橋',
  repLastNameKana: 'タカハシ',
  repEmail: 'takahashi@carbon-ops.example.jp',
  repPhoneNumber: '0398765432',
  repPosition: '代表取締役',
  picFirstName: '美咲',
  picFirstNameKana: 'ミサキ',
  picLastName: '伊藤',
  picLastNameKana: 'イトウ',
  picEmail: 'ito@carbon-ops.example.jp',
  picPhoneNumber: '0398765433',
  picPosition: 'プロジェクトマネージャー',
  dataManagementPic: '伊藤 美咲',
  companyName: 'カーボンオペレーションズ株式会社',
  regionalCategory: '全国',
  methodologyVersion: 'Ver.3.2',
  methodologyCategory: '再生可能エネルギー',
  methodologyName: '太陽光発電設備の導入',
  methodologyMainEmission: '電力の使用',
  methodCombiFeasibility: true,
  methodDetails: '自家消費型太陽光発電設備の導入により、系統電力の使用量を削減する。',
  operatorCompanyId: '9001',
  startedAt: '2025-04-01T00:00:00Z',
  usePurpose: '自社のカーボンニュートラル目標達成のため',
};

export type MockProject = typeof PROJECT_BASE & {
  id: string;
  projectName: string;
  projectNumber: string;
  projectStatus: string;
  methodologyNumber: string;
  emissionReduction: number;
  baselineEmission: number;
  postProjectEmission: number;
  newMembersTotal: number;
  withdrawnMembers: number;
  cumulativeMembers: number;
  membersTotal: number;
  createdAt: string;
  updatedAt: string;
};

export const projects: MockProject[] = [
  {
    ...PROJECT_BASE,
    id: 'P001',
    projectName: '太陽光発電設備導入プロジェクト',
    projectNumber: 'JCL-2025-001',
    projectStatus: '実施中',
    methodologyNumber: 'EN-R-002',
    emissionReduction: 393.3,
    baselineEmission: 1609.0,
    postProjectEmission: 1215.7,
    newMembersTotal: 4,
    withdrawnMembers: 0,
    cumulativeMembers: 12,
    membersTotal: 12,
    createdAt: '2025-03-01T09:00:00Z',
    updatedAt: '2026-09-15T09:00:00Z',
  },
  {
    ...PROJECT_BASE,
    id: 'P002',
    projectName: '蓄電池併設プロジェクト',
    projectNumber: 'JCL-2025-002',
    projectStatus: '検証中',
    methodologyNumber: 'EN-S-019',
    methodologyCategory: '省エネルギー',
    methodologyName: '蓄電池の導入',
    emissionReduction: 152.8,
    baselineEmission: 820.5,
    postProjectEmission: 667.7,
    newMembersTotal: 2,
    withdrawnMembers: 1,
    cumulativeMembers: 6,
    membersTotal: 5,
    createdAt: '2025-06-10T09:00:00Z',
    updatedAt: '2026-08-30T09:00:00Z',
  },
  {
    ...PROJECT_BASE,
    id: 'P003',
    projectName: '高効率空調更新プロジェクト',
    projectNumber: 'JCL-2026-003',
    projectStatus: '準備中',
    methodologyNumber: 'EN-S-007',
    methodologyCategory: '省エネルギー',
    methodologyName: '空調設備の導入',
    methodologyMainEmission: '電力の使用',
    emissionReduction: 0,
    baselineEmission: 540.0,
    postProjectEmission: 410.0,
    newMembersTotal: 0,
    withdrawnMembers: 0,
    cumulativeMembers: 0,
    membersTotal: 0,
    createdAt: '2026-07-01T09:00:00Z',
    updatedAt: '2026-07-01T09:00:00Z',
  },
  {
    ...PROJECT_BASE,
    id: 'P004',
    projectName: 'LED照明更新プロジェクト',
    projectNumber: 'JCL-2024-004',
    projectStatus: 'クレジット申請中',
    methodologyNumber: 'EN-S-001',
    methodologyCategory: '省エネルギー',
    methodologyName: '照明設備の導入',
    emissionReduction: 88.1,
    baselineEmission: 300.0,
    postProjectEmission: 211.9,
    newMembersTotal: 1,
    withdrawnMembers: 0,
    cumulativeMembers: 9,
    membersTotal: 9,
    createdAt: '2024-05-01T09:00:00Z',
    updatedAt: '2026-06-01T09:00:00Z',
  },
  {
    ...PROJECT_BASE,
    id: 'P005',
    projectName: 'バイオマスボイラー導入プロジェクト',
    projectNumber: 'JCL-2026-005',
    projectStatus: '妥当性確認処理',
    methodologyNumber: 'EN-R-001',
    methodologyName: 'バイオマス固形燃料の導入',
    methodologyMainEmission: '燃料の使用',
    emissionReduction: 0,
    baselineEmission: 950.0,
    postProjectEmission: 120.0,
    newMembersTotal: 0,
    withdrawnMembers: 0,
    cumulativeMembers: 0,
    membersTotal: 0,
    createdAt: '2026-08-20T09:00:00Z',
    updatedAt: '2026-09-28T09:00:00Z',
  },
];

/** Which site takes part in which project, and how far the application went. */
export type MockParticipation = {
  participationId: number;
  projectId: string;
  siteId: string;
  status: 'OPEN' | 'APPROVED' | 'REJECTED';
  pdfStatus: string;
  submittedAt: string;
  equipmentId: number;
  companyName: string;
};

export const participations: MockParticipation[] = [
  { participationId: 501, projectId: 'P001', siteId: 'S001', status: 'APPROVED', pdfStatus: 'COMPLETED', submittedAt: '2025-04-20T10:00:00Z', equipmentId: 701, companyName: COMPANY.nameJA },
  { participationId: 502, projectId: 'P001', siteId: 'S002', status: 'APPROVED', pdfStatus: 'COMPLETED', submittedAt: '2025-06-01T10:00:00Z', equipmentId: 702, companyName: COMPANY.nameJA },
  { participationId: 503, projectId: 'P002', siteId: 'S002', status: 'OPEN', pdfStatus: 'PENDING', submittedAt: '2026-09-20T10:00:00Z', equipmentId: 703, companyName: COMPANY.nameJA },
  { participationId: 504, projectId: 'P001', siteId: 'S101', status: 'OPEN', pdfStatus: 'PENDING', submittedAt: '2026-10-01T10:00:00Z', equipmentId: 704, companyName: '東日本ソーラー株式会社' },
  { participationId: 505, projectId: 'P002', siteId: 'S102', status: 'REJECTED', pdfStatus: 'REJECTED', submittedAt: '2026-08-11T10:00:00Z', equipmentId: 705, companyName: '西日本ロジスティクス株式会社' },
];

const solarEquipment = (
  id: number,
  siteId: string,
  overrides: Partial<SiteProjectEquipmentApiItem> = {},
): SiteProjectEquipmentApiItem => ({
  id,
  equipmentId: id,
  siteId,
  methodologyNumber: 'EN-R-002',
  isEligible: true,
  equipmentOpDate: '2025-04-15',
  nominalMaxOutput: 49.5,
  pcsManufacturer: 'オムロン',
  pcsModel: 'KPW-A55-J4',
  deviceNo: `PCS-${id}`,
  pcsStandby: 12,
  hasBattery: false,
  batteryMaker: null,
  batteryModel: null,
  batteryCapacity: null,
  totalInvestment: 12_500_000,
  hasSubsidy: true,
  subsidyName: '再エネ導入促進補助金',
  subsidyAmount: 3_000_000,
  preProjectUnitPrice: 27.5,
  evidenceDocuments: [],
  files: [
    { id: id * 10 + 1, fileType: 'solar_contract', fileId: `F${id}1`, s3Key: `demo/${id}/solar_contract.pdf`, fileName: '設置契約書.pdf' },
    { id: id * 10 + 2, fileType: 'pcs_spec', fileId: `F${id}2`, s3Key: `demo/${id}/pcs_spec.pdf`, fileName: 'PCS仕様書.pdf' },
  ],
  ...overrides,
});

export const equipments: SiteProjectEquipmentApiItem[] = [
  solarEquipment(701, 'S001'),
  solarEquipment(702, 'S002', { nominalMaxOutput: 120, deviceNo: 'PCS-702', totalInvestment: 28_000_000 }),
  solarEquipment(703, 'S002', {
    methodologyNumber: 'EN-S-019',
    hasBattery: true,
    batteryMaker: 'パナソニック',
    batteryModel: 'LJB2356',
    batteryCapacity: 16.5,
    deviceNo: 'BAT-703',
  }),
  solarEquipment(704, 'S101', { pcsManufacturer: '田淵電機', pcsModel: 'EPD-T550' }),
  solarEquipment(705, 'S102', { hasSubsidy: false, subsidyName: null, subsidyAmount: null }),
];

/** Project ↔ equipment link, used by GET /site/{siteId}/{projectId}/equipment. */
export const projectEquipment: Record<string, number[]> = {
  'P001:S001': [701],
  'P001:S002': [702],
  'P002:S002': [703],
};

export type MockNotification = {
  id: number;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
};

export const notifications: MockNotification[] = [
  { id: 1, title: '入会届が承認されました', message: '「太陽光発電設備導入プロジェクト」への入会届（大阪工場）が承認されました。', read: false, createdAt: '2026-10-06T09:12:00Z' },
  { id: 2, title: 'モニタリングデータ提出のお願い', message: '9月分のモニタリングデータを10月15日までに提出してください。', read: false, createdAt: '2026-10-01T08:00:00Z' },
  { id: 3, title: '削減量の算定が完了しました', message: '2026年度上期の排出削減量の算定が完了しました。ダッシュボードでご確認ください。', read: true, createdAt: '2026-09-20T12:30:00Z' },
  { id: 4, title: 'システムメンテナンスのお知らせ', message: '10月20日 2:00〜4:00 にメンテナンスを実施します。', read: true, createdAt: '2026-09-10T03:00:00Z' },
];

const monthsBack = (count: number) =>
  Array.from({ length: count }, (_, index) => {
    const date = new Date(Date.UTC(2026, 8 - index, 1));
    return date.toISOString().slice(0, 10);
  });

export type MockMonitoringRecord = {
  id: string;
  projectId: string;
  siteId: string;
  equipmentId: number;
  participatingUserId: number;
  dateRecorded: string;
  timeUpdated: string;
  elPv: number;
  elPvr: number;
  elpjBatteryValue: number | null;
  elpjControlValue: number;
  elpjAccuracy: number;
  isActive: boolean;
  changedBattery: boolean;
};

export const monitoringRecords: MockMonitoringRecord[] = [
  ...monthsBack(14).map((date, index) => ({
    id: `M-S001-${index}`,
    projectId: 'P001',
    siteId: 'S001',
    equipmentId: 701,
    participatingUserId: 2,
    dateRecorded: date,
    timeUpdated: `${date}T10:00:00Z`,
    elPv: Math.round(5200 + Math.sin(index / 2) * 1400),
    elPvr: Math.round(180 + (index % 3) * 25),
    elpjBatteryValue: null,
    elpjControlValue: 12,
    elpjAccuracy: 0.98,
    isActive: true,
    changedBattery: false,
  })),
  ...monthsBack(12).map((date, index) => ({
    id: `M-S002-${index}`,
    projectId: 'P001',
    siteId: 'S002',
    equipmentId: 702,
    participatingUserId: 3,
    dateRecorded: date,
    timeUpdated: `${date}T11:00:00Z`,
    elPv: Math.round(11800 + Math.cos(index / 2) * 2600),
    elPvr: Math.round(420 + (index % 4) * 30),
    elpjBatteryValue: null,
    elpjControlValue: 18,
    elpjAccuracy: 0.97,
    isActive: true,
    changedBattery: false,
  })),
  ...monthsBack(6).map((date, index) => ({
    id: `M-S002-B${index}`,
    projectId: 'P002',
    siteId: 'S002',
    equipmentId: 703,
    participatingUserId: 3,
    dateRecorded: date,
    timeUpdated: `${date}T12:00:00Z`,
    elPv: Math.round(4300 + index * 120),
    elPvr: 150,
    elpjBatteryValue: Math.round(320 + index * 10),
    elpjControlValue: 9,
    elpjAccuracy: 0.99,
    isActive: true,
    changedBattery: index === 2,
  })),
];

export const cancelRequests: MonitoringCancelRequestListItem[] = [
  { id: 801, requestedAt: '2026-10-03T05:20:00Z', reason: '発電量の入力値を誤って登録したため', recordPk: 'M-S001-0', status: 'PENDING', requestedBy: '佐藤 花子', siteName: '東京本社', decidedAt: null, decidedBy: null },
  { id: 802, requestedAt: '2026-09-12T02:10:00Z', reason: 'エビデンスファイルの差し替え', recordPk: 'M-S002-1', status: 'APPROVED', requestedBy: '鈴木 健', siteName: '大阪工場', decidedAt: '2026-09-13T01:00:00Z', decidedBy: '管理 次郎' },
];

const companyRecord = (
  companyId: string,
  nameJA: string,
  status: AdminCompanyListItem['status'],
  createdAt: string,
  prefecture: string,
): AdminCompanyListItem => ({
  id: companyId,
  companyId,
  nameJA,
  nameFurigana: 'デモカブシキガイシャ',
  nameEN: 'Demo Co., Ltd.',
  email: `contact@${companyId.toLowerCase()}.example.jp`,
  phoneNumber: '0300000000',
  fax: null,
  corpNumber: `90000000000${companyId.slice(-2)}`,
  address: `${prefecture}デモ市1-2-3`,
  postalCode: '1000001',
  prefecture,
  municipality: 'デモ市',
  blocknum: '1-2-3',
  buildingName: null,
  representativeFirstName: '代表',
  representativeLastName: '田中',
  representativeFirstNameKana: 'ダイヒョウ',
  representativeLastNameKana: 'タナカ',
  representativePosition: '代表取締役',
  representativePhoneNumber: '09000000000',
  requestedMethodologies: ['EN-R-002'],
  activityInformation: '自社工場への太陽光発電設備導入を検討中です。',
  status,
  isActive: status === 'ACTIVE',
  createdAt,
  updatedAt: createdAt,
});

export const companies: AdminCompanyListItem[] = [
  {
    ...companyRecord('C0001', COMPANY.nameJA, 'ACTIVE', '2025-03-15T09:00:00Z', '東京都'),
    email: COMPANY.email,
    address: COMPANY.address,
    nameEN: COMPANY.nameEN,
    nameFurigana: COMPANY.nameFurigana,
  },
  companyRecord('C0002', '東日本ソーラー株式会社', 'ACTIVE', '2025-07-02T09:00:00Z', '宮城県'),
  companyRecord('C0003', '西日本ロジスティクス株式会社', 'SUSPENDED', '2025-08-19T09:00:00Z', '福岡県'),
  companyRecord('C0004', 'さくら食品工業株式会社', 'PENDING', '2026-09-28T09:00:00Z', '静岡県'),
  companyRecord('C0005', '北海道エコファーム合同会社', 'PENDING', '2026-10-05T09:00:00Z', '北海道'),
  companyRecord('C0006', 'ミナト精密株式会社', 'REJECTED', '2026-08-02T09:00:00Z', '神奈川県'),
];

export const suspensions: Record<string, CompanySuspension[]> = {
  C0003: [
    {
      id: 901,
      reasonCode: 'BILLING',
      reasonDetail: '利用料金の未払いが3か月続いているため',
      createdAt: '2026-09-01T00:00:00Z',
      createdBy: '管理 次郎',
      resolvedAt: null,
      resolvedBy: null,
      resolutionNote: null,
      active: true,
    },
  ],
};

export const ALL_PERMISSIONS = [
  'PROJECT_CREATE',
  'PROJECT_READ',
  'PROJECT_UPDATE',
  'PROJECT_STATUS_UPDATE',
  'PROJECT_APPLICATION_OUTPUT',
  'PARTICIPATING_COMPANY_READ',
  'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
  'PARTICIPATING_COMPANY_SUSPEND',
  'MASTER_USER_PASSWORD_RESET',
  'MONITORING_READ',
  'MONITORING_DELETE',
  'MONITORING_CANCEL',
  'MONITORING_BATCH_HISTORY_READ',
  'MONITORING_BATCH_ERROR_READ',
  'OPERATOR_USER_READ',
  'OPERATOR_USER_CREATE',
  'OPERATOR_USER_UPDATE',
  'METHODOLOGY_READ',
  'APPLICATION_FORM_READ',
];

export const operators: AdminOperatorApiItem[] = [
  {
    id: '101',
    username: 'admin',
    role: 'admin',
    email: 'admin@carbon-ops.example.jp',
    firstName: '次郎',
    firstNameKana: 'ジロウ',
    lastName: '管理',
    lastNameKana: 'カンリ',
    phoneNumber: '0398765400',
    userPosition: 'システム管理者',
    companyId: '9001',
    permissions: ALL_PERMISSIONS,
  },
  {
    id: '102',
    username: 'operator01',
    role: 'operator',
    email: 'operator01@carbon-ops.example.jp',
    firstName: '美咲',
    firstNameKana: 'ミサキ',
    lastName: '伊藤',
    lastNameKana: 'イトウ',
    phoneNumber: '0398765433',
    userPosition: 'プロジェクトマネージャー',
    companyId: '9001',
    permissions: ['PROJECT_READ', 'PROJECT_UPDATE', 'MONITORING_READ', 'PARTICIPATING_COMPANY_READ'],
  },
  {
    id: '103',
    username: 'operator02',
    role: 'operator',
    email: 'operator02@carbon-ops.example.jp',
    firstName: '翔',
    firstNameKana: 'ショウ',
    lastName: '中村',
    lastNameKana: 'ナカムラ',
    phoneNumber: '0398765434',
    userPosition: '審査担当',
    companyId: '9001',
    permissions: ['PARTICIPATING_COMPANY_READ', 'PARTICIPATING_COMPANY_APPLICATION_REVIEW'],
  },
];

export const projectFiles: Record<string, ProjectFile[]> = {
  P001: [
    { id: 3001, fileType: 'PROJECT_PLAN', ownedByKanrisha: true, originalFileName: 'プロジェクト計画書_v2.xlsx', uploadedBy: '伊藤 美咲', uploadedAt: '2026-05-10T04:00:00Z', versionNo: 2 },
    { id: 3000, fileType: 'PROJECT_PLAN', ownedByKanrisha: true, originalFileName: 'プロジェクト計画書_v1.xlsx', uploadedBy: '伊藤 美咲', uploadedAt: '2025-03-05T04:00:00Z', versionNo: 1 },
    { id: 3002, fileType: 'VALIDATION_REPORT', ownedByKanrisha: false, originalFileName: '妥当性確認報告書.pdf', uploadedBy: '管理 次郎', uploadedAt: '2025-03-30T04:00:00Z', versionNo: 1 },
    { id: 3003, fileType: 'MONITORING_REPORT', ownedByKanrisha: true, originalFileName: 'モニタリング報告書_2025.xlsx', uploadedBy: '伊藤 美咲', uploadedAt: '2026-06-01T04:00:00Z', versionNo: 1 },
    { id: 3004, fileType: 'ACTIVITY_RESULT_LIST', ownedByKanrisha: true, originalFileName: '個別活動実績報告リスト.xlsx', uploadedBy: '伊藤 美咲', uploadedAt: '2026-06-01T05:00:00Z', versionNo: 1 },
    { id: 3005, fileType: 'ADDITIONAL_MATERIAL', ownedByKanrisha: true, originalFileName: '追加資料_設備写真.pdf', uploadedBy: '中村 翔', uploadedAt: '2026-07-12T04:00:00Z', versionNo: 1 },
  ],
};

export const planForms: Record<string, Record<string, unknown>> = {
  P001: {
    unitEmission: 't-CO2',
    certTargetPeriodStart: '2025-04-01',
    certTargetPeriodEnd: '2033-03-31',
    years: [2025, 2026, 2027, 2028, 2029, 2030, 2031, 2032].map((year, index) => ({
      year,
      newMembersTotal: index < 3 ? 4 : 2,
      cumulativeMembersTotal: Math.min(4 * (index + 1), 12 + index * 2),
    })),
  },
};

export const batchHistory = Array.from({ length: 23 }, (_, index) => {
  const start = new Date(Date.UTC(2026, 9, 7 - index, 15, 0, 0));
  const end = new Date(start.getTime() + (8 + (index % 5)) * 60 * 1000);
  const status = index === 3 ? 'FAILED' : index === 0 ? 'RUNNING' : 'COMPLETED';
  return {
    id: 2000 - index,
    status,
    is_active: true,
    created_at: start.toISOString(),
    created_by: 'scheduler',
    updated_at: end.toISOString(),
    updated_by: 'scheduler',
    start_time: start.toISOString(),
    end_time: status === 'RUNNING' ? null : end.toISOString(),
    claimed_by: 'batch-worker-1',
    claimed_at: start.toISOString(),
  };
});

export const findUser = (id: string | undefined | null) => users.find((user) => user.id === id);
export const findSite = (id: string | undefined | null) => sites.find((site) => site.siteId === id);
export const findProject = (id: string | undefined | null) => projects.find((project) => project.id === id);
export const siteName = (siteId: string) =>
  findSite(siteId)?.siteName ?? (siteId === 'S101' ? '仙台事業所' : siteId === 'S102' ? '福岡倉庫' : siteId);
