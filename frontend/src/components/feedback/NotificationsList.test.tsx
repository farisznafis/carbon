import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { UserNotification } from '@/api/notifications';
import { NotificationsList } from './NotificationsList';

const notifications: UserNotification[] = [
  {
    id: 1,
    title: 'プロジェクト申請が承認されました',
    message: '拠点Aの参加申請が承認されました。',
    read: false,
    operatingUserId: 10,
    participatingUserId: null,
    createdAt: '2026-07-29T10:30:00',
    updatedAt: '2026-07-29T10:30:00',
  },
  {
    id: 2,
    title: '既読のお知らせ',
    message: '確認済みです。',
    read: true,
    operatingUserId: 10,
    participatingUserId: null,
    createdAt: '2026-07-28T09:00:00',
    updatedAt: '2026-07-28T09:00:00',
  },
];

describe('NotificationsList', () => {
  it('shows an unread dot, timestamp, and mark-as-read action only when unread', () => {
    render(
      <NotificationsList
        notifications={notifications}
        loading={false}
        onMarkAsRead={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    expect(screen.getByLabelText('未読')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '既読にする' })).toBeInTheDocument();
    expect(screen.getByText('2026年07月29日 10:30')).toBeInTheDocument();
    expect(screen.getAllByLabelText('通知メニュー')).toHaveLength(2);
    const unreadTitle = screen.getByText('プロジェクト申請が承認されました');
    const unreadHeader = unreadTitle.parentElement?.parentElement;
    expect(unreadHeader).toHaveClass('flex', 'justify-between');
    expect(unreadHeader).toContainElement(
      screen.getAllByLabelText('通知メニュー')[0],
    );
    screen.getAllByRole('listitem').forEach((notificationCard) => {
      expect(notificationCard).toHaveClass(
        'rounded-xl',
        'border',
        'bg-card',
        'p-4',
      );
    });
  });

  it('runs read and delete actions from the notification controls', async () => {
    const user = userEvent.setup();
    const onMarkAsRead = vi.fn().mockResolvedValue(undefined);
    const onDelete = vi.fn().mockResolvedValue(undefined);

    render(
      <NotificationsList
        notifications={notifications}
        loading={false}
        onMarkAsRead={onMarkAsRead}
        onDelete={onDelete}
      />,
    );

    await user.click(screen.getByRole('button', { name: '既読にする' }));
    expect(onMarkAsRead).toHaveBeenCalledWith(1);

    await user.click(screen.getAllByLabelText('通知メニュー')[0]);
    await user.click(await screen.findByRole('menuitem', { name: '削除' }));
    expect(onDelete).toHaveBeenCalledWith(1);
  });
});
