import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';

/**
 * Cards with a single destination (拠点, 社員) are clickable as a whole: the
 * card's own action button is stretched over it with an ::after overlay, so
 * there is still exactly one interactive element per card.
 */
export const clickableCardClassName =
  'relative flex flex-col overflow-hidden rounded-lg border border-border bg-card transition-colors hover:border-primary/40 hover:bg-secondary/30 has-[button:focus-visible]:ring-2 has-[button:focus-visible]:ring-ring md:flex-row';

export const stretchedActionClassName =
  'after:absolute after:inset-0 after:content-[""]';

export const formatDashboardDateTime = (date: Date | null) => {
  if (!date) return '-';

  const pad = (value: number) => String(value).padStart(2, '0');

  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`;
};

export const sortMasterUsersFirst = (users: UserList[]) => {
  return [...users].sort((a, b) => {
    const aIsMaster = a.role === 'master';
    const bIsMaster = b.role === 'master';

    if (aIsMaster && !bIsMaster) return -1;
    if (!aIsMaster && bIsMaster) return 1;
    return 0;
  });
};

export const sortSitesById = (sites: Site[]) => {
  return [...sites].sort((a, b) => {
    return String(a.siteId ?? '').localeCompare(String(b.siteId ?? ''), 'ja');
  });
};

export const formatSiteAddress = (site: Site) => {
  const composedAddress = [
    site.prefectureName,
    site.cityName,
    site.blockName,
    site.buildingName,
  ]
    .filter(Boolean)
    .join('');

  const postal = site.postalCode ? `〒${site.postalCode}` : '';
  const address = composedAddress || site.address || '';

  return [postal, address].filter(Boolean).join(' ') || '-';
};

export const formatPersonName = (
  lastName?: string | null,
  firstName?: string | null,
) => {
  const fullName = `${lastName ?? ''} ${firstName ?? ''}`.trim();
  return fullName || '-';
};
