import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({
  getIndividualActivityRows: vi.fn(),
  getIndividualActivityDetail: vi.fn(),
}));
vi.mock('@/api/admin/individualActivity', () => api);
const siteApi = vi.hoisted(() => ({ getSiteProjectEquipmentPresignedUrl: vi.fn() }));
vi.mock('@/api/site', () => siteApi);

import { TooltipProvider } from '@/components/ui/tooltip';
import type { IndividualActivityDetail } from '@/api/admin/individualActivity';
import { IndividualActivityView } from './IndividualActivityView';
import { evidenceFilesOf, toActivityGroups } from './individualActivitySections';

const row = (equipmentId: number, companyName: string, hasBattery: boolean, reportReadiness: string) => ({
  equipmentId,
  companyId: `C${equipmentId}`,
  companyName,
  siteId: `S${equipmentId}`,
  siteAddress: `東京都千代田区${equipmentId}`,
  hasBattery,
  eligibilityStatus: 'ELIGIBLE',
  reportReadiness,
  generationKwh: 12345.6,
  saleKwh: 1000,
});

const rows = [
  row(7, '山田工業', false, 'MISSING_EVIDENCE'),
  row(8, '佐藤商事', true, 'READY'),
  row(9, '鈴木電機', false, 'READY'),
];

const detailOf = (equipmentId: number): IndividualActivityDetail => ({
  equipmentId,
  member: {
    companyId: 'C7',
    companyName: '山田工業',
    siteId: 'S7',
    address: '東京都千代田区1-1',
    membershipApplicationDate: '2025-04-01',
    eligibilityStatus: 'ELIGIBLE',
    businessOperatorFlags: { carbonNeutralActionPlan: true, energySavingActSpecifiedOperator: false, warmingActSpecifiedEmitter: null, emitterCode: null },
  },
  equipment: {
    pcs: { manufacturer: 'オムロン', model: null, unitCount: 1, serialNumber: 'SN-1', standbyPowerKw: 0.01, meterClass: null, maxErrorRatePercent: null },
    nominalOutputKw: null,
    battery: { manufacturer: null, model: null, capacityKwh: 9.8, chargeEfficiencyPercent: null, dischargeEfficiencyPercent: null },
    ictConsumptionKw: null,
    operationStartDate: '2024-01-01',
    effectiveFrom: null,
    effectiveTo: null,
    evidenceFiles: [
      { fileType: 'pcs_spec', url: 's3://gx-equipments-files-dev/equipment/3/pcs_spec/abc.pdf' },
      { fileType: 'solar_contract', url: 's3://gx-equipments-files-dev/equipment/3/solar_contract/def.pdf' },
    ],
  },
  additionality: null,
  registrationRequirements: { operatingInJapan: true, withinTwoYearsOfApplication: false, hasAdditionality: null, noDuplicateRegistration: null },
  monitoring: { samplingStatus: null, meteringPosition: null, generationMethod: null, saleMethod: null, windowStart: null, windowEnd: null, evidenceFiles: [] },
});

const renderView = () =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <TooltipProvider>
        <IndividualActivityView projectId="P1" copiedKey={null} onCopy={vi.fn()} />
      </TooltipProvider>
    </QueryClientProvider>,
  );

const itemsOf = (groups: ReturnType<typeof toActivityGroups>) =>
  Object.fromEntries(groups.flatMap((g) => g.sections.flatMap((s) => s.items)).map((i) => [i.key, i]));

