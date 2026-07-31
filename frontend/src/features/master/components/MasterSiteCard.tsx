import type { Site } from '@/api/site';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import {
  formatPersonName,
  formatSiteAddress,
} from '@/features/master/components/masterDashboardUtils';
import {
  Building2,
  ChevronDown,
  ChevronRight,
  FolderKanban,
  MapPin,
  User,
} from 'lucide-react';

type MasterSiteCardProps = {
  site: Site;
  onOpen: (site: Site) => void;
};

export function MasterSiteCard({
  site,
  onOpen,
}: MasterSiteCardProps) {
  const projects = Array.isArray(site.project) ? site.project : [];
  const visibleProjects = projects.slice(0, 2);

  return (
    <article className="grid min-h-28 w-full overflow-hidden rounded-lg border border-border bg-card shadow-app-sm transition hover:border-primary/60 md:grid-cols-[minmax(0,1fr)_96px]">
      <Button
        type="button"
        variant="ghost"
        onClick={() => onOpen(site)}
        className="grid h-auto min-w-0 justify-normal gap-5 whitespace-normal rounded-none px-5 py-5 text-left shadow-none hover:bg-transparent md:grid-cols-[minmax(0,1fr)_minmax(280px,0.8fr)] md:items-start md:px-7 xl:grid-cols-[minmax(0,1fr)_minmax(430px,0.75fr)]"
      >
        <div className="min-w-0">
          <div className="flex min-w-0 items-center gap-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary text-primary">
              <Building2 className="h-5 w-5" />
            </span>
            <div className="flex min-w-0 flex-wrap items-baseline gap-x-3 gap-y-1">
              <h3 className="truncate text-foreground">
                {site.siteName || '-'}
              </h3>
              {/* ponytail: address shown in 所在地 row below, remove comment when design confirmed
              <span className="truncate text-sm font-medium leading-6 text-foreground">
                {formatSiteAddress(site)}
              </span>
              */}
            </div>
          </div>

          <div className="pl-3 mt-3 flex flex-col gap-2 text-sm font-bold leading-6 text-foreground">
            <span className="inline-flex items-center gap-1.5">
              <User className="h-4 w-4 text-muted-foreground" />
              <span className="text-foreground">責任者</span>
              <span>{formatPersonName(site.picLastName, site.picFirstName)}</span>
            </span>
            <span className="inline-flex min-w-0 items-center gap-1.5">
              <MapPin className="h-4 w-4 shrink-0 text-muted-foreground" />
              <span className="text-foreground">所在地</span>
              <span className="min-w-0 break-words">{formatSiteAddress(site)}</span>
            </span>
          </div>
        </div>

        <div className="min-w-0 pt-1 text-sm font-bold leading-6 text-foreground md:pt-0">
          <div className="flex min-w-0 items-start gap-2">
            <FolderKanban className="mt-1 h-4 w-4 shrink-0 text-muted-foreground" />
            <span className="shrink-0 text-foreground">プロジェクト</span>
            <div className="min-w-0 flex-1">
              {visibleProjects.length > 0 ? (
                <>
                  <div>
                    {visibleProjects.map((project, index) => (
                      <p
                        key={`${site.siteId}-${project}-${index}`}
                        className="truncate"
                      >
                        {project}
                      </p>
                    ))}
                  </div>
                  {projects.length > 2 && (
                    <span className="mt-1 inline-flex items-center gap-2 text-sm font-medium leading-6 text-foreground">
                      もっと見る
                      <ChevronDown className="h-4 w-4 text-muted-foreground" />
                    </span>
                  )}
                </>
              ) : (
                <StatusBadge tone="neutral">
                  未参加
                </StatusBadge>
              )}
            </div>
          </div>
        </div>
      </Button>

      <div className="flex min-h-28 items-center justify-center border-t border-border px-4 py-4 md:border-l md:border-t-0">
        <Button
          type="button"
          size="icon"
          aria-label={`${site.siteName || '拠点'}のダッシュボードを開く`}
          onClick={() => onOpen(site)}
          className="shrink-0"
        >
          <ChevronRight className="h-6 w-6" />
        </Button>
      </div>
    </article>
  );
}
