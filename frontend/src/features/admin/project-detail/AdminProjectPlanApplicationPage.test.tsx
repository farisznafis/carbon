import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminProjectDetail, ProjectPlanFormResponse } from '@/api/admin/types';
import {
  AdminPermissionContext,
  type AdminPermissionContextValue,
} from '@/features/admin/permissions/useAdminPermissions';
import AdminProjectPlanApplicationPage from './AdminProjectPlanApplicationPage';

const api = vi.hoisted(() => ({
  getAdminProjectDetail: vi.fn(),
  getProjectPlanProgress: vi.fn(),
  getProjectPlanForm: vi.fn(),
  getProjectPlanFileDownloadUrl: vi.fn(),
}));

vi.mock('@/api/admin/projects', () => api);
vi.mock('@/hooks/use-toast', () => ({ toast: vi.fn(), useToast: () => ({ toast: vi.fn() }) }));

const project = {
  id: 'PROJ1',
  projectName: '太陽光プログラム',
  projectNumber: 'PROJ1',
  projectStatus: 'ACTIVE',
} as unknown as AdminProjectDetail;

const storedOtherOperatorPage: ProjectPlanFormResponse = {
  statusCode: 'OK',
  formSlug: 'other-operator-projects',
  status: 'COMPLETE',
  updatedAt: '2026-09-10T01:00:00Z',
  updatedBy: 'operator1',
  conditionalFields: [
    { field: 'energyActOpeNumber', when: 'hasEnergyActPeriodReport', is: true },
  ],
  // Backend names: the form calls these projectOperatorNameKana / hasEnergyActReport.
  data: {
    projectOperatorKana: 'テストカブシキガイシャ',
    hasEnergyActPeriodReport: false,
    energyActOpeNumber: '無のはずの番号',
  },
};

/** Grants or withholds permissions without loading the operator profile. */
function permissionsWith(granted: string[]): AdminPermissionContextValue {
  const has = (code: string) => granted.includes(code);
  return {
    operatorId: '7',
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

function WizardProbe() {
  const { state } = useLocation() as { state?: { planSlug?: string } };
  return <p>wizard:{state?.planSlug ?? 'none'}</p>;
}

function renderPage(granted = ['PROJECT_READ', 'PROJECT_UPDATE']) {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <AdminPermissionContext.Provider value={permissionsWith(granted)}>
        <MemoryRouter initialEntries={['/admin/projects/PROJ1/project-plan']}>
          <Routes>
            <Route
              path="/admin/projects/:projectId/project-plan"
              element={<AdminProjectPlanApplicationPage />}
            />
            <Route path="/admin/project-register" element={<WizardProbe />} />
          </Routes>
        </MemoryRouter>
      </AdminPermissionContext.Provider>
    </QueryClientProvider>,
  );
}

/** Steps forward with 次へ; the dropdown is a Radix select, awkward in jsdom. */
async function goToStep(step: number) {
  await screen.findByText('プロジェクト計画書', { selector: 'h1' });
  for (let current = 1; current < step; current += 1) {
    fireEvent.click(screen.getByRole('button', { name: '次へ' }));
  }
}

describe('AdminProjectPlanApplicationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.getAdminProjectDetail.mockResolvedValue(project);
    api.getProjectPlanProgress.mockResolvedValue({
      totalForms: 31,
      completedForms: 1,
      forms: [
        { formSlug: 'other-operator-projects', displayName: '', order: 3, status: 'COMPLETE' },
      ],
    });
    api.getProjectPlanForm.mockImplementation(async (_id: string, slug: string) =>
      slug === 'other-operator-projects'
        ? storedOtherOperatorPage
        : { statusCode: 'OK', formSlug: slug, status: 'NOT_STARTED', data: {} },
    );
  });

  it('shows a stored value under a field the form names differently', async () => {
    renderPage();
    await goToStep(3);

    expect(await screen.findByText('テストカブシキガイシャ')).toBeInTheDocument();
    expect(screen.getByText('入力済み: 1 / 31 ページ')).toBeInTheDocument();
  });

  it('hides a field whose renamed 有/無 toggle is answered 無', async () => {
    renderPage();
    await goToStep(3);

    await screen.findByText('テストカブシキガイシャ');
    expect(screen.queryByText('無のはずの番号')).not.toBeInTheDocument();
  });

  it('does not present an unsaved page as filled in', async () => {
    renderPage();
    await goToStep(2);

    expect(await screen.findByText('このページはまだ入力されていません。')).toBeInTheDocument();
  });

  it('opens the wizard on the page being read', async () => {
    renderPage();
    await goToStep(3);
    await screen.findByText('テストカブシキガイシャ');

    fireEvent.click(screen.getByRole('button', { name: '更新' }));

    expect(await screen.findByText('wizard:other-operator-projects')).toBeInTheDocument();
  });

  it('keeps an operator without PROJECT_UPDATE on the page', async () => {
    renderPage(['PROJECT_READ']);
    await goToStep(3);
    await screen.findByText('テストカブシキガイシャ');

    fireEvent.click(screen.getByRole('button', { name: '更新' }));

    expect(screen.queryByText(/^wizard:/)).not.toBeInTheDocument();
    expect(screen.getByText('テストカブシキガイシャ')).toBeInTheDocument();
  });
});
