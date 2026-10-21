import { StrictMode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import MasterSetupPage from './MasterSetupPage';

const { exchangeActionLinkMock, setupMasterAccountMock, toastMock } = vi.hoisted(() => ({
  exchangeActionLinkMock: vi.fn(),
  setupMasterAccountMock: vi.fn(),
  toastMock: vi.fn(),
}));

vi.mock('@/api/auth', () => ({
  exchangeActionLink: (...args: unknown[]) => exchangeActionLinkMock(...args),
  setupMasterAccount: (...args: unknown[]) => setupMasterAccountMock(...args),
}));
vi.mock('@/hooks/use-toast', () => ({ useToast: () => ({ toast: toastMock }) }));

function renderSetupPage() {
  return render(
    <MemoryRouter
      initialEntries={['/account/setup']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <StrictMode>
        <Routes>
          <Route path="/account/setup" element={<MasterSetupPage />} />
          <Route path="/login" element={<div>ログイン画面</div>} />
        </Routes>
      </StrictMode>
    </MemoryRouter>,
  );
}

async function fillForm(username: string, password = 'Password123', confirmation = password) {
  const user = userEvent.setup();
  await user.type(await screen.findByLabelText('ユーザー名'), username);
  await user.type(screen.getByLabelText('パスワード'), password);
  await user.type(screen.getByLabelText('パスワード（確認）'), confirmation);
  await user.click(screen.getByRole('button', { name: '設定する' }));
}

describe('MasterSetupPage', () => {
  beforeEach(() => {
    exchangeActionLinkMock.mockReset().mockResolvedValue({
      statusCode: 'OK',
      actionSessionToken: 'setup-session',
      purpose: 'MASTER_ACCOUNT_SETUP',
      expiresInSeconds: 3600,
    });
    setupMasterAccountMock.mockReset().mockResolvedValue({ statusCode: 'OK', message: '完了' });
    toastMock.mockReset();
    window.history.replaceState(window.history.state, '', '/account/setup#token=email-link-token');
  });

  it('exchanges the fragment token once, clears it, and sets up the account', async () => {
    renderSetupPage();
    await fillForm('yamada_taro');

    expect(exchangeActionLinkMock).toHaveBeenCalledTimes(1);
    expect(exchangeActionLinkMock).toHaveBeenCalledWith('email-link-token');
    expect(window.location.hash).toBe('');
    await waitFor(() =>
      expect(setupMasterAccountMock).toHaveBeenCalledWith('setup-session', {
        username: 'yamada_taro',
        password: 'Password123',
      }),
    );
    expect(await screen.findByText('ログイン画面')).toBeInTheDocument();
  });

  it('shows a taken username under the field', async () => {
    setupMasterAccountMock.mockRejectedValue(
      new ApiError('このユーザ名は既に使用されています。', {
        status: 409,
        payload: { statusCode: 'USED_USERNAME', message: 'このユーザ名は既に使用されています。' },
      }),
    );
    renderSetupPage();
    await fillForm('taken_name');

    const field = screen.getByLabelText('ユーザー名');
    expect(await screen.findByText('このユーザ名は既に使用されています。')).toBeInTheDocument();
    expect(field).toHaveAttribute('aria-invalid', 'true');
  });

  it('checks the username and password rules before calling the backend', async () => {
    renderSetupPage();
    await fillForm('ab', 'short', 'other');

    expect(await screen.findByText('ユーザー名は3文字以上で入力してください。')).toBeInTheDocument();
    expect(screen.getByText('パスワードは8文字以上で入力してください')).toBeInTheDocument();
    expect(screen.getByText('パスワードが一致していません。')).toBeInTheDocument();
    expect(setupMasterAccountMock).not.toHaveBeenCalled();
  });

  it('explains an expired or used link', async () => {
    exchangeActionLinkMock.mockRejectedValue(
      new ApiError('invalid', { status: 401, payload: { statusCode: 'INVALID_OR_EXPIRED_TOKEN' } }),
    );
    renderSetupPage();

    expect(await screen.findByRole('heading', { name: '設定リンクを利用できません' })).toBeInTheDocument();
  });

  it('rejects a link issued for another purpose', async () => {
    exchangeActionLinkMock.mockResolvedValue({
      statusCode: 'OK',
      actionSessionToken: 'other-session',
      purpose: 'USER_INVITATION',
      expiresInSeconds: 3600,
    });
    renderSetupPage();

    expect(await screen.findByRole('heading', { name: '設定リンクを利用できません' })).toBeInTheDocument();
  });
});
