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
  /**
   * Steps already saved. Marked with a check so an operator returning to a
   * part-filled plan can see how far they got. Omit for the plain list.
   */
  completedIds?: ReadonlyArray<number | string>;
}

export const PageDropdown: React.FC<PageDropdownProps> = ({
  steps,
  value,
  onChange,
  label = 'ページ',
  className = '',
  completedIds,
}) => (
  <div className={className}>
    {/* adjust label gap from form */}
    <div className='block text-sm font-medium mb-2 ml-2'>
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
            {completedIds?.some((id) => String(id) === String(s.id)) && (
              <span
                aria-label='入力済み'
                className='mr-1 text-green-600'
              >
                ✓
              </span>
            )}
            {s.id}. {s.title}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  </div>
);
