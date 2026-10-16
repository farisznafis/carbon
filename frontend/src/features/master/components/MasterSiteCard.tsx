import type { Site } from '@/api/site';
import { RowActionButton } from '@/components/data-display/RowActionButton';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import {
  clickableCardClassName,
  formatPersonName,
  formatSiteAddress,
  stretchedActionClassName,
} from '@/features/master/components/masterDashboardUtils';
import { Building2, FolderKanban, MapPin, User } from 'lucide-react';

type MasterSiteCardProps = {
  site: Site;
  onOpen: (site: Site) => void;
};

/**
 * One site summary. Every project is shown as a chip, since a site joins only a
 * handful of projects and a もっと見る toggle hid the very thing being scanned.
 */
export function MasterSiteCard({
  site,
  onOpen,
}: MasterSiteCardProps) {
  const projects = Array.isArray(site.project) ? site.project : [];
  const address = formatSiteAddress(site);

  return (
    <article className={clickableCardClassName}>
      <div className="min-w-0 flex-1 space-y-3 px-5 py-4 md:px-6">
        <div className="flex min-w-0 items-center gap-3">
          <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-secondary text-primary">
            <Building2 className="h-5 w-5" aria-hidden="true" />
          </span>
          <h3 className="min-w-0 truncate text-foreground" title={site.siteName}>
            {site.siteName || '-'}
          </h3>
        </div>

        <dl className="grid gap-x-8 gap-y-1.5 text-sm leading-6 text-foreground sm:grid-cols-[minmax(0,auto)_minmax(0,1fr)]">
          <div className="flex min-w-0 items-center gap-1.5">
            <User className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden="true" />
            <dt className="shrink-0 text-muted-foreground">責任者</dt>
            <dd className="truncate font-bold">{formatPersonName(site.picLastName, site.picFirstName)}</dd>
          </div>
          <div className="flex min-w-0 items-center gap-1.5">
            <MapPin className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden="true" />
            <dt className="shrink-0 text-muted-foreground">所在地</dt>
            <dd className="truncate font-bold" title={address}>{address}</dd>
          </div>
        </dl>

        {/* Labelled like 責任者 / 所在地 above, so the chips read as projects rather than loose tags. */}
        <div className="flex min-w-0 items-start gap-1.5 text-sm leading-6">
          <FolderKanban className="mt-1 h-4 w-4 shrink-0 text-muted-foreground" aria-hidden="true" />
          <span id={`site-${site.siteId}-projects`} className="shrink-0 text-muted-foreground">
            参加プロジェクト
            {projects.length > 0 ? (
              <span className="tabular-nums">（{projects.length}件）</span>
            ) : null}
          </span>
          {projects.length > 0 ? (
            <ul
              className="flex min-w-0 flex-wrap gap-1.5 pt-0.5"
              aria-labelledby={`site-${site.siteId}-projects`}
            >
              {projects.map((project, index) => (
                <li
                  key={`${site.siteId}-${project}-${index}`}
                  className="max-w-full truncate rounded-md border border-border bg-muted/40 px-2 py-0.5 text-xs font-medium text-foreground sm:max-w-[22rem]"
                  title={project}
                >
                  {project}
                </li>
              ))}
            </ul>
          ) : (
            <StatusBadge tone="neutral">未参加</StatusBadge>
          )}
        </div>
      </div>

      <div className="flex items-center justify-end border-t border-border px-4 py-3 md:w-32 md:shrink-0 md:justify-center md:border-l md:border-t-0">
        <RowActionButton
          aria-label={`${site.siteName || '拠点'}の詳細`}
          className={stretchedActionClassName}
          onClick={() => onOpen(site)}
        />
      </div>
    </article>
  );
}
