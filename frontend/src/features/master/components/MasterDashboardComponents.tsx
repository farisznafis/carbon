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
import { Building2, Mail, Plus, User } from 'lucide-react';

const dashboardContainer = 'mx-auto w-full max-w-[1200px] px-4 sm:px-6 lg:px-0';

export function MasterDashboardHero({
  companyName,
  lastUpdated,
}: {
  companyName: string;
  lastUpdated: Date | null;
}) {
  return (
    <header className="relative overflow-hidden border-b border-border bg-background">
      <div className="relative flex min-h-[180px] flex-col items-center justify-center px-6 pt-10 text-center">
        <p className="text-small text-muted-foreground">
          {/* マスターダッシュボード */}
        </p>
        <h1 className="mt-2 max-w-5xl text-foreground">
          {companyName}
        </h1>
        <p className="text-small mt-2 inline-flex items-center gap-2 text-muted-foreground">
          サービスステータス:
          <span className="inline-flex items-center gap-1.5 text-foreground">
            <span className="h-3 w-3 rounded-full bg-green-500" />
            利用中
          </span>
        </p>
      </div>

      <div
        className={cn(
          'relative flex justify-end pb-8 text-sm font-medium leading-6 text-muted-foreground',
          dashboardContainer,
        )}
      >
        {/* 最終更新: {formatDashboardDateTime(lastUpdated)} */}
      </div>
    </header>
  );
}

export function MasterDashboardTabs() {
  const triggerClassName = cn(
    'relative h-14 rounded-none border-0 bg-transparent px-8 text-sm font-bold text-primary shadow-none transition-colors',
    'after:absolute after:bottom-0 after:left-0 after:right-0 after:h-[3px] after:bg-transparent',
    'hover:bg-secondary/50 hover:text-primary/90 hover:after:bg-primary/40',
    'data-[state=active]:bg-transparent data-[state=active]:text-primary data-[state=active]:shadow-none data-[state=active]:after:bg-primary',
  );

  return (
    <div className="bg-card pt-4">
      <TabsList className="mx-auto flex h-14 w-fit gap-4 rounded-none bg-transparent p-0 text-primary">
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
    <section className={cn('py-8', dashboardContainer)}>
      <div className="mb-5 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h2 className="text-foreground">
            <span className="text-primary">{title.slice(0, 1)}</span>
            {title.slice(1)}
          </h2>
          <p className="text-small mt-2 text-muted-foreground">
            {description}
          </p>
        </div>
        <Button
          type="button"
          size="sm"
          onClick={onAdd}
          disabled={addDisabled}
          className="self-start sm:self-auto"
        >
          <Plus className="h-4 w-4" />
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
  const isDisconnectedFromSite =
    user.role !== 'master' && !user.siteId && !user.siteName;

  return (
    <article className="flex flex-col overflow-hidden rounded-[8px] border border-border bg-card shadow-app-sm transition hover:border-primary/60 md:min-h-[112px] md:flex-row">
      <div className="min-w-0 flex-1 px-5 py-5 md:px-7">
        <div className="flex flex-wrap items-center gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary text-primary">
            <User className="h-5 w-5" />
          </span>
          <h3 className="text-foreground">
            {fullName}
          </h3>
          {user.role === 'master' && (
            <Badge className="border-transparent bg-blue-600 px-3 py-1 text-sm font-medium text-white hover:bg-blue-600">
              マスター
            </Badge>
          )}
        </div>

        <div className="mt-4 grid gap-x-8 gap-y-3 text-sm font-bold leading-6 text-foreground xl:grid-cols-[minmax(360px,1.7fr)_minmax(220px,1fr)_minmax(220px,1fr)]">
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <Mail className="h-4 w-4 shrink-0" />
            <span className="shrink-0 text-muted-foreground">
              メールアドレス
            </span>
            <span className="min-w-0 break-all">{user.email || '-'}</span>
          </span>
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <User className="h-4 w-4 shrink-0" />
            <span className="shrink-0 text-muted-foreground">
              ユーザ名
            </span>
            <span className="truncate">{user.username || '-'}</span>
          </span>
          <span className="inline-flex min-w-0 items-center gap-1.5">
            <Building2 className="h-4 w-4 shrink-0" />
            <span className="shrink-0 text-muted-foreground">
              拠点
            </span>
            {user.role === 'master' ? (
              <Badge className="border-transparent bg-blue-600 px-3 py-0.5 text-sm font-medium text-white hover:bg-blue-600">
                全て
              </Badge>
            ) : isDisconnectedFromSite ? (
              <Badge className="border-transparent bg-destructive px-3 py-0.5 text-sm font-medium text-destructive-foreground hover:bg-destructive">
                未設定
              </Badge>
            ) : (
              <span className="truncate">
                {user.siteName || user.siteId || '-'}
              </span>
            )}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-2 border-t border-border md:w-[128px] md:shrink-0 md:grid-cols-1 md:border-l md:border-t-0">
        <button
          type="button"
          onClick={() => onProfile(user)}
          className="bg-primary px-4 py-4 text-sm font-medium text-primary-foreground transition hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        >
          プロファイル
        </button>
        <button
          type="button"
          onClick={() => onEdit(user)}
          className="bg-card px-4 py-4 text-sm font-medium text-primary transition hover:bg-secondary/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
        >
          更新
        </button>
      </div>
    </article>
  );
}
