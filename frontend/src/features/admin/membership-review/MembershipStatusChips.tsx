import type { ParticipationReviewStatus } from '@/api/admin';
import { MembershipStatusBadge } from './MembershipStatusBadge';
import {
  REVIEW_STATUSES,
  REVIEW_STATUS_CONTENT,
} from './participationReviewStatusContent';

type MembershipStatusChipsProps = {
  counts: Record<ParticipationReviewStatus, number> | null;
  onSelect: (status: ParticipationReviewStatus) => void;
};

/** One clickable chip per membership status, in the same tones as the 入会届 list. */
export function MembershipStatusChips({
  counts,
  onSelect,
}: Readonly<MembershipStatusChipsProps>) {
  return (
    <ul className="flex flex-wrap justify-end gap-2">
      {REVIEW_STATUSES.map((status) => {
        const label = REVIEW_STATUS_CONTENT[status].label;
        const count = counts?.[status];

        return (
          <li key={status}>
            <button
              type="button"
              onClick={() => onSelect(status)}
              aria-label={`${label}の入会届${count ?? ''}件を表示`}
              className="relative rounded-full transition before:absolute before:inset-x-0 before:-inset-y-[10px] before:content-[''] hover:brightness-95 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <MembershipStatusBadge status={status} className="gap-1.5 py-0.5">
                {label}
                <span className="font-bold tabular-nums">{count ?? '-'}</span>
              </MembershipStatusBadge>
            </button>
          </li>
        );
      })}
    </ul>
  );
}
