import type { ProjectStatus } from '@/api/admin/projects';
import type { StatusBadgeTone } from '@/components/data-display/StatusBadge';

/**
 * One tone per project status, shared by every admin surface (badges, table
 * selects, filters). Tones follow StatusBadge meaning:
 * - info: the project side is working on it (準備中, 変更中)
 * - warning: waiting on another party's review (妥当性確認処理, 検証中, クレジット申請中)
 * - success: running (実施中) · danger: rejected (却下) · neutral: no activity (作成済み, 終了)
 */
export type ProjectStatusPresentation = {
  status: ProjectStatus;
  tone: StatusBadgeTone;
};

export const PROJECT_STATUS_PRESENTATIONS: readonly ProjectStatusPresentation[] = [
  { status: '作成済み', tone: 'neutral' },
  { status: '準備中', tone: 'info' },
  { status: '変更中', tone: 'info' },
  { status: '妥当性確認処理', tone: 'warning' },
  { status: '却下', tone: 'danger' },
  { status: '実施中', tone: 'success' },
  { status: '検証中', tone: 'warning' },
  { status: 'クレジット申請中', tone: 'warning' },
  { status: '終了', tone: 'neutral' },
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

/** Tone for any API status value (Japanese or English alias); unknown values stay neutral. */
export function getProjectStatusTone(status: string): StatusBadgeTone {
  const normalized = normalizeProjectStatus(String(status ?? ''));
  return PROJECT_STATUS_PRESENTATIONS.find((item) => item.status === normalized)?.tone ?? 'neutral';
}
