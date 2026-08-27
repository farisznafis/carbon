import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminOperator } from '@/api/admin/operators';
import AdminOperatorListPage from './AdminOperatorListPage';

const { queryState } = vi.hoisted(() => ({
  queryState: {
    data: { statusCode: 'OK', message: '', users: [] as AdminOperator[] },
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  },
}));

vi.mock('@/hooks/admin/useAdminOperatorsQuery', () => ({
  useAdminOperatorsQuery: () => queryState,
}));

const operators: AdminOperator[] = [
  {
    id: '1',
    username: 'ito.kenichi',
    name: '伊藤 健一',
    role: 'operator',
    email: 'ito@example.com',
    firstName: '健一',
    firstNameKana: 'ケンイチ',
    lastName: '伊藤',
    lastNameKana: 'イトウ',
    phoneNumber: '0354321235',
    userPosition: '担当者',
    siteId: '5',
    siteName: '東京拠点',
    companyId: 'COMP-1',
    permissions: ['PROJECT_READ', 'PROJECT_UPDATE'],
  },
  {
    id: '2',
    username: 'sato.hana',
    name: '佐藤 花',
    role: 'operator',
    email: 'hana@example.com',
    firstName: '花',
    firstNameKana: 'ハナ',
    lastName: '佐藤',
    lastNameKana: 'サトウ',
    phoneNumber: '0311112222',
    userPosition: '部長',
    siteId: '6',
    siteName: '大阪拠点',
    companyId: 'COMP-1',
    permissions: [],
  },
];

/** Renders the operator route together with its add and profile targets. */
function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/admin/operators']}>
      <Routes>
        <Route path="/admin/operators" element={<AdminOperatorListPage />} />
        <Route path="/admin/operators/new" element={<div>登録ページ</div>} />
        <Route path="/admin/operators/:userId" element={<div>プロファイルページ</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('AdminOperatorListPage', () => {
  beforeEach(() => {
    queryState.data = { statusCode: 'OK', message: '', users: operators };
    queryState.isLoading = false;
    queryState.isError = false;
    queryState.refetch.mockReset();
  });

  it('shows actual count, permission summary, and filters by search text', async () => {
    const user = userEvent.setup();
    renderPage();

    expect(screen.getByText('2件')).toBeInTheDocument();
    expect(screen.getByText('2機能')).toBeInTheDocument();
    expect(screen.getByText('プロジェクト管理 +1')).toBeInTheDocument();
    expect(screen.getByText('権限なし')).toBeInTheDocument();
    expect(screen.queryByText('役職')).not.toBeInTheDocument();
    expect(screen.queryByText('担当拠点')).not.toBeInTheDocument();

    await user.type(
      screen.getByPlaceholderText('氏名・ユーザー名・メールアドレスで検索'),
      'hana@example.com',
    );

    expect(screen.getByText('佐藤 花')).toBeInTheDocument();
    expect(screen.queryByText('伊藤 健一')).not.toBeInTheDocument();
  });

  it('filters operators by their actual permission', () => {
    renderPage();

    const permissionFilter = screen.getByRole('combobox', {
      name: '権限で絞り込む',
    });
    fireEvent.keyDown(permissionFilter, { key: 'Enter' });
    fireEvent.click(screen.getByRole('option', { name: 'プロジェクト閲覧' }));

    expect(screen.getByText('伊藤 健一')).toBeInTheDocument();
    expect(screen.queryByText('佐藤 花')).not.toBeInTheDocument();
  });

  it('uses the same add route for the empty-state CTA', async () => {
    const user = userEvent.setup();
    queryState.data = { statusCode: 'OK', message: '', users: [] };
    renderPage();

    expect(screen.getByText('0件')).toBeInTheDocument();
    expect(screen.getByText('運用者がまだ登録されていません')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /最初の運用者を追加/ }));
    expect(await screen.findByText('登録ページ')).toBeInTheDocument();
  });

  it('opens the admin operator profile route', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getAllByRole('button', { name: 'プロファイル' })[0]);
    expect(await screen.findByText('プロファイルページ')).toBeInTheDocument();
  });
});
