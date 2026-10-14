import type { ReductionResultItem } from '@/api/batch';
import type { Site, SiteProject } from '@/api/site';
import type { UserList } from '@/api/users';

/** One 拠点 × プロジェクト pair the company is approved for and the site takes part in. */
export type TargetPair = {
  siteId: string;
  siteName: string;
  projectId: string;
  projectName: string;
};

export type SiteProjects = { siteId: string; projects: SiteProject[] };

export type MonthlyEmission = { month: string; empj: number; siteCount: number };

export type ActionItem = {
  kind: 'pending' | 'unsubmitted' | 'noSite' | 'unread';
  key: string;
  text: string;
  siteId?: string;
  userId?: string;
};

export type SiteRow = {
  site: Site;
  projectCount: number;
  userCount: number;
  lastSubmittedAt: string | null;
  thisMonth: 'submitted' | 'unsubmitted' | 'none';
};

const jstMonthFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Tokyo',
  year: 'numeric',
  month: '2-digit',
});

/** "YYYY-MM" of a date in JST, or null when the value cannot be parsed. */
export function toJstMonth(value: string | Date | null | undefined): string | null {
  if (!value) return null;
  // Date-only values are already local dates; no time zone may shift them.
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) return value.slice(0, 7);
  // The API sends nanosecond fractions some browsers cannot parse.
  const date = value instanceof Date ? value : new Date(value.replace(/(\.\d{3})\d+/, '$1'));
  if (Number.isNaN(date.getTime())) return null;
  return jstMonthFormatter.format(date).slice(0, 7);
}

export function getTargetPairs(siteProjects: SiteProjects[], sites: Site[]): TargetPair[] {
  const siteNames = new Map(sites.map((site) => [site.siteId, site.siteName]));
  return siteProjects.flatMap(({ siteId, projects }) =>
    projects
      .filter((project) => project.participationStatus === 'APPROVED' && project.hasSiteRelation)
      .map((project) => ({
        siteId,
        siteName: siteNames.get(siteId) ?? siteId,
        projectId: project.id,
        projectName: project.projectName,
      })),
  );
}

/** Latest dateRecorded (raw string) for each "siteId::projectId". */
export type LatestSubmissions = Map<string, string | null>;

export const pairKey = (siteId: string, projectId: string) => `${siteId}::${projectId}`;

export function latestDate(dates: string[]): string | null {
  return dates.reduce<string | null>((latest, date) => {
    const time = new Date(date).getTime();
    if (Number.isNaN(time)) return latest;
    return latest === null || time > new Date(latest).getTime() ? date : latest;
  }, null);
}

/** Sums empj per JST month of processing_date, oldest first, with how many sites contributed. */
export function toMonthlyEmissions(
  results: { pair: TargetPair; items: ReductionResultItem[] }[],
): MonthlyEmission[] {
  const byMonth = new Map<string, { empj: number; sites: Set<string> }>();
  results.forEach(({ pair, items }) =>
    items.forEach((item) => {
      const month = toJstMonth(item.processingDate);
      if (!month || item.empj === null) return;
      const entry = byMonth.get(month) ?? { empj: 0, sites: new Set<string>() };
      entry.empj += item.empj;
      entry.sites.add(pair.siteId);
      byMonth.set(month, entry);
    }),
  );
  return [...byMonth.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([month, { empj, sites }]) => ({ month, empj, siteCount: sites.size }));
}

export function buildMasterHome({
  sites,
  users,
  siteProjects,
  latestSubmissions,
  unreadCount,
  now = new Date(),
}: {
  sites: Site[];
  users: UserList[];
  siteProjects: SiteProjects[];
  latestSubmissions: LatestSubmissions;
  unreadCount: number;
  now?: Date;
}) {
  const currentMonth = toJstMonth(now);
  const targets = getTargetPairs(siteProjects, sites);
  const isSubmitted = (pair: TargetPair) =>
    toJstMonth(latestSubmissions.get(pairKey(pair.siteId, pair.projectId))) === currentMonth;

  const allProjects = siteProjects.flatMap(({ projects }) => projects);
  const uniqueById = (projects: SiteProject[]) => [
    ...new Map(projects.map((project) => [project.id, project])).values(),
  ];
  const approvedProjects = uniqueById(
    allProjects.filter((project) => project.participationStatus === 'APPROVED'),
  );
  const pendingProjects = uniqueById(
    allProjects.filter((project) => project.participationStatus === 'OPEN'),
  );
  const participatingSiteCount = siteProjects.filter(({ projects }) =>
    projects.some((project) => project.hasSiteRelation),
  ).length;

  const unsubmitted = targets.filter((pair) => !isSubmitted(pair));
  const usersWithoutSite = users.filter((user) => user.role !== 'master' && !user.siteId);

  const actions: ActionItem[] = [
    ...pendingProjects.map((project) => ({
      kind: 'pending' as const,
      key: `pending-${project.id}`,
      text: `${project.projectName} の入会申請が審査中です`,
    })),
    ...unsubmitted.map((pair) => ({
      kind: 'unsubmitted' as const,
      key: `unsubmitted-${pairKey(pair.siteId, pair.projectId)}`,
      text: `${pair.siteName}・${pair.projectName}：今月のモニタリングが未提出です`,
      siteId: pair.siteId,
    })),
    ...usersWithoutSite.map((user) => ({
      kind: 'noSite' as const,
      key: `no-site-${user.id}`,
      text: `${user.lastName ?? ''} ${user.firstName ?? ''} さんの拠点が未設定です`.trim(),
      userId: user.id,
    })),
    ...(unreadCount > 0
      ? [{ kind: 'unread' as const, key: 'unread', text: `未読のお知らせが${unreadCount}件あります` }]
      : []),
  ];

  const siteRows: SiteRow[] = sites.map((site) => {
    const projects = siteProjects.find((entry) => entry.siteId === site.siteId)?.projects ?? [];
    const siteTargets = targets.filter((pair) => pair.siteId === site.siteId);
    return {
      site,
      projectCount: projects.filter((project) => project.hasSiteRelation).length,
      userCount: users.filter((user) => user.siteId === site.siteId).length,
      lastSubmittedAt: latestDate(
        siteTargets.flatMap((pair) => latestSubmissions.get(pairKey(pair.siteId, pair.projectId)) ?? []),
      ),
      thisMonth:
        siteTargets.length === 0
          ? 'none'
          : siteTargets.every(isSubmitted)
            ? 'submitted'
            : 'unsubmitted',
    };
  });

  return {
    targets,
    siteCount: sites.length,
    participatingSiteCount,
    approvedProjectCount: approvedProjects.length,
    pendingProjectCount: pendingProjects.length,
    submittedCount: targets.length - unsubmitted.length,
    actions,
    siteRows,
  };
}
