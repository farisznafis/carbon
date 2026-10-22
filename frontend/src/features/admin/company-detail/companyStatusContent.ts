import type { CompanyStatus } from '@/api/admin';
import type { AdminOperatorPermissionCode } from '@/api/admin/operators';
import type { StatusBadgeTone } from '@/components/data-display/StatusBadge';

export const COMPANY_STATUSES: CompanyStatus[] = [
  'PENDING',
  'APPROVED_PENDING_SETUP',
  'REJECTED',
  'ACTIVE',
  'SUSPENDED',
];

/** Statuses that belong to the application review flow (not yet participating). */
// 設定待ち belongs here: it is the last step of onboarding, and resending the link needs the review permission.
export const COMPANY_APPLICATION_STATUSES: CompanyStatus[] = ['PENDING', 'APPROVED_PENDING_SETUP', 'REJECTED'];

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
  APPROVED_PENDING_SETUP: {
    label: 'アカウント設定待ち',
    tone: 'warning',
    description: '承認済みで、マスターアカウントの設定を待っている企業です。設定リンクの有効期限が切れた場合は再送してください。',
    emptyMessage: 'アカウント設定待ちの企業はありません。',
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

/** 企業一覧の区分。ホームの「参加企業」「企業申請」カードの件数と同じステータスの組み合わせ。 */
export type CompanyStatusGroup = 'participating' | 'applications';

export const COMPANY_STATUS_GROUPS: CompanyStatusGroup[] = ['participating', 'applications'];

export const COMPANY_STATUS_GROUP_CONTENT: Record<
  CompanyStatusGroup,
  {
    label: string;
    statuses: CompanyStatus[];
    description: string;
    emptyMessage: string;
  }
> = {
  participating: {
    label: '参加企業',
    statuses: COMPANY_PARTICIPATING_STATUSES,
    description: '承認済み・利用停止中の参加企業を表示しています。',
    emptyMessage: '参加企業はありません。',
  },
  applications: {
    label: '企業申請',
    statuses: COMPANY_APPLICATION_STATUSES,
    description: '申請受付・アカウント設定待ち・却下の企業申請を表示しています。',
    emptyMessage: '企業申請はありません。',
  },
};

export type CompanyStatusFilter = 'all' | CompanyStatusGroup | CompanyStatus;

export const ALL_COMPANIES_CONTENT = {
  label: 'すべて',
  description: '参加申請中の企業と参加企業をまとめて表示しています。',
  emptyMessage: '企業はありません。',
} as const;

function isCompanyStatusGroup(value: string | null): value is CompanyStatusGroup {
  return COMPANY_STATUS_GROUPS.includes(value as CompanyStatusGroup);
}

/** フィルター値が属する区分。ステータス単体の場合はその所属区分、すべての場合は 'all'。 */
export function getCompanyStatusGroup(filter: CompanyStatusFilter): CompanyStatusGroup | 'all' {
  if (filter === 'all' || isCompanyStatusGroup(filter)) return filter;
  return COMPANY_APPLICATION_STATUSES.includes(filter) ? 'applications' : 'participating';
}

/** フィルター値に対応し、かつ閲覧者が参照できるステータスの一覧（表示順）。 */
export function getCompanyFilterStatuses(
  filter: CompanyStatusFilter,
  visibleStatuses: readonly CompanyStatus[],
): CompanyStatus[] {
  if (filter === 'all') return [...visibleStatuses];
  if (isCompanyStatusGroup(filter)) {
    return visibleStatuses.filter((status) =>
      COMPANY_STATUS_GROUP_CONTENT[filter].statuses.includes(status),
    );
  }
  return [filter];
}

/** 閲覧者が両方の区分を参照できる場合のみ、区分での絞り込みに意味がある。 */
export function getVisibleCompanyStatusGroups(
  visibleStatuses: readonly CompanyStatus[],
): CompanyStatusGroup[] {
  const groups = COMPANY_STATUS_GROUPS.filter(
    (group) => getCompanyFilterStatuses(group, visibleStatuses).length > 0,
  );
  return groups.length > 1 ? groups : [];
}

/** 一覧の説明文・空表示の文言。 */
export function getCompanyFilterContent(filter: CompanyStatusFilter) {
  if (filter === 'all') return ALL_COMPANIES_CONTENT;
  if (isCompanyStatusGroup(filter)) return COMPANY_STATUS_GROUP_CONTENT[filter];
  return COMPANY_STATUS_CONTENT[filter];
}

/** Resolves a status query value to one the viewer may see, defaulting to すべて. */
export function resolveCompanyStatusFilter(
  value: string | null,
  visibleStatuses: readonly CompanyStatus[],
): CompanyStatusFilter {
  if (isCompanyStatusGroup(value)) {
    // 片方の区分しか参照できない閲覧者には、区分の絞り込みは「すべて」と同じ
    return getVisibleCompanyStatusGroups(visibleStatuses).includes(value) ? value : 'all';
  }
  return visibleStatuses.includes(value as CompanyStatus)
    ? (value as CompanyStatus)
    : 'all';
}
