import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminOperator } from '@/api/admin/operators';
import AdminOperatorProfilePage from './AdminOperatorProfilePage';

const operator: AdminOperator = {
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
};

vi.mock('@/hooks/admin/useAdminOperatorsQuery', () => ({
  useAdminOperatorProfileQuery: () => ({
    data: operator,
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  }),
}));

/** Renders the read-only admin operator profile route. */
function renderProfilePage() {
  return render(
    <MemoryRouter initialEntries={['/admin/operators/2']}>
      <Routes>
        <Route
          path="/admin/operators/:userId"
          element={<AdminOperatorProfilePage />}
        />
        <Route
          path="/admin/operators/:userId/edit"
          element={<p>編集ページ</p>}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('AdminOperatorProfilePage', () => {
  beforeEach(() => {
    operator.permissions = [];
  });

  it('omits position and assigned site while showing an empty permission state', () => {
    renderProfilePage();

    expect(screen.getByRole('heading', { name: '運用者のプロファイル' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '運用者管理' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '編集' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '基本情報' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'アカウント情報' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '権限設定' })).toBeInTheDocument();
    expect(screen.queryByText('役職')).not.toBeInTheDocument();
    expect(screen.queryByText('担当拠点')).not.toBeInTheDocument();
    expect(screen.getByText('権限なし')).toBeInTheDocument();
    expect(screen.getByText('0機能')).toBeInTheDocument();
    expect(
      screen.getByText('利用権限は実行できる操作を表します。'),
    ).toBeInTheDocument();
  });

  it('groups permission chips by their business category', () => {
    operator.permissions = [
      'PROJECT_READ',
      'PROJECT_UPDATE',
      'MONITORING_READ',
    ];
    renderProfilePage();

    expect(screen.getByText('プロジェクト管理')).toBeInTheDocument();
    expect(screen.getByText('モニタリング管理')).toBeInTheDocument();
    expect(screen.getByText('プロジェクト参照')).toBeInTheDocument();
    expect(screen.getByText('モニタリングデータ参照')).toBeInTheDocument();
    expect(screen.getByText('2件')).toBeInTheDocument();
  });

  it('opens the edit route from the profile header', async () => {
    const user = userEvent.setup();
    renderProfilePage();

    await user.click(screen.getByRole('button', { name: '編集' }));

    expect(screen.getByText('編集ページ')).toBeInTheDocument();
  });
});
