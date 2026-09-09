import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { GroupedNumberInput } from './GroupedNumberInput';

/** Provides controlled state so tests cover the same value flow used by form controllers. */
function GroupedNumberInputHarness({
  allowDecimal = false,
}: Readonly<{ allowDecimal?: boolean }>) {
  const [value, setValue] = useState('');

  return (
    <>
      <GroupedNumberInput
        aria-label="金額"
        allowDecimal={allowDecimal}
        value={value}
        onChange={setValue}
      />
      <output aria-label="raw value">{value}</output>
    </>
  );
}

describe('GroupedNumberInput', () => {
  it('groups yen amounts visually while retaining an unformatted raw value', () => {
    render(<GroupedNumberInputHarness />);

    fireEvent.change(screen.getByLabelText('金額'), {
      target: { value: '1234567' },
    });

    expect(screen.getByLabelText('金額')).toHaveValue('1,234,567');
    expect(screen.getByLabelText('raw value')).toHaveTextContent('1234567');
  });

  it('preserves decimal input for yen unit prices', () => {
    render(<GroupedNumberInputHarness allowDecimal />);

    fireEvent.change(screen.getByLabelText('金額'), {
      target: { value: '12345.67' },
    });

    expect(screen.getByLabelText('金額')).toHaveValue('12,345.67');
    expect(screen.getByLabelText('raw value')).toHaveTextContent('12345.67');
  });
});
