import type { ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminOperator } from '@/api/admin/operators';
import type { UserFormData } from '@/api/users';
import AdminOperatorEditPage from './AdminOperatorEditPage';

const { operatorState, operatorListState, updateAdminOperator } = vi.hoisted(() => ({
  operatorState: { current: null as AdminOperator | null },
  operatorListState: { current: [] as AdminOperator[] },
  updateAdminOperator: vi.fn(),
}));

vi.mock('@/api/admin/operators', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/admin/operators')>();
  return { ...actual, updateAdminOperator };
});

vi.mock('@/hooks/admin/useAdminOperatorsQuery', () => ({
  adminOperatorsQueryKey: ['admin', 'operators'],
  useAdminOperatorsQuery: () => ({
    data: {
      statusCode: 'OK',
      message: '',
      users: operatorListState.current,
    },
  }),
  useAdminOperatorProfileQuery: () => ({
    data: operatorState.current,
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  }),
}));

vi.mock('@/hooks/use-toast', () => ({ toast: vi.fn() }));

const submittedValues: UserFormData = {
  username: 'admin.primary',
  lastName: '管理',
  firstName: '太郎',
  lastNameKana: 'カンリ',
  firstNameKana: 'タロウ',
  phoneNumber: '03-1111-2222',
  email: 'admin@example.com',
};

vi.mock('@/features/master/forms/UserForm', () => ({
  UserForm: ({
    additionalSections,
    onNext,
  }: {
    additionalSections?: ReactNode;
    onNext: (values: UserFormData) => Promise<void>;
  }) => (
    <div>
      {additionalSections}
      <button type="button" onClick={() => void onNext(submittedValues)}>
        更新する
      </button>
    </div>
  ),
}));

const baseOperator: AdminOperator = {
  id: '1',
  username: 'admin.primary',
  name: '管理 太郎',
  role: 'admin',
  email: 'admin@example.com',
  firstName: '太郎',
  firstNameKana: 'タロウ',
  lastName: '管理',
  lastNameKana: 'カンリ',
  phoneNumber: '0311112222',
  userPosition: '',
  siteId: null,
  siteName: '',
  companyId: 'COMP-1',
  permissions: ['PROJECT_READ'],
};

/** Renders the edit route with the React Query provider used by the page. */
function renderEditPage(initialEntry = '/admin/operators/1/edit#permissions') {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route
            path="/admin/operators/:userId/edit"
            element={<AdminOperatorEditPage />}
          />
          <Route
            path="/admin/operators/:userId"
            element={<p>プロファイルページ</p>}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('AdminOperatorEditPage', () => {
  beforeEach(() => {
    operatorState.current = { ...baseOperator };
    operatorListState.current = [{ ...baseOperator }];
    updateAdminOperator.mockReset();
    updateAdminOperator.mockResolvedValue(baseOperator);
  });

  it('hides permission editing and omits permissionCodes for an admin', async () => {
    renderEditPage();

    expect(screen.queryByRole('heading', { name: '権限' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '更新する' }));

    await waitFor(() => expect(updateAdminOperator).toHaveBeenCalledTimes(1));
    expect(updateAdminOperator).toHaveBeenCalledWith(
      '1',
      expect.not.objectContaining({ permissionCodes: expect.anything() }),
    );
  });

  it('keeps the permission section for an operator', () => {
    operatorState.current = { ...baseOperator };
    operatorListState.current = [{ ...baseOperator, role: 'operator' }];
    renderEditPage('/admin/operators/1/edit');

    expect(screen.getByRole('heading', { name: '権限' })).toBeInTheDocument();
  });
});
