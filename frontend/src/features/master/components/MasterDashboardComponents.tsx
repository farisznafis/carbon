import type { ReactNode } from 'react';
import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';
import { RowActionButton } from '@/components/data-display/RowActionButton';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import {
  dashboardEmptyStateClassName,
} from '@/components/layout/DashboardLayout';
import { MasterSiteCard } from '@/features/master/components/MasterSiteCard';
import {
  clickableCardClassName,
  formatPersonName,
  stretchedActionClassName,
} from '@/features/master/components/masterDashboardUtils';
import { cn } from '@/lib/utils';
import { Building2, Mail, User } from 'lucide-react';
import { ActionButton } from '@/components/actions';

/** Renders one dashboard section with shared heading and action spacing. */
export function DashboardSection({
  title,
  description,
  addLabel,
  onAdd,
  addDisabled,
  showAddAction = true,
  children,
}: {
  title: string;
  description?: string;
  addLabel: string;
  onAdd: () => void;
  addDisabled?: boolean;
  showAddAction?: boolean;
  children: ReactNode;
}) {
  return (
    <section>
      <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-foreground">{title}</h2>
          <p className="text-small mt-2 text-foreground">
            {description}
          </p>
        </div>
        {showAddAction && (
          <ActionButton
            kind="add"
            onClick={onAdd}
            disabled={addDisabled}
            className="self-start sm:self-auto"
          >
            {addLabel}
          </ActionButton>
        )}
      </div>
      {children}
    </section>
  );
}

/** Renders either a simple dashboard message or a structured onboarding empty state. */
export function DashboardEmptyState({
  title,
  description,
  action,
  children,
}: {
  title?: string;
  description?: string;
  action?: ReactNode;
  children?: ReactNode;
}) {
  return (
    <div
      className={cn(
        dashboardEmptyStateClassName,
        'flex flex-col items-center justify-center',
      )}
    >
      {title && <h3 className="text-base font-bold text-foreground">{title}</h3>}
      {description && (
        <p className={cn('max-w-2xl text-muted-foreground', title && 'mt-2')}>
          {description}
        </p>
      )}
      {children && (
        <div className={cn((title || description) && 'mt-2')}>{children}</div>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function SiteDashboardCard({
  site,
  onOpen,
}: {
  site: Site;
  onOpen: (site: Site) => void;
}) {
  return <MasterSiteCard site={site} onOpen={onOpen} />;
}

export function UserDashboardCard({
  user,
  onProfile,
}: {
  user: UserList;
  onProfile: (user: UserList) => void;
}) {
  const fullName = formatPersonName(user.lastName, user.firstName);
  const isDisconnectedFromSite =
    user.role !== 'master' && !user.siteId && !user.siteName;

  return (
    <article className={clickableCardClassName}>
      <div className="min-w-0 flex-1 px-5 py-4 md:px-6">
        <div className="flex flex-wrap items-center gap-3">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary text-primary-strong">
            <User className="h-5 w-5" />
          </span>
          <h3 className="text-foreground">
            {fullName}
          </h3>
          {user.role === 'master' && (
            <StatusBadge tone="info">
              マスター
            </StatusBadge>
          )}
        </div>

        <div className="pl-3 mt-2 grid gap-x-8 gap-y-2 text-sm font-medium leading-6 text-foreground xl:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)_minmax(0,1.2fr)]">
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <Mail className="h-4 w-4 shrink-0 text-muted-foreground" />
            <span className="shrink-0 font-normal text-muted-foreground">
              メールアドレス
            </span>
            <span className="min-w-0 truncate" title={user.email || undefined}>{user.email || '-'}</span>
          </span>
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <User className="h-4 w-4 shrink-0 text-muted-foreground" />
            <span className="shrink-0 font-normal text-muted-foreground">
              ユーザーネーム
            </span>
            <span className="truncate">{user.username || '-'}</span>
          </span>
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <Building2 className="h-4 w-4 shrink-0 text-muted-foreground" />
            <span className="shrink-0 font-normal text-muted-foreground">
              拠点
            </span>
            {user.role === 'master' ? (
              <StatusBadge tone="info">
                全て
              </StatusBadge>
            ) : isDisconnectedFromSite ? (
              <StatusBadge tone="danger">
                未設定
              </StatusBadge>
            ) : (
              <span className="min-w-0 truncate" title={user.siteName || undefined}>
                {user.siteName || user.siteId || '-'}
              </span>
            )}
          </span>
        </div>
      </div>

      <div className="flex items-center justify-end border-t border-border px-4 py-3 md:w-32 md:shrink-0 md:justify-center md:border-l md:border-t-0">
        <RowActionButton
          aria-label={`${fullName || '社員'}のプロファイル`}
          className={stretchedActionClassName}
          onClick={() => onProfile(user)}
        >
          プロファイル
        </RowActionButton>
      </div>
    </article>
  );
}
