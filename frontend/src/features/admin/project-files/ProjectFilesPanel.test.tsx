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

import { useState } from 'react';
import { ProjectDocumentsSummaryCard } from './ProjectDocumentsSummaryCard';
import { ProjectFilesPanel, type ProjectFileUploadRequest } from './ProjectFilesPanel';
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

/** The panel with the upload request held the way ProjectDocumentsTab holds it. */
function PanelHarness() {
  const [uploadRequest, setUploadRequest] = useState<ProjectFileUploadRequest>(null);
  return (
    <ProjectFilesPanel
      projectId="PROJ1"
      uploadRequest={uploadRequest}
      onUploadRequestChange={setUploadRequest}
    />
  );
}

function renderPanel() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <PanelHarness />
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

describe('ProjectDocumentsSummaryCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    filesApi.getProjectFileListing.mockResolvedValue(listing);
  });

  it('shows the current version of each type and offers to add a missing one', async () => {
    const onUpload = vi.fn();
    const onOpenDocuments = vi.fn();
    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <ProjectDocumentsSummaryCard
          projectId="PROJ1"
          onOpenDocuments={onOpenDocuments}
          onUpload={onUpload}
        />
      </QueryClientProvider>,
    );

    expect(await screen.findByText('report_v2.pdf')).toBeInTheDocument();
    expect(screen.getByText('plan.xlsx')).toBeInTheDocument();
    expect(screen.queryByText('report_v1.xlsx')).not.toBeInTheDocument();
    expect(screen.getByText('主要様式 2/3')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '追加' }));
    expect(onUpload).toHaveBeenCalledWith('ACTIVITY_RESULT_LIST');

    fireEvent.click(screen.getByRole('button', { name: 'すべて見る' }));
    expect(onOpenDocuments).toHaveBeenCalled();
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
    expect(screen.getAllByText('未アップロード')).toHaveLength(7);
    expect(screen.queryByText('個別活動実績報告リストはまだアップロードされていません。')).not.toBeInTheDocument();

    const rows = screen.getAllByRole('listitem');
    expect(within(rows[0]).getByText('v1')).toBeInTheDocument(); // PROJECT_PLAN section comes first
    expect(within(rows[1]).getByText('v2')).toBeInTheDocument();
    expect(within(rows[1]).getByText('最新')).toBeInTheDocument();
  });

  it('lists every document type under its J-Credit stage, each with its own 追加', async () => {
    renderPanel();

    for (const stage of ['プロジェクト登録', 'モニタリング・検証・認証', '審査中のやり取り']) {
      expect(await screen.findByRole('heading', { name: stage })).toBeInTheDocument();
    }
    const registration = screen.getByRole('region', { name: 'プロジェクト登録' });
    expect(within(registration).getByText('妥当性確認報告書')).toBeInTheDocument();
    expect(within(registration).getByText('登録通知')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: /にファイルを追加$/ })).toHaveLength(9);
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

  it('uploads into the section whose 追加 was pressed', async () => {
    renderPanel();

    fireEvent.click(
      await screen.findByRole('button', { name: '個別活動実績報告リストにファイルを追加' }),
    );
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText('各書類は10件まで保存できます。')).toBeInTheDocument();

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

    fireEvent.click(
      await screen.findByRole('button', { name: '個別活動実績報告リストにファイルを追加' }),
    );
    const dialog = await screen.findByRole('dialog');

    expect(
      within(dialog).getByText('アップロードできるファイル数の上限に達しています'),
    ).toBeInTheDocument();
    expect(within(dialog).getByText(/既存のファイルを1件削除してください/)).toBeInTheDocument();
    expect(dialog.querySelector('input[type="file"]')).toBeNull();
    expect(within(dialog).getByRole('button', { name: 'アップロード' })).toBeDisabled();
  });
});
