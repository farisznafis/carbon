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
  /** Steps already stored, marked so an operator can see what is left. */
  completedIds?: ReadonlyArray<number | string>;
}

export const PageDropdown: React.FC<PageDropdownProps> = ({
  steps,
  value,
  onChange,
  label = 'ページ',
  className = '',
  completedIds = [],
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
            {/* No number: which pages apply depends on the 申請区分, so the
                official sheet numbers have gaps that read as missing pages. */}
            {s.title}
            {completedIds.includes(s.id) && (
              <span
                className='ml-2 text-muted-foreground'
                aria-label='入力済み'
              >
                ✓
              </span>
            )}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  </div>
);
