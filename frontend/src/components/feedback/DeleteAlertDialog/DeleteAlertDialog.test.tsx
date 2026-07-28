// DeleteAlertDialog.test.tsx (Vitest)
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DeleteAlertDialog } from './DeleteAlertDialog';

describe('DeleteAlertDialog', () => {
  it('renders trigger button', () => {
    render(
      <DeleteAlertDialog
        open={false}
        onOpenChange={() => {}}
        onDelete={() => {}}
      />
    );
    expect(screen.getByText('削除')).toBeInTheDocument();
  });

  it('shows dialog content when open', () => {
    render(
      <DeleteAlertDialog
        open
        onOpenChange={() => {}}
        onDelete={() => {}}
        label="テスト"
      />
    );
    expect(screen.getByText('テストを削除しますか？')).toBeInTheDocument();
  });

  it('calls onDelete when confirm button clicked', () => {
    const onDelete = vi.fn();
    render(
      <DeleteAlertDialog
        open
        onOpenChange={() => {}}
        onDelete={onDelete}
      />
    );
    fireEvent.click(screen.getByText('削除する'));
    expect(onDelete).toHaveBeenCalledTimes(1);
  });
});