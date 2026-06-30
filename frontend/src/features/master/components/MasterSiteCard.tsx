import type { Site } from '@/api/site';
import { Badge } from '@/components/ui/badge';
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

export function MasterSiteCard({ site, onOpen }: MasterSiteCardProps) {
  const projects = Array.isArray(site.project) ? site.project : [];
  const visibleProjects = projects.slice(0, 2);

  return (
    <button
      type="button"
      onClick={() => onOpen(site)}
      className="group grid min-h-[112px] w-full grid-cols-[minmax(0,1fr)_80px] overflow-hidden rounded-[8px] border border-border bg-card text-left shadow-app-sm transition hover:border-primary/60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 md:grid-cols-[minmax(0,1fr)_96px]"
    >
      <div className="grid min-w-0 gap-5 px-5 py-5 md:grid-cols-[minmax(0,1fr)_minmax(280px,0.8fr)] md:items-start md:px-7 xl:grid-cols-[minmax(0,1fr)_minmax(430px,0.75fr)]">
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
              <span className="truncate text-sm font-medium leading-6 text-muted-foreground">
                {formatSiteAddress(site)}
              </span>
              */}
            </div>
          </div>

          <div className="mt-4 flex flex-col gap-2 text-sm font-bold leading-6 text-foreground">
            <span className="inline-flex items-center gap-1.5">
              <User className="h-4 w-4" />
              <span className="text-muted-foreground">担当者</span>
              <span>{formatPersonName(site.picLastName, site.picFirstName)}</span>
            </span>
            <span className="inline-flex min-w-0 items-start gap-1.5">
              <MapPin className="h-4 w-4 shrink-0" />
              <span className="text-muted-foreground">所在地</span>
              <span className="min-w-0 break-words">{formatSiteAddress(site)}</span>
            </span>
          </div>
        </div>

        <div className="min-w-0 pt-1 text-sm font-bold leading-6 text-foreground md:pt-0">
          <div className="flex min-w-0 items-start gap-2">
            <FolderKanban className="mt-1 h-4 w-4 shrink-0" />
            <span className="shrink-0 text-muted-foreground">プロジェクト</span>
            <div className="min-w-0 flex-1">
              {visibleProjects.length > 0 ? (
                <>
                  <div>
                    {visibleProjects.map((project, index) => (
                      <p
                        key={`${site.id}-${project}-${index}`}
                        className="truncate"
                      >
                        {project}
                      </p>
                    ))}
                  </div>
                  {projects.length > 2 && (
                    <span className="mt-1 inline-flex items-center gap-2 text-sm font-medium leading-6 text-muted-foreground">
                      もっと見る
                      <ChevronDown className="h-4 w-4" />
                    </span>
                  )}
                </>
              ) : (
                <Badge className="border-transparent bg-muted px-3 py-0.5 text-sm font-medium text-muted-foreground hover:bg-muted">
                  未参加
                </Badge>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="flex h-full min-h-[112px] items-center justify-center">
        <button className="flex h-12 w-12 items-center justify-center rounded-full bg-primary text-primary-foreground transition hover:bg-[#EEF2CE] hover:text-primary">
          <ChevronRight className="h-6 w-6" />
        </button>
      </div>
    </button>
  );
}
