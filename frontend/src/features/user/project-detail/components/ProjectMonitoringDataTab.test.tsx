import { render, screen, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { MonitoringRecordListItem } from '@/api/monitoring';
import { ProjectMonitoringDataTab } from './ProjectMonitoringDataTab';

const { getMonitoringRecordsMock, createCancelRequestMock, toastMock } = vi.hoisted(() => ({
  getMonitoringRecordsMock: vi.fn(),
  createCancelRequestMock: vi.fn(),
  toastMock: vi.fn(),
}));

vi.mock('@/api/monitoring', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/monitoring')>()),
  getMonitoringRecords: getMonitoringRecordsMock,
  createMonitoringCancelRequest: createCancelRequestMock,
}));

vi.mock('@/hooks/use-toast', () => ({ useToast: () => ({ toast: toastMock }) }));

function makeRecord(overrides: Partial<MonitoringRecordListItem> = {}): MonitoringRecordListItem {
  return {
    id: 'rec-1',
    projectId: 'P1',
    siteId: 'S1',
    equipmentId: 7,
    participatingUserId: 1,
    timeUpdated: '2026-10-01T00:00:00Z',
    dateRecorded: '2026-09-30',
    elpjControlValue: null,
    elpjControlEquipmentSpecFiles: null,
    elpjBatteryValue: null,
    elpjAccuracy: null,
    elpjBatteryEvidence: null,
    elPv: 1200,
    elPvEvidence: null,
    elPvr: 300,
    elPvrEvidence: null,
    elPvEvidenceUrl: null,
    elPvrEvidenceUrl: null,
    elpjBatterySpecificationUrl: null,
    elpjControlEvidenceUrl: null,
    changedBattery: false,
    isActive: true,
    processedAt: null,
    methodologyType: 'EN-R-002',
    ...overrides,
  };
}

/** A fresh cache per test; no retries so a failed load shows its error at once. */
function renderTab(queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })) {
  const tab = (
    <QueryClientProvider client={queryClient}>
      <ProjectMonitoringDataTab siteId="S1" projectId="P1" />
    </QueryClientProvider>
  );
  return { ...render(tab), tab };
}

describe('ProjectMonitoringDataTab', () => {
  beforeEach(() => {
    getMonitoringRecordsMock.mockReset();
    createCancelRequestMock.mockReset();
    toastMock.mockReset();
  });

  it('lists the distinguishing columns with units in the headers', async () => {
    getMonitoringRecordsMock.mockResolvedValue({
      items: [
        makeRecord({ processedAt: '2026-10-02T00:00:00Z' }),
        makeRecord({ id: 'rec-2', isActive: false, equipmentId: 99 }),
      ],
      pagination: { totalItems: 2, totalPages: 1 },
    });
    renderTab();

    expect(await screen.findByText('発電量 (kWh)')).toBeInTheDocument();
    expect(screen.queryByText('設備')).not.toBeInTheDocument();
    expect(screen.getAllByText('1,200')).toHaveLength(2);
    expect(screen.getByText('集計済み')).toBeInTheDocument();
    expect(screen.getByText('取消済み')).toBeInTheDocument();
    // Detail-only values stay out of the list, and so does the per-row 取消申請.
    expect(screen.queryByText('測定精度')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '取消申請' })).not.toBeInTheDocument();
    expect(getMonitoringRecordsMock).toHaveBeenCalledWith({
      siteId: 'S1',
      projectId: 'P1',
      page: 1,
      limit: 10,
    });
  });

  it('shows the whole record in the side panel', async () => {
    const user = userEvent.setup();
    getMonitoringRecordsMock.mockResolvedValue({
      items: [
        makeRecord({
          changedBattery: true,
          elpjBatteryValue: 15.5,
          elpjControlValue: 24,
          elpjAccuracy: 98,
          timeUpdated: '2026-08-07T03:03:28.139719300Z',
        }),
      ],
      pagination: { totalItems: 1, totalPages: 1 },
    });
    renderTab();

    await user.click(await screen.findByRole('button', { name: '2026年9月30日のモニタリングデータの詳細を開く' }));
    const panel = await screen.findByRole('dialog', { name: '2026年9月30日 のモニタリングデータ' });

    expect(within(panel).getByText('15.5 kWh')).toBeInTheDocument();
    expect(within(panel).getByText('24 時間')).toBeInTheDocument();
    expect(within(panel).getByText('98 %')).toBeInTheDocument();
    // Nanosecond timestamps from the API still format.
    expect(within(panel).getByText(/2026年8月7日 \d{2}:\d{2}/)).toBeInTheDocument();
    expect(within(panel).getByText('未集計')).toBeInTheDocument();
    expect(within(panel).getByRole('button', { name: '取消申請' })).toBeInTheDocument();
  });

  it('sends the record site, keys, and reason in the cancel request', async () => {
    const user = userEvent.setup();
    getMonitoringRecordsMock.mockResolvedValue({
      items: [makeRecord()],
      pagination: { totalItems: 1, totalPages: 1 },
    });
    createCancelRequestMock.mockResolvedValue({ statusCode: 'OK' });
    renderTab();

    await user.click(await screen.findByRole('button', { name: /詳細を開く/ }));
    await user.click(await screen.findByRole('button', { name: '取消申請' }));
    const confirm = screen.getByRole('button', { name: '取消申請を送信' });
    expect(confirm).toBeDisabled();

    await user.type(screen.getByLabelText(/理由/), '  二重登録  ');
    await user.click(confirm);

    expect(createCancelRequestMock).toHaveBeenCalledWith({
      siteId: 'S1',
      recordPK: 'rec-1',
      recordSK: '2026-10-01T00:00:00Z',
      reasonText: '二重登録',
    });
    // The panel stays open and now explains the request is pending.
    expect(await screen.findByText(/取消申請を送信済みです/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '取消申請' })).not.toBeInTheDocument();
  });

  it('reopens from cache without refetching and keeps the 取消申請中 mark', async () => {
    const user = userEvent.setup();
    getMonitoringRecordsMock.mockResolvedValue({
      items: [makeRecord()],
      pagination: { totalItems: 1, totalPages: 1 },
    });
    createCancelRequestMock.mockResolvedValue({ statusCode: 'OK' });
    const { unmount, tab } = renderTab();

    await user.click(await screen.findByRole('button', { name: /詳細を開く/ }));
    await user.click(await screen.findByRole('button', { name: '取消申請' }));
    await user.type(screen.getByLabelText(/理由/), '二重登録');
    await user.click(screen.getByRole('button', { name: '取消申請を送信' }));
    await screen.findByText(/取消申請を送信済みです/);

    // Switching tabs unmounts the panel; coming back must not lose either.
    unmount();
    render(tab);

    expect(screen.getByText('取消申請中')).toBeInTheDocument();
    expect(getMonitoringRecordsMock).toHaveBeenCalledTimes(1);
  });

  it('shows the empty state and retries after a load failure', async () => {
    const user = userEvent.setup();
    getMonitoringRecordsMock
      .mockRejectedValueOnce(new Error('取得できませんでした'))
      .mockResolvedValueOnce({ items: [], pagination: { totalItems: 0, totalPages: 0 } });
    renderTab();

    expect(await screen.findByRole('alert')).toHaveTextContent('取得できませんでした');
    await user.click(screen.getByRole('button', { name: /再/ }));
    expect(await screen.findByText('モニタリングデータはまだ登録されていません。')).toBeInTheDocument();
  });
});
