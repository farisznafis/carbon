import type { ParticipationReviewStatus } from '@/api/admin';
import type { StatusBadgeTone } from '@/components/data-display/StatusBadge';

export const REVIEW_STATUSES: ParticipationReviewStatus[] = [
  'OPEN',
  'APPROVED',
  'REJECTED',
];

/**
 * Label, tone and copy for each membership status. Every place that shows a
 * membership status (list badge, 概要 chips, filter tabs) reads it from here.
 */
export const REVIEW_STATUS_CONTENT: Record<
  ParticipationReviewStatus,
  {
    label: string;
    tone: StatusBadgeTone;
    description: string;
    emptyMessage: string;
  }
> = {
  OPEN: {
    label: '承認待ち',
    tone: 'warning',
    description: '管理者の承認を待っている入会届を表示しています。',
    emptyMessage: '承認待ちの入会届はありません。',
  },
  APPROVED: {
    label: '承認済み',
    tone: 'success',
    description: '2名の管理者による承認が完了した入会届を表示しています。',
    emptyMessage: '承認済みの入会届はありません。',
  },
  REJECTED: {
    label: '却下',
    tone: 'danger',
    description: '管理者によって却下された入会届を表示しています。',
    emptyMessage: '却下された入会届はありません。',
  },
};

/** Resolves a status query value to a supported status, defaulting to 承認待ち. */
export function resolveReviewStatus(value: string | null): ParticipationReviewStatus {
  return REVIEW_STATUSES.includes(value as ParticipationReviewStatus)
    ? (value as ParticipationReviewStatus)
    : 'OPEN';
}
