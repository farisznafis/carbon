import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import { UserLoginForm } from './UserLoginForm';

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ isAuthenticated: false, user: null }),
}));

const renderForm = (onSubmit = vi.fn()) => {
  render(
    <MemoryRouter>
      <UserLoginForm onSubmit={onSubmit} />
    </MemoryRouter>,
  );
  return onSubmit;
};

const fillAndSubmit = async (identifier: string) => {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('企業ID'), '1234567890111');
  await user.type(screen.getByLabelText('メールアドレスまたはユーザー名'), identifier);
  await user.type(screen.getByLabelText('パスワード'), 'secret-pass');
  await user.click(screen.getByRole('button', { name: 'ログイン' }));
};

describe('UserLoginForm', () => {
  it('shows an inline message under each empty field and does not submit', async () => {
    const onSubmit = renderForm();

    await userEvent.setup().click(screen.getByRole('button', { name: 'ログイン' }));

    expect(await screen.findByText('企業IDを入力してください。')).toBeInTheDocument();
    expect(screen.getByText('メールアドレスまたはユーザー名を入力してください。')).toBeInTheDocument();
    expect(screen.getByText('パスワードを入力してください。')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('accepts a username without @ in the email field', async () => {
    const onSubmit = renderForm();

    await fillAndSubmit('name_company');

    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        companyId: '1234567890111',
        email: 'name_company',
        password: 'secret-pass',
      }),
    );
  });

  it('puts a 400 VALIDATION_ERROR on its fields without a banner', async () => {
    renderForm(
      vi.fn().mockRejectedValue(
        new ApiError('invalid', {
          status: 400,
          payload: {
            statusCode: 'VALIDATION_ERROR',
            message: 'invalid',
            errors: { identifier: 'ユーザーの形式が正しくありません。' },
          },
        }),
      ),
    );

    await fillAndSubmit('name_company');

    expect(await screen.findByText('ユーザーの形式が正しくありません。')).toBeInTheDocument();
    expect(screen.queryByText('invalid')).not.toBeInTheDocument();
  });

  it('shows a 401 INVALID_CREDENTIALS message in the banner only and clears the password', async () => {
    renderForm(
      vi.fn().mockRejectedValue(
        new ApiError('wrong', {
          status: 401,
          payload: { statusCode: 'INVALID_CREDENTIALS', message: 'ログイン情報が正しくありません。' },
        }),
      ),
    );

    await fillAndSubmit('user@example.com');

    expect(await screen.findByText('ログイン情報が正しくありません。')).toBeInTheDocument();
    expect(screen.getByLabelText('パスワード')).toHaveValue('');
    expect(screen.getByLabelText('企業ID')).not.toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByLabelText('メールアドレスまたはユーザー名')).not.toHaveAttribute('aria-invalid', 'true');
  });
});
