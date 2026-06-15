// FileSelectButton.tsx
import { Upload } from 'lucide-react';

type Props = {
  id: string;
  value?: File | null;
  onChange: (file: File | null) => void;
  label?: string;
  className?: string;
};

export function FileSelectButton({
  id,
  value,
  onChange,
  label = 'ファイルを選択',
  className = '',
}: Readonly<Props>) {
  return (
    <div className={`space-y-2 ${className}`}>
      <label
        htmlFor={id}
        className="cursor-pointer text-sm text-gray-800 bg-gray-200 hover:bg-gray-300 py-1 px-3 rounded-md shadow-sm flex items-center w-fit"
      >
        <Upload className="mr-2 h-4 w-4" />
        {label}
      </label>
      <input
        id={id}
        type="file"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0] ?? null;
          onChange(file);
        }}
      />
      {value && (
        <span className="text-sm text-gray-700">{value.name}</span>
      )}
    </div>
  );
}