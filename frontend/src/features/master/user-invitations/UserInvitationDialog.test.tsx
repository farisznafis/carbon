import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import { UserInvitationDialog } from './UserInvitationDialog';

const { createUserInvitationMock, toastMock } = vi.hoisted(() => ({
  createUserInvitationMock: vi.fn(),
  toastMock: vi.fn(),
}));

vi.mock('@/api/user-invitations', () => ({
  createUserInvitation: (...args: unknown[]) =>
    createUserInvitationMock(...args),
}));

vi.mock('@/hooks/use-toast', () => ({
  useToast: () => ({ toast: toastMock }),
}));

vi.mock('@/hooks/sites/useSitesQuery', () => ({
  useSitesQuery: () => ({
    data: {
      sites: [
        { siteId: 'site001', siteName: '東京拠点' },
        { siteId: 'site002', siteName: '大阪拠点' },
      ],
    },
    isLoading: false,
    isError: false,
  }),
}));

/** Keeps dialog open state realistic while exposing the shared component. */
function DialogHarness({ onSuccess }: Readonly<{ onSuccess?: () => void }>) {
  const [open, setOpen] = useState(true);
  return (
    <UserInvitationDialog
      open={open}
      onOpenChange={setOpen}
      onSuccess={onSuccess}
    />
  );
}

describe('UserInvitationDialog', () => {
  beforeEach(() => {
    createUserInvitationMock.mockReset();
    toastMock.mockReset();
    Element.prototype.hasPointerCapture = vi.fn();
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
    Element.prototype.scrollIntoView = vi.fn();
  });

  it('submits raw values without client validation and closes on success', async () => {
    const user = userEvent.setup();
    const onSuccess = vi.fn();
    createUserInvitationMock.mockResolvedValue({
      statusCode: 'OK',
      message: '招待メールを送信しました。',
    });
    render(<DialogHarness onSuccess={onSuccess} />);

    await user.click(
      screen.getByRole('button', { name: '招待メールを送信' }),
    );

    await waitFor(() => {
      expect(createUserInvitationMock).toHaveBeenCalledWith({
        email: '',
        siteId: null,
      });
    });
    expect(onSuccess).toHaveBeenCalledOnce();
    expect(toastMock).toHaveBeenCalledWith(
      expect.objectContaining({ variant: 'success' }),
    );
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('sends the selected existing site id', async () => {
    const user = userEvent.setup();
    createUserInvitationMock.mockResolvedValue({ statusCode: 'OK' });
    render(<DialogHarness />);

    await user.type(
      screen.getByLabelText(/メールアドレス/),
      'invitee@example.com',
    );
    await user.click(screen.getByRole('combobox', { name: '拠点（任意）' }));
    expect(screen.getByTestId('invitation-site-separator')).toBeVisible();
    await user.click(screen.getByRole('option', { name: '東京拠点' }));
    await user.click(
      screen.getByRole('button', { name: '招待メールを送信' }),
    );

    await waitFor(() => {
      expect(createUserInvitationMock).toHaveBeenCalledWith({
        email: 'invitee@example.com',
        siteId: 'site001',
      });
    });
  });

  it('renders backend field errors and the feature-unavailable state', async () => {
    const user = userEvent.setup();
    createUserInvitationMock.mockRejectedValue(
      new ApiError('Not found', {
        status: 404,
        payload: {
          statusCode: 'NOT_FOUND',
          message: 'Not found',
          errors: { email: 'メールアドレスを入力してください。' },
        },
      }),
    );
    render(<DialogHarness />);

    await user.click(
      screen.getByRole('button', { name: '招待メールを送信' }),
    );

    expect(
      await screen.findByText('メールアドレスを入力してください。'),
    ).toBeVisible();
    expect(
      screen.getByText('ユーザー招待機能は現在利用できません。'),
    ).toBeVisible();
  });
});
