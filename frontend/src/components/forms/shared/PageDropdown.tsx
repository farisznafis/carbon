import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@/components/ui/select';

// Accepts readonly or mutable arrays
export interface PageDropdownStep {
  id: number;
  title: string;
}

export interface PageDropdownProps {
  steps: ReadonlyArray<PageDropdownStep>; // <-- Accepts readonly array!
  value: number | string;
  onChange: (id: number | string) => void;
  label?: string;
  className?: string;
}

export const PageDropdown: React.FC<PageDropdownProps> = ({
  steps,
  value,
  onChange,
  label = 'ページ',
  className = '',
}) => (
  <div className={className}>
    {/* adjust label gap from form */}
    <div className='mb-2 ml-2 block text-small'>
      {label && <label>{label}</label>}
    </div>
    <Select
      value={String(value)}
      onValueChange={(v) => onChange(Number(v))}
    >
      <SelectTrigger>
        <SelectValue />
      </SelectTrigger>
      <SelectContent>
        {steps.map((s) => (
          <SelectItem
            key={s.id}
            value={String(s.id)}
          >
            {s.id}. {s.title}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  </div>
);
