import type { Site } from '@/api/site';
import {
  formatPersonName,
  formatSiteAddress,
} from '@/features/master/components/masterDashboardUtils';
import { ChevronDown, ChevronRight, Factory, RefreshCw, User } from 'lucide-react';

type MasterSiteCardProps = {
  site: Site;
  onOpen: (site: Site) => void;
};

export function MasterSiteCard({ site, onOpen }: MasterSiteCardProps) {
  const projects = Array.isArray(site.project) ? site.project : [];
  const visibleProjects = projects.slice(0, 2);

  return (
    <button
      type="button"
      onClick={() => onOpen(site)}
      className="group grid min-h-[66px] w-full grid-cols-[minmax(0,1fr)_56px] overflow-hidden rounded-[8px] border border-border bg-card text-left shadow-app-sm transition hover:border-primary/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 md:grid-cols-[minmax(0,1fr)_56px]"
    >
      <div className="grid min-w-0 gap-3 px-3 py-2.5 md:grid-cols-[minmax(0,1fr)_minmax(260px,0.75fr)] md:items-center md:px-4 xl:grid-cols-[minmax(0,1fr)_minmax(400px,0.72fr)]">
        <div className="min-w-0">
          <div className="flex min-w-0 items-center gap-2">
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-secondary text-primary">
              <Factory className="h-4 w-4" />
            </span>
            <div className="flex min-w-0 flex-wrap items-baseline gap-x-2 gap-y-0.5">
              <h3 className="truncate text-base font-bold leading-6 tracking-normal text-foreground">
                {site.siteName || '-'}
              </h3>
              <span className="truncate text-[9px] font-medium leading-4 text-muted-foreground">
                {formatSiteAddress(site)}
              </span>
            </div>
          </div>

          <div className="mt-2 flex flex-wrap gap-x-8 gap-y-1 text-[10px] font-bold leading-4 text-foreground">
            <span className="inline-flex items-center gap-1.5">
              <User className="h-3 w-3" />
              担当者: {formatPersonName(site.picLastName, site.picFirstName)}
            </span>
            <span className="inline-flex items-center gap-1.5">
              <RefreshCw className="h-3 w-3" />
              所在地: {site.postalCode || '-'}
            </span>
          </div>
        </div>

        <div className="min-w-0 text-[10px] font-bold leading-4 text-foreground">
          {visibleProjects.length > 0 ? (
            <>
              <div>
                {visibleProjects.map((project, index) => (
                  <p key={`${site.id}-${project}-${index}`} className="truncate">
                    {index === 0 ? 'プロジェクト: ' : ''}
                    {project}
                  </p>
                ))}
              </div>
              {projects.length > 2 && (
                <span className="mt-0.5 inline-flex items-center gap-1 text-[9px] font-medium leading-4 text-muted-foreground">
                  もっと見る
                  <ChevronDown className="h-3 w-3" />
                </span>
              )}
            </>
          ) : (
            <span className="text-muted-foreground">プロジェクト: -</span>
          )}
        </div>
      </div>

      <div className="flex h-full min-h-[66px] items-center justify-center bg-primary text-primary-foreground transition group-hover:bg-primary/90">
        <ChevronRight className="h-8 w-8" />
      </div>
    </button>
  );
}
