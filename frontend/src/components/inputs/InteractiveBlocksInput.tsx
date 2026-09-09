import React, { useRef } from "react";

interface InteractiveBlocksInputProps {
  value: string;
  onChange: (value: string) => void;
  length?: number;
}

export function InteractiveBlocksInput({
  value = "",
  onChange,
  length = 7,
}: InteractiveBlocksInputProps) {
  const inputsRef = useRef<Array<HTMLInputElement | null>>([]);

  const values = value.padEnd(length, "").slice(0, length).split("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>, index: number) => {
    const val = e.target.value.replace(/\D/g, "");
    if (!val) return;

    const newValues = [...values];
    newValues[index] = val[0];
    onChange(newValues.join(""));

    if (index < length - 1) {
      inputsRef.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>, index: number) => {
    if (e.key === "Backspace") {
      e.preventDefault();
      const newValues = [...values];
      newValues[index] = "";
      onChange(newValues.join(""));

      if (index > 0) {
        inputsRef.current[index - 1]?.focus();
      }
    }
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData("Text").replace(/\D/g, "").slice(0, length);
    if (!pasted) return;
    onChange(pasted);

    // Focus last block or end of pasted digits
    const focusIndex = Math.min(pasted.length, length - 1);
    inputsRef.current[focusIndex]?.focus();
  };

  return (
    <div className="flex gap-2">
      {Array.from({ length }).map((_, i) => (
        <input
          key={i}
          ref={(el) => (inputsRef.current[i] = el)}
          value={values[i] || ""}
          onChange={(e) => handleChange(e, i)}
          onKeyDown={(e) => handleKeyDown(e, i)}
          onPaste={handlePaste}
          maxLength={1}
          type="text"
          inputMode="numeric"
          className="
            h-12 w-10
            rounded border border-input
            bg-card
            text-center text-xl font-bold
            transition-colors
            focus:border-primary
            focus:outline-none
            focus:ring-2
            focus:ring-ring
            focus:ring-offset-1
          "
        />
      ))}
    </div>
  );
}
