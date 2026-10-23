import { StrictMode } from 'react';
import { act, render, screen, waitFor } from '@testing-library/react';
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
      siteId: 'site001',
      siteName: '東京拠点',
    });
    sessionStorage.clear();
    window.history.replaceState(
      window.history.state,
      '',
      '/user/invitation#token=email-link-token',
    );
  });

  it('restores the form from the stored session after a refresh', async () => {
    sessionStorage.setItem(
      'action-session:user-invitation',
      JSON.stringify({ actionSessionToken: 'stored-session', expiresAt: Date.now() + 60_000 }),
    );
    window.history.replaceState(window.history.state, '', '/user/invitation');
    renderInvitationPage({ strict: true });

    expect(await screen.findByRole('heading', { name: 'アカウント登録' })).toBeVisible();
    expect(exchangeActionLinkMock).not.toHaveBeenCalled();
    expect(getUserInvitationContextMock).toHaveBeenCalledWith('stored-session');
  });

  it('shows the invalid state and removes an expired stored session', async () => {
    sessionStorage.setItem(
      'action-session:user-invitation',
      JSON.stringify({ actionSessionToken: 'old-session', expiresAt: Date.now() - 1 }),
    );
    window.history.replaceState(window.history.state, '', '/user/invitation');
    renderInvitationPage();

    expect(await screen.findByText('招待リンクを利用できません')).toBeInTheDocument();
    expect(sessionStorage.getItem('action-session:user-invitation')).toBeNull();
    expect(getUserInvitationContextMock).not.toHaveBeenCalled();
  });

  it('exchanges and clears the fragment once while showing readonly context', async () => {
    const user = userEvent.setup();
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
    expect(
      screen.getByText(
        '招待情報を確認し、個人情報とログイン情報を入力してください。',
      ),
    ).toBeVisible();
    expect(screen.getByText('変更不可')).toBeVisible();
    expect(
      screen.getByText(
        /招待リンクから自動入力されています。内容は変更できません。/,
      ),
    ).toBeVisible();
    expect(
      screen.getByText(
        /内容に誤りがある場合は、招待元のマスターにお問い合わせください。/,
      ),
    ).toBeVisible();

    const invitationSummary = screen.getByText('メールアドレス').closest('dl');
    expect(invitationSummary).toBeInTheDocument();
    expect(invitationSummary).toHaveAttribute(
      'aria-describedby',
      'invitation-readonly-note',
    );
    expect(screen.getByText('invitee@example.com').tagName).toBe('DD');
    expect(screen.getByText('東京拠点').tagName).toBe('DD');
    expect(
      screen.queryByDisplayValue('invitee@example.com'),
    ).not.toBeInTheDocument();

    const lastNameInput = screen.getByLabelText('姓');
    expect(lastNameInput).not.toHaveAttribute('readonly');
    await user.type(lastNameInput, '山田');
    expect(lastNameInput).toHaveValue('山田');
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
    await user.type(screen.getByLabelText('ユーザー名'), 'yamada_taro');
    await user.type(
      screen.getByLabelText('パスワード'),
      'Strong-password-123',
    );
    await user.type(
      screen.getByLabelText('パスワード（確認）'),
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
    expect(completeUserInvitationMock.mock.calls[0]?.[1]).not.toHaveProperty(
      'passwordConfirmation',
    );
    expect(await screen.findByText('ログイン画面')).toBeVisible();
    expect(toastMock).toHaveBeenCalledWith(
      expect.objectContaining({ variant: 'success' }),
    );
  });

  it('reports password matching live and blocks a mismatched confirmation', async () => {
    const user = userEvent.setup();
    renderInvitationPage();

    const passwordInput = await screen.findByLabelText('パスワード');
    const confirmationInput = screen.getByLabelText('パスワード（確認）');

    expect(
      screen.getByText('確認のため、パスワードをもう一度入力してください。'),
    ).toBeVisible();

    await user.type(passwordInput, 'Strong-password-123');
    await user.type(confirmationInput, 'Strong-password-12');
    expect(screen.getByText('パスワードが一致していません。')).toBeVisible();

    await user.click(screen.getByRole('button', { name: '登録する' }));
    expect(completeUserInvitationMock).not.toHaveBeenCalled();

    await user.type(confirmationInput, '3');
    expect(screen.getByText('パスワードが一致しています。')).toBeVisible();
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
            username: 'ユーザー名を入力してください。',
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
      await screen.findByText('ユーザー名を入力してください。'),
    ).toBeVisible();
    expect(
      screen.getByText('パスワードは15文字以上で入力してください。'),
    ).toBeVisible();
    // Field errors are listed in the summary; the generic line would only repeat them.
    expect(screen.getByText('入力内容を確認してください（2件）')).toBeVisible();
    expect(screen.queryByText('入力内容に不備があります。')).not.toBeInTheDocument();
  });

  it('processes a new invitation token opened in the same tab', async () => {
    exchangeActionLinkMock
      .mockRejectedValueOnce(
        new ApiError('アクセスできません。', {
          status: 401,
          payload: { statusCode: 'INVALID_OR_EXPIRED_TOKEN' },
        }),
      )
      .mockResolvedValueOnce({
        statusCode: 'OK',
        actionSessionToken: 'fresh-action-session',
        purpose: 'USER_INVITATION',
        expiresInSeconds: 3600,
      });
    getUserInvitationContextMock.mockResolvedValueOnce({
      email: 'fresh-invitee@example.com',
      companyName: 'GX株式会社',
      siteId: null,
      siteName: '',
    });
    renderInvitationPage();

    await waitFor(() => {
      expect(exchangeActionLinkMock).toHaveBeenCalledWith('email-link-token');
    });
    expect(
      await screen.findByRole('heading', {
        name: '招待リンクを利用できません',
      }),
    ).toBeVisible();

    act(() => {
      window.history.pushState(
        window.history.state,
        '',
        '/user/invitation#token=fresh-email-link-token',
      );
      window.dispatchEvent(new Event('hashchange'));
    });

    await waitFor(() => {
      expect(exchangeActionLinkMock).toHaveBeenNthCalledWith(
        2,
        'fresh-email-link-token',
      );
    });
    expect(getUserInvitationContextMock).toHaveBeenCalledWith(
      'fresh-action-session',
    );
    expect(await screen.findByText('fresh-invitee@example.com')).toBeVisible();
    expect(window.location.hash).toBe('');
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
