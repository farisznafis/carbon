import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const projectsApi = vi.hoisted(() => ({
  createAdminProject: vi.fn(),
}));
const toastMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/admin/projects', () => projectsApi);
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 'admin-1' } }),
}));
vi.mock('@/hooks/use-toast', () => ({ toast: toastMock }));
// The project record's own inputs are covered by DynamicFormField's tests.
vi.mock('./forms/DynamicFormRenderer', () => ({
  DynamicFormRenderer: ({ fields }: { fields: Array<{ name: string }> }) => (
    <div data-testid="project-record-fields">{fields.map((field) => field.name).join(' ')}</div>
  ),
}));
vi.mock('./forms/TemporaryAdminProjectAutofillButton', () => ({
  TemporaryAdminProjectAutofillButton: () => null,
}));

import { AdminProjectRegister } from './AdminProjectRegisterPage';

/** Stands in for the page creation leads to: shows where it went. */
function LandingProbe() {
  const location = useLocation();
  return (
    <p>
      detail: {location.pathname}
      {location.search}
    </p>
  );
}

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/admin/project-register']}>
      <Routes>
        <Route path="/admin/project-register" element={<AdminProjectRegister />} />
        <Route path="/admin/projects/:projectId" element={<LandingProbe />} />
      </Routes>
    </MemoryRouter>,
  );
}

const save = () => fireEvent.click(screen.getByRole('button', { name: '保存' }));

describe('AdminProjectRegister', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    projectsApi.createAdminProject.mockResolvedValue([{ id: 'project-1' }]);
  });

  it('is one page: the project record, and the plan file last', () => {
    renderPage();

    const headings = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent);
    expect(headings).toEqual(['基本情報', '方法論', '代表者・責任者', '排出量', '計画書ファイル']);
    expect(screen.queryByRole('button', { name: '次へ' })).not.toBeInTheDocument();
  });

  it('does not ask for 認証対象期間 or 会員数, which are filled in on the Excel forms', () => {
    renderPage();

    expect(screen.queryByText(/認証対象期間/)).not.toBeInTheDocument();
    expect(screen.queryByText(/会員数/)).not.toBeInTheDocument();
    const fieldNames = screen
      .getAllByTestId('project-record-fields')
      .map((group) => group.textContent)
      .join(' ');
    expect(fieldNames).toContain('projectBase.baselineEmissions');
    expect(fieldNames).not.toMatch(/[mM]embers/);
  });

  it('has no file input before the project exists, and says when the file can be uploaded', () => {
    renderPage();

    expect(document.querySelector('input[type="file"]')).toBeNull();
    expect(
      screen.getByText(/プロジェクトを保存した後、プロジェクト詳細の「書類」タブでアップロードします/),
    ).toBeInTheDocument();
  });

  it('saves the project and opens the 書類 tab ready to upload the plan', async () => {
    renderPage();

    save();

    expect(
      await screen.findByText('detail: /admin/projects/project-1?tab=documents&upload=PROJECT_PLAN'),
    ).toBeInTheDocument();
    expect(projectsApi.createAdminProject).toHaveBeenCalledTimes(1);
    expect(toastMock).toHaveBeenCalledWith(expect.objectContaining({ variant: 'success' }));
  });

  it('stays on the page when the project is refused', async () => {
    projectsApi.createAdminProject.mockRejectedValue(new Error('プロジェクト番号は必須です。'));
    renderPage();

    save();

    expect(await screen.findByText('プロジェクト番号は必須です。')).toBeInTheDocument();
    expect(toastMock).toHaveBeenCalledWith(expect.objectContaining({ variant: 'destructive' }));
    await waitFor(() => expect(screen.getByRole('button', { name: '保存' })).toBeEnabled());
  });
});
