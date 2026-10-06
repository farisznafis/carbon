import type { CompanyStatus } from '@/api/admin';
import type { AdminOperatorPermissionCode } from '@/api/admin/operators';
import type { StatusBadgeTone } from '@/components/data-display/StatusBadge';

export const COMPANY_STATUSES: CompanyStatus[] = [
  'PENDING',
  'REJECTED',
  'ACTIVE',
  'SUSPENDED',
];

/** Statuses that belong to the application review flow (not yet participating). */
export const COMPANY_APPLICATION_STATUSES: CompanyStatus[] = ['PENDING', 'REJECTED'];

/** Statuses of companies that were approved and participate in the program. */
export const COMPANY_PARTICIPATING_STATUSES: CompanyStatus[] = ['ACTIVE', 'SUSPENDED'];

/**
 * Label, tone, copy and required permission for each company status. Every
 * place that shows a company status (list badge, filter, detail header, Home)
 * reads it from here.
 */
export const COMPANY_STATUS_CONTENT: Record<
  CompanyStatus,
  {
    label: string;
    tone: StatusBadgeTone;
    description: string;
    emptyMessage: string;
    permission: AdminOperatorPermissionCode;
  }
> = {
  PENDING: {
    label: '申請受付',
    tone: 'warning',
    description: '参加申請を受け付けた企業です。内容を確認し、承認または却下を行ってください。',
    emptyMessage: '申請受付中の企業はありません。',
    permission: 'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
  },
  REJECTED: {
    label: '却下',
    tone: 'danger',
    description: '参加申請が却下された企業です。',
    emptyMessage: '却下された企業はありません。',
    permission: 'PARTICIPATING_COMPANY_APPLICATION_REVIEW',
  },
  ACTIVE: {
    label: '承認済み',
    tone: 'success',
    description: '承認済みの参加企業です。企業情報と運用状況を確認できます。',
    emptyMessage: '承認済みの企業はありません。',
    permission: 'PARTICIPATING_COMPANY_READ',
  },
  SUSPENDED: {
    label: '利用停止',
    tone: 'danger',
    description: 'アカウントが停止されている参加企業です。',
    emptyMessage: '利用停止中の企業はありません。',
    permission: 'PARTICIPATING_COMPANY_READ',
  },
};

export type CompanyStatusFilter = 'all' | CompanyStatus;

export const ALL_COMPANIES_CONTENT = {
  label: 'すべて',
  description: '参加申請中の企業と参加企業をまとめて表示しています。',
  emptyMessage: '企業はありません。',
} as const;

/** Resolves a status query value to one the viewer may see, defaulting to すべて. */
export function resolveCompanyStatusFilter(
  value: string | null,
  visibleStatuses: readonly CompanyStatus[],
): CompanyStatusFilter {
  return visibleStatuses.includes(value as CompanyStatus)
    ? (value as CompanyStatus)
    : 'all';
}
