import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const api = vi.hoisted(() => ({
  getIndividualActivityRows: vi.fn(),
  getIndividualActivityDetail: vi.fn(),
}));
vi.mock('@/api/admin/individualActivity', () => api);

import { TooltipProvider } from '@/components/ui/tooltip';
import type { IndividualActivityDetail } from '@/api/admin/individualActivity';
import { IndividualActivityView } from './IndividualActivityView';
import { countBlanks, toSheetRow } from './individualActivitySections';

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

const detail: IndividualActivityDetail = {
  equipmentId: 7,
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
    nominalOutputKw: 5.5,
    battery: { manufacturer: null, model: null, capacityKwh: 9.8, chargeEfficiencyPercent: null, dischargeEfficiencyPercent: null },
    ictConsumptionKw: null,
    operationStartDate: '2024-01-01',
    effectiveFrom: null,
    effectiveTo: null,
    evidenceFiles: [{ fileType: 'PCS_PHOTO', url: 'https://files.example/equipment/pcs.pdf?sig=1' }],
  },
  additionality: null,
  registrationRequirements: { operatingInJapan: true, withinTwoYearsOfApplication: false, hasAdditionality: null, noDuplicateRegistration: null },
  monitoring: { samplingStatus: null, meteringPosition: null, generationMethod: null, saleMethod: null, windowStart: null, windowEnd: null, evidenceFiles: [] },
};

let writeText: ReturnType<typeof vi.fn>;

const renderView = () =>
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <TooltipProvider>
        <IndividualActivityView projectId="P1" copiedKey={null} onCopy={(_key, text) => void writeText(text)} />
      </TooltipProvider>
    </QueryClientProvider>,
  );

const itemsOf = (groups: ReturnType<typeof toSheetRow>['groups']) =>
  Object.fromEntries(groups.flatMap((g) => g.sections.flatMap((s) => s.items)).map((i) => [i.key, i]));

describe('toSheetRow', () => {
  it('lays the 蓄電池なし row out on the sheet’s columns, C to BB', () => {
    const { groups, tsv, range } = toSheetRow(detail, 'without', '1-1');
    const items = itemsOf(groups);
    expect(range).toBe('C〜BB');
    expect(tsv.split('\t')).toHaveLength(52);
    expect(items.id).toMatchObject({ value: '1-1', column: 'C' });
    expect(items.pcsUnitCount.column).toBe('M');
    expect(items.operatingInJapan).toMatchObject({ value: '○', column: 'AK' });
    expect(items.withinTwoYearsOfApplication.value).toBe('×');
    expect(items).not.toHaveProperty('batteryCapacityKwh');
  });

  it('adds the three battery columns on 蓄電池あり, shifting the rest', () => {
    const { groups, tsv, range } = toSheetRow(detail, 'with', '2-1');
    const items = itemsOf(groups);
    expect(range).toBe('C〜BE');
    expect(tsv.split('\t')).toHaveLength(55);
    expect(items.batteryCapacityKwh).toMatchObject({ value: '9.8', column: 'S' });
    expect(items.operatingInJapan.column).toBe('AN');
  });

  it('uses the pulldown wording and tells blanks to look up from manual ones', () => {
    const items = itemsOf(toSheetRow(detail, 'without', '1-1').groups);
    expect(items.eligibilityStatus.value).toBe('可');
    expect(items.carbonNeutralActionPlan.value).toBe('有');
    expect(items.energySavingActSpecifiedOperator.value).toBe('無');
    expect(items.pcsModel).toMatchObject({ value: null, source: 'equipment' });
    expect(items.pcsMeterClass).toMatchObject({ value: null, source: 'manual' });
    expect(countBlanks(toSheetRow(detail, 'without', '1-1').groups).manual).toBeGreaterThan(20);
  });
});

describe('IndividualActivityView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    writeText = vi.fn();
    api.getIndividualActivityRows.mockResolvedValue(rows);
    api.getIndividualActivityDetail.mockResolvedValue(detail);
  });

  it('splits rows by sheet and numbers them per sheet', async () => {
    renderView();
    expect(await screen.findByText('山田工業')).toBeInTheDocument();
    expect(screen.getByText('1-1')).toBeInTheDocument();
    expect(screen.getByText('1-2')).toBeInTheDocument();
    expect(screen.queryByText('佐藤商事')).not.toBeInTheDocument();

    fireEvent.mouseDown(screen.getByRole('tab', { name: /蓄電池あり/ }));
    expect(await screen.findByText('佐藤商事')).toBeInTheDocument();
    expect(screen.getByText('2-1')).toBeInTheDocument();
  });

  it('keeps IDs while filtering by report status', async () => {
    renderView();
    await screen.findByText('山田工業');
    fireEvent.click(within(screen.getByRole('group', { name: '報告状況で絞り込み' })).getByRole('button', { name: /報告可/ }));
    expect(screen.queryByText('山田工業')).not.toBeInTheDocument();
    expect(screen.getByText('1-2')).toBeInTheDocument();
  });

  it('copies a whole row without opening it', async () => {
    renderView();
    fireEvent.click(await screen.findByRole('button', { name: 'ID 1-1の行をコピー' }));
    await waitFor(() => expect(writeText).toHaveBeenCalled());
    expect(api.getIndividualActivityDetail).toHaveBeenCalledWith('P1', 7);
    expect(writeText.mock.calls[0][0].split('\t').slice(0, 4)).toEqual(['1-1', '可', '2025-04-01', '山田工業']);
  });

  it('loads the detail when a row opens, with each group’s evidence and column letters', async () => {
    renderView();
    fireEvent.click(await screen.findByRole('button', { name: /山田工業/ }));
    expect(await screen.findByText('パワーコンディショナー設備情報')).toBeInTheDocument();

    const equipment = screen.getByRole('region', { name: '設備' });
    expect(within(equipment).getByRole('button', { name: 'pcs.pdf' })).toBeInTheDocument();
    expect(within(equipment).getAllByText('設備の証憑で確認').length).toBeGreaterThan(0);

    const monitoring = screen.getByRole('region', { name: 'モニタリング情報' });
    expect(within(monitoring).getByText('まだ登録されていません')).toBeInTheDocument();
    expect(within(monitoring).getAllByText('Excelで手入力').length).toBeGreaterThan(0);
    expect(screen.getByText('C〜BB')).toBeInTheDocument();
  });
});
