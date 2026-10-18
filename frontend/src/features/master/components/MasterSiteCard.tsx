import { useState } from 'react';
import type { Site } from '@/api/site';
import { RowActionButton } from '@/components/data-display/RowActionButton';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { Button } from '@/components/ui/button';
import {
  formatPersonName,
  formatSiteAddress,
} from '@/features/master/components/masterDashboardUtils';
import {
  Building2,
  ChevronDown,
  FolderKanban,
  MapPin,
  User,
} from 'lucide-react';

type MasterSiteCardProps = {
  site: Site;
  onOpen: (site: Site) => void;
};

/** Displays one site summary with separate project expansion and navigation actions. */
export function MasterSiteCard({
  site,
  onOpen,
}: MasterSiteCardProps) {
  const projects = Array.isArray(site.project) ? site.project : [];
  const [areProjectsExpanded, setAreProjectsExpanded] = useState(false);
  const visibleProjects = areProjectsExpanded ? projects : projects.slice(0, 2);

  return (
    <article className="grid w-full overflow-hidden rounded-lg border border-border bg-card shadow-app-sm md:grid-cols-[minmax(0,1fr)_152px]">
      <div className="grid min-w-0 gap-4 px-5 py-4 text-left md:grid-cols-[minmax(0,1fr)_minmax(280px,0.8fr)] md:items-start md:px-6 xl:grid-cols-[minmax(0,1fr)_minmax(430px,0.75fr)]">
        <div className="min-w-0">
          <div className="flex min-w-0 items-center gap-3">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary text-primary">
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

          <div className="pl-3 mt-2 flex flex-col gap-1.5 text-sm font-medium leading-6 text-foreground">
            <span className="inline-flex items-center gap-1.5">
              <User className="h-4 w-4 text-muted-foreground" />
              <span className="font-normal text-muted-foreground">責任者</span>
              <span>{formatPersonName(site.picLastName, site.picFirstName)}</span>
            </span>
            <span className="inline-flex min-w-0 items-center gap-1.5">
              <MapPin className="h-4 w-4 shrink-0 text-muted-foreground" />
              <span className="font-normal text-muted-foreground">所在地</span>
              <span className="min-w-0 break-words">{formatSiteAddress(site)}</span>
            </span>
          </div>
        </div>

        <div className="min-w-0 pt-1 text-sm font-medium leading-6 text-foreground md:pt-0">
          <div className="flex min-w-0 items-start gap-2">
            <FolderKanban className="mt-1 h-4 w-4 shrink-0 text-muted-foreground" />
            <span className="shrink-0 font-normal text-muted-foreground">プロジェクト</span>
            <div className="min-w-0 flex-1">
              {visibleProjects.length > 0 ? (
                <>
                  <div>
                    {visibleProjects.map((project, index) => (
                      <p
                        key={`${site.siteId}-${project}-${index}`}
                        className="truncate font-medium text-foreground"
                      >
                        {project}
                      </p>
                    ))}
                  </div>
                  {projects.length > 2 && (
                    <Button
                      type="button"
                      variant="link"
                      aria-expanded={areProjectsExpanded}
                      onClick={() =>
                        setAreProjectsExpanded((isExpanded) => !isExpanded)
                      }
                      className="mt-1 h-auto justify-start gap-2 p-0 text-sm font-medium leading-6 text-foreground"
                    >
                      {areProjectsExpanded ? '閉じる' : 'もっと見る'}
                      <ChevronDown
                        className={
                          areProjectsExpanded
                            ? 'rotate-180 text-muted-foreground transition-transform'
                            : 'text-muted-foreground transition-transform'
                        }
                      />
                    </Button>
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
      </div>

      <div className="flex items-center justify-end border-t border-border px-4 py-3 md:justify-center md:border-l md:border-t-0">
        <RowActionButton
          aria-label={`${site.siteName || '拠点'}の詳細`}
          onClick={() => onOpen(site)}
        />
      </div>
    </article>
  );
}
