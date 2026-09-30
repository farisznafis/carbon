import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type {
  MembershipStatusFilter as MembershipStatusFilterValue,
  ProjectMembershipStatus,
} from '@/features/user/project-registration/projectMembershipStatus';
import { cn } from '@/lib/utils';

type MembershipStatusFilterProps = Readonly<{
  counts: Record<ProjectMembershipStatus, number>;
  activeStatus: MembershipStatusFilterValue;
  onStatusChange: (
    status: MembershipStatusFilterValue,
  ) => void;
  disabled?: boolean;
}>;

type FilterOption = Readonly<{
  value: MembershipStatusFilterValue;
  label: string;
  activeClassName: string;
}>;

const FILTER_OPTIONS: FilterOption[] = [
  {
    value: 'all',
    label: 'すべて',
    activeClassName:
      'border-border bg-secondary text-secondary-foreground',
  },
  {
    value: 'available',
    label: '未入会',
    activeClassName:
      'border-primary/40 bg-accent text-accent-foreground',
  },
  {
    value: 'pending',
    label: '申請中',
    activeClassName:
      'border-warning-border bg-warning-subtle text-[hsl(var(--warning-800))]',
  },
  {
    value: 'joined',
    label: '入会中',
    activeClassName:
      'border-success-border bg-success-subtle text-[hsl(var(--success-800))]',
  },
];

function getStatusCount(
  status: MembershipStatusFilterValue,
  counts: Record<ProjectMembershipStatus, number>,
) {
  if (status === 'all') {
    return (
      counts.available +
      counts.pending +
      counts.joined
    );
  }

  return counts[status];
}

/**
 * Compact membership status filter.
 *
 * Desktop:
 * segmented controls with status counts.
 *
 * Tablet/mobile:
 * select control to avoid horizontal overflow.
 */
export function MembershipStatusFilter({
  counts,
  activeStatus,
  onStatusChange,
  disabled = false,
}: MembershipStatusFilterProps) {
  return (
    <div className="shrink-0">
      {/* Desktop */}
      <div className="hidden items-center gap-2 lg:flex">
        <span className="text-small text-muted-foreground">
          ステータス
        </span>

        <div
          role="group"
          aria-label="入会ステータスで絞り込む"
          className="flex items-center gap-1 rounded-full border border-border bg-card p-1 shadow-app-sm"
        >
          {FILTER_OPTIONS.map((option) => {
            const count = getStatusCount(
              option.value,
              counts,
            );

            const isActive =
              activeStatus === option.value;

            return (
              <button
                key={option.value}
                type="button"
                aria-pressed={isActive}
                aria-label={`${option.label} ${count}件`}
                disabled={disabled}
                onClick={() =>
                  onStatusChange(option.value)
                }
                className={cn(
                  'inline-flex h-9 items-center gap-2 rounded-full border border-transparent px-3 text-sm font-medium text-muted-foreground transition-colors',
                  'hover:bg-muted hover:text-foreground',
                  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                  'disabled:pointer-events-none disabled:opacity-50',
                  isActive &&
                    option.activeClassName,
                )}
              >
                <span>{option.label}</span>

                <span
                  aria-hidden="true"
                  className={cn(
                    'min-w-5 rounded-full bg-background/80 px-1.5 py-0.5 text-center text-xs tabular-nums',
                    isActive
                      ? 'text-current'
                      : 'text-muted-foreground',
                  )}
                >
                  {count}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Tablet / mobile */}
      <div className="flex items-center gap-2 lg:hidden">
        <span className="shrink-0 text-small text-muted-foreground">
          ステータス
        </span>

        <Select
          value={activeStatus}
          disabled={disabled}
          onValueChange={(value) =>
            onStatusChange(
              value as MembershipStatusFilterValue,
            )
          }
        >
          <SelectTrigger
            className="w-[180px]"
            aria-label="入会ステータスで絞り込む"
          >
            <SelectValue />
          </SelectTrigger>

          <SelectContent>
            {FILTER_OPTIONS.map((option) => {
              const count = getStatusCount(
                option.value,
                counts,
              );

              return (
                <SelectItem
                  key={option.value}
                  value={option.value}
                >
                  {option.label}（{count}）
                </SelectItem>
              );
            })}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}