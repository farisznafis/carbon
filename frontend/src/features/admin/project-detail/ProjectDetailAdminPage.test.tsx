import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminProjectDetail } from '@/api/admin/types';
import { mapParticipationReviewDetail } from '@/api/admin/participationReviewMapper';
import {
  AdminPermissionContext,
  type AdminPermissionContextValue,
} from '@/features/admin/permissions/useAdminPermissions';
import ProjectDetailAdmin from './ProjectDetailAdminPage';

const projectsApi = vi.hoisted(() => ({
  getAdminProjectDetail: vi.fn(),
}));
const projectFilesApi = vi.hoisted(() => ({
  getProjectFiles: vi.fn(),
}));
const reviewsApi = vi.hoisted(() => ({
  getParticipationReviewsByStatus: vi.fn(),
}));

vi.mock('@/api/admin/projects', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/admin/projects')>()),
  ...projectsApi,
}));
vi.mock('@/api/admin/projectFiles', () => projectFilesApi);
vi.mock('@/api/admin', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/admin')>()),
  ...projectsApi,
  ...reviewsApi,
}));
// Charts and the monitoring table have their own tests and data sources.
vi.mock('./components/ProjectEmissionTrendCard', () => ({
  ProjectEmissionTrendCard: () => <div>emission chart</div>,
}));
vi.mock('@/features/admin/monitoring/MonitoringDeletionHistoryTable', () => ({
  MonitoringDeletionHistoryDialog: () => <button type="button">削除履歴</button>,
}));
vi.mock('@/features/admin/monitoring/ProjectMonitoringTable', () => ({
  ProjectMonitoringTable: () => <div>monitoring table</div>,
}));

const project = {
  id: '7',
  projectName: '太陽光プロジェクト',
  projectStatus: '実施中',
} as unknown as AdminProjectDetail;

/** Creates a mapped membership review owned by the given project. */
function review(participationId: number, projectId: number) {
  return mapParticipationReviewDetail({
    participationId,
    siteId: participationId,
    projectId,
    projectName: projectId === 7 ? '太陽光プロジェクト' : `別プロジェクト${projectId}`,
    siteName: `拠点${participationId}`,
    companyName: `会社${participationId}`,
    submittedAt: '2026-07-30T06:50:55.732Z',
    equipment: {},
    confirmationItems: {},
    sustainabilityCommitment: {},
    review: { primaryApplReviewerName: '', secondaryApplReviewerName: '' },
  });
}

/** Grants permissions without loading the operator profile. */
function permissionsWith(granted: string[]): AdminPermissionContextValue {
  const has = (code: string) => granted.includes(code);
  return {
    operatorId: '1',
    role: 'operator',
    permissions: new Set(granted) as AdminPermissionContextValue['permissions'],
    isAdmin: false,
    hasAnyPermission: granted.length > 0,
    isLoading: false,
    isError: false,
    hasPermission: has,
    guardAction: (code, action) => {
      if (!has(code)) return false;
      action();
      return true;
    },
    retry: vi.fn(),
  };
}

function LocationProbe() {
  const location = useLocation();
  return <output data-testid="location">{`${location.pathname}${location.search}`}</output>;
}

/** Project and 入会届 load one after another; allow for a busy test runner. */
const LOAD = { timeout: 5000 };

const ALL_PERMISSIONS = [
  'PROJECT_READ',
  'MONITORING_READ',
  'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
];

function renderPage(granted = ALL_PERMISSIONS) {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <AdminPermissionContext.Provider value={permissionsWith(granted)}>
        <MemoryRouter initialEntries={['/admin/projects/7']}>
          <LocationProbe />
          <Routes>
            <Route path="/admin/projects/:projectId" element={<ProjectDetailAdmin />} />
          </Routes>
        </MemoryRouter>
      </AdminPermissionContext.Provider>
    </QueryClientProvider>,
  );
}

describe('ProjectDetailAdmin membership visibility', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    projectsApi.getAdminProjectDetail.mockResolvedValue(project);
    projectFilesApi.getProjectFiles.mockResolvedValue([
      { id: 2, originalFileName: '計画書_v2.xlsx', uploadedAt: '2026-10-07T01:00:00' },
      { id: 1, originalFileName: '計画書.xlsx', uploadedAt: '2026-10-01T01:00:00' },
    ]);
    reviewsApi.getParticipationReviewsByStatus.mockResolvedValue({
      OPEN: [review(1, 7), review(2, 7), review(3, 8)],
      APPROVED: [review(4, 7), review(5, 7), review(6, 7)],
      REJECTED: [],
    });
  });

  it('shows the pending banner, status chips and tab badge for this project only', async () => {
    renderPage();

    expect(
      await screen.findByText('承認待ちの入会届が2件あります。', undefined, LOAD),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('tab', { name: '入会届（承認待ち2件）' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: '承認待ちの入会届2件を表示' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: '承認済みの入会届3件を表示' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: '却下の入会届0件を表示' }),
    ).toBeInTheDocument();
  });

  it('opens the 入会届 tab on the chosen status from the banner and chips', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: '確認する' }, LOAD));
    expect(screen.getByTestId('location')).toHaveTextContent(
      '/admin/projects/7?tab=membership&status=OPEN',
    );
    expect(screen.getByRole('tab', { name: '入会届（承認待ち2件）' })).toHaveAttribute(
      'data-state',
      'active',
    );

    await user.click(screen.getByRole('tab', { name: '概要' }));
    await user.click(screen.getByRole('button', { name: '承認済みの入会届3件を表示' }));
    expect(screen.getByTestId('location')).toHaveTextContent(
      '/admin/projects/7?tab=membership&status=APPROVED',
    );
  });

  it('hides the banner and badge when nothing is waiting', async () => {
    reviewsApi.getParticipationReviewsByStatus.mockResolvedValue({
      OPEN: [review(3, 8)],
      APPROVED: [review(4, 7)],
      REJECTED: [],
    });
    renderPage();

    expect(
      await screen.findByRole('button', { name: '承認待ちの入会届0件を表示' }, LOAD),
    ).toBeInTheDocument();
    expect(screen.queryByText(/承認待ちの入会届が/)).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '入会届' })).toBeInTheDocument();
  });

  it('does not load or show membership data without the review permission', async () => {
    renderPage(['PROJECT_READ', 'MONITORING_READ']);

    expect(await screen.findByText('プロジェクト概要', undefined, LOAD)).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /の入会届.*件を表示/ }),
    ).not.toBeInTheDocument();
    expect(screen.queryByText(/承認待ちの入会届が/)).not.toBeInTheDocument();
    expect(reviewsApi.getParticipationReviewsByStatus).not.toHaveBeenCalled();
  });
});
