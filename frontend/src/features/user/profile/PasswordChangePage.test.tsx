import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '@/api/_shared/errors';
import { paths } from '@/routes/paths';
import PasswordChange from './PasswordChangePage';

const { authState, changePassword, logout, toast } = vi.hoisted(() => ({
  authState: { user: { id: 'master-1', role: 'master', siteId: '' } },
  changePassword: vi.fn(),
  logout: vi.fn(),
  toast: vi.fn(),
}));

vi.mock('@/api/auth', () => ({ changePassword }));
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({ user: authState.user, logout }),
}));
vi.mock('@/hooks/use-toast', () => ({ useToast: () => ({ toast }) }));

/** Mounts the shared password form with its real profile destinations. */
function renderPasswordChange(initialPath: string) {
  return render(
    <MemoryRouter
      initialEntries={[initialPath]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route path={paths.passwordChange} element={<PasswordChange />} />
        <Route path={paths.masterUserProfileRoute} element={<div>Employee profile</div>} />
        <Route path={paths.login} element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

/** Enters the new password and confirmation in the shared form. */
function enterNewPassword(password: string, confirmation: string) {
  fireEvent.change(screen.getByLabelText('新しいパスワード'), {
    target: { value: password },
  });
  fireEvent.change(screen.getByLabelText('新しいパスワード（確認）'), {
    target: { value: confirmation },
  });
}

/** Submits the shared password form through its visible action. */
function submitPasswordChange() {
  fireEvent.click(screen.getByRole('button', { name: 'パスワードを変更' }));
}

describe('PasswordChangePage', () => {
  beforeEach(() => {
    changePassword.mockReset().mockResolvedValue({ statusCode: 'OK' });
    logout.mockReset().mockResolvedValue(undefined);
    toast.mockReset();
  });

  it('requires and sends the current password for the signed-in account', async () => {
    renderPasswordChange(paths.passwordChange);
    enterNewPassword('NewPassword123', 'NewPassword123');

    submitPasswordChange();
    expect(await screen.findByText('現在のパスワードを入力してください')).toBeInTheDocument();
    expect(changePassword).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText('現在のパスワード'), {
      target: { value: 'OldPassword123' },
    });
    submitPasswordChange();

    await waitFor(() =>
      expect(changePassword).toHaveBeenCalledWith({
        userId: 'master-1',
        currentPassword: 'OldPassword123',
        newPassword: 'NewPassword123',
      }),
    );
    expect(await screen.findByText('Login page')).toBeInTheDocument();
    expect(logout).toHaveBeenCalledOnce();
  });

  it('hides the current password and sends null for another account', async () => {
    renderPasswordChange(paths.passwordChangeForUser('employee-2'));
    expect(screen.queryByLabelText('現在のパスワード')).not.toBeInTheDocument();

    enterNewPassword('NewPassword123', 'Different123');
    submitPasswordChange();
    expect(await screen.findByText('新しいパスワードが一致しません')).toBeInTheDocument();
    expect(changePassword).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText('新しいパスワード（確認）'), {
      target: { value: 'NewPassword123' },
    });
    submitPasswordChange();

    await waitFor(() =>
      expect(changePassword).toHaveBeenCalledWith({
        userId: 'employee-2',
        currentPassword: null,
        newPassword: 'NewPassword123',
      }),
    );
    expect(await screen.findByText('Employee profile')).toBeInTheDocument();
    expect(logout).not.toHaveBeenCalled();
  });

  it('shows a backend current-password error when that field is hidden', async () => {
    changePassword.mockRejectedValueOnce(
      new ApiError('Password update failed', {
        status: 400,
        payload: {
          errors: { currentPassword: 'Password validation failed' },
        },
      }),
    );
    renderPasswordChange(paths.passwordChangeForUser('employee-2'));
    enterNewPassword('NewPassword123', 'NewPassword123');
    submitPasswordChange();

    await waitFor(() =>
      expect(toast).toHaveBeenCalledWith(
        expect.objectContaining({ description: 'Password validation failed' }),
      ),
    );
    expect(screen.queryByLabelText('現在のパスワード')).not.toBeInTheDocument();
  });
});
