import { StrictMode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { User } from '@/api/users';
import { useProfileStore } from '@/stores/ProfileStore';
import { useAccountIdentity } from './useAccountIdentity';

const { getProfile } = vi.hoisted(() => ({ getProfile: vi.fn() }));

vi.mock('@/api/profile', () => ({ getProfile }));

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
    </div>
  );
}

describe('useAccountIdentity', () => {
  beforeEach(() => {
    getProfile.mockReset();
    useProfileStore.getState().clearProfile();
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
