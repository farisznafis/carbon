import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ASSIGNABLE_OPERATOR_PERMISSION_OPTIONS } from './operatorPermissions';
import { OperatorPermissionSection } from './OperatorPermissionSection';

describe('OperatorPermissionSection', () => {
  it('shows the selected count and exposes global and group select-all controls', () => {
    render(
      <OperatorPermissionSection
        selectedPermissions={[]}
        onChange={vi.fn()}
        sectionNumber={3}
      />,
    );

    expect(screen.getByText('選択済み 0 / 17')).toBeInTheDocument();
    expect(screen.queryByText('運用者作成')).not.toBeInTheDocument();
    expect(screen.queryByText('運用者更新')).not.toBeInTheDocument();
    expect(
      screen.getByRole('checkbox', { name: 'すべての権限を選択' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('checkbox', {
        name: 'プロジェクト管理をすべて選択',
      }),
    ).toBeInTheDocument();
  });

  it('selects every known permission from the global control', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <OperatorPermissionSection
        selectedPermissions={[]}
        onChange={onChange}
        sectionNumber={3}
      />,
    );

    await user.click(
      screen.getByRole('checkbox', { name: 'すべての権限を選択' }),
    );

    expect(onChange).toHaveBeenCalledWith(
      ASSIGNABLE_OPERATOR_PERMISSION_OPTIONS.map((permission) => permission.code),
    );
  });

  it('marks a partially selected group as indeterminate and completes only that group', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <OperatorPermissionSection
        selectedPermissions={['PROJECT_READ', 'FUTURE_PERMISSION']}
        onChange={onChange}
        sectionNumber={3}
      />,
    );

    const projectSelectAll = screen.getByRole('checkbox', {
      name: 'プロジェクト管理をすべて選択',
    });
    expect(projectSelectAll).toHaveAttribute('data-state', 'indeterminate');

    await user.click(projectSelectAll);

    expect(onChange).toHaveBeenCalledWith([
      'PROJECT_READ',
      'FUTURE_PERMISSION',
      'PROJECT_CREATE',
      'PROJECT_UPDATE',
      'PROJECT_STATUS_UPDATE',
      'PROJECT_APPLICATION_OUTPUT',
    ]);
  });

  it('clears known permissions globally without dropping unknown backend values', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const selectedPermissions = [
      ...ASSIGNABLE_OPERATOR_PERMISSION_OPTIONS.map((permission) => permission.code),
      'FUTURE_PERMISSION',
    ];
    render(
      <OperatorPermissionSection
        selectedPermissions={selectedPermissions}
        onChange={onChange}
        sectionNumber={3}
      />,
    );

    await user.click(
      screen.getByRole('checkbox', { name: 'すべての権限を選択' }),
    );

    expect(onChange).toHaveBeenCalledWith(['FUTURE_PERMISSION']);
  });
});