describe('toActivityGroups', () => {
  it('follows the sheet’s columns, adding battery ones only on 蓄電池あり', () => {
    const titles = (sheet: 'with' | 'without') =>
      toActivityGroups(detailOf(7), sheet).flatMap((g) => g.sections.map((s) => s.title));
    expect(titles('without')).toEqual([
      '会員基本情報',
      'パワーコンディショナー設備情報',
      '太陽光発電設備情報',
      'ICT機器',
      '追加的設備情報',
      '基準日',
      '認証対象期間',
      '追加性評価',
      'プロジェクト登録要件',
      'モニタリング',
      '今回認証のモニタリング開始日',
      '今回認証のモニタリング終了日',
    ]);
    expect(titles('with')).toContain('蓄電池');
  });

  it('uses the pulldown wording and pairs each column with its evidence', () => {
    const items = itemsOf(toActivityGroups(detailOf(7), 'without'));
    expect(items).not.toHaveProperty('id');
    expect(items.eligibilityStatus.value).toBe('可');
    expect(items.carbonNeutralActionPlan.value).toBe('有');
    expect(items.operatingInJapan.value).toBe('○');
    expect(items.withinTwoYearsOfApplication.value).toBe('×');
    expect(items.pcsModel).toMatchObject({ value: null, source: 'evidence', evidence: 'pcs_spec' });
    expect(items.nominalOutputKw).toMatchObject({ value: null, evidence: 'panel_catalog' });
    expect(items.totalInvestment.evidence).toBe('construction_invoice');
    expect(items.pcsMeterClass).toMatchObject({ value: null, source: 'manual' });
  });
});

describe('evidenceFilesOf', () => {
  it('keeps the S3 key to sign, in upload order', () => {
    expect(evidenceFilesOf(detailOf(7))).toEqual([
      { location: 'equipment/3/solar_contract/def.pdf', kind: 'solar_contract', fileName: 'def.pdf' },
      { location: 'equipment/3/pcs_spec/abc.pdf', kind: 'pcs_spec', fileName: 'abc.pdf' },
    ]);
  });
});

describe('IndividualActivityView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.getIndividualActivityRows.mockResolvedValue(rows);
    siteApi.getSiteProjectEquipmentPresignedUrl.mockResolvedValue('https://signed.example/abc.pdf');
    api.getIndividualActivityDetail.mockImplementation((_projectId: string, id: number) => Promise.resolve(detailOf(id)));
  });

  it('lists one sheet at a time and shows the first row', async () => {
    renderView();
    const list = await screen.findByRole('complementary', { name: '設備一覧' });
    expect(within(list).getByText('山田工業')).toBeInTheDocument();
    expect(within(list).getByText('鈴木電機')).toBeInTheDocument();
    expect(within(list).queryByText(/^\d-\d+$/)).not.toBeInTheDocument();
    expect(within(list).queryByText('佐藤商事')).not.toBeInTheDocument();
    // The first row opens on its own, on the 会員 group.
    expect(await screen.findByText('会員基本情報')).toBeInTheDocument();
    expect(api.getIndividualActivityDetail).toHaveBeenCalledWith('P1', 7);

    fireEvent.mouseDown(screen.getByRole('tab', { name: /蓄電池あり/ }));
    expect(await within(list).findByText('佐藤商事')).toBeInTheDocument();
  });

  it('walks the rows with previous/next', async () => {
    renderView();
    const next = await screen.findByRole('button', { name: '次の設備' });
    expect(screen.getByText('1 / 2')).toBeInTheDocument();
    fireEvent.click(next);
    expect(screen.getByText('2 / 2')).toBeInTheDocument();
    expect(api.getIndividualActivityDetail).toHaveBeenCalledWith('P1', 9);
    expect(screen.getByRole('button', { name: '次の設備' })).toBeDisabled();
  });

  it('shows blanks as a dash, with each evidence source under its own tab', async () => {
    renderView();
    fireEvent.mouseDown(await screen.findByRole('tab', { name: '設備' }));

    const details = screen.getByRole('region', { name: '設備の詳細' });
    expect(await within(details).findByText('パワーコンディショナー設備情報')).toBeInTheDocument();
    expect(within(details).getAllByText('—').length).toBeGreaterThan(0);

    fireEvent.mouseDown(within(details).getByRole('tab', { name: 'モニタリング' }));
    expect(within(details).getByText('登録されていません')).toBeInTheDocument();
    expect(within(details).queryByRole('button', { name: 'PCS仕様書' })).not.toBeInTheDocument();

    fireEvent.mouseDown(within(details).getByRole('tab', { name: '設備' }));
    fireEvent.click(within(details).getByRole('button', { name: 'PCS仕様書' }));
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(siteApi.getSiteProjectEquipmentPresignedUrl).toHaveBeenCalledWith('equipment/3/pcs_spec/abc.pdf');
  });
});
