import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const dataApi = vi.hoisted(() => ({ getProjectDocumentData: vi.fn() }));
const toastMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/admin/projectDocumentData', () => dataApi);
vi.mock('@/hooks/use-toast', () => ({ toast: toastMock }));

import { MemoryRouter, useLocation } from 'react-router-dom';
import { ApiError } from '@/api/_shared/errors';
import { ProjectDocumentDataPanel } from './ProjectDocumentDataPanel';

const planData = {
  projectId: 'PROJ1',
  fileType: 'PROJECT_PLAN',
  sections: [
    {
      key: 'project',
      title: 'プロジェクト概要',
      items: [
        { key: 'projectName', label: 'プロジェクト名称', value: 'GX太陽光', unit: null, type: 'TEXT' },
        { key: 'projectStartedAt', label: 'プロジェクト開始日', value: '2025-04-01', unit: null, type: 'DATE' },
      ],
      table: null,
    },
    {
      key: 'plan',
      title: '認証対象期間・会員数計画',
      items: [],
      table: {
        columns: [
          { key: 'year', label: '年度', unit: null, type: 'TEXT' },
          { key: 'members', label: '会員数', unit: '社', type: 'NUMBER' },
        ],
        rows: [['2025年度', '500']],
      },
    },
  ],
};

let writeText: ReturnType<typeof vi.fn>;

function LocationProbe() {
  const location = useLocation();
  return <output data-testid="search">{location.search}</output>;
}

function renderPanel(initialEntry = '/?doc=acquisition') {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <ProjectDocumentDataPanel projectId="PROJ1" />
        <LocationProbe />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const nav = () => screen.getByRole('tablist', { name: 'データを表示する書類' });

describe('ProjectDocumentDataPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    dataApi.getProjectDocumentData.mockResolvedValue(planData);
    writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
  });

  it('opens the first form and shows its items and table', async () => {
    renderPanel();

    expect(await screen.findByText('GX太陽光')).toBeInTheDocument();
    expect(dataApi.getProjectDocumentData).toHaveBeenCalledWith('PROJ1', 'PROJECT_PLAN');
    expect(screen.getByRole('heading', { level: 2, name: 'プロジェクト計画書' })).toBeInTheDocument();
    expect(within(nav()).getByRole('tab', { name: /プロジェクト計画書/ })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(screen.getByRole('table', { name: '認証対象期間・会員数計画の表' })).toBeInTheDocument();
    // Units are left out: values go into Excel cells that already carry them.
    expect(screen.getByRole('columnheader', { name: '会員数' })).toBeInTheDocument();
    expect(screen.queryByText(/社/)).not.toBeInTheDocument();
  });

  it('lists only the three forms entered in the SaaS, with no file filters', async () => {
    renderPanel();
    await screen.findByText('GX太陽光');

    expect(within(nav()).getAllByRole('tab')).toHaveLength(3);
    expect(within(nav()).queryByRole('tab', { name: /妥当性確認報告書/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('searchbox', { name: 'ファイル名を検索' })).not.toBeInTheDocument();
    expect(screen.queryByText('アップロード済みファイル')).not.toBeInTheDocument();
  });

  it('ignores a dataType the tab does not read', async () => {
    renderPanel('/?doc=acquisition&dataType=VALIDATION_REPORT');

    expect(await screen.findByText('GX太陽光')).toBeInTheDocument();
    expect(dataApi.getProjectDocumentData).toHaveBeenCalledWith('PROJ1', 'PROJECT_PLAN');
  });

  it('copies one value without its unit and announces it', async () => {
    renderPanel();

    fireEvent.click(await screen.findByRole('button', { name: 'プロジェクト名称をコピー' }));

    await waitFor(() => expect(writeText).toHaveBeenCalledWith('GX太陽光'));
    expect(await screen.findByText('プロジェクト名称をコピーしました')).toBeInTheDocument();
  });

  it('copies one table cell at a time, with no bulk copy', async () => {
    renderPanel();

    fireEvent.click(await screen.findByRole('button', { name: '2025年度 会員数をコピー' }));
    await waitFor(() => expect(writeText).toHaveBeenLastCalledWith('500'));
    expect(screen.queryByRole('button', { name: /表をコピー|すべてコピー|セクション/ })).not.toBeInTheDocument();
  });

  it('switches document from the list and keeps it in the URL', async () => {
    renderPanel();
    await screen.findByText('GX太陽光');

    fireEvent.mouseDown(within(nav()).getByRole('tab', { name: /モニタリング報告書/ }));

    await waitFor(() =>
      expect(dataApi.getProjectDocumentData).toHaveBeenCalledWith('PROJ1', 'MONITORING_REPORT'),
    );
    expect(screen.getByTestId('search')).toHaveTextContent('dataType=MONITORING_REPORT');
  });

  it('explains when nothing has been entered for the form yet', async () => {
    dataApi.getProjectDocumentData.mockRejectedValue(new ApiError('not found', { status: 404 }));
    renderPanel();

    expect(await screen.findByText('まだデータがありません。')).toBeInTheDocument();
  });

  it('retries after a failed request', async () => {
    dataApi.getProjectDocumentData.mockRejectedValueOnce(new ApiError('サーバーエラー', { status: 500 }));
    renderPanel();

    expect(await screen.findByText('サーバーエラー')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /再/ }));

    expect(await screen.findByText('GX太陽光')).toBeInTheDocument();
  });

  it('narrows the items with the filter', async () => {
    renderPanel();
    await screen.findByText('GX太陽光');

    fireEvent.change(screen.getByRole('searchbox', { name: '項目名・値で絞り込み' }), {
      target: { value: '開始日' },
    });

    expect(screen.queryByText('GX太陽光')).not.toBeInTheDocument();
    expect(screen.getByText('2025-04-01')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();

    fireEvent.change(screen.getByRole('searchbox', { name: '項目名・値で絞り込み' }), {
      target: { value: 'zzz' },
    });
    expect(screen.getByText('「zzz」に一致する項目はありません。')).toBeInTheDocument();
  });
});
