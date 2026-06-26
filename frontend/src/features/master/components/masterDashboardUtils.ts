import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';

export const formatDashboardDateTime = (date: Date | null) => {
  if (!date) return '-';

  const pad = (value: number) => String(value).padStart(2, '0');

  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(
    date.getDate(),
  )} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
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

export const formatSiteAddress = (site: Site) => {
  const composedAddress = [
    site.prefectureName,
    site.cityName,
    site.blockName,
    site.addressDetail,
  ]
    .filter(Boolean)
    .join('');

  return site.address || composedAddress || '-';
};

export const formatPersonName = (
  lastName?: string | null,
  firstName?: string | null,
) => {
  const fullName = `${lastName ?? ''} ${firstName ?? ''}`.trim();
  return fullName || '-';
};
