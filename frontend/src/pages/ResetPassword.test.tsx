import { StrictMode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ResetPassword from './ResetPassword';

const { exchangePasswordResetActionLinkMock } = vi.hoisted(() => ({
  exchangePasswordResetActionLinkMock: vi.fn(),
}));

vi.mock('@/api/auth', () => ({
  completePasswordReset: vi.fn(),
  exchangePasswordResetActionLink: (...args: unknown[]) =>
    exchangePasswordResetActionLinkMock(...args),
}));

describe('ResetPassword', () => {
  beforeEach(() => {
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
});
