import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const filesApi = vi.hoisted(() => ({
  getProjectFileListing: vi.fn(),
  uploadProjectFile: vi.fn(),
  deleteProjectFile: vi.fn(),
  getProjectFileDownloadUrl: vi.fn(),
  projectFileProblem: vi.fn(),
  PROJECT_FILE_ACCEPT: '.xlsx,.pdf',
}));
const toastMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/admin/projectFiles', () => filesApi);
vi.mock('@/hooks/use-toast', () => ({ toast: toastMock }));
vi.mock('@/features/admin/permissions/useAdminPermissions', () => ({
  useAdminPermissions: () => ({
    hasPermission: () => true,
    guardAction: (_permission: string, action: () => void) => {
      action();
      return true;
    },
  }),
}));

import { ProjectFilesPanel } from './ProjectFilesPanel';
import { groupProjectFiles } from './projectFileTypes';

const listing = {
  files: [
    { id: 12, fileType: 'MONITORING_REPORT', originalFileName: 'report_v2.pdf', uploadedAt: '2026-10-01T10:00:00', uploadedBy: 'operator1', versionNo: 2 },
    { id: 11, fileType: 'MONITORING_REPORT', originalFileName: 'report_v1.xlsx', uploadedAt: '2026-09-20T10:00:00', versionNo: 1 },
    { id: 5, fileType: 'PROJECT_PLAN', originalFileName: 'plan.xlsx', uploadedAt: '2026-09-01T10:00:00', versionNo: 1 },
  ],
  versionSummary: [
    { fileType: 'PROJECT_PLAN', activeCount: 10, limit: 10, canUpload: false },
    { fileType: 'MONITORING_REPORT', activeCount: 2, limit: 10, canUpload: true },
    { fileType: 'ACTIVITY_RESULT_LIST', activeCount: 0, limit: 10, canUpload: true },
  ],
};

function renderPanel() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <ProjectFilesPanel projectId="PROJ1" />
    </QueryClientProvider>,
  );
}

describe('groupProjectFiles', () => {
  it('numbers versions by upload order when the server sends none', () => {
    const groups = groupProjectFiles({
      files: [
        { id: 2, fileType: 'PROJECT_PLAN', originalFileName: 'b.xlsx' },
        { id: 1, fileType: 'PROJECT_PLAN', originalFileName: 'a.xlsx' },
      ],
      versionSummary: [],
    });

    const plan = groups.find((group) => group.type === 'PROJECT_PLAN');
    expect(plan?.files.map((file) => [file.id, file.version])).toEqual([[2, 2], [1, 1]]);
    expect(plan).toMatchObject({ activeCount: 2, limit: 10, canUpload: true });
  });
});

describe('ProjectFilesPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    filesApi.getProjectFileListing.mockResolvedValue(listing);
    filesApi.projectFileProblem.mockReturnValue(null);
    filesApi.uploadProjectFile.mockResolvedValue({ id: 20 });
    filesApi.deleteProjectFile.mockResolvedValue(undefined);
    filesApi.getProjectFileDownloadUrl.mockResolvedValue('https://signed');
    vi.spyOn(window, 'open').mockImplementation(() => null);
  });

  it('groups versions per type, newest first, with the latest marked and no limit shown', async () => {
    renderPanel();

    expect(await screen.findByText('report_v2.pdf')).toBeInTheDocument();
    expect(screen.getByText('2件')).toBeInTheDocument();
    expect(screen.queryByText(/\d+\/10件/)).not.toBeInTheDocument();
    // Sections without files start collapsed.
    expect(screen.getByText('未アップロード')).toBeInTheDocument();
    expect(screen.queryByText('個別活動実績報告リストはまだアップロードされていません。')).not.toBeInTheDocument();

    const rows = screen.getAllByRole('listitem');
    expect(within(rows[0]).getByText('v1')).toBeInTheDocument(); // PROJECT_PLAN section comes first
    expect(within(rows[1]).getByText('v2')).toBeInTheDocument();
    expect(within(rows[1]).getByText('最新')).toBeInTheDocument();
  });

  it('previews both PDF and Excel files', async () => {
    renderPanel();

    expect(await screen.findByRole('button', { name: 'report_v2.pdf を表示' })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'report_v1.xlsx を表示' })).toBeEnabled();
  });

  it('asks before deleting, then deletes', async () => {
    renderPanel();

    fireEvent.click(await screen.findByRole('button', { name: 'report_v1.xlsx を削除' }));
    expect(filesApi.deleteProjectFile).not.toHaveBeenCalled();

    fireEvent.click(await screen.findByRole('button', { name: '削除する' }));
    await waitFor(() => expect(filesApi.deleteProjectFile).toHaveBeenCalledWith('PROJ1', 11));
  });

  it('uploads into the type chosen from an empty section', async () => {
    renderPanel();

    fireEvent.click(await screen.findByRole('button', { name: /個別活動実績報告リスト/ }));
    fireEvent.click(await screen.findByRole('button', { name: 'アップロード' }));
    const dialog = await screen.findByRole('dialog');

    const file = new File(['x'], 'list.xlsx');
    fireEvent.change(dialog.querySelector('input[type="file"]') as HTMLInputElement, {
      target: { files: [file] },
    });
    expect(within(dialog).getByText('v1')).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'アップロード' }));

    await waitFor(() =>
      expect(filesApi.uploadProjectFile).toHaveBeenCalledWith('PROJ1', 'ACTIVITY_RESULT_LIST', file),
    );
    await waitFor(() => expect(filesApi.getProjectFileListing).toHaveBeenCalledTimes(2));
  });

  it('warns instead of offering the upload when the type already holds its limit', async () => {
    // The server's summary decides; here it reports the empty section's type as full,
    // which reaches the dialog with that type preselected.
    filesApi.getProjectFileListing.mockResolvedValue({
      ...listing,
      versionSummary: listing.versionSummary.map((item) =>
        item.fileType === 'ACTIVITY_RESULT_LIST'
          ? { ...item, activeCount: 10, canUpload: false }
          : item,
      ),
    });
    renderPanel();

    fireEvent.click(await screen.findByRole('button', { name: /個別活動実績報告リスト/ }));
    fireEvent.click(await screen.findByRole('button', { name: 'アップロード' }));
    const dialog = await screen.findByRole('dialog');

    expect(
      within(dialog).getByText('アップロードできるファイル数の上限に達しています'),
    ).toBeInTheDocument();
    expect(within(dialog).getByText(/既存のファイルを1件削除してください/)).toBeInTheDocument();
    expect(dialog.querySelector('input[type="file"]')).toBeNull();
    expect(within(dialog).getByRole('button', { name: 'アップロード' })).toBeDisabled();
  });
});
