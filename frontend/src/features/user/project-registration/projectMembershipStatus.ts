import type { SiteProject } from '@/api/site';

export type ProjectMembershipStatus = 'available' | 'pending' | 'joined';
export type MembershipStatusFilter = 'all' | ProjectMembershipStatus;
export type ProjectMembershipListItem = Readonly<{
  project: SiteProject;
  status: ProjectMembershipStatus;
}>;

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
