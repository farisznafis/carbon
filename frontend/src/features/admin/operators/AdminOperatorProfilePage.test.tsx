import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
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
      </Routes>
    </MemoryRouter>,
  );
}

describe('AdminOperatorProfilePage', () => {
  it('omits position and assigned site while showing an empty permission state', () => {
    renderProfilePage();

    expect(screen.queryByText('役職')).not.toBeInTheDocument();
    expect(screen.queryByText('担当拠点')).not.toBeInTheDocument();
    expect(screen.getByText('権限なし')).toBeInTheDocument();
  });
});
