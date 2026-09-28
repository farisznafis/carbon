import * as React from 'react';
import { Input, type InputProps } from '@/components/ui/input';
import {
  formatGroupedNumberValue,
  normalizeGroupedNumberValue,
} from '@/lib/groupedNumber';

type GroupedNumberInputProps = Omit<
  InputProps,
  'inputMode' | 'onChange' | 'type' | 'value'
> & {
  value?: string;
  onChange: (value: string) => void;
  allowDecimal?: boolean;
};

/** Renders a numeric input with visual thousands separators and an unformatted form value. */
export const GroupedNumberInput = React.forwardRef<
  HTMLInputElement,
  GroupedNumberInputProps
>(
  (
    { allowDecimal = false, onChange, value = '', ...inputProps },
    ref,
  ) => (
    <Input
      {...inputProps}
      ref={ref}
      type="text"
      inputMode={allowDecimal ? 'decimal' : 'numeric'}
      value={formatGroupedNumberValue(value, allowDecimal)}
      onChange={(event) =>
        onChange(
          normalizeGroupedNumberValue(event.target.value, allowDecimal),
        )
      }
    />
  ),
);

GroupedNumberInput.displayName = 'GroupedNumberInput';
