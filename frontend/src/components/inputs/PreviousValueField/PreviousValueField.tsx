import React, { useState, useRef, useEffect } from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { FormLabel } from "@/components/ui/form";

/**
 * PreviousValueField
 *
 * A reusable form field component for displaying an input field alongside
 * a previous month's value, with a button to copy the previous value into the input.
 *
 * Props:
 * @param label - Field label (e.g., "電力使用量（kWh）")
 * @param value - Current value of the input (controlled)
 * @param onChange - Callback when input value changes
 * @param previousValue - Optional previous month's value to show/copy
 * @param unit - Optional unit string (e.g., "kWh") to append to previous value
 *
 * Behavior:
 * - Input starts empty.
 * - Previous value is read-only.
 * - Clicking the button copies previous value into the input, focuses the input,
 *   and shows a confirmation state.
 * - If the user edits the input after copying, the button becomes clickable again.
 */
interface PreviousValueFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  previousValue?: string;
  unit?: string;
  placeholder?: string; 
  disabled?: boolean;
}

export const PreviousValueField: React.FC<PreviousValueFieldProps> = ({
  label,
  value,
  onChange,
  previousValue,
  unit,
  placeholder,
  disabled,
}) => {
  const [copied, setCopied] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleCopyPrevious = () => {
    if (previousValue) {
      onChange(previousValue);
      setCopied(true);
      inputRef.current?.focus();
    }
  };

  // Reset the copied state if the user edits the input to something different than previousValue
  useEffect(() => {
    if (copied && value !== previousValue) {
      setCopied(false);
    }
  }, [value, previousValue, copied]);

  return (
    <div className="flex flex-col space-y-1">
      <FormLabel>{label}</FormLabel>
      <Input
        ref={inputRef}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
      />
      {previousValue && (
        <div className="mt-1 flex items-center gap-2 text-sm text-foreground">
          <span>
            以前の値：{previousValue}
            {unit ? ` ${unit}` : ""}
          </span>
          <Button
            size="sm"
            variant="outline"
            onClick={handleCopyPrevious}
            disabled={disabled || copied}
          >
            {copied ? "✓ 使用しました" : "以前の値を使用"}
          </Button>
        </div>
      )}
    </div>
  );
};
