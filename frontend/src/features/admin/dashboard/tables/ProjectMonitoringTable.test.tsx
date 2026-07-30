import { fireEvent, render, screen } from '@testing-library/react';
import {
  getAdminProjectMonitoringRecords,
  type MonitoringRecordListItem,
} from '@/api/monitoring';
import type { Project } from '@/api/projects';
import { ProjectMonitoringTable } from './ProjectMonitoringTable';

vi.mock('@/api/monitoring', () => ({
  getAdminProjectMonitoringRecords: vi.fn(),
}));

const project: Project = {
  id: 'PROJ0007',
  projectName: '太陽光発電プロジェクト',
  projectStatus: 'ACTIVE',
  methodologyVersion: '1.0',
  methodCategory: 'EN-R',
  methodNumber: '002',
  methodName: '太陽光発電設備の導入',
  methodMainEmission: '排出削減活動',
  methodCombiFeasibility: false,
};

const monitoringRecord: MonitoringRecordListItem = {
  id: 'monitoring-1',
  projectId: 'PROJ0007',
  siteId: 'SITE0003',
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

const defaultProps = {
  projects: [project],
  projectsLoading: false,
  projectsError: null,
  onProjectChange: vi.fn(),
  onViewDetail: vi.fn(),
};

describe('ProjectMonitoringTable', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('does not request monitoring data before a project is selected', () => {
    render(
      <ProjectMonitoringTable
        {...defaultProps}
        selectedProjectId=""
      />,
    );

    expect(
      screen.getByText('表示するプロジェクトを選択してください。'),
    ).toBeInTheDocument();
    expect(getAdminProjectMonitoringRecords).not.toHaveBeenCalled();
  });

  it('loads the selected project monitoring data and opens its detail', async () => {
    vi.mocked(getAdminProjectMonitoringRecords).mockResolvedValue({
      items: [monitoringRecord],
      pagination: {
        page: 1,
        limit: 10,
        totalItems: 1,
        totalPages: 1,
        nextPageToken: null,
        hasMore: false,
      },
    });
    const onViewDetail = vi.fn();

    render(
      <ProjectMonitoringTable
        {...defaultProps}
        selectedProjectId="PROJ0007"
        onViewDetail={onViewDetail}
      />,
    );

    expect(await screen.findByText('120 kWh')).toBeInTheDocument();
    expect(getAdminProjectMonitoringRecords).toHaveBeenCalledWith({
      projectId: 'PROJ0007',
      page: 1,
      limit: 10,
    });

    fireEvent.click(screen.getByRole('button', { name: '詳細' }));
    expect(onViewDetail).toHaveBeenCalledWith(monitoringRecord, 1, 10);
  });
});
