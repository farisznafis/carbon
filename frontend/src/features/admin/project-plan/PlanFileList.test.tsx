import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const filesApi = vi.hoisted(() => ({
  getProjectFiles: vi.fn(),
  uploadProjectFile: vi.fn(),
  getProjectFileDownloadUrl: vi.fn(),
  projectPlanFileProblem: vi.fn(),
  PROJECT_PLAN_FILE_ACCEPT: '.xlsx,.pdf',
}));
const toastMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/admin/projectFiles', () => filesApi);
vi.mock('@/hooks/use-toast', () => ({ toast: toastMock }));

import { PlanFileList } from './PlanFileList';

const files = [
  { id: 2, originalFileName: '計画書_修正.xlsx', uploadedAt: '2026-10-07T01:00:00', uploadedBy: 'operator1' },
  { id: 1, originalFileName: '計画書.xlsx', uploadedAt: '2026-10-01T01:00:00' },
];

function renderList(canUpload = false) {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <PlanFileList projectId="PROJ1" canUpload={canUpload} />
    </QueryClientProvider>,
  );
}

describe('PlanFileList', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    filesApi.getProjectFiles.mockResolvedValue(files);
    filesApi.projectPlanFileProblem.mockReturnValue(null);
    filesApi.uploadProjectFile.mockResolvedValue({ id: 3 });
    filesApi.getProjectFileDownloadUrl.mockResolvedValue('https://signed');
    vi.spyOn(window, 'open').mockImplementation(() => null);
  });

  it('lists the plan files newest first and marks the current one', async () => {
    renderList();

    expect(await screen.findByText('計画書_修正.xlsx')).toBeInTheDocument();
    expect(screen.getByText('計画書.xlsx')).toBeInTheDocument();
    expect(screen.getAllByText('最新')).toHaveLength(1);
    expect(filesApi.getProjectFiles).toHaveBeenCalledWith('PROJ1', 'PROJECT_PLAN');
  });

  it('says so when nothing has been uploaded, and offers no upload on a read-only page', async () => {
    filesApi.getProjectFiles.mockResolvedValue([]);
    renderList();

    expect(
      await screen.findByText('計画書ファイルはまだアップロードされていません。'),
    ).toBeInTheDocument();
    expect(screen.queryByLabelText('計画書ファイル')).not.toBeInTheDocument();
  });

  it('opens the signed URL of the file that was asked for', async () => {
    renderList();
    await screen.findByText('計画書.xlsx');

    fireEvent.click(screen.getByRole('button', { name: '計画書.xlsx をダウンロード' }));

    await waitFor(() =>
      expect(window.open).toHaveBeenCalledWith('https://signed', '_blank', 'noopener,noreferrer'),
    );
    expect(filesApi.getProjectFileDownloadUrl).toHaveBeenCalledWith('PROJ1', 1);
  });

  it('uploads a chosen file at once and reloads the list', async () => {
    renderList(true);
    await screen.findByText('計画書.xlsx');
    const file = new File(['x'], '計画書_v3.pdf');

    fireEvent.change(screen.getByLabelText('計画書ファイル'), { target: { files: [file] } });

    await waitFor(() =>
      expect(filesApi.uploadProjectFile).toHaveBeenCalledWith('PROJ1', 'PROJECT_PLAN', file),
    );
    await waitFor(() => expect(filesApi.getProjectFiles).toHaveBeenCalledTimes(2));
    expect(toastMock).toHaveBeenCalledWith(expect.objectContaining({ variant: 'success' }));
  });

  it('shows why the server refused an upload and keeps the list as it was', async () => {
    filesApi.uploadProjectFile.mockRejectedValue(new Error('このリソースにアクセスすることができません。'));
    renderList(true);
    await screen.findByText('計画書.xlsx');

    fireEvent.change(screen.getByLabelText('計画書ファイル'), {
      target: { files: [new File(['x'], '計画書.xlsx')] },
    });

    await waitFor(() =>
      expect(toastMock).toHaveBeenCalledWith(
        expect.objectContaining({
          description: 'このリソースにアクセスすることができません。',
          variant: 'destructive',
        }),
      ),
    );
    expect(filesApi.getProjectFiles).toHaveBeenCalledTimes(1);
  });
});
