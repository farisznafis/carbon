import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { StatusBadgeTone } from '@/components/data-display/StatusBadge';
import {
  SegmentButton,
  SegmentGroup,
  TabCount,
} from '@/components/navigation/PageTabs';
import {
  MEMBERSHIP_STATUS_PRESENTATION,
  type MembershipStatusFilter as MembershipStatusFilterValue,
  type ProjectMembershipStatus,
} from '@/features/user/project-registration/projectMembershipStatus';

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
  tone: StatusBadgeTone;
}>;

/** すべて plus each membership state, in the same tones as the project cards. */
const FILTER_OPTIONS: FilterOption[] = [
  { value: 'all', label: 'すべて', tone: 'neutral' },
  { value: 'available', ...MEMBERSHIP_STATUS_PRESENTATION.available },
  { value: 'pending', ...MEMBERSHIP_STATUS_PRESENTATION.pending },
  { value: 'joined', ...MEMBERSHIP_STATUS_PRESENTATION.joined },
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

        <SegmentGroup aria-label="入会ステータスで絞り込む">
          {FILTER_OPTIONS.map((option) => {
            const count = getStatusCount(
              option.value,
              counts,
            );

            return (
              <SegmentButton
                key={option.value}
                tone={option.tone}
                active={activeStatus === option.value}
                aria-label={`${option.label} ${count}件`}
                disabled={disabled}
                onClick={() =>
                  onStatusChange(option.value)
                }
              >
                <span>{option.label}</span>
                <span aria-hidden="true">
                  <TabCount count={count} />
                </span>
              </SegmentButton>
            );
          })}
        </SegmentGroup>
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