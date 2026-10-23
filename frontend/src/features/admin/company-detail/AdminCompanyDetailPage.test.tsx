import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import AdminCompanyDetailPage from './AdminCompanyDetailPage';

const {
  detailQuery,
  permissions,
  resendSignupRevisionLinkMock,
  toastMock,
} = vi.hoisted(() => ({
  detailQuery: {
    data: null as unknown,
    isLoading: false,
    isError: false,
    error: null,
    refetch: vi.fn(),
  },
  permissions: { granted: true },
  resendSignupRevisionLinkMock: vi.fn(),
  toastMock: vi.fn(),
}));

vi.mock('@/hooks/admin/useAdminCompanyDetailQuery', () => ({
  useAdminCompanyDetailQuery: () => detailQuery,
}));
vi.mock('@/hooks/admin/useAdminProjectDetailQuery', () => ({
  useAdminProjectDetailQuery: () => ({ data: undefined }),
}));
vi.mock('@/features/admin/permissions/useAdminPermissions', () => ({
  useAdminPermissions: () => ({
    hasPermission: () => permissions.granted,
    guardAction: (_code: string, action: () => void) => {
      if (permissions.granted) action();
      return permissions.granted;
    },
  }),
}));
vi.mock('@/contexts/AuthContext', () => ({ useAuth: () => ({ user: { companyId: 'OWN' } }) }));
vi.mock('@/components/navigation/pageBreadcrumbContext', () => ({ usePageBreadcrumb: () => undefined }));
vi.mock('@/hooks/use-toast', () => ({ useToast: () => ({ toast: toastMock }) }));
vi.mock('@/api/admin', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/api/admin')>()),
  resendSignupRevisionLink: (...args: unknown[]) => resendSignupRevisionLinkMock(...args),
}));

function companyDetail(companyStatus: string) {
  return {
    company: {
      companyId: 'C1',
      nameJA: '株式会社サンプル',
      email: 'info@example.com',
      companyStatus,
    },
    masterAccount: null,
    users: [],
    sites: [],
  };
}

function renderPage() {
  return render(
    <MemoryRouter
      initialEntries={['/admin/companies/C1']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route path="/admin/companies/:companyId" element={<AdminCompanyDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

const resendButtonName = '修正用リンクを再送';

describe('AdminCompanyDetailPage signup revision link', () => {
  beforeEach(() => {
    detailQuery.data = companyDetail('REJECTED');
    detailQuery.refetch.mockReset();
    permissions.granted = true;
    resendSignupRevisionLinkMock.mockReset().mockResolvedValue({ message: '修正用リンクを送信しました。' });
    toastMock.mockReset();
  });

  it('resends after confirming that the previous link stops working', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: resendButtonName }));
    expect(screen.getByText('以前に送信したリンクは無効になります。')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '再送信する' }));

    await waitFor(() => expect(resendSignupRevisionLinkMock).toHaveBeenCalledWith('C1'));
    expect(toastMock).toHaveBeenCalledWith(
      expect.objectContaining({ description: '修正用リンクを送信しました。', variant: 'success' }),
    );
  });

  it('is hidden without the application review permission', () => {
    permissions.granted = false;
    renderPage();

    expect(screen.queryByRole('button', { name: resendButtonName })).not.toBeInTheDocument();
  });

  it('is shown only for rejected companies', () => {
    detailQuery.data = companyDetail('PENDING');
    renderPage();

    expect(screen.queryByRole('button', { name: resendButtonName })).not.toBeInTheDocument();
  });

  it('refreshes the company when it is no longer rejected', async () => {
    const user = userEvent.setup();
    resendSignupRevisionLinkMock.mockRejectedValue(
      new ApiError('Conflict', { status: 409, payload: { statusCode: 'CONFLICT' } }),
    );
    renderPage();

    await user.click(screen.getByRole('button', { name: resendButtonName }));
    await user.click(screen.getByRole('button', { name: '再送信する' }));

    await waitFor(() => expect(detailQuery.refetch).toHaveBeenCalled());
    expect(toastMock).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'リンクを再送信できませんでした', variant: 'destructive' }),
    );
  });
});
