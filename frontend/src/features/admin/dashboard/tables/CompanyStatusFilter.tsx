import type { CompanyStatus } from '@/api/admin';
import type { StatusBadgeTone } from '@/components/data-display/StatusBadge';
import {
  SegmentButton,
  SegmentGroup,
  TabCount,
} from '@/components/navigation/PageTabs';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  ALL_COMPANIES_CONTENT,
  COMPANY_STATUS_CONTENT,
  type CompanyStatusFilter as CompanyStatusFilterValue,
} from '@/features/admin/company-detail/companyStatusContent';

type CompanyStatusFilterProps = Readonly<{
  /** Statuses the viewer has permission to see, in display order. */
  statuses: readonly CompanyStatus[];
  /** API totals per status; null while they are loading. */
  counts: Partial<Record<CompanyStatus, number>> | null;
  activeStatus: CompanyStatusFilterValue;
  onStatusChange: (status: CompanyStatusFilterValue) => void;
}>;

type FilterOption = Readonly<{
  value: CompanyStatusFilterValue;
  label: string;
  tone: StatusBadgeTone;
}>;

function getStatusCount(
  status: CompanyStatusFilterValue,
  statuses: readonly CompanyStatus[],
  counts: Partial<Record<CompanyStatus, number>> | null,
) {
  if (!counts) return null;
  if (status === 'all') {
    return statuses.reduce((total, item) => total + (counts[item] ?? 0), 0);
  }
  return counts[status] ?? 0;
}

/**
 * Company status filter, in the same tones as the status badges.
 *
 * Desktop: segmented controls with status counts.
 * Tablet/mobile: select control to avoid horizontal overflow.
 */
export function CompanyStatusFilter({
  statuses,
  counts,
  activeStatus,
  onStatusChange,
}: CompanyStatusFilterProps) {
  const options: FilterOption[] = [
    { value: 'all', label: ALL_COMPANIES_CONTENT.label, tone: 'neutral' },
    ...statuses.map((status) => ({
      value: status,
      label: COMPANY_STATUS_CONTENT[status].label,
      tone: COMPANY_STATUS_CONTENT[status].tone,
    })),
  ];

  return (
    <div className="shrink-0">
      {/* Desktop */}
      <div className="hidden items-center gap-2 lg:flex">
        <span className="text-small text-muted-foreground">ステータス</span>

        <SegmentGroup aria-label="企業ステータスで絞り込む">
          {options.map((option) => {
            const count = getStatusCount(option.value, statuses, counts);

            return (
              <SegmentButton
                key={option.value}
                tone={option.tone}
                active={activeStatus === option.value}
                aria-label={
                  count === null ? option.label : `${option.label} ${count}件`
                }
                onClick={() => onStatusChange(option.value)}
              >
                <span>{option.label}</span>
                {count !== null && (
                  <span aria-hidden="true">
                    <TabCount count={count} />
                  </span>
                )}
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
          onValueChange={(value) =>
            onStatusChange(value as CompanyStatusFilterValue)
          }
        >
          <SelectTrigger className="w-[180px]" aria-label="企業ステータスで絞り込む">
            <SelectValue />
          </SelectTrigger>

          <SelectContent>
            {options.map((option) => {
              const count = getStatusCount(option.value, statuses, counts);

              return (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                  {count !== null && `（${count}）`}
                </SelectItem>
              );
            })}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
