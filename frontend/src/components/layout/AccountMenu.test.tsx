import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AccountMenu } from './AccountMenu';

describe('AccountMenu', () => {
  it('shows account identity and exposes profile and logout actions', async () => {
    const user = userEvent.setup();
    const onProfile = vi.fn();
    const onLogout = vi.fn().mockResolvedValue(undefined);

    render(
      <AccountMenu
        displayName="山田 太郎"
        email="taro@example.com"
        roleLabel="運用者"
        onProfile={onProfile}
        onLogout={onLogout}
      />,
    );

    const trigger = screen.getByRole('button', {
      name: 'アカウントメニューを開く：山田 太郎',
    });
    expect(trigger).toHaveTextContent('山田 太郎');
    expect(trigger).toHaveTextContent('運用者');
    expect(trigger).not.toHaveTextContent('taro@example.com');

    await user.click(trigger);
    expect(screen.getByText('taro@example.com')).toBeInTheDocument();
    expect(screen.getAllByText('山田 太郎')).toHaveLength(2);
    await user.click(screen.getByRole('menuitem', { name: 'プロファイル' }));
    expect(onProfile).toHaveBeenCalledTimes(1);

    await user.click(trigger);
    await user.click(screen.getByRole('menuitem', { name: 'ログアウト' }));
    expect(onLogout).toHaveBeenCalledTimes(1);
  });

  it('prevents repeated logout while the first request is pending', async () => {
    const user = userEvent.setup();
    const onLogout = vi.fn(() => new Promise<void>(() => undefined));

    render(
      <AccountMenu
        displayName="管理者"
        email="admin@example.com"
        roleLabel="管理者"
        onProfile={vi.fn()}
        onLogout={onLogout}
      />,
    );

    await user.click(
      screen.getByRole('button', {
        name: 'アカウントメニューを開く：管理者',
      }),
    );
    const logoutItem = screen.getByRole('menuitem', { name: 'ログアウト' });
    await user.click(logoutItem);

    expect(onLogout).toHaveBeenCalledTimes(1);
    await user.click(
      screen.getByRole('button', {
        name: 'アカウントメニューを開く：管理者',
      }),
    );
    const pendingLogoutItem = screen.getByRole('menuitem', {
      name: 'ログアウト中...',
    });
    expect(pendingLogoutItem).toHaveAttribute('data-disabled');
    await user.click(pendingLogoutItem);
    expect(onLogout).toHaveBeenCalledTimes(1);
  });
});
