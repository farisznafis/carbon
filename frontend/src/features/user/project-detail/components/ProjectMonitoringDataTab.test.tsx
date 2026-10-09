import { render, screen, within } from '@testing-library/react';
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

function renderTab() {
  render(
    <ProjectMonitoringDataTab
      siteId="S1"
      projectId="P1"
      equipments={[{ equipmentId: 7, deviceNo: 'PCS-001' }]}
    />,
  );
}

describe('ProjectMonitoringDataTab', () => {
  beforeEach(() => {
    getMonitoringRecordsMock.mockReset();
    createCancelRequestMock.mockReset();
    toastMock.mockReset();
  });

  it('lists records with their equipment name and status', async () => {
    getMonitoringRecordsMock.mockResolvedValue({
      items: [makeRecord(), makeRecord({ id: 'rec-2', isActive: false, equipmentId: 99 })],
      pagination: { totalItems: 2, totalPages: 1 },
    });
    renderTab();

    const rows = await screen.findAllByRole('row');
    expect(within(rows[1]).getByText('PCS-001')).toBeInTheDocument();
    expect(within(rows[1]).getByText('1,200 kWh')).toBeInTheDocument();
    expect(within(rows[1]).getByText('有効')).toBeInTheDocument();
    // Unknown equipment falls back to its id; withdrawn records cannot be requested again.
    expect(within(rows[2]).getByText('設備ID: 99')).toBeInTheDocument();
    expect(within(rows[2]).getByText('取消済み')).toBeInTheDocument();
    expect(within(rows[2]).queryByRole('button')).not.toBeInTheDocument();
    expect(getMonitoringRecordsMock).toHaveBeenCalledWith({
      siteId: 'S1',
      projectId: 'P1',
      page: 1,
      limit: 10,
    });
  });

  it('sends the record site, keys, and reason in the cancel request', async () => {
    const user = userEvent.setup();
    getMonitoringRecordsMock.mockResolvedValue({
      items: [makeRecord()],
      pagination: { totalItems: 1, totalPages: 1 },
    });
    createCancelRequestMock.mockResolvedValue({ statusCode: 'OK' });
    renderTab();

    await user.click(await screen.findByRole('button', { name: /取消申請$/ }));
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
    expect(await screen.findByText('取消申請中')).toBeInTheDocument();
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
