// Verifies that site editing picks the PIC by picUserId and offers the PIC candidates the backend accepts.
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Site, SiteFormData } from '@/api/site';
import type { UserList } from '@/api/users';
import SiteEdit from './SiteEditPage';

const { storeState, updateSite, validateUpdateSite } = vi.hoisted(() => ({
  storeState: {
    sites: [] as Site[],
    users: [] as UserList[],
    editSite: vi.fn(),
    setUsers: vi.fn(),
  },
  updateSite: vi.fn(),
  validateUpdateSite: vi.fn(),
}));

vi.mock('@/api/site', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/site')>();
  return { ...actual, updateSite, validateUpdateSite };
});

vi.mock('@/features/master/forms/SiteForm', () => ({
  SiteForm: ({
    defaultValues,
    userOptions,
    onNext,
  }: {
    defaultValues?: SiteFormData;
    userOptions: UserList[];
    onNext: (values: SiteFormData) => Promise<void>;
  }) => (
    <div>
      <span data-testid="selected-pic">{defaultValues?.userId}</span>
      <ul aria-label="責任者候補">
        {userOptions.map((user) => (
          <li key={user.id}>{user.id}</li>
        ))}
      </ul>
      <button type="button" onClick={() => defaultValues && void onNext(defaultValues)}>
        入力を確認
      </button>
    </div>
  ),
}));

vi.mock('./SiteConfirmation', () => ({
  SiteConfirmation: ({ onSubmit }: { onSubmit: () => Promise<void> }) => (
    <button type="button" onClick={() => void onSubmit()}>
      更新する
    </button>
  ),
}));

vi.mock('@/hooks/users/useUsersQuery', () => ({
  useUsersQuery: () => ({ data: undefined, isLoading: false, isError: false }),
}));

vi.mock('@/hooks/use-toast', () => ({ toast: vi.fn() }));

vi.mock('@/stores/dashboardStore', () => ({
  useDashboardStore: (selector: (state: typeof storeState) => unknown) =>
    selector(storeState),
}));

const user = (overrides: Partial<UserList>): UserList =>
  ({
    firstName: '陽斗',
    lastName: '佐藤',
    email: '',
    role: 'user',
    siteId: null,
    ...overrides,
  }) as UserList;

const site = {
  siteId: 'SITE-1',
  siteName: '東京拠点',
  postalCode: '',
  prefectureName: '',
  cityName: '',
  blockName: '',
  picId: 'USR-s02',
  picFirstName: '陽斗',
  picLastName: '佐藤',
  picEmail: '',
  picPosition: '',
  companyId: '',
} satisfies Site;

function renderPage() {
  render(
    <MemoryRouter initialEntries={['/sites/SITE-1/edit']}>
      <Routes>
        <Route path="/sites/:siteId/edit" element={<SiteEdit />} />
        <Route path="/master/dashboard" element={<div>ダッシュボード</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('SiteEdit PIC', () => {
  beforeEach(() => {
    storeState.sites = [site];
    // master.c01 and user.s02 share the name 佐藤 陽斗.
    storeState.users = [
      user({ id: 'USR-c01', role: 'master', email: 'master.c01@example.com' }),
      user({ id: 'USR-s02', siteId: 'SITE-1', email: 'user.s02@example.com' }),
      user({ id: 'USR-free', firstName: '花', lastName: '田中' }),
      user({ id: 'USR-other', siteId: 'SITE-2', firstName: '一郎', lastName: '鈴木' }),
    ];
    storeState.editSite.mockReset();
    updateSite.mockReset();
    validateUpdateSite.mockReset();
    validateUpdateSite.mockResolvedValue(undefined);
  });

  it('selects the PIC from picUserId when two candidates share a name', () => {
    renderPage();
    expect(screen.getByTestId('selected-pic')).toHaveTextContent('USR-s02');
  });

  it('does not guess between same-name candidates when picUserId is missing', () => {
    storeState.sites = [{ ...site, picId: '' }];
    renderPage();
    expect(screen.getByTestId('selected-pic')).toBeEmptyDOMElement();
  });

  it('uses the email to pick the right same-name candidate when picUserId is missing', () => {
    storeState.sites = [{ ...site, picId: '', picEmail: 'user.s02@example.com' }];
    renderPage();
    expect(screen.getByTestId('selected-pic')).toHaveTextContent('USR-s02');
  });

  it('offers the master, unassigned users and this site’s users, not other sites’ users', () => {
    renderPage();
    const options = screen.getByRole('list', { name: '責任者候補' });
    expect(options).toHaveTextContent('USR-c01');
    expect(options).toHaveTextContent('USR-s02');
    expect(options).toHaveTextContent('USR-free');
    expect(options).not.toHaveTextContent('USR-other');
  });

  it('stores the PIC returned by PATCH /site', async () => {
    const actor = userEvent.setup();
    updateSite.mockResolvedValue({ statusCode: 'OK', site: { picUserId: 'USR-c01' } });
    renderPage();

    await actor.click(screen.getByRole('button', { name: '入力を確認' }));
    await actor.click(await screen.findByRole('button', { name: '更新する' }));

    expect(await screen.findByText('ダッシュボード')).toBeInTheDocument();
    expect(storeState.editSite).toHaveBeenCalledWith(
      'SITE-1',
      expect.objectContaining({ picId: 'USR-c01' }),
    );
  });
});
