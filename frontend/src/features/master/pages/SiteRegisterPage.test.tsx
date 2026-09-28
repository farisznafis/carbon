// Verifies that site creation returns to project registration with the created site selected.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Site, SiteFormData } from '@/api/site';
import { createSiteRegistrationReturnState } from '@/features/master/navigation/siteRegistrationFlow';
import SiteRegister from './SiteRegisterPage';

const { registerSite, validateCreateSite, addSite, setUsers } = vi.hoisted(
  () => ({
    registerSite: vi.fn(),
    validateCreateSite: vi.fn(),
    addSite: vi.fn(),
    setUsers: vi.fn(),
  }),
);

const siteFormData = {
  siteName: '新宿拠点',
  siteNameKana: 'シンジュクキョテン',
  postalCode: '1600022',
  prefectureName: '東京都',
  cityName: '新宿区',
  townName: '新宿',
  blockName: '1-1-1',
  addressDetail: 'GXビル',
  picFirstName: '',
  picFirstNameKana: '',
  picLastName: '',
  picLastNameKana: '',
  picEmail: '',
  picPhoneNumber: '',
  picPosition: '',
  userId: '',
} satisfies SiteFormData;

const createdSite = {
  siteId: 'site-new',
  siteName: '新宿拠点',
} as Site;

vi.mock('@/api/site', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/site')>();
  return { ...actual, registerSite, validateCreateSite };
});

vi.mock('@/features/master/forms/SiteForm', () => ({
  SiteForm: ({
    onNext,
    onAddUser,
    defaultValues,
  }: {
    onNext: (values: SiteFormData) => Promise<void>;
    onAddUser?: (values: SiteFormData) => void;
    defaultValues: SiteFormData;
  }) => (
    <div>
      <span data-testid="site-draft-name">{defaultValues.siteName}</span>
      <button type="button" onClick={() => void onNext(siteFormData)}>
        入力を確認
      </button>
      <button type="button" onClick={() => onAddUser?.(siteFormData)}>
        社員を追加
      </button>
    </div>
  ),
}));

vi.mock('@/features/master/user-invitations/UserInvitationDialog', () => ({
  UserInvitationDialog: ({
    open,
    onOpenChange,
  }: {
    open: boolean;
    onOpenChange: (open: boolean) => void;
  }) =>
    open ? (
      <div role="dialog" aria-label="社員を招待">
        <button type="button" onClick={() => onOpenChange(false)}>
          招待を閉じる
        </button>
      </div>
    ) : null,
}));

vi.mock('./SiteConfirmation', () => ({
  SiteConfirmation: ({ onSubmit }: { onSubmit: () => Promise<void> }) => (
    <button type="button" onClick={() => void onSubmit()}>
      拠点を登録
    </button>
  ),
}));

vi.mock('@/hooks/users/useUsersQuery', () => ({
  useUsersQuery: () => ({
    data: { users: [] },
    isLoading: false,
    isError: false,
    error: null,
  }),
}));

vi.mock('@/hooks/use-toast', () => ({ toast: vi.fn() }));

vi.mock('@/stores/dashboardStore', () => ({
  useDashboardStore: (
    selector: (state: {
      addSite: typeof addSite;
      setUsers: typeof setUsers;
    }) => unknown,
  ) => selector({ addSite, setUsers }),
}));

function ProjectRegistrationDestination() {
  const location = useLocation();
  const state = location.state as {
    createdSiteId?: string;
    pendingEquipment?: { id?: string };
  } | null;

  return (
    <div>
      <span>プロジェクト参加申請</span>
      <span>{state?.createdSiteId}</span>
      <span>{state?.pendingEquipment?.id}</span>
    </div>
  );
}

describe('SiteRegister return flow', () => {
  beforeEach(() => {
    registerSite.mockReset();
    validateCreateSite.mockReset();
    addSite.mockReset();
    setUsers.mockReset();
    validateCreateSite.mockResolvedValue(undefined);
    registerSite.mockResolvedValue({ statusCode: 'OK', site: createdSite });
  });

  it('returns to project registration after creating a site', async () => {
    const user = userEvent.setup();
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={[
            {
              pathname: '/master/site-register',
              state: createSiteRegistrationReturnState({
                path: '/projects/site001/PROJ001/register',
                state: { pendingEquipment: { id: 'equipment-1' } },
              }),
            },
          ]}
          future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        >
          <Routes>
            <Route path="/master/site-register" element={<SiteRegister />} />
            <Route
              path="/projects/:siteId/:projectId/register"
              element={<ProjectRegistrationDestination />}
            />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await user.click(screen.getByRole('button', { name: '入力を確認' }));
    await user.click(screen.getByRole('button', { name: '拠点を登録' }));

    expect(await screen.findByText('プロジェクト参加申請')).toBeVisible();
    expect(screen.getByText('site-new')).toBeVisible();
    expect(screen.getByText('equipment-1')).toBeVisible();
    await waitFor(() => {
      expect(queryClient.getQueryData(['sites'])).toEqual({
        sites: [createdSite],
      });
    });
  });

  it('keeps the site draft when invitation dialog opens and closes', async () => {
    const user = userEvent.setup();
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter
          initialEntries={['/master/site-register']}
          future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
        >
          <Routes>
            <Route path="/master/site-register" element={<SiteRegister />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    await user.click(screen.getByRole('button', { name: '社員を追加' }));
    expect(
      screen.getByRole('dialog', { name: '社員を招待' }),
    ).toBeVisible();
    expect(screen.getByTestId('site-draft-name')).toHaveTextContent(
      siteFormData.siteName,
    );

    await user.click(screen.getByRole('button', { name: '招待を閉じる' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByTestId('site-draft-name')).toHaveTextContent(
      siteFormData.siteName,
    );
  });
});
