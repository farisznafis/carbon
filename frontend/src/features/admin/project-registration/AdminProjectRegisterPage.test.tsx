import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const projectsApi = vi.hoisted(() => ({
  createAdminProject: vi.fn(),
  updateAdminProject: vi.fn(),
  saveProjectPlanForm: vi.fn(),
}));
const filesApi = vi.hoisted(() => ({
  uploadProjectFile: vi.fn(),
  projectPlanFileProblem: vi.fn(),
  PROJECT_PLAN_FILE_ACCEPT: '.xlsx,.pdf',
}));
const toastMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/admin/projects', () => projectsApi);
vi.mock('@/api/admin/projectFiles', () => filesApi);
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: 'admin-1' } }),
}));
vi.mock('@/hooks/use-toast', () => ({ toast: toastMock }));
// The project record's own inputs are covered by DynamicFormField's tests.
vi.mock('./forms/DynamicFormRenderer', () => ({
  DynamicFormRenderer: () => <div data-testid="project-record-fields" />,
}));
vi.mock('./forms/TemporaryAdminProjectAutofillButton', () => ({
  TemporaryAdminProjectAutofillButton: () => null,
}));

import { AdminProjectRegister } from './AdminProjectRegisterPage';

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/admin/project-register']}>
      <Routes>
        <Route path="/admin/project-register" element={<AdminProjectRegister />} />
        <Route path="/admin/projects/:projectId" element={<p>project detail</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

const planFile = () => new File(['x'], '計画書.xlsx');

const chooseFile = (file: File) =>
  fireEvent.change(screen.getByLabelText('計画書ファイル'), { target: { files: [file] } });

const enterPeriod = (start: string, end: string) => {
  fireEvent.change(screen.getByLabelText('認証対象期間（開始日）'), { target: { value: start } });
  fireEvent.change(screen.getByLabelText('認証対象期間（終了日）'), { target: { value: end } });
};

const save = () => fireEvent.click(screen.getByRole('button', { name: '保存' }));

describe('AdminProjectRegister', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    projectsApi.createAdminProject.mockResolvedValue([{ id: 'project-1' }]);
    projectsApi.updateAdminProject.mockResolvedValue({});
    projectsApi.saveProjectPlanForm.mockResolvedValue({});
    filesApi.uploadProjectFile.mockResolvedValue({ id: 1 });
    filesApi.projectPlanFileProblem.mockReturnValue(null);
  });

  it('is one page: the plan file, the plan values, then the project record', () => {
    renderPage();

    const headings = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent);
    expect(headings).toEqual([
      '計画書ファイル',
      '認証対象期間・会員数計画',
      '基本情報',
      '方法論',
      '代表者・責任者',
      '会員数（実績・合計）・排出量',
    ]);
    expect(screen.queryByRole('button', { name: '次へ' })).not.toBeInTheDocument();
  });

  it('shows one row per 年度 once the period is entered', () => {
    renderPage();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();

    enterPeriod('2025-10-01', '2027-03-31');

    expect(screen.getByRole('rowheader', { name: '2025年度' })).toBeInTheDocument();
    expect(screen.getByRole('rowheader', { name: '2026年度' })).toBeInTheDocument();
    expect(screen.queryByRole('rowheader', { name: '2027年度' })).not.toBeInTheDocument();
  });

  it('saves the project, then the file, then the plan values, and opens the project', async () => {
    renderPage();
    const file = planFile();
    chooseFile(file);
    enterPeriod('2025-10-01', '2027-03-31');
    fireEvent.change(screen.getByLabelText('2025年度 新規会員数（件）'), {
      target: { value: '500' },
    });
    fireEvent.change(screen.getByLabelText('2025年度 認証対象となる累計会員数（件）'), {
      target: { value: '500' },
    });

    save();

    expect(await screen.findByText('project detail')).toBeInTheDocument();
    expect(projectsApi.createAdminProject).toHaveBeenCalledTimes(1);
    expect(filesApi.uploadProjectFile).toHaveBeenCalledWith('project-1', 'PROJECT_PLAN', file);
    expect(projectsApi.saveProjectPlanForm).toHaveBeenCalledWith(
      'project-1',
      'emission-reduction-plan',
      {
        unitEmission: 't-CO2',
        certTargetPeriodStart: '2025-10-01',
        certTargetPeriodEnd: '2027-03-31',
        years: [
          { year: 2025, newMembersTotal: 500, cumulativeMembersTotal: 500 },
          { year: 2026, newMembersTotal: 0, cumulativeMembersTotal: 0 },
        ],
      },
    );

    const createOrder = projectsApi.createAdminProject.mock.invocationCallOrder[0];
    const uploadOrder = filesApi.uploadProjectFile.mock.invocationCallOrder[0];
    const planOrder = projectsApi.saveProjectPlanForm.mock.invocationCallOrder[0];
    expect(createOrder).toBeLessThan(uploadOrder);
    expect(uploadOrder).toBeLessThan(planOrder);
  });

  it('creates just the project when no file and no plan values are given', async () => {
    renderPage();

    save();

    expect(await screen.findByText('project detail')).toBeInTheDocument();
    expect(filesApi.uploadProjectFile).not.toHaveBeenCalled();
    expect(projectsApi.saveProjectPlanForm).not.toHaveBeenCalled();
  });

  it('does not create anything while the plan values are inconsistent', async () => {
    renderPage();
    fireEvent.change(screen.getByLabelText('認証対象期間（開始日）'), {
      target: { value: '2025-10-01' },
    });

    save();

    expect(await screen.findAllByText(/両方を入力してください/)).not.toHaveLength(0);
    expect(projectsApi.createAdminProject).not.toHaveBeenCalled();
  });

  it('keeps the project when the upload fails and retries without creating it again', async () => {
    filesApi.uploadProjectFile.mockRejectedValueOnce(new Error('ファイルを保存できませんでした。'));
    renderPage();
    const file = planFile();
    chooseFile(file);

    save();

    expect(
      await screen.findByText(/プロジェクトは保存されましたが、計画書ファイルをアップロードできませんでした/),
    ).toBeInTheDocument();
    expect(screen.queryByText('project detail')).not.toBeInTheDocument();

    save();

    expect(await screen.findByText('project detail')).toBeInTheDocument();
    expect(projectsApi.createAdminProject).toHaveBeenCalledTimes(1);
    expect(projectsApi.updateAdminProject).toHaveBeenCalledWith('project-1', expect.anything());
    expect(filesApi.uploadProjectFile).toHaveBeenCalledTimes(2);
  });

  it('stays on the page when the project itself is refused', async () => {
    projectsApi.createAdminProject.mockRejectedValue(new Error('プロジェクト番号は必須です。'));
    renderPage();
    chooseFile(planFile());

    save();

    expect(await screen.findByText('プロジェクト番号は必須です。')).toBeInTheDocument();
    expect(filesApi.uploadProjectFile).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.getByRole('button', { name: '保存' })).toBeEnabled());
  });

  it('turns away a file the server would refuse, without keeping it', () => {
    filesApi.projectPlanFileProblem.mockReturnValue('Excelファイル（.xlsx）またはPDFファイルを選択してください。');
    renderPage();

    chooseFile(new File(['x'], '計画書.docx'));

    expect(screen.getByRole('alert')).toHaveTextContent('.xlsx');
    expect(screen.getByText('ファイルは選択されていません。')).toBeInTheDocument();
  });
});
