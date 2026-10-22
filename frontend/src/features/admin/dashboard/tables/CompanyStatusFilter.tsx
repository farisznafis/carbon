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
  COMPANY_STATUS_GROUP_CONTENT,
  getCompanyFilterStatuses,
  getCompanyStatusGroup,
  getVisibleCompanyStatusGroups,
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
  return getCompanyFilterStatuses(status, statuses).reduce(
    (total, item) => total + (counts[item] ?? 0),
    0,
  );
}

type FilterControlProps = Readonly<{
  label: string;
  ariaLabel: string;
  options: readonly FilterOption[];
  statuses: readonly CompanyStatus[];
  counts: Partial<Record<CompanyStatus, number>> | null;
  activeValue: CompanyStatusFilterValue;
  onChange: (value: CompanyStatusFilterValue) => void;
}>;

/**
 * Desktop: segmented controls with counts.
 * Tablet/mobile: select control to avoid horizontal overflow.
 */
function FilterControl({
  label,
  ariaLabel,
  options,
  statuses,
  counts,
  activeValue,
  onChange,
}: FilterControlProps) {
  return (
    <>
      {/* Desktop */}
      <div className="hidden items-center gap-2 lg:flex">
        <span className="text-small text-muted-foreground">{label}</span>

        <SegmentGroup aria-label={ariaLabel}>
          {options.map((option) => {
            const count = getStatusCount(option.value, statuses, counts);

            return (
              <SegmentButton
                key={option.value}
                tone={option.tone}
                active={activeValue === option.value}
                aria-label={
                  count === null ? option.label : `${option.label} ${count}件`
                }
                onClick={() => onChange(option.value)}
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
          {label}
        </span>

        <Select
          value={activeValue}
          onValueChange={(value) => onChange(value as CompanyStatusFilterValue)}
        >
          <SelectTrigger className="w-[180px]" aria-label={ariaLabel}>
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
    </>
  );
}

/**
 * Company status filter, in the same tones as the status badges.
 *
 * 区分（参加企業 / 企業申請）を選ぶと、ステータスの選択肢はその区分のものに絞られる。
 * 区分はホームのカードの件数と一致させるため、閲覧者が両方の区分を参照できる場合のみ表示する。
 */
export function CompanyStatusFilter({
  statuses,
  counts,
  activeStatus,
  onStatusChange,
}: CompanyStatusFilterProps) {
  const groups = getVisibleCompanyStatusGroups(statuses);
  const activeGroup = groups.length > 0 ? getCompanyStatusGroup(activeStatus) : 'all';

  const groupOptions: FilterOption[] = [
    { value: 'all', label: ALL_COMPANIES_CONTENT.label, tone: 'neutral' },
    ...groups.map((group) => ({
      value: group,
      label: COMPANY_STATUS_GROUP_CONTENT[group].label,
      tone: 'neutral' as const,
    })),
  ];
  // 区分を選択中は、その区分内の「すべて」が区分そのものを指す
  const statusOptions: FilterOption[] = [
    { value: activeGroup, label: ALL_COMPANIES_CONTENT.label, tone: 'neutral' },
    ...getCompanyFilterStatuses(activeGroup, statuses).map((status) => ({
      value: status,
      label: COMPANY_STATUS_CONTENT[status].label,
      tone: COMPANY_STATUS_CONTENT[status].tone,
    })),
  ];

  return (
    <div className="flex shrink-0 flex-wrap items-center gap-x-6 gap-y-3">
      {groups.length > 0 && (
        <FilterControl
          label="区分"
          ariaLabel="企業の区分で絞り込む"
          options={groupOptions}
          statuses={statuses}
          counts={counts}
          activeValue={activeGroup}
          onChange={onStatusChange}
        />
      )}
      <FilterControl
        label="ステータス"
        ariaLabel="企業ステータスで絞り込む"
        options={statusOptions}
        statuses={statuses}
        counts={counts}
        activeValue={activeStatus}
        onChange={onStatusChange}
      />
    </div>
  );
}
