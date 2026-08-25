import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ForgotPassword from './ForgotPassword';

const { handleError, requestPasswordReset } = vi.hoisted(() => ({
  handleError: vi.fn(),
  requestPasswordReset: vi.fn(),
}));

vi.mock('@/api/auth', () => ({
  requestPasswordReset,
}));

vi.mock('@/hooks/useErrorHandler', () => ({
  useErrorHandler: () => ({ handleError }),
}));

function renderPage() {
  return render(
    <MemoryRouter
      initialEntries={['/forgot-password']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

function submitEmail() {
  fireEvent.change(screen.getByLabelText('メールアドレス'), {
    target: { value: 'user@example.com' },
  });
  fireEvent.click(screen.getByRole('button', { name: 'リセットリンクを送信' }));
}

describe('ForgotPassword', () => {
  beforeEach(() => {
    handleError.mockReset();
    requestPasswordReset.mockReset();
  });

  it('shows the email-sent state after a successful request and returns on demand', async () => {
    requestPasswordReset.mockResolvedValue({ statusCode: 'OK' });
    renderPage();

    submitEmail();

    expect(
      await screen.findByRole('heading', {
        name: 'パスワード再設定メールを送信しました',
      }),
    ).toBeInTheDocument();
    expect(requestPasswordReset).toHaveBeenCalledWith('user@example.com');
    expect(
      screen.getByText(/迷惑メールフォルダをご確認ください/),
    ).toBeInTheDocument();
    expect(screen.queryByLabelText('メールアドレス')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'ログインへ戻る' }));
    expect(await screen.findByText('Login page')).toBeInTheDocument();
  });

  it('keeps the form visible when the request fails', async () => {
    const error = new Error('request failed');
    requestPasswordReset.mockRejectedValue(error);
    renderPage();

    submitEmail();

    await waitFor(() => expect(handleError).toHaveBeenCalledWith(error));
    expect(screen.getByLabelText('メールアドレス')).toBeInTheDocument();
    expect(
      screen.queryByText('パスワード再設定メールを送信しました'),
    ).not.toBeInTheDocument();
  });

  it('disables the submit button while the request is pending', async () => {
    requestPasswordReset.mockReturnValue(new Promise(() => undefined));
    renderPage();

    submitEmail();

    await waitFor(() =>
      expect(screen.getByRole('button', { name: '送信中...' })).toBeDisabled(),
    );
    fireEvent.click(screen.getByRole('button', { name: '送信中...' }));
    expect(requestPasswordReset).toHaveBeenCalledTimes(1);
  });
});
