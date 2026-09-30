import { CheckCircle2, ClipboardCheck, FolderOpen } from 'lucide-react';
import { cn } from '@/lib/utils';
import type {
  MembershipStatusFilter,
  ProjectMembershipStatus,
} from '@/features/user/project-registration/projectMembershipStatus';

type MembershipStatusOverviewProps = Readonly<{
  counts: Record<ProjectMembershipStatus, number>;
  activeStatus: MembershipStatusFilter;
  onStatusChange: (status: ProjectMembershipStatus) => void;
}>;

const MEMBERSHIP_STATUS_PRESENTATION = {
  available: {
    label: '未入会',
    description: '入会手続きを開始できます',
    icon: FolderOpen,
    activeClassName: 'border-primary bg-primary/5',
    iconClassName: 'bg-primary/10 text-primary',
  },
  pending: {
    label: '申請中',
    description: '管理者による確認を待っています',
    icon: ClipboardCheck,
    activeClassName: 'border-warning-border bg-warning-subtle',
    iconClassName: 'bg-warning-subtle text-warning-strong',
  },
  joined: {
    label: '入会中',
    description: '承認済みのプロジェクトです',
    icon: CheckCircle2,
    activeClassName: 'border-success-border bg-success-subtle',
    iconClassName: 'bg-success-subtle text-success-strong',
  },
} as const;

/** Presents the three membership states as accessible filter controls. */
export function MembershipStatusOverview({
  counts,
  activeStatus,
  onStatusChange,
}: MembershipStatusOverviewProps) {
  return (
    <section
      aria-label="入会ステータス概要"
      className="grid grid-flow-dense gap-3 sm:grid-cols-3"
      data-membership-reveal
    >
      {(Object.keys(MEMBERSHIP_STATUS_PRESENTATION) as ProjectMembershipStatus[]).map(
        (status) => {
          const presentation = MEMBERSHIP_STATUS_PRESENTATION[status];
          const Icon = presentation.icon;
          const isActive = activeStatus === status;

          return (
            <button
              key={status}
              type="button"
              aria-pressed={isActive}
              onClick={() => onStatusChange(status)}
              className={cn(
                'group flex min-h-28 items-center gap-4 rounded-2xl border border-border bg-card p-5 text-left shadow-app-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                isActive && presentation.activeClassName,
              )}
            >
              <span
                className={cn(
                  'flex h-11 w-11 shrink-0 items-center justify-center rounded-xl transition-transform duration-300 group-hover:scale-105',
                  presentation.iconClassName,
                )}
              >
                <Icon className="h-5 w-5" aria-hidden="true" />
              </span>

              <span className="min-w-0 flex-1">
                <span className="flex items-baseline justify-between gap-3">
                  <span className="font-semibold text-foreground">
                    {presentation.label}
                  </span>
                  <strong className="text-2xl tabular-nums text-foreground">
                    {counts[status]}
                  </strong>
                </span>
                <span className="mt-1 block text-small leading-5 text-muted-foreground">
                  {presentation.description}
                </span>
              </span>
            </button>
          );
        },
      )}
    </section>
  );
}
