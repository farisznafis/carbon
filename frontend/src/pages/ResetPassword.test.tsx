import { StrictMode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import ResetPassword from './ResetPassword';

const {
  completePasswordResetMock,
  exchangePasswordResetActionLinkMock,
} = vi.hoisted(() => ({
  completePasswordResetMock: vi.fn(),
  exchangePasswordResetActionLinkMock: vi.fn(),
}));

vi.mock('@/api/auth', () => ({
  completePasswordReset: (...args: unknown[]) =>
    completePasswordResetMock(...args),
  exchangePasswordResetActionLink: (...args: unknown[]) =>
    exchangePasswordResetActionLinkMock(...args),
}));

describe('ResetPassword', () => {
  beforeEach(() => {
    completePasswordResetMock.mockReset();
    exchangePasswordResetActionLinkMock.mockReset();
    window.history.replaceState(
      window.history.state,
      '',
      '/reset-password#token=email-link-token',
    );
  });

  it('exchanges a single-use token only once in React StrictMode', async () => {
    exchangePasswordResetActionLinkMock.mockResolvedValue({
      statusCode: 'OK',
      actionSessionToken: 'action-session',
      purpose: 'PASSWORD_RESET',
      expiresInSeconds: 900,
    });

    render(
      <StrictMode>
        <MemoryRouter initialEntries={['/reset-password']}>
          <ResetPassword />
        </MemoryRouter>
      </StrictMode>,
    );

    expect(
      await screen.findByRole('heading', {
        name: '新しいパスワードを設定',
      }),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(exchangePasswordResetActionLinkMock).toHaveBeenCalledOnce();
    });
    expect(exchangePasswordResetActionLinkMock).toHaveBeenCalledWith(
      'email-link-token',
    );
    expect(window.location.hash).toBe('');
  });

  it('shows new-password validation returned by the backend', async () => {
    const user = userEvent.setup();
    exchangePasswordResetActionLinkMock.mockResolvedValue({
      statusCode: 'OK',
      actionSessionToken: 'action-session',
      purpose: 'PASSWORD_RESET',
      expiresInSeconds: 900,
    });
    completePasswordResetMock.mockRejectedValue(
      new ApiError('入力内容に誤りがあります。', {
        status: 400,
        payload: {
          statusCode: 'VALIDATION_ERROR',
          errors: {
            'request::newPassword': 'バックエンドのパスワードエラー',
          },
        },
      }),
    );

    render(
      <MemoryRouter initialEntries={['/reset-password']}>
        <ResetPassword />
      </MemoryRouter>,
    );

    await user.type(
      await screen.findByLabelText('新しいパスワード'),
      'same-password',
    );
    await user.type(
      screen.getByLabelText('新しいパスワード（確認）'),
      'same-password',
    );
    await user.click(
      screen.getByRole('button', { name: 'パスワードを再設定' }),
    );

    expect(
      await screen.findByText('バックエンドのパスワードエラー'),
    ).toBeInTheDocument();
  });
});
