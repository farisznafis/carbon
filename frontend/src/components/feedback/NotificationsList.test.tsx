import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { UserNotification } from '@/api/notifications';
import { NotificationsList } from './NotificationsList';

const notifications: UserNotification[] = [
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
];

describe('NotificationsList', () => {
  it('shows clickable read and unread notification cards without a separate read action', () => {
    render(
      <NotificationsList
        notifications={notifications}
        loading={false}
        onMarkAsRead={vi.fn()}
        onDelete={vi.fn()}
      />,
    );

    expect(screen.getByLabelText('未読')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: '既読にする' }),
    ).not.toBeInTheDocument();
    expect(screen.getByText('2026年07月29日 10:30')).toBeInTheDocument();
    expect(
      screen.getByRole('button', {
        name: '「プロジェクト申請が承認されました」の詳細を表示',
      }),
    ).toBeInTheDocument();
    expect(screen.getAllByLabelText('通知メニュー')).toHaveLength(2);

    const notificationCards = screen.getAllByRole('listitem');
    notificationCards.forEach((notificationCard) => {
      expect(notificationCard).toHaveClass('rounded-lg', 'px-3', 'py-4');
    });
    expect(
      within(notificationCards[0]).getByText(
        'プロジェクト申請が承認されました',
      ),
    ).toHaveClass('font-bold');
    expect(notificationCards[0]).toHaveClass('bg-card');
    expect(
      within(notificationCards[0]).getByText(
        '拠点Aの参加申請が承認されました。',
      ),
    ).toHaveClass('line-clamp-2');
    expect(
      within(notificationCards[1]).getByText('既読のお知らせ'),
    ).toHaveClass('font-normal');
    expect(notificationCards[1]).toHaveClass('bg-muted/40');
    expect(within(notificationCards[1]).queryByLabelText('未読')).toBeNull();
  });

  it('opens notification details and marks only unread notifications as read', async () => {
    const user = userEvent.setup();
    const onMarkAsRead = vi.fn().mockResolvedValue(undefined);

    render(
      <NotificationsList
        notifications={notifications}
        loading={false}
        onMarkAsRead={onMarkAsRead}
      />,
    );

    const unreadDetailButton = screen.getByRole('button', {
      name: '「プロジェクト申請が承認されました」の詳細を表示',
    });
    await user.click(unreadDetailButton);

    expect(onMarkAsRead).toHaveBeenCalledWith(1);
    expect(onMarkAsRead).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(
      within(screen.getByRole('dialog')).getByText(
        'プロジェクト申請が承認されました',
      ),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole('dialog')).getByText(
        '拠点Aの参加申請が承認されました。',
      ),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole('dialog')).getByText('2026年07月29日 10:30'),
    ).toBeInTheDocument();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(unreadDetailButton).toHaveFocus();

    const readDetailButton = screen.getByRole('button', {
      name: '「既読のお知らせ」の詳細を表示',
    });
    readDetailButton.focus();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(onMarkAsRead).toHaveBeenCalledTimes(1);
  });

  it('keeps delete separate from opening notification details', async () => {
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

    await user.click(screen.getAllByLabelText('通知メニュー')[0]);
    await user.click(await screen.findByRole('menuitem', { name: '削除' }));

    expect(onDelete).toHaveBeenCalledWith(1);
    expect(onMarkAsRead).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('keeps the detail dialog open and the notification unread when marking fails', async () => {
    const user = userEvent.setup();
    const onMarkAsRead = vi.fn().mockRejectedValue(new Error('request failed'));

    render(
      <NotificationsList
        notifications={notifications}
        loading={false}
        onMarkAsRead={onMarkAsRead}
      />,
    );

    await user.click(
      screen.getByRole('button', {
        name: '「プロジェクト申請が承認されました」の詳細を表示',
      }),
    );

    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByLabelText('未読')).toBeInTheDocument();
  });
});
