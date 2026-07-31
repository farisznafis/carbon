import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AdminMasterPasswordResetDialog } from './AdminMasterPasswordResetDialog';

describe('AdminMasterPasswordResetDialog', () => {
  it('shows the master account and submits after confirmation', async () => {
    const onConfirm = vi.fn();
    const user = userEvent.setup();

    render(
      <AdminMasterPasswordResetDialog
        open
        masterName="山田 太郎"
        masterEmail="master@example.com"
        isSubmitting={false}
        onOpenChange={vi.fn()}
        onConfirm={onConfirm}
      />,
    );

    expect(screen.getByText('山田 太郎')).toBeInTheDocument();
    expect(screen.getByText('master@example.com')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '送信する' }));

    expect(onConfirm).toHaveBeenCalledOnce();
  });
});
