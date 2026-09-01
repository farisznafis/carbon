import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { OperatorPermissionFilter } from './OperatorPermissionFilter';
import {
  createAllOperatorPermissionFilterValue,
  matchesOperatorPermissionFilter,
  type OperatorPermissionFilterValue,
} from './operatorPermissionFilterModel';

/** Keeps the controlled filter value synchronized during interaction tests. */
function PermissionFilterHarness() {
  const [value, setValue] = useState<OperatorPermissionFilterValue>(
    createAllOperatorPermissionFilterValue,
  );
  return <OperatorPermissionFilter value={value} onChange={setValue} />;
}

describe('OperatorPermissionFilter', () => {
  it('starts with every permission selected as an unrestricted filter', async () => {
    const user = userEvent.setup();
    render(<PermissionFilterHarness />);

    expect(
      screen.getByRole('combobox', { name: '権限で絞り込む' }),
    ).toHaveTextContent('すべての権限');

    await user.click(
      screen.getByRole('combobox', { name: '権限で絞り込む' }),
    );

    expect(screen.getByText('選択済み 19 / 19')).toBeInTheDocument();
    expect(
      screen.getByRole('checkbox', { name: 'すべての権限を選択' }),
    ).toBeChecked();
    expect(
      screen.queryByRole('checkbox', { name: '権限なし' }),
    ).not.toBeInTheDocument();
  });

  it('keeps the popover open and supports global and group selection', async () => {
    const user = userEvent.setup();
    render(<PermissionFilterHarness />);

    const trigger = screen.getByRole('combobox', { name: '権限で絞り込む' });
    await user.click(trigger);
    await user.click(
      screen.getByRole('checkbox', { name: 'すべての権限を選択' }),
    );

    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    expect(trigger).toHaveTextContent('権限なし');
    expect(screen.getByText('選択済み 0 / 19')).toBeInTheDocument();

    await user.click(
      screen.getByRole('checkbox', {
        name: 'プロジェクト管理をすべて選択',
      }),
    );

    expect(trigger).toHaveTextContent('5件選択');
    expect(screen.getByText('選択済み 5 / 19')).toBeInTheDocument();
    expect(
      screen.getByRole('checkbox', { name: 'すべての権限を選択' }),
    ).toHaveAttribute('data-state', 'indeterminate');
  });

  it('requires all selected permissions and treats an empty selection as permissionless', () => {
    const selectedFilter: OperatorPermissionFilterValue = {
      permissionCodes: ['PROJECT_READ', 'PROJECT_UPDATE'],
    };

    expect(
      matchesOperatorPermissionFilter(
        ['PROJECT_READ', 'PROJECT_UPDATE'],
        selectedFilter,
      ),
    ).toBe(true);
    expect(
      matchesOperatorPermissionFilter(['PROJECT_READ'], selectedFilter),
    ).toBe(false);
    expect(matchesOperatorPermissionFilter([], selectedFilter)).toBe(false);
    expect(
      matchesOperatorPermissionFilter([], { permissionCodes: [] }),
    ).toBe(true);
    expect(
      matchesOperatorPermissionFilter(['PROJECT_READ'], { permissionCodes: [] }),
    ).toBe(false);
  });

  it('treats the complete default selection as no restriction', () => {
    const value = createAllOperatorPermissionFilterValue();

    expect(matchesOperatorPermissionFilter([], value)).toBe(true);
    expect(matchesOperatorPermissionFilter(['FUTURE_PERMISSION'], value)).toBe(
      true,
    );
  });
});
