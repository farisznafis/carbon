import { StrictMode } from 'react';
import { render, renderHook, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminOperator } from '@/api/admin/operators';
import type { User } from '@/api/users';
import { useProfileStore } from '@/stores/ProfileStore';
import { useAccountIdentity } from './useAccountIdentity';

const { getProfile, operatorQueryState } = vi.hoisted(() => ({
  getProfile: vi.fn(),
  operatorQueryState: {
    data: undefined as
      | { statusCode: string; message: string; users: AdminOperator[] }
      | undefined,
    refetch: vi.fn(),
  },
}));

vi.mock('@/api/profile', () => ({ getProfile }));
vi.mock('@/hooks/admin/useAdminOperatorsQuery', () => ({
  useAdminOperatorsQuery: () => operatorQueryState,
}));

const sessionUser: User = {
  id: 'USER-1',
  email: 'session@example.com',
  username: 'session-user',
  companyName: 'GX株式会社',
  role: 'user',
  siteId: 'SITE-1',
};

function IdentityHarness({ user }: { user: User }) {
  const identity = useAccountIdentity(user);
  return (
    <div>
      <span>{identity.displayName}</span>
      <span>{identity.email}</span>
      <span>{identity.roleLabel}</span>
    </div>
  );
}

describe('useAccountIdentity', () => {
  beforeEach(() => {
    getProfile.mockReset();
    operatorQueryState.data = undefined;
    operatorQueryState.refetch.mockReset();
    useProfileStore.getState().clearProfile();
  });

  it('uses the matched operator list identity without calling the user profile API', () => {
    operatorQueryState.data = {
      statusCode: 'OK',
      message: '',
      users: [
        {
          id: 'ADMIN-1',
          username: 'admin.primary',
          name: '管理 太郎',
          role: 'operator',
          email: 'admin.profile@example.com',
          firstName: '太郎',
          firstNameKana: 'タロウ',
          lastName: '管理',
          lastNameKana: 'カンリ',
          phoneNumber: '0311112222',
          userPosition: '',
          siteId: null,
          siteName: '',
          companyId: 'COMP-1',
          permissions: [],
        },
      ],
    };

    render(
      <IdentityHarness
        user={{
          id: 'ADMIN-1',
          username: 'admin.primary',
          email: 'session@example.com',
          role: 'admin',
          siteId: null,
        }}
      />,
    );

    expect(screen.getByText('管理 太郎')).toBeInTheDocument();
    expect(screen.getByText('admin.profile@example.com')).toBeInTheDocument();
    expect(screen.getByText('運用者')).toBeInTheDocument();
    expect(getProfile).not.toHaveBeenCalled();
  });

  it('refetches once before resolving an uncached admin profile path', async () => {
    operatorQueryState.refetch.mockResolvedValue({
      data: {
        statusCode: 'OK',
        message: '',
        users: [
          {
            id: '9',
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
            permissions: [],
          },
        ],
      },
    });

    const { result } = renderHook(() =>
      useAccountIdentity({
        id: 'missing',
        username: 'admin.primary',
        email: 'admin@example.com',
        role: 'admin',
        siteId: null,
      }),
    );

    await expect(result.current.resolveProfilePath()).resolves.toBe(
      '/admin/operators/9',
    );
    expect(operatorQueryState.refetch).toHaveBeenCalledTimes(1);
  });

  it('uses the full profile name and profile email when available', async () => {
    getProfile.mockResolvedValue({
      firstName: '太郎',
      lastName: '山田',
      firstNameKana: 'タロウ',
      lastNameKana: 'ヤマダ',
      email: 'profile@example.com',
      userPosition: '担当者',
      phoneNumber: '0000000000',
      company: null,
    });

    render(
      <StrictMode>
        <IdentityHarness user={sessionUser} />
      </StrictMode>,
    );

    expect(await screen.findByText('山田 太郎')).toBeInTheDocument();
    expect(screen.getByText('profile@example.com')).toBeInTheDocument();
    expect(screen.getByText('社員')).toBeInTheDocument();
    expect(getProfile).toHaveBeenCalledTimes(1);
  });

  it('keeps session identity when the profile endpoint is unavailable', async () => {
    getProfile.mockRejectedValue(new Error('unsupported role'));

    render(<IdentityHarness user={sessionUser} />);

    await waitFor(() => expect(getProfile).toHaveBeenCalledTimes(1));
    expect(screen.getByText('session-user')).toBeInTheDocument();
    expect(screen.getByText('session@example.com')).toBeInTheDocument();
  });

  it('reuses the loaded profile when the navbar mounts on another page', async () => {
    getProfile.mockResolvedValue({
      firstName: '健一',
      lastName: '伊藤',
      firstNameKana: 'ケンイチ',
      lastNameKana: 'イトウ',
      email: 'ito.kenichi@example.com',
      userPosition: '担当者',
      phoneNumber: '0000000000',
      company: null,
    });

    const firstPage = render(<IdentityHarness user={sessionUser} />);
    expect(await screen.findByText('伊藤 健一')).toBeInTheDocument();
    firstPage.unmount();

    render(<IdentityHarness user={sessionUser} />);

    expect(screen.getByText('伊藤 健一')).toBeInTheDocument();
    expect(screen.getByText('ito.kenichi@example.com')).toBeInTheDocument();
    expect(getProfile).toHaveBeenCalledTimes(1);
  });
});
