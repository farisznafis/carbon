import type { SiteProject } from '@/api/site';
import type { StatusBadgeTone } from '@/components/data-display/StatusBadge';

export type ProjectMembershipStatus = 'available' | 'pending' | 'joined';
export type MembershipStatusFilter = 'all' | ProjectMembershipStatus;
export type ProjectMembershipListItem = Readonly<{
  project: SiteProject;
  status: ProjectMembershipStatus;
}>;

/**
 * Label and StatusBadge tone per membership state. The status filter and the
 * project cards both read from here, so a state never changes colour between
 * them. 未入会 has no state yet, so it is neutral.
 */
export const MEMBERSHIP_STATUS_PRESENTATION: Record<
  ProjectMembershipStatus,
  { label: string; tone: StatusBadgeTone }
> = {
  available: { label: '未入会', tone: 'neutral' },
  pending: { label: '申請中', tone: 'warning' },
  joined: { label: '入会中', tone: 'success' },
};

export const MEMBERSHIP_STATUS_ORDER: Record<ProjectMembershipStatus, number> = {
  available: 0,
  pending: 1,
  joined: 2,
};

/** Resolves the application state using the API participation and relation fields. */
export function getMembershipStatus(project: SiteProject): ProjectMembershipStatus {
  if (project.participationStatus === 'OPEN') {
    return 'pending';
  }

  if (project.participationStatus === 'APPROVED') {
    return 'joined';
  }

  if (project.participationStatus === 'REJECTED') {
    return 'available';
  }

  if (project.isCompanyJoined || project.hasSiteRelation) {
    return 'joined';
  }

  return 'available';
}
