import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { MonitoringRecordListItem } from '@/api/monitoring';
import { PageBreadcrumb } from '@/components/navigation/PageBreadcrumb';
import { BreadcrumbProvider } from '@/components/navigation/BreadcrumbProvider';
import { usePageBreadcrumbItems } from '@/components/navigation/pageBreadcrumbContext';
import ProjectMonitoringDetailPage from './ProjectMonitoringDetailPage';

/** Renders the trail the page declares, as the app topbar does. */
function TopbarBreadcrumb() {
  return <PageBreadcrumb items={usePageBreadcrumbItems()} />;
}

const { getAdminProjectMonitoringRecords } = vi.hoisted(() => ({
  getAdminProjectMonitoringRecords: vi.fn(),
}));

vi.mock('@/api/monitoring', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/monitoring')>();
  return {
    ...actual,
    getAdminProjectMonitoringRecords,
  };
});

const record: MonitoringRecordListItem = {
  id: 'MON0001',
  projectId: 'PROJ0123',
  siteId: 'SITE0001',
  equipmentId: 100,
  participatingUserId: 7,
  timeUpdated: '2026-07-23T12:30:00Z',
  dateRecorded: '2026-07-23',
  elpjControlValue: 10,
  elpjControlEquipmentSpecFiles: 'control.pdf',
  elpjBatteryValue: 20,
  elpjAccuracy: 99.5,
  elpjBatteryEvidence: 'battery.pdf',
  elPv: 30,
  elPvEvidence: 'elpv.pdf',
  elPvr: 15,
  elPvrEvidence: 'elpvr.pdf',
  elPvEvidenceUrl: 'https://example.com/elpv.pdf',
  elPvrEvidenceUrl: 'https://example.com/elpvr.pdf',
  elpjBatterySpecificationUrl: 'https://example.com/battery.pdf',
  elpjControlEvidenceUrl: 'https://example.com/control.pdf',
  changedBattery: false,
  isActive: true,
  processedAt: '2026-07-23T13:00:00Z',
  methodologyType: 'EN-R-002',
};

describe('ProjectMonitoringDetailPage', () => {
  beforeEach(() => {
    getAdminProjectMonitoringRecords.mockReset();
    getAdminProjectMonitoringRecords.mockResolvedValue({
      items: [record],
      pagination: {
        page: 1,
        limit: 10,
        totalItems: 1,
        totalPages: 1,
        nextPageToken: null,
        hasMore: false,
      },
    });
  });

  it('loads the selected list record and displays all detail sections', async () => {
    render(
      <MemoryRouter
        initialEntries={[
          '/admin/projects/PROJ0123/monitoring/MON0001?page=1&limit=10',
        ]}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/admin/projects/:projectId/monitoring/:monitoringId"
            element={<ProjectMonitoringDetailPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'モニタリング詳細',
      }),
    ).toBeInTheDocument();
    expect(await screen.findByText('基本情報')).toBeInTheDocument();
    expect(screen.getByText('モニタリング値')).toBeInTheDocument();
    expect(screen.getByText('エビデンス情報')).toBeInTheDocument();
    expect(screen.getAllByText('MON0001').length).toBeGreaterThan(0);
    expect(screen.getByText('PROJ0123')).toBeInTheDocument();
    expect(screen.getByText('SITE0001')).toBeInTheDocument();
    expect(screen.getByText('EN-R-002')).toBeInTheDocument();
    expect(screen.getByText('30 kWh')).toBeInTheDocument();
    expect(screen.getByText('control.pdf')).toBeInTheDocument();
    expect(
      screen
        .getAllByRole('link', { name: /ファイルを開く/ })
        .some(
          (link) =>
            link.getAttribute('href') === 'https://example.com/elpv.pdf',
        ),
    ).toBe(true);
  });

  it('uses the clicked row data even when the list still returns a legacy project ID', async () => {
    const legacyProjectRecord = {
      ...record,
      projectId: '2',
    };

    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname:
              '/admin/projects/PROJ0002/monitoring/MON0001',
            search: '?page=1&limit=10',
            state: { monitoringRecord: legacyProjectRecord },
          },
        ]}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <Routes>
          <Route
            path="/admin/projects/:projectId/monitoring/:monitoringId"
            element={<ProjectMonitoringDetailPage />}
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(await screen.findByText('基本情報')).toBeInTheDocument();
    expect(screen.getByText('モニタリング値')).toBeInTheDocument();
    expect(screen.getByText('エビデンス情報')).toBeInTheDocument();
    expect(screen.getByText('30 kWh')).toBeInTheDocument();
    expect(getAdminProjectMonitoringRecords).not.toHaveBeenCalled();
  });

  it('returns to the project detail instead of the admin dashboard', async () => {
    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/admin/projects/PROJ0123/monitoring/MON0001',
            search: '?page=2&limit=20',
            state: { monitoringRecord: record },
          },
        ]}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <BreadcrumbProvider>
        <TopbarBreadcrumb />
        <Routes>
          <Route
            path="/admin/projects/:projectId/monitoring/:monitoringId"
            element={<ProjectMonitoringDetailPage />}
          />
          <Route
            path="/admin/projects/:projectId"
            element={<div>プロジェクト詳細へ戻りました</div>}
          />
        </Routes>
        </BreadcrumbProvider>
      </MemoryRouter>,
    );

    fireEvent.click(
      await screen.findByRole('link', {
        name: 'モニタリングデータ',
      }),
    );

    expect(
      await screen.findByText('プロジェクト詳細へ戻りました'),
    ).toBeInTheDocument();
  });
});
