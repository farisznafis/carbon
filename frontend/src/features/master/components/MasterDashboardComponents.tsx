import type { ReactNode } from 'react';
import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { TabsList, TabsTrigger } from '@/components/ui/tabs';
import { MasterSiteCard } from '@/features/master/components/MasterSiteCard';
import {
  formatDashboardDateTime,
  formatPersonName,
} from '@/features/master/components/masterDashboardUtils';
import { cn } from '@/lib/utils';
import { Building2, CalendarDays, Mail, Plus, User } from 'lucide-react';

const dashboardContainer = 'mx-auto w-full max-w-[1600px] px-4 sm:px-6 lg:px-0';

export function MasterDashboardHero({
  companyName,
  lastUpdated,
}: {
  companyName: string;
  lastUpdated: Date | null;
}) {
  return (
    <header className="relative overflow-hidden border-b border-border bg-background">
      <div className="relative flex min-h-[122px] flex-col items-center justify-center px-6 pt-8 text-center">
        <p className="text-xs font-medium leading-5 text-muted-foreground">
          マスターダッシュボード
        </p>
        <h1 className="mt-1 max-w-5xl text-2xl font-bold leading-8 tracking-normal text-foreground sm:text-[26px]">
          {companyName}
        </h1>
        <p className="mt-1 inline-flex items-center gap-1.5 text-[10px] font-medium leading-4 text-muted-foreground">
          サービスステータス:
          <span className="inline-flex items-center gap-1.5 text-foreground">
            <span className="h-2 w-2 rounded-full bg-green-500" />
            利用中
          </span>
        </p>
      </div>

      <div
        className={cn(
          'relative flex justify-end pb-3 text-[10px] font-medium leading-4 text-muted-foreground',
          dashboardContainer,
        )}
      >
        最終更新: {formatDashboardDateTime(lastUpdated)}
      </div>
    </header>
  );
}

export function MasterDashboardTabs() {
  const triggerClassName = cn(
    'relative h-10 rounded-none border-0 bg-transparent px-5 text-xs font-bold text-primary shadow-none transition-none',
    'after:absolute after:bottom-0 after:left-0 after:right-0 after:h-0.5 after:bg-transparent',
    'data-[state=active]:bg-transparent data-[state=active]:text-primary data-[state=active]:shadow-none data-[state=active]:after:bg-primary',
  );

  return (
    <div className="bg-card pt-4">
      <TabsList className="mx-auto flex h-10 w-fit gap-2 rounded-none bg-transparent p-0 text-primary">
        <TabsTrigger value="sites" className={triggerClassName}>
          拠点管理
        </TabsTrigger>
        <TabsTrigger value="users" className={triggerClassName}>
          ユーザ管理
        </TabsTrigger>
      </TabsList>
    </div>
  );
}

export function DashboardSection({
  title,
  description,
  addLabel,
  onAdd,
  addDisabled,
  children,
}: {
  title: string;
  description: string;
  addLabel: string;
  onAdd: () => void;
  addDisabled?: boolean;
  children: ReactNode;
}) {
  return (
    <section className={cn('py-3 sm:py-4', dashboardContainer)}>
      <div className="mb-2 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-[26px] font-bold leading-8 tracking-normal text-foreground">
            <span className="text-primary">{title.slice(0, 1)}</span>
            {title.slice(1)}
          </h2>
          <p className="mt-1 text-[10px] font-medium leading-4 text-muted-foreground">
            {description}
          </p>
        </div>
        <Button
          type="button"
          size="sm"
          onClick={onAdd}
          disabled={addDisabled}
          // className="h-7 min-w-[78px] self-start px-4 text-[10px] shadow-none sm:self-auto"
        >
          <Plus className="h-3 w-3" />
          {addLabel}
        </Button>
      </div>
      {children}
    </section>
  );
}

export function DashboardEmptyState({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-[8px] border border-dashed border-border bg-card px-6 py-8 text-center text-sm font-medium text-muted-foreground">
      {children}
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
  onEdit,
}: {
  user: UserList;
  onProfile: (user: UserList) => void;
  onEdit: (user: UserList) => void;
}) {
  const fullName = formatPersonName(user.lastName, user.firstName);

  return (
    <article className="flex flex-col overflow-hidden rounded-[8px] border border-border bg-card shadow-app-sm transition hover:border-primary/60 md:min-h-[72px] md:flex-row">
      <div className="min-w-0 flex-1 px-4 py-3 md:px-4">
        <div className="flex flex-wrap items-center gap-3">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-secondary text-primary">
            <User className="h-4 w-4" />
          </span>
          <h3 className="text-base font-bold leading-6 tracking-normal text-foreground">
            {fullName}
          </h3>
          {user.role === 'master' && (
            <Badge className="border-transparent bg-blue-600 px-2 py-0.5 text-[10px] font-medium text-white hover:bg-blue-600">
              マスター
            </Badge>
          )}
        </div>

        <div className="mt-3 grid gap-2 text-[11px] font-bold leading-5 text-foreground sm:grid-cols-3">
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <Mail className="h-3.5 w-3.5 shrink-0" />
            <span className="shrink-0 text-muted-foreground">
              メールアドレス:
            </span>
            <span className="truncate">{user.email || '-'}</span>
          </span>
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <CalendarDays className="h-3.5 w-3.5 shrink-0" />
            <span className="shrink-0 text-muted-foreground">
              ユーザ名:
            </span>
            <span className="truncate">{user.username || '-'}</span>
          </span>
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <Building2 className="h-3.5 w-3.5 shrink-0" />
            <span className="shrink-0 text-muted-foreground">
              拠点:
            </span>
            {user.role === 'master' ? (
              <Badge className="border-transparent bg-blue-600 px-2 py-0.5 text-[10px] font-medium text-white hover:bg-blue-600">
                全て
              </Badge>
            ) : (
              <span className="truncate">
                {user.siteName || user.siteId || '-'}
              </span>
            )}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-2 border-t border-border md:w-[112px] md:shrink-0 md:grid-cols-1 md:border-l md:border-t-0">
        <button
          type="button"
          onClick={() => onProfile(user)}
          className="bg-primary px-3 py-3 text-xs font-medium text-primary-foreground transition hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        >
          プロフィール
        </button>
        <button
          type="button"
          onClick={() => onEdit(user)}
          className="bg-card px-3 py-3 text-xs font-medium text-primary transition hover:bg-secondary/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        >
          更新
        </button>
      </div>
    </article>
  );
}
