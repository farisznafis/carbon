import type { ReactNode } from 'react';
import type { ParticipationReviewStatus } from '@/api/admin';
import { StatusBadge } from '@/components/data-display/StatusBadge';
import { REVIEW_STATUS_CONTENT } from './participationReviewStatusContent';

/** Shows a membership status in its shared tone; `children` can refine the label (e.g. 一次確認待ち). */
export function MembershipStatusBadge({
  status,
  children,
  className,
}: Readonly<{
  status: ParticipationReviewStatus;
  children?: ReactNode;
  className?: string;
}>) {
  const content = REVIEW_STATUS_CONTENT[status];

  return (
    <StatusBadge tone={content.tone} className={className}>
      {children ?? content.label}
    </StatusBadge>
  );
}
