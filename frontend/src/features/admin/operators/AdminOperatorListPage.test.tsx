import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminOperator } from '@/api/admin/operators';
import AdminOperatorListPage from './AdminOperatorListPage';

const { authState, queryState } = vi.hoisted(() => ({
  authState: {
    user: {
      id: 'session-admin',
      username: 'session.admin',
      email: 'session@example.com',
    },
  },
  queryState: {
    data: { statusCode: 'OK', message: '', users: [] as AdminOperator[] },
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  },
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => authState,
}));

vi.mock('@/hooks/admin/useAdminOperatorsQuery', () => ({
  useAdminOperatorsQuery: () => queryState,
}));

const operators: AdminOperator[] = [
  {
    id: '1',
    username: 'ito.kenichi',
    name: '伊藤 健一',
    role: 'admin',
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
    authState.user = {
      id: 'session-admin',
      username: 'session.admin',
      email: 'session@example.com',
    };
    operators[0].permissions = ['PROJECT_READ', 'PROJECT_UPDATE'];
    queryState.data = { statusCode: 'OK', message: '', users: operators };
    queryState.isLoading = false;
    queryState.isError = false;
    queryState.refetch.mockReset();
  });

  it('does not show the signed-in account in the manageable operator list', () => {
    authState.user = {
      id: operators[0].id,
      username: operators[0].username,
      email: operators[0].email,
    };
    renderPage();

    expect(screen.queryByText(operators[0].username)).not.toBeInTheDocument();
    expect(screen.getByText(operators[1].username)).toBeInTheDocument();
  });

  it('uses immutable operator IDs for the default row order', () => {
    queryState.data = {
      statusCode: 'OK',
      message: '',
      users: [operators[1], operators[0]],
    };
    renderPage();

    const firstOperator = screen.getByText(operators[0].username);
    const secondOperator = screen.getByText(operators[1].username);
    expect(
      firstOperator.compareDocumentPosition(secondOperator) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  it('does not offer permission editing for an admin with no explicit permissions', () => {
    operators[0].permissions = [];
    renderPage();

    expect(screen.getAllByText('権限なし')).toHaveLength(2);
    expect(
      screen.getAllByRole('link', { name: '権限を設定 →' }),
    ).toHaveLength(1);
    expect(
      screen.getByRole('link', { name: '権限を設定 →' }),
    ).toHaveAttribute('href', '/admin/operators/2/edit#permissions');
  });

  it('shows actual count, permission summary, and filters by search text', async () => {
    const user = userEvent.setup();
    renderPage();

    expect(screen.getByText('2件')).toBeInTheDocument();
    expect(screen.getByText('2機能')).toBeInTheDocument();
    expect(screen.getByText('プロジェクト管理 +1')).toBeInTheDocument();
    expect(screen.getByText('権限なし')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '権限を設定 →' })).toHaveAttribute(
      'href',
      '/admin/operators/2/edit#permissions',
    );
    expect(screen.queryByText('ロール')).not.toBeInTheDocument();
    expect(screen.queryByText('役職')).not.toBeInTheDocument();
    expect(screen.queryByText('担当拠点')).not.toBeInTheDocument();

    await user.type(
      screen.getByPlaceholderText('氏名・ユーザー名・メールアドレスで検索'),
      'hana@example.com',
    );

    expect(screen.getByText('佐藤 花')).toBeInTheDocument();
    expect(screen.queryByText('伊藤 健一')).not.toBeInTheDocument();
  });

  it('requires every selected permission when filtering operators', async () => {
    const user = userEvent.setup();
    queryState.data = {
      statusCode: 'OK',
      message: '',
      users: [
        ...operators,
        {
          ...operators[0],
          id: '3',
          username: 'project.reader',
          name: '閲覧 一郎',
          firstName: '一郎',
          lastName: '閲覧',
          email: 'reader@example.com',
          role: 'operator',
          permissions: ['PROJECT_READ'],
        },
      ],
    };
    renderPage();

    const permissionFilter = screen.getByRole('combobox', {
      name: '権限で絞り込む',
    });
    await user.click(permissionFilter);
    await user.click(
      screen.getByRole('checkbox', { name: 'すべての権限を選択' }),
    );

    expect(screen.getByText('佐藤 花')).toBeInTheDocument();
    expect(screen.queryByText('伊藤 健一')).not.toBeInTheDocument();
    expect(screen.queryByText('閲覧 一郎')).not.toBeInTheDocument();

    await user.click(screen.getByRole('checkbox', { name: 'プロジェクト参照' }));
    await user.click(screen.getByRole('checkbox', { name: 'プロジェクト更新' }));

    expect(screen.getByText('伊藤 健一')).toBeInTheDocument();
    expect(screen.queryByText('佐藤 花')).not.toBeInTheDocument();
    expect(screen.queryByText('閲覧 一郎')).not.toBeInTheDocument();
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
