import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { SiteProject } from '@/api/site';
import ProjectMembershipPage from './ProjectMembershipPage';

const { getSiteData, getSiteProjects, downloadProjectMembershipPdf } =
  vi.hoisted(() => ({
    getSiteData: vi.fn(),
    getSiteProjects: vi.fn(),
    downloadProjectMembershipPdf: vi.fn(),
  }));

vi.mock('@/api/site', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/site')>();
  return {
    ...actual,
    getSiteData,
    getSiteProjects,
    downloadProjectMembershipPdf,
  };
});

vi.mock('@/hooks/use-toast', () => ({
  useToast: () => ({ toast: vi.fn() }),
}));

vi.mock('@/stores/dashboardStore', () => ({
  useDashboardStore: (
    selector: (state: { setCurrentProject: ReturnType<typeof vi.fn> }) => unknown,
  ) => selector({ setCurrentProject: vi.fn() }),
}));

function createProject(
  id: string,
  projectName: string,
  isJoined: boolean,
): SiteProject {
  return {
    id,
    projectName,
    projectNumber: id,
    projectStatus: '実施中',
    isJoined,
    methodName: '方法論',
    methodNumber: '001',
    methodMainEmission: 'CO2',
  } as SiteProject;
}

function renderPage() {
  return render(
    <MemoryRouter
      initialEntries={['/projects/site001/register']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route
          path="/projects/:siteId/register"
          element={<ProjectMembershipPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProjectMembershipPage tabs', () => {
  beforeEach(() => {
    getSiteData.mockReset();
    getSiteProjects.mockReset();
    downloadProjectMembershipPdf.mockReset();
    getSiteData.mockResolvedValue({
      site: { siteId: 'site001', siteName: '東京拠点' },
      metrics: null,
    });
  });

  it('shows available projects by default and switches to joined projects', async () => {
    const user = userEvent.setup();
    getSiteProjects.mockResolvedValue([
      createProject('PROJ001', '入会可能プロジェクト', false),
      createProject('PROJ002', '入会中プロジェクト', true),
    ]);

    renderPage();

    expect(
      await screen.findByRole('heading', { name: '入会可能プロジェクト' }),
    ).toBeVisible();
    expect(
      screen.getByRole('tab', { name: /入会可能なプロジェクト\s*1/ }),
    ).toHaveAttribute('data-state', 'active');
    expect(
      screen.getByRole('tab', { name: /入会中のプロジェクト\s*1/ }),
    ).toHaveAttribute('data-state', 'inactive');

    await user.click(
      screen.getByRole('tab', { name: /入会中のプロジェクト\s*1/ }),
    );

    expect(
      screen.getByRole('heading', { name: '入会中プロジェクト' }),
    ).toBeVisible();
    expect(
      screen.queryByRole('heading', { name: '入会可能プロジェクト' }),
    ).not.toBeInTheDocument();
  });

  it('shows the empty state only inside its selected tab', async () => {
    const user = userEvent.setup();
    getSiteProjects.mockResolvedValue([
      createProject('PROJ001', '入会可能プロジェクト', false),
    ]);

    renderPage();

    await screen.findByRole('heading', { name: '入会可能プロジェクト' });
    expect(
      screen.queryByText('入会中のプロジェクトはありません。'),
    ).not.toBeInTheDocument();

    await user.click(
      screen.getByRole('tab', { name: /入会中のプロジェクト\s*0/ }),
    );

    expect(screen.getByText('入会中のプロジェクトはありません。')).toBeVisible();
  });
});
