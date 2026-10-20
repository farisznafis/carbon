import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';

const adminApi = vi.hoisted(() => ({
  getAdminProjectDetail: vi.fn(),
  updateAdminProject: vi.fn(),
}));
const toastMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/admin', () => adminApi);
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 'admin-1' } }),
}));
vi.mock('@/hooks/use-toast', () => ({ toast: toastMock }));

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

const update = async () =>
  fireEvent.click(await screen.findByRole('button', { name: '更新' }));

describe('AdminProjectEditPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminApi.getAdminProjectDetail.mockResolvedValue({
      id: 'PROJ1',
      projectName: '太陽光プログラム',
      projectNumber: 'P001',
      projectStatus: '実施中',
      regionalCategory: '関東',
      startedAt: '2026-03-31T15:00:00Z',
      usePurpose: '地域脱炭素施策',
      operatorCompanyId: 7,
    });
    adminApi.updateAdminProject.mockResolvedValue({});
  });

  it('has the same sections as プロジェクト作成', async () => {
    renderPage();
    await screen.findByRole('button', { name: '更新' });

    const headings = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent);
    expect(headings).toEqual(['基本情報', '方法論', '代表者・責任者', '排出量', '計画書ファイル']);
    expect(screen.getByText('プロジェクト更新')).toBeInTheDocument();
  });

  it('shows 番号, ステータス and 運営会社ID but does not let them be changed', async () => {
    renderPage();
    await screen.findByRole('button', { name: '更新' });

    expect(screen.getByLabelText(/^プロジェクト番号/)).toBeDisabled();
    expect(screen.getByLabelText(/^プロジェクト番号/)).toHaveValue('P001');
    expect(screen.getByLabelText(/^ステータス/)).toBeDisabled();
    expect(screen.getByLabelText(/^運営会社ID/)).toBeDisabled();
    expect(screen.getByLabelText(/^プロジェクト名称/)).toBeEnabled();
  });

  it('does not ask for 認証対象期間 or 会員数, which are filled in on the Excel forms', async () => {
    renderPage();
    await screen.findByRole('button', { name: '更新' });

    expect(screen.queryByText(/認証対象期間/)).not.toBeInTheDocument();
    expect(screen.queryByText(/会員数/)).not.toBeInTheDocument();
    expect(screen.queryByText(/社員数/)).not.toBeInTheDocument();
  });

  it('has no file upload of its own: files belong to the 書類 tab', async () => {
    renderPage();
    await screen.findByRole('button', { name: '更新' });

    expect(document.querySelector('input[type="file"]')).toBeNull();
    expect(screen.getByText(/「書類」タブで管理します/)).toBeInTheDocument();
  });

  it('更新 saves the project record, 開始日 and 利用目的 included, and returns to the project', async () => {
    renderPage();

    await update();

    expect(await screen.findByText('project detail')).toBeInTheDocument();
    expect(adminApi.updateAdminProject).toHaveBeenCalledTimes(1);
    expect(adminApi.updateAdminProject).toHaveBeenCalledWith(
      'PROJ1',
      expect.objectContaining({
        projectName: '太陽光プログラム',
        processedBy: 'admin-1',
        startedAt: '2026-03-31T15:00:00.000Z',
        usePurpose: '地域脱炭素施策',
      }),
    );
  });

  it('stays on the page and says why when the update is refused', async () => {
    adminApi.updateAdminProject.mockRejectedValue(new Error('入力内容に誤りがあります。'));
    renderPage();

    await update();

    expect(await screen.findByRole('alert')).toHaveTextContent('入力内容に誤りがあります。');
    expect(toastMock).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '更新に失敗しました',
        description: '入力内容に誤りがあります。',
        variant: 'destructive',
      }),
    );
    expect(screen.queryByText('project detail')).not.toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('button', { name: '更新' })).toBeEnabled());
  });

  it('marks the field the backend refused, as プロジェクト作成 does', async () => {
    adminApi.updateAdminProject.mockRejectedValue(
      new ApiError('入力内容に誤りがあります。', {
        status: 400,
        payload: { errors: { repEmail: '代表者メールアドレスの形式が正しくありません。' } },
      }),
    );
    renderPage();

    await update();

    expect(
      await screen.findByText('代表者メールアドレスの形式が正しくありません。'),
    ).toBeInTheDocument();
  });
});
