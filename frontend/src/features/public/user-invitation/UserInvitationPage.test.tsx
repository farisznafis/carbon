import { StrictMode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import UserInvitationPage from './UserInvitationPage';

const {
  exchangeActionLinkMock,
  getUserInvitationContextMock,
  completeUserInvitationMock,
  toastMock,
} = vi.hoisted(() => ({
  exchangeActionLinkMock: vi.fn(),
  getUserInvitationContextMock: vi.fn(),
  completeUserInvitationMock: vi.fn(),
  toastMock: vi.fn(),
}));

vi.mock('@/api/auth', () => ({
  exchangeActionLink: (...args: unknown[]) => exchangeActionLinkMock(...args),
}));

vi.mock('@/api/user-invitations', () => ({
  getUserInvitationContext: (...args: unknown[]) =>
    getUserInvitationContextMock(...args),
  completeUserInvitation: (...args: unknown[]) =>
    completeUserInvitationMock(...args),
}));

vi.mock('@/hooks/use-toast', () => ({
  useToast: () => ({ toast: toastMock }),
}));

/** Renders the public invitation route and a visible login destination. */
function renderInvitationPage({ strict = false } = {}) {
  const page = (
    <Routes>
      <Route path="/user/invitation" element={<UserInvitationPage />} />
      <Route path="/login" element={<div>ログイン画面</div>} />
    </Routes>
  );

  return render(
    <MemoryRouter
      initialEntries={['/user/invitation']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      {strict ? <StrictMode>{page}</StrictMode> : page}
    </MemoryRouter>,
  );
}

describe('UserInvitationPage', () => {
  beforeEach(() => {
    exchangeActionLinkMock.mockReset();
    getUserInvitationContextMock.mockReset();
    completeUserInvitationMock.mockReset();
    toastMock.mockReset();
    exchangeActionLinkMock.mockResolvedValue({
      statusCode: 'OK',
      actionSessionToken: 'invitation-action-session',
      purpose: 'USER_INVITATION',
      expiresInSeconds: 3600,
    });
    getUserInvitationContextMock.mockResolvedValue({
      email: 'invitee@example.com',
      companyName: 'GX株式会社',
      siteId: 42,
      siteName: '東京拠点',
    });
    window.history.replaceState(
      window.history.state,
      '',
      '/user/invitation#token=email-link-token',
    );
  });

  it('exchanges and clears the fragment once while showing readonly context', async () => {
    renderInvitationPage({ strict: true });

    expect(
      await screen.findByRole('heading', { name: 'アカウント登録' }),
    ).toBeVisible();
    await waitFor(() => expect(exchangeActionLinkMock).toHaveBeenCalledOnce());
    expect(exchangeActionLinkMock).toHaveBeenCalledWith('email-link-token');
    expect(getUserInvitationContextMock).toHaveBeenCalledWith(
      'invitation-action-session',
    );
    expect(window.location.hash).toBe('');
    expect(screen.getByDisplayValue('invitee@example.com')).toHaveAttribute(
      'readonly',
    );
    expect(screen.getByDisplayValue('GX株式会社')).toHaveAttribute('readonly');
    expect(screen.getByDisplayValue('東京拠点')).toHaveAttribute('readonly');
  });

  it('submits only the account fields and redirects to login', async () => {
    const user = userEvent.setup();
    completeUserInvitationMock.mockResolvedValue({
      statusCode: 'OK',
      message: '登録しました。',
    });
    renderInvitationPage();

    await screen.findByRole('heading', { name: 'アカウント登録' });
    await user.type(screen.getByLabelText('姓'), '山田');
    await user.type(screen.getByLabelText('名'), '太郎');
    await user.type(screen.getByLabelText('姓（カナ）'), 'ヤマダ');
    await user.type(screen.getByLabelText('名（カナ）'), 'タロウ');
    await user.type(
      screen.getByLabelText('電話番号（ハイフン無し）'),
      '09012345678',
    );
    await user.type(screen.getByLabelText('役職'), '部長');
    await user.type(screen.getByLabelText('ユーザーネーム'), 'yamada_taro');
    await user.type(
      screen.getByLabelText('パスワード'),
      'Strong-password-123',
    );
    await user.click(screen.getByRole('button', { name: '登録する' }));

    await waitFor(() => {
      expect(completeUserInvitationMock).toHaveBeenCalledWith(
        'invitation-action-session',
        {
          username: 'yamada_taro',
          firstName: '太郎',
          firstNameKana: 'タロウ',
          lastName: '山田',
          lastNameKana: 'ヤマダ',
          phoneNumber: '09012345678',
          userPosition: '部長',
          password: 'Strong-password-123',
        },
      );
    });
    expect(completeUserInvitationMock.mock.calls[0]?.[1]).not.toHaveProperty(
      'email',
    );
    expect(await screen.findByText('ログイン画面')).toBeVisible();
    expect(toastMock).toHaveBeenCalledWith(
      expect.objectContaining({ variant: 'success' }),
    );
  });

  it('submits empty values and maps backend validation errors verbatim', async () => {
    const user = userEvent.setup();
    completeUserInvitationMock.mockRejectedValue(
      new ApiError('入力内容に不備があります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          message: '入力内容に不備があります。',
          errors: {
            username: 'ユーザーネームを入力してください。',
            password: 'パスワードは15文字以上で入力してください。',
          },
        },
      }),
    );
    renderInvitationPage();

    await user.click(
      await screen.findByRole('button', { name: '登録する' }),
    );

    expect(completeUserInvitationMock).toHaveBeenCalledWith(
      'invitation-action-session',
      expect.objectContaining({ username: '', password: '' }),
    );
    expect(
      await screen.findByText('ユーザーネームを入力してください。'),
    ).toBeVisible();
    expect(
      screen.getByText('パスワードは15文字以上で入力してください。'),
    ).toBeVisible();
    expect(screen.getByText('入力内容に不備があります。')).toBeVisible();
  });

  it('distinguishes invalid links from a disabled feature', async () => {
    exchangeActionLinkMock.mockRejectedValueOnce(
      new ApiError('アクセスできません。', {
        status: 401,
        payload: { statusCode: 'INVALID_OR_EXPIRED_TOKEN' },
      }),
    );
    renderInvitationPage();

    expect(
      await screen.findByRole('heading', {
        name: '招待リンクを利用できません',
      }),
    ).toBeVisible();

    window.history.replaceState(
      window.history.state,
      '',
      '/user/invitation#token=second-link-token',
    );
    exchangeActionLinkMock.mockRejectedValueOnce(
      new ApiError('Not found', {
        status: 404,
        payload: { statusCode: 'NOT_FOUND' },
      }),
    );
    renderInvitationPage();

    expect(
      await screen.findByRole('heading', {
        name: 'ユーザー招待機能は現在利用できません',
      }),
    ).toBeVisible();
  });
});
