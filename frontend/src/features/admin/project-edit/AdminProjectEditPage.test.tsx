import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const adminApi = vi.hoisted(() => ({
  getAdminProjectDetail: vi.fn(),
  getProjectPlanForm: vi.fn(),
  updateAdminProject: vi.fn(),
  saveProjectPlanForm: vi.fn(),
}));
const toastMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/admin', () => adminApi);
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 'admin-1' } }),
}));
vi.mock('@/hooks/use-toast', () => ({ toast: toastMock }));
// The file list has its own tests and its own data source.
vi.mock('@/features/admin/project-plan/PlanFileList', () => ({
  PlanFileList: ({ canUpload }: { canUpload?: boolean }) => (
    <div data-testid="plan-files">{canUpload ? 'upload allowed' : 'read only'}</div>
  ),
}));

import AdminProjectEditPage from './AdminProjectEditPage';

function renderPage() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={['/admin/projects/PROJ1/edit']}>
        <Routes>
          <Route path="/admin/projects/:projectId/edit" element={<AdminProjectEditPage />} />
          <Route path="/admin/projects/:projectId" element={<p>project detail</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('AdminProjectEditPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminApi.getAdminProjectDetail.mockResolvedValue({
      id: 'PROJ1',
      projectName: '太陽光プログラム',
      regionalCategory: '関東',
    });
    adminApi.getProjectPlanForm.mockResolvedValue({
      data: {
        unitEmission: 'kg-CO2',
        certTargetPeriodStart: '2025-10-01',
        certTargetPeriodEnd: '2027-03-31',
        years: [
          { year: 2025, newMembersTotal: 500, cumulativeMembersTotal: 500 },
          { year: 2026, newMembersTotal: 450, cumulativeMembersTotal: 950 },
        ],
      },
    });
    adminApi.updateAdminProject.mockResolvedValue({});
    adminApi.saveProjectPlanForm.mockResolvedValue({});
  });

  it('opens with the plan file, the stored plan values and the project record on one page', async () => {
    renderPage();

    expect(await screen.findByLabelText('認証対象期間（開始日）')).toHaveValue('2025-10-01');
    expect(screen.getByLabelText('認証対象期間（終了日）')).toHaveValue('2027-03-31');
    expect(screen.getByLabelText('2026年度 新規会員数（件）')).toHaveValue(450);
    expect(screen.getByTestId('plan-files')).toHaveTextContent('upload allowed');
    expect(adminApi.getProjectPlanForm).toHaveBeenCalledWith('PROJ1', 'emission-reduction-plan');
    expect(screen.queryByRole('button', { name: '次へ' })).not.toBeInTheDocument();
  });

  it('更新 saves the project record and then the plan values', async () => {
    renderPage();
    fireEvent.change(await screen.findByLabelText('2026年度 新規会員数（件）'), {
      target: { value: '400' },
    });

    fireEvent.click(screen.getByRole('button', { name: '更新' }));

    expect(await screen.findByText('project detail')).toBeInTheDocument();
    expect(adminApi.updateAdminProject).toHaveBeenCalledWith(
      'PROJ1',
      expect.objectContaining({ projectName: '太陽光プログラム', processedBy: 'admin-1' }),
    );
    expect(adminApi.saveProjectPlanForm).toHaveBeenCalledWith(
      'PROJ1',
      'emission-reduction-plan',
      {
        // The unit the stored page already had is sent back, not replaced.
        unitEmission: 'kg-CO2',
        certTargetPeriodStart: '2025-10-01',
        certTargetPeriodEnd: '2027-03-31',
        years: [
          { year: 2025, newMembersTotal: 500, cumulativeMembersTotal: 500 },
          { year: 2026, newMembersTotal: 400, cumulativeMembersTotal: 950 },
        ],
      },
    );
    expect(adminApi.updateAdminProject.mock.invocationCallOrder[0]).toBeLessThan(
      adminApi.saveProjectPlanForm.mock.invocationCallOrder[0],
    );
  });

  it('still lets the project be edited when the plan values cannot be read', async () => {
    adminApi.getProjectPlanForm.mockRejectedValue(new Error('取得できませんでした。'));
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: '更新' }));

    expect(await screen.findByText('project detail')).toBeInTheDocument();
    expect(adminApi.updateAdminProject).toHaveBeenCalledTimes(1);
    expect(adminApi.saveProjectPlanForm).not.toHaveBeenCalled();
  });

  it('says the project was saved when only the plan values are refused', async () => {
    adminApi.saveProjectPlanForm.mockRejectedValue(new Error('入力内容に誤りがあります。'));
    renderPage();

    fireEvent.click(await screen.findByRole('button', { name: '更新' }));

    await waitFor(() =>
      expect(toastMock).toHaveBeenCalledWith(
        expect.objectContaining({
          description: 'プロジェクト情報は更新されました。入力内容に誤りがあります。',
          variant: 'destructive',
        }),
      ),
    );
    expect(screen.queryByText('project detail')).not.toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('入力内容に誤りがあります。');
  });
});
