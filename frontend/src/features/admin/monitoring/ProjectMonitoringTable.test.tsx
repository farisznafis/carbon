import { fireEvent, render, screen } from '@testing-library/react';
import {
  getAdminProjectMonitoringRecords,
  type MonitoringRecordListItem,
} from '@/api/monitoring';
import { ProjectMonitoringTable } from './ProjectMonitoringTable';

vi.mock('@/api/monitoring', () => ({
  getAdminProjectMonitoringRecords: vi.fn(),
}));

const monitoringRecord: MonitoringRecordListItem = {
  id: 'monitoring-1',
  projectId: 'PROJ0007',
  siteId: 'SITE0003',
  equipmentId: 100,
  participatingUserId: 11,
  timeUpdated: '2026-07-23T10:30:00.000Z',
  dateRecorded: '2026-07-23',
  elpjControlValue: 8,
  elpjControlEquipmentSpecFiles: 'control.pdf',
  elpjBatteryValue: 12,
  elpjAccuracy: 99,
  elpjBatteryEvidence: 'battery.pdf',
  elPv: 120,
  elPvEvidence: 'elpv.pdf',
  elPvr: 75,
  elPvrEvidence: 'elpvr.pdf',
  elPvEvidenceUrl: 'https://example.com/elpv.pdf',
  elPvrEvidenceUrl: 'https://example.com/elpvr.pdf',
  elpjBatterySpecificationUrl: 'https://example.com/battery.pdf',
  elpjControlEvidenceUrl: 'https://example.com/control.pdf',
  changedBattery: false,
  isActive: true,
  processedAt: '2026-07-23T10:31:00.000Z',
  methodologyType: 'EN-R',
};

describe('ProjectMonitoringTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads records directly from the project-detail route context', async () => {
    vi.mocked(getAdminProjectMonitoringRecords).mockResolvedValue({
      items: [monitoringRecord],
      pagination: {
        page: 2,
        limit: 20,
        totalItems: 1,
        totalPages: 2,
        nextPageToken: null,
        hasMore: false,
      },
    });
    const onViewDetail = vi.fn();

    render(
      <ProjectMonitoringTable
        projectId="PROJ0007"
        initialPage={2}
        initialLimit={20}
        onViewDetail={onViewDetail}
      />,
    );

    expect(await screen.findByText('120 kWh')).toBeInTheDocument();
    expect(
      screen.queryByText('プロジェクトを選択してください'),
    ).not.toBeInTheDocument();
    expect(getAdminProjectMonitoringRecords).toHaveBeenCalledWith({
      projectId: 'PROJ0007',
      page: 2,
      limit: 20,
    });

    fireEvent.click(screen.getByRole('button', { name: '詳細' }));
    expect(onViewDetail).toHaveBeenCalledWith(monitoringRecord, 2, 20);
  });
});
