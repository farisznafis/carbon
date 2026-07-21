import type { CompanyStatus } from '@/api/admin';

export function displayCompanyDetailValue(
  value?: string | number | null,
): string {
  if (value === null || value === undefined || value === '') return '-';
  return String(value);
}

export function formatAdminCompanyDateTime(value?: string | null): string {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ja-JP', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatAdminCompanyAddress({
  postalCode,
  prefecture,
  municipality,
  blocknum,
  buildingName,
  address,
}: {
  postalCode?: string | null;
  prefecture?: string | null;
  municipality?: string | null;
  blocknum?: string | null;
  buildingName?: string | null;
  address?: string | null;
}): string {
  const separatedAddress = [
    postalCode ? `〒${postalCode}` : '',
    prefecture,
    municipality,
    blocknum,
    buildingName,
  ]
    .filter(Boolean)
    .join(' ')
    .trim();

  return separatedAddress || displayCompanyDetailValue(address);
}

const companyStatusLabels: Record<CompanyStatus, string> = {
  PENDING: '申請受付',
  REJECTED: '却下',
  ACTIVE: '承認済み',
  SUSPENDED: '利用停止',
};

const roleLabels: Record<string, string> = {
  admin: '管理者',
  master: 'マスター',
  user: 'ユーザ',
};

const subscriptionStatusLabels: Record<string, string> = {
  ACTIVE: '有効',
  INACTIVE: '無効',
  SUSPENDED: '利用停止',
  CANCELLED: '解約済み',
};

export function formatCompanyStatus(status: CompanyStatus): string {
  return companyStatusLabels[status] ?? status;
}

export function formatAdminRole(role?: string | null): string {
  if (!role) return '-';
  return roleLabels[role.toLowerCase()] ?? role;
}

export function formatSubscriptionStatus(status?: string | null): string {
  if (!status) return '-';
  return subscriptionStatusLabels[status] ?? status;
}