import type {
  AdminProjectDetail,
  ParticipationReviewDetail,
} from '@/api/admin';

type ParticipationProjectIdentity = Pick<
  AdminProjectDetail,
  'id' | 'projectName'
>;

function normalizeIdentity(value: string | number | null | undefined) {
  return value == null ? '' : String(value).trim();
}

export function isParticipationReviewForProject(
  review: ParticipationReviewDetail,
  routeProjectId: string,
  project: ParticipationProjectIdentity,
) {
  const reviewProjectId = normalizeIdentity(review.projectId);
  const knownProjectIds = [
    normalizeIdentity(routeProjectId),
    normalizeIdentity(project.id),
  ].filter(Boolean);

  if (knownProjectIds.includes(reviewProjectId)) return true;

  const reviewProjectName = normalizeIdentity(review.projectName);
  const projectName = normalizeIdentity(project.projectName);

  return Boolean(projectName && reviewProjectName === projectName);
}
