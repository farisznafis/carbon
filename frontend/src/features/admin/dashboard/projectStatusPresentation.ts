import type { ProjectStatus } from '@/api/admin/projects';

export type ProjectStatusPresentation = {
  status: ProjectStatus;
  dotClassName: string;
};

export const PROJECT_STATUS_PRESENTATIONS: readonly ProjectStatusPresentation[] = [
  { status: '作成済み', dotClassName: 'bg-muted-foreground' },
  { status: '準備中', dotClassName: 'bg-info' },
  { status: '変更中', dotClassName: 'bg-primary' },
  { status: '妥当性確認処理', dotClassName: 'bg-primary-strong' },
  { status: '却下', dotClassName: 'bg-destructive' },
  { status: '実施中', dotClassName: 'bg-success' },
  { status: '検証中', dotClassName: 'bg-warning' },
  { status: 'クレジット申請中', dotClassName: 'bg-success-strong' },
  { status: '終了', dotClassName: 'bg-muted-foreground' },
] as const;

const PROJECT_STATUS_ALIASES: Readonly<Record<string, ProjectStatus>> = {
  CREATED: '作成済み',
  PREPARING: '準備中',
  CHANGING: '変更中',
  VALIDATING: '妥当性確認処理',
  REJECTED: '却下',
  ACTIVE: '実施中',
  RUNNING: '実施中',
  VERIFYING: '検証中',
  VERIFICATION: '検証中',
  CREDIT_APPLICATION: 'クレジット申請中',
  ENDED: '終了',
  INACTIVE: '終了',
};

/** Converts an API project status into the canonical Japanese status when known. */
export function normalizeProjectStatus(status: string) {
  const normalizedStatus = status.trim();
  const knownStatus = PROJECT_STATUS_PRESENTATIONS.find(
    (presentation) => presentation.status === normalizedStatus,
  );

  return (
    knownStatus?.status ??
    PROJECT_STATUS_ALIASES[normalizedStatus.toUpperCase()] ??
    normalizedStatus
  );
}

/** Formats a nullable project timestamp for compact dashboard display. */
export function formatProjectDateTime(value?: string | null) {
  if (!value) return '—';

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
